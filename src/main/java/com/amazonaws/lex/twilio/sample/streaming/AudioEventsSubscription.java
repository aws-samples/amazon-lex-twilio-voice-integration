package com.amazonaws.lex.twilio.sample.streaming;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import com.google.gson.JsonObject;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lexruntimev2.model.AudioInputEvent;
import software.amazon.awssdk.services.lexruntimev2.model.ConfigurationEvent;
import software.amazon.awssdk.services.lexruntimev2.model.DisconnectionEvent;
import software.amazon.awssdk.services.lexruntimev2.model.PlaybackCompletionEvent;
import software.amazon.awssdk.services.lexruntimev2.model.StartConversationRequestEventStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

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
public class AudioEventsSubscription implements Subscription {
    private static final String AUDIO_CONTENT_TYPE = "audio/lpcm; sample-rate=8000; sample-size-bits=16; channel-count=1; is-big-endian=false";
    private static final String RESPONSE_TYPE = "audio/pcm; sample-rate=8000";
    private static final AtomicLong eventIdGenerator = new AtomicLong(0);

    private Subscriber<? super StartConversationRequestEventStream> subscriber;
    private final EventWriter eventWriter;
    private CompletableFuture<?> eventWriterFuture;

    public AudioEventsSubscription() {
        this.eventWriter = new EventWriter();
        configureConversation();
    }

    public void setSubscriber(Subscriber<? super StartConversationRequestEventStream> subscriber) {
        this.subscriber = subscriber;
        this.eventWriter.setSubscriber(subscriber);
    }

    /**
     * this method will always be called after setSubsriber method.
     */
    @Override
    public void request(long demand) {
        // start a thread to write events, if it has not been started so far.
        if (eventWriterFuture == null) {
            eventWriterFuture = CompletableFuture.runAsync(eventWriter).whenComplete((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                }
            });
        }
        eventWriter.addDemand(demand);
    }

    @Override
    public void cancel() {
        subscriber.onError(new RuntimeException("stream was cancelled"));
    }

    public void configureConversation() {
        String eventId = "ConfigurationEvent-" + eventIdGenerator.incrementAndGet();

        ConfigurationEvent configurationEvent = StartConversationRequestEventStream
                .configurationEventBuilder()
                .eventId(eventId)
                .clientTimestampMillis(System.currentTimeMillis())
                .responseContentType(RESPONSE_TYPE)
                .build();

        eventWriter.writeConfigurationEvent(configurationEvent);
        System.out.println("sending a ConfigurationEvent to server:" + configurationEvent);
    }


    public void disconnect() {

        String eventId = "DisconnectionEvent-" + eventIdGenerator.incrementAndGet();

        DisconnectionEvent disconnectionEvent = StartConversationRequestEventStream
                .disconnectionEventBuilder()
                .eventId(eventId)
                .clientTimestampMillis(System.currentTimeMillis())
                .build();

        eventWriter.writeDisconnectEvent(disconnectionEvent);

        System.out.println("sending a DisconnectionEvent to server:" + disconnectionEvent);

        eventWriter.stop();
    }

    //notify subscriber that we are done.
    public void stop() {
        disconnect();
        subscriber.onComplete();
    }

    public void playbackFinished() {
        String eventId = "PlaybackCompletion-" + eventIdGenerator.incrementAndGet();

        PlaybackCompletionEvent playbackCompletionEvent = StartConversationRequestEventStream
                .playbackCompletionEventBuilder()
                .eventId(eventId)
                .clientTimestampMillis(System.currentTimeMillis())
                .build();

        eventWriter.writePlaybackFinishedEvent(playbackCompletionEvent);

        System.out.println("sending a PlaybackCompletionEvent to server:" + playbackCompletionEvent);
    }

    public void writeAudioEvent(ByteBuffer byteBuffer) {
        String eventId = "AudioInputEvent-" + eventIdGenerator.incrementAndGet();

        AudioInputEvent audioInputEvent = StartConversationRequestEventStream
                .audioInputEventBuilder()
                .eventId(eventId)
                .clientTimestampMillis(System.currentTimeMillis())
                .audioChunk(SdkBytes.fromByteBuffer(byteBuffer))
                .contentType(AUDIO_CONTENT_TYPE)
                .build();

        eventWriter.writeAudioInputEvent(audioInputEvent);
    }

    private static class EventWriter implements Runnable {
        private final BlockingQueue<StartConversationRequestEventStream> eventQueue;
        private final AtomicLong demand;
        private Subscriber<? super StartConversationRequestEventStream> subscriber;
        private boolean stop;

        public EventWriter() {
            this.eventQueue = new LinkedBlockingQueue<>();
            this.demand = new AtomicLong(0);
        }

        public void setSubscriber(Subscriber<? super StartConversationRequestEventStream> subscriber) {
            this.subscriber = subscriber;
        }

        public void writeConfigurationEvent(ConfigurationEvent configurationEvent) {
            eventQueue.add(configurationEvent);
        }

        public void writeDisconnectEvent(DisconnectionEvent disconnectionEvent) {
            eventQueue.add(disconnectionEvent);
        }

        public void writePlaybackFinishedEvent(PlaybackCompletionEvent playbackCompletionEvent) {
            eventQueue.add(playbackCompletionEvent);
        }

        void addDemand(long l) {
            this.demand.addAndGet(l);
        }

        @Override
        public void run() {
            try {

                while (true) {
                    if (stop) {
                        break;
                    }

                    long currentDemand = demand.get();

                    if (currentDemand > 0) {
                        // try to read from queue of events.
                        // if nothing in queue at this point, read as many audio events directly from audio stream
                        for (long i = 0; i < currentDemand; i++) {

                            if (eventQueue.peek() != null) {
                                subscriber.onNext(eventQueue.take());
                                demand.decrementAndGet();
                            }

                        }
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("interrupted when reading data to be sent to server");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void stop() {
            stop = true;
        }

        public void writeAudioInputEvent(AudioInputEvent audioInputEvent) {
            eventQueue.add(audioInputEvent);
        }
    }


}
