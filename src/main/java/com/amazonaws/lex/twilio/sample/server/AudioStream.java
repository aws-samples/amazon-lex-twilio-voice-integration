package com.amazonaws.lex.twilio.sample.server;


import com.amazonaws.lex.twilio.sample.conversation.TwilioCallOperator;
import com.amazonaws.lex.twilio.sample.server.messages.MarkMessage;
import com.amazonaws.lex.twilio.sample.server.messages.Message;
import com.amazonaws.lex.twilio.sample.server.media.DecompressInputStream;
import com.amazonaws.lex.twilio.sample.server.messages.MediaMessage;
import com.amazonaws.lex.twilio.sample.server.messages.MessageDecoder;
import com.amazonaws.lex.twilio.sample.server.messages.MessageEncoder;
import com.amazonaws.lex.twilio.sample.server.messages.MessageType;
import com.amazonaws.lex.twilio.sample.server.messages.StartMessage;
import com.amazonaws.lex.twilio.sample.server.messages.StopMessage;
import com.amazonaws.lex.twilio.sample.conversation.BotConversation;
import com.amazonaws.lex.twilio.sample.streaming.LexBidirectionalStreamingClient;
import com.google.common.primitives.Bytes;
import org.apache.log4j.Logger;


import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

@ServerEndpoint(
        value = "/audiostream",
        decoders = MessageDecoder.class,
        encoders = MessageEncoder.class)
public class AudioStream {

    private static final Logger LOG = Logger.getLogger(AudioStream.class);
    private final Map<String, byte[]> rawBytes;

    private Session session;
    private BotConversation botConversation;
    private TwilioCallOperator twilioCallOperator;

    public AudioStream() {
        this.rawBytes = new HashMap<>();
    }

    /**
     * This is called when call gets connected. Return a valid Twilio response to ask it to
     * start streaming data to a another endpoint.
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;

        LOG.info("onOpen triggered by Twilio");
    }

    @OnMessage
    public void onMessage(Message message) {
        //LOG.info("message ..." + message);
        if (message.eventType().equals(MessageType.CONNECTED)) {
            // first message, does not contain anything useful
            // apart from some meta data.
        } else if (message.eventType().equals(MessageType.START)) {
            //going to start getting media, this message contains
            // stream id, account id, call id. remember to update them
            // later
            StartMessage startMessage = message.asStartMessage();
            LOG.info("got a start message from twilio:" + startMessage);

            CallIdentifier callIdentifier = startMessage.getCallIdentifier();
            this.twilioCallOperator = new TwilioCallOperator(callIdentifier, session);
            try {
                this.botConversation = new LexBidirectionalStreamingClient().startConversation(twilioCallOperator);
            } catch (URISyntaxException e) {
                LOG.error(e);
            }
        } else if (message.eventType().equals(MessageType.MEDIA)) {
            // contains audio data, decode for inbound audio
            // and send it to bot
            MediaMessage mediaMessage = message.asMediaMessage();

            byte[] uLawEncodedByte = mediaMessage.getDecodedPayload();
            byte[] uncompressedBytes = DecompressInputStream.decompressULawBytes(uLawEncodedByte);
            byte[] copiedBytes = Arrays.copyOf(uncompressedBytes, uncompressedBytes.length);
            //might need to split into smaller events of max size 320, if server throws an error.
            this.botConversation.writeUserInputAudio(ByteBuffer.wrap(uncompressedBytes));

            // uncomment this to keep adding incoming bytes to memory as well.
            // this is useful to captures raw audio that Twilio sends to this application
            // see counterpart method (persistBytesToDisk)

            //persistBytesInMemory(mediaMessage.getStreamSid(), copiedBytes);
        } else if (message.eventType().equals(MessageType.STOP)) {
            StopMessage stopMessage = message.asStopMessage();
            LOG.info("got a stop message from twilio:" + stopMessage);

            this.botConversation.stopConversation();

            //persistBytesToDisk(stopMessage.getCallIdentifier().getStreamSid());

        } else if (message.eventType().equals(MessageType.MARK)) {
            MarkMessage markMessage = message.asMarkMessage();
            LOG.info("got a mark message from twilio:" + markMessage);

            if (this.twilioCallOperator.getCurrentPlaybackLabel().isPresent() && this.twilioCallOperator.getCurrentPlaybackLabel().get().equals(markMessage.getMarkName())) {
                botConversation.informPlaybackFinished();
            }
        }
    }

    private void persistBytesInMemory(String streamSid, byte[] bytes) {
        rawBytes.merge(streamSid, bytes, Bytes::concat);
    }

    /**
     * This method writes the raw incoming bytes from Twilio to a local disk. You can
     * use this method to listen the raw audio file in any audio player that can play
     * .wav files.
     */
    private void persistBytesToDisk(String streamSid) {

        try {
            Path tempFile = Files.createTempFile(streamSid, ".wav");
            AudioFormat format = new AudioFormat(8000, 16, 1, true, false);
            AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(
                    rawBytes.get(streamSid)), format, rawBytes.get(streamSid).length), AudioFileFormat.Type.WAVE, tempFile.toFile());

            LOG.info("persisted data @ " + tempFile.toAbsolutePath());
        } catch (IOException e) {
            LOG.error("IOException when writing data to disk", e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        LOG.info("onClose triggered");
        this.session = session;
    }
}
