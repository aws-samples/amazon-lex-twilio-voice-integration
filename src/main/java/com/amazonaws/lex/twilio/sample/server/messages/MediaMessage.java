package com.amazonaws.lex.twilio.sample.server.messages;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Base64;
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
public class MediaMessage {
    private final JsonObject jsonObject;

    public MediaMessage(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public MediaMessage(byte[] bytes, String streamSid) {
        this.jsonObject = new JsonObject();

        jsonObject.add("event", new JsonPrimitive("media"));
        jsonObject.add("streamSid", new JsonPrimitive(streamSid));
        JsonObject payload = new JsonObject();
        payload.add("payload", new JsonPrimitive(Base64.getEncoder().encodeToString(bytes)));
        jsonObject.add("media", payload);
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public String getStreamSid() {
        return jsonObject.get("streamSid").getAsString();
    }

    public byte[] getDecodedPayload() {
        String payload = jsonObject.get("media").getAsJsonObject().get("payload").getAsString();
        return Base64.getDecoder().decode(payload);
    }

    public String getTrack() {
        return jsonObject.get("media").getAsJsonObject().get("track").getAsString();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MediaMessage.class.getSimpleName() + "[", "]")
                .add("jsonObject=" + jsonObject)
                .toString();
    }
}
