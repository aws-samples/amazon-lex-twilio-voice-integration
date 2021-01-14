package com.amazonaws.lex.twilio.sample.streaming;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.services.lexruntimev2.model.StartConversationRequestEventStream;

import java.nio.ByteBuffer;
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
 * This is the publisher of various events to server. When a connection is established, subscriber (server) calls
 * subscribe() method. At this point, the events publisher starts sending events. Server requests more data by invoking
 * "request" method on the subscription.
 */
public class EventsPublisher implements Publisher<StartConversationRequestEventStream> {

    private AudioEventsSubscription audioEventsSubscription = new AudioEventsSubscription();

    @Override
    public void subscribe(Subscriber<? super StartConversationRequestEventStream> subscriber) {
        audioEventsSubscription.setSubscriber(subscriber);
        subscriber.onSubscribe(audioEventsSubscription);
    }

    public void stop() {
        audioEventsSubscription.stop();
    }

    public void playbackFinished() {
        audioEventsSubscription.playbackFinished();
    }

    public void writeUserInputAudio(ByteBuffer byteBuffer) {
        audioEventsSubscription.writeAudioEvent(byteBuffer);
    }
}
