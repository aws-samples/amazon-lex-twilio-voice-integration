package com.amazonaws.lex.twilio.sample.server.media;

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

public class MediaFormat {
    private final String encoding;
    private final int sampleRate;
    private final int numOfChannels;

    public MediaFormat(String encoding, int sampleRate, int numOfChannels) {
        this.encoding = encoding;
        this.sampleRate = sampleRate;
        this.numOfChannels = numOfChannels;
    }

    public String getEncoding() {
        return encoding;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getNumOfChannels() {
        return numOfChannels;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MediaFormat.class.getSimpleName() + "[", "]")
                .add("encoding='" + encoding + "'")
                .add("sampleRate=" + sampleRate)
                .add("numOfChannels=" + numOfChannels)
                .toString();
    }
}
