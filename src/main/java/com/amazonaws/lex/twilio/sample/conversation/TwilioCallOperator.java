package com.amazonaws.lex.twilio.sample.conversation;

import com.amazonaws.lex.twilio.sample.server.CallIdentifier;
import com.amazonaws.lex.twilio.sample.server.media.CompressInputStream;
import com.amazonaws.lex.twilio.sample.server.messages.ClearMessage;
import com.amazonaws.lex.twilio.sample.server.messages.MarkMessage;
import com.amazonaws.lex.twilio.sample.server.messages.MediaMessage;
import com.amazonaws.lex.twilio.sample.server.messages.Message;
import com.amazonaws.lex.twilio.sample.streaming.AudioResponse;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;

import javax.websocket.EncodeException;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class TwilioCallOperator {
    private static final Logger LOG = Logger.getLogger(TwilioCallOperator.class);

    public static final int MAX_BYTES_TO_READ = 10000; // better to read more
    private final CallIdentifier callIdentifier;
    private final Session session;
    private final AtomicBoolean interruptSendingDataToTwilio;
    private Optional<String> currentPlaybackLabel;

    public TwilioCallOperator(CallIdentifier callIdentifier, Session session) {
        this.callIdentifier = callIdentifier;
        this.session = session;
        this.interruptSendingDataToTwilio = new AtomicBoolean(false);
        this.currentPlaybackLabel = Optional.empty();
    }

    public Optional<String> getCurrentPlaybackLabel() {
        return currentPlaybackLabel;
    }

    // send back media events as per https://www.twilio.com/docs/voice/twiml/stream#message-media-to-twilio
    public void playback(AudioResponse audioResponse) {

        try (CompressInputStream responseStream = new CompressInputStream(audioResponse, false)) {
            byte[] bytes = new byte[MAX_BYTES_TO_READ];

            int numOfBytesRead = responseStream.read(bytes);
            //while not end of stream, or not playback paused
            while (numOfBytesRead != -1 && !interruptSendingDataToTwilio.get()) {
                byte[] copy = Arrays.copyOf(bytes, numOfBytesRead);
                MediaMessage mediaMessage = new MediaMessage(copy, callIdentifier.getStreamSid());
                writeToStream(mediaMessage.getJsonObject(), false);
                bytes = new byte[MAX_BYTES_TO_READ];
                numOfBytesRead = responseStream.read(bytes);
            }
            //if it was not interrupted, it means it has reached end of stream.
            if (!interruptSendingDataToTwilio.get()) {
                // mark the end of stream and when we get back the same mark, we inform bot that
                // playback is complete.

                LOG.info("audio stream has ended, marking  a message ");
                currentPlaybackLabel = Optional.of(UUID.randomUUID().toString());
                writeToStream(new MarkMessage(callIdentifier.getStreamSid(), currentPlaybackLabel.get()).getJsonObject(), true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //send clear message as per https://www.twilio.com/docs/voice/twiml/stream#message-clear-to-twilio
    // by the time pausePlayback comes, all data might have already been sent to the Twilio server
    public void pausePlayback() {
        //update call
        interruptSendingDataToTwilio.set(true);

        LOG.info("user seems to have interrupted playback, requesting twilio to stop playback with clear message");

        ClearMessage clearMessage = new ClearMessage(callIdentifier.getStreamSid());

        currentPlaybackLabel = Optional.empty();

        writeToStream(clearMessage.getJsonObject(), true);

        interruptSendingDataToTwilio.set(false);
    }

    public void hangUp() {
    }


    private void writeToStream(JsonObject message, boolean log) {
        try {
            if (log) {
                LOG.info("Sending message to Twilio:" + message);
            }
            session.getBasicRemote().sendObject(new Message(message.toString()));
        } catch (IOException | EncodeException e) {
            LOG.error(e);
        }
    }
}
