package com.amazonaws.lex.twilio.sample.streaming;

import com.amazonaws.lex.twilio.sample.conversation.BotConversation;
import com.amazonaws.lex.twilio.sample.conversation.TwilioCallOperator;
import org.apache.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lexruntimev2.LexRuntimeV2AsyncClient;
import software.amazon.awssdk.services.lexruntimev2.model.ConversationMode;
import software.amazon.awssdk.services.lexruntimev2.model.StartConversationRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.UUID;
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
 * This is a starting point to use Lex streaming API. For this example, you will need to use AWS SDK for Lex V2
 * (async client). Furthermore, you will need to be familiar with Reactive streams programming model.
 * See https://github.com/reactive-streams/reactive-streams-jvm
 * <p>
 * The sample application allows you to interact with a Lex bot using the streaming API. The application uses Audio
 * conversation mode and gets back audio responses.
 * <p>
 * There are three important components in this sample code.
 * <p>
 * 1. Configuring details about conversation. These details include which bot to converse with, conversation mode etc.
 * 2. An events publisher that passed the audio events to server after connection is established. The code listens
 * to the mic on your machine to send data to Lex.
 * 3. A response handler, which handles bot responses and plays back the audio to you.
 */
public class LexBidirectionalStreamingClient {

    private static final Logger LOG = Logger.getLogger(LexBidirectionalStreamingClient.class);

    private static final String botId;
    private static final String botAliasId;
    private static final String localeId;
    private static final String accessKey;
    private static final String secretKey;
    private static final Region region;
    private static final LexRuntimeV2AsyncClient lexRuntimeServiceClient;
    private final String sessionId;

    static {
        Properties properties = readProperties();

        botId = properties.getProperty("botId");
        botAliasId = properties.getProperty("botAliasId");
        localeId = properties.getProperty("localeId");
        accessKey = properties.getProperty("accessKey");
        secretKey = properties.getProperty("secretKey");
        region = Region.of(properties.getProperty("region"));

        AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider
                .create(AwsBasicCredentials.create(accessKey, secretKey));

        lexRuntimeServiceClient = LexRuntimeV2AsyncClient.builder()
                .region(region)
                .credentialsProvider(awsCredentialsProvider)
                //.endpointOverride(new URI("https://runtime-v2-lex.us-west-2.amazonaws.com"))
                .build();
    }

    private static Properties readProperties() {
        try (InputStream input = LexBidirectionalStreamingClient.class.getClassLoader().getResourceAsStream("bot-configuration.properties")) {

            Properties prop = new Properties();
            // load a properties file
            prop.load(input);

            return prop;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public LexBidirectionalStreamingClient() {
        this.sessionId = UUID.randomUUID().toString();
    }


    public BotConversation startConversation(TwilioCallOperator twilioCallOperator) throws URISyntaxException {

        // create a new SDK client. you will need to use an async client.
        LOG.info("step 1: creating a new Lex SDK client");
        // this is created once at class creation time in static block.

        // configure bot, alias and locale with which to have a conversation.
        LOG.info("step 2: configuring bot details");
        StartConversationRequest.Builder startConversationRequestBuilder = StartConversationRequest.builder()
                .botId(botId)
                .botAliasId(botAliasId)
                .localeId(localeId);

        // configure the conversation mode with bot (defaults to audio)
        LOG.info("step 3: choosing conversation mode");
        startConversationRequestBuilder = startConversationRequestBuilder.conversationMode(ConversationMode.AUDIO);

        // assign a unique identifier for the conversation
        LOG.info("step 4: choosing a unique conversation identifier");
        startConversationRequestBuilder = startConversationRequestBuilder.sessionId(sessionId);

        // build the initial request
        StartConversationRequest startConversationRequest = startConversationRequestBuilder.build();

        // create a stream of audio data to server. stream will start after connection is established with server.
        EventsPublisher eventsPublisher = new EventsPublisher();

        BotConversation botConversation = new BotConversation(eventsPublisher);

        // create a class to handle responses from bot. after server processes streamed user data, it will respond back
        // on another stream.
        BotResponseHandler botResponseHandler = new BotResponseHandler(botConversation, twilioCallOperator);

        // start a connection and pass in the a publisher that will stream audio and process bot responses.
        LOG.info("step 5: starting the conversation ...");
        CompletableFuture<Void> conversation = lexRuntimeServiceClient.startConversation(
                startConversationRequest,
                eventsPublisher,
                botResponseHandler);

        // wait till conversation finishes. conversation will finish if dialog state reaches "Closed" state - at which point
        // client should gracefully stop the connection,or some exception occurs during the conversation - at which point
        // client should send a disconnection event.
        conversation.whenComplete((result, exception) -> {
            if (exception != null) {
                eventsPublisher.stop();
                twilioCallOperator.hangUp(true);
            }
        });

        return botConversation;

    }
}
