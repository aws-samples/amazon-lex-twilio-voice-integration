package com.amazonaws.lex.twilio.sample.streaming;

import com.amazonaws.lex.twilio.sample.conversation.BotConversation;
import com.amazonaws.lex.twilio.sample.conversation.TwilioCallOperator;
import org.apache.log4j.Logger;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.lexruntimev2.model.AudioResponseEvent;
import software.amazon.awssdk.services.lexruntimev2.model.DialogActionType;
import software.amazon.awssdk.services.lexruntimev2.model.IntentResultEvent;
import software.amazon.awssdk.services.lexruntimev2.model.PlaybackInterruptionEvent;
import software.amazon.awssdk.services.lexruntimev2.model.StartConversationResponse;
import software.amazon.awssdk.services.lexruntimev2.model.StartConversationResponseEventStream;
import software.amazon.awssdk.services.lexruntimev2.model.StartConversationResponseHandler;
import software.amazon.awssdk.services.lexruntimev2.model.TextResponseEvent;
import software.amazon.awssdk.services.lexruntimev2.model.TranscriptEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;

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

/**
 * This class is responsible for processing events sent from server. Because server will send multiple audio events,
 * this concatenates the audio events and uses a publicly available Java audio player to play out the message to
 * end user.
 */
public class BotResponseHandler implements StartConversationResponseHandler {

    private static final Logger LOG = Logger.getLogger(BotResponseHandler.class);

    private final BotConversation botConversation;
    private final TwilioCallOperator twilioCallOperator;
    private boolean isDialogStateClosed;
    private AudioResponse audioResponse;




    public BotResponseHandler(BotConversation botConversation, TwilioCallOperator twilioCallOperator) {
        this.twilioCallOperator = twilioCallOperator;
        this.botConversation = botConversation;
        this.isDialogStateClosed = false; // at start, dialog state is open
    }

    @Override
    public void responseReceived(StartConversationResponse startConversationResponse) {
        LOG.info("successfully established the connection with server. request id:" + startConversationResponse.responseMetadata().requestId()); // would have 2XX, request id.
    }

    @Override
    public void onEventStream(SdkPublisher<StartConversationResponseEventStream> sdkPublisher) {

        sdkPublisher.subscribe(event -> {
            if (event instanceof PlaybackInterruptionEvent) {
                handle((PlaybackInterruptionEvent) event);
            } else if (event instanceof TranscriptEvent) {
                handle((TranscriptEvent) event);
            } else if (event instanceof IntentResultEvent) {
                handle((IntentResultEvent) event);
            } else if (event instanceof TextResponseEvent) {
                handle((TextResponseEvent) event);
            } else if (event instanceof AudioResponseEvent) {
                handle((AudioResponseEvent) event);
            } else{
                LOG.info("Getting an unknown event ..."+event);
            }
        });
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        LOG.error(throwable);
        System.err.println("got an exception:" + throwable);
    }

    @Override
    public void complete() {
        LOG.info("Publisher sent on complete. No more events will be sent from server.");
    }

    private void handle(PlaybackInterruptionEvent event) {
        LOG.info("Got a PlaybackInterruptionEvent: " + event);

        twilioCallOperator.pausePlayback();

        try {
            if (audioResponse != null) {
                audioResponse.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        LOG.info("Done with a  PlaybackInterruptionEvent: " + event);
    }

    private void handle(TranscriptEvent event) {
        LOG.info("Got a TranscriptEvent: " + event);
    }


    private void handle(IntentResultEvent event) {
        LOG.info("Got an IntentResultEvent: " + event);
        isDialogStateClosed = DialogActionType.CLOSE.equals(event.sessionState().dialogAction().type());

        // if dialog state is closed, stop sending events to lex server
        // after the last message is played back, ask twilio call operator to hang up
        if (isDialogStateClosed) {
            isDialogStateClosed = true;
        }
    }

    private void handle(TextResponseEvent event) {
        LOG.info("Got an TextResponseEvent: " + event);
        event.messages().forEach(message -> {
            LOG.info("Message content type:" + message.contentType());
            LOG.info("Message content:" + message.content());
        });

        // if bot does not have a message, and this dialog was closed, we should hang up because
        // we will never send a message to play to twilio and subsequently, never get back a
        // mark message from twilio that playback for last message has finished. therefore,
        // short circuit the call here and just hangup Twilio call.
        if (isDialogStateClosed && !event.hasMessages()) {
            LOG.info("dialog is closed, and there is no message to playback. hanging up the twilio call.");
            twilioCallOperator.hangUp(false);
        }
    }

    private void handle(AudioResponseEvent event) {//synthesize speech
        // LOG.info("Got a AudioResponseEvent: " + event);
        //if (audioResponse == null && event.audioChunk() != null) {
        if (audioResponse == null ) {

            LOG.info("got a non empty audio response. starting a audio response collector in a separate thread");
	    LOG.info("Audiochunk is non-empty? : " + (event.audioChunk() != null));
	    if (event.audioChunk() == null) {
		CompletableFuture.runAsync(() -> twilioCallOperator.writeEmptyStream()).whenComplete((result, error) -> {
			if (error != null) {
				error.printStackTrace();
			}
	    	});
	    }
            audioResponse = new AudioResponse();
            CompletableFuture.runAsync(() -> twilioCallOperator.playback(audioResponse)).whenComplete((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                }
            });
        }

        if (event.audioChunk() != null) {
            audioResponse.write(event.audioChunk().asByteArray());
        } else {
            // no audio bytes means audio prompt has ended.
            closeAudioStream();
            //if dialog is closed now, we can also end the conversation, and send a disconnect to server.
            if(isDialogStateClosed){
                botConversation.stopConversation();
            }
        }
    }

    private void closeAudioStream(){
        try {
            if (audioResponse != null) {
                LOG.info("Closing writing to in memory audio response....");
                audioResponse.close();
            }
            audioResponse = null;  // prepare  for next audio prompt.
        } catch (IOException e) {
            throw new UncheckedIOException("got an exception when closing the audio response", e);
        }
    }

}
