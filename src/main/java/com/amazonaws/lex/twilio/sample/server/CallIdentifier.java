package com.amazonaws.lex.twilio.sample.server;

import java.util.Objects;

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
public class CallIdentifier {

    private final String accountId;
    private final String callId;
    private final String streamSid;

    public CallIdentifier(String accountId, String callId, String streamSid) {
        this.accountId = accountId;
        this.callId = callId;
        this.streamSid = streamSid;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCallId() {
        return callId;
    }

    public String getStreamSid() {
        return streamSid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallIdentifier that = (CallIdentifier) o;
        return accountId.equals(that.accountId) &&
                callId.equals(that.callId) &&
                streamSid.equals(that.streamSid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, callId, streamSid);
    }
}
