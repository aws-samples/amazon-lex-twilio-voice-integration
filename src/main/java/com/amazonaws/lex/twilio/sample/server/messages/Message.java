package com.amazonaws.lex.twilio.sample.server.messages;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.StringJoiner;

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
 * See details at https://www.twilio.com/docs/voice/twiml/stream
 */
public class Message {

    private static final Gson GSON = new Gson();
    private final JsonObject jsonObject;

    public Message(String json) {
        this.jsonObject = GSON.fromJson(json, JsonObject.class);
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public MessageType eventType() {
        return MessageType.fromString(jsonObject.get("event").getAsString());
    }

    public ConnectedMessage asConnectedMessage() {
        return new ConnectedMessage(jsonObject);
    }

    public StartMessage asStartMessage() {
        return new StartMessage(jsonObject);
    }

    public MediaMessage asMediaMessage() {
        return new MediaMessage(jsonObject);
    }

    public StopMessage asStopMessage() {
        return new StopMessage(jsonObject);
    }

    public MarkMessage asMarkMessage() {
        return new MarkMessage(jsonObject);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Message.class.getSimpleName() + "[", "]")
                .add("jsonObject=" + jsonObject)
                .toString();
    }
}
