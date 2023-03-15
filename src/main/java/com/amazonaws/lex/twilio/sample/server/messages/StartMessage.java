package com.amazonaws.lex.twilio.sample.server.messages;

import com.amazonaws.lex.twilio.sample.server.CallIdentifier;
import com.amazonaws.lex.twilio.sample.server.media.MediaFormat;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;

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
public class StartMessage {
    private final JsonObject jsonObject;

    private static final Logger LOG = Logger.getLogger(StartMessage.class);

    public StartMessage(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public CallIdentifier getCallIdentifier() {
        JsonObject startObj = getStartObject();
	LOG.info("Got Caller Identifier for call incoming: " + startObj);

        return new CallIdentifier(
                startObj.get("accountSid").getAsString(),
                startObj.get("callSid").getAsString(),
                startObj.get("streamSid").getAsString());
    }


    public MediaFormat getMediaFormat() {
        JsonObject startObj = getStartObject();

        return new MediaFormat(
                startObj.get("encoding").getAsString(),
                startObj.get("sammpleRate").getAsInt(),
                startObj.get("channels").getAsInt());
    }

    private JsonObject getStartObject() {
        return jsonObject.get("start").getAsJsonObject();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", StartMessage.class.getSimpleName() + "[", "]")
                .add("jsonObject=" + jsonObject)
                .toString();
    }
}
