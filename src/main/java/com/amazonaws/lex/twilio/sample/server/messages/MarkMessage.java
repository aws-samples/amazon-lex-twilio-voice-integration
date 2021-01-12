package com.amazonaws.lex.twilio.sample.server.messages;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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
public class MarkMessage {
    private final JsonObject jsonObject;

    public MarkMessage(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public MarkMessage(String streamSid, String markName) {
        this.jsonObject = new JsonObject();
        jsonObject.add("event", new JsonPrimitive("mark"));
        jsonObject.add("streamSid", new JsonPrimitive(streamSid));

        JsonObject markObject = new JsonObject();
        markObject.add("name", new JsonPrimitive(markName));
        jsonObject.add("mark", markObject);
    }

    public String getMarkName() {
        return jsonObject.getAsJsonObject("mark").getAsJsonPrimitive("name").getAsString();
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MarkMessage.class.getSimpleName() + "[", "]")
                .add("jsonObject=" + jsonObject)
                .toString();
    }
}
