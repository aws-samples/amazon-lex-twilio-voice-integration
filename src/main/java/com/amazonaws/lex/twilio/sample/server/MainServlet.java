package com.amazonaws.lex.twilio.sample.server;

import com.twilio.twiml.TwiMLException;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Connect;
import com.twilio.twiml.voice.Say;
import com.twilio.twiml.voice.Stream;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
@WebServlet("/voice")
public class

MainServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(MainServlet.class);

    public MainServlet() {
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws
            IOException {

        String pathForWebsocketsStream = String.format("wss://%s%s/%s",
                request.getServerName(),
                request.getContextPath(),
                "audiostream");

        LOGGER.info(String.format("websockets stream url %s", pathForWebsocketsStream));

        Say say = new Say.Builder().addText("Welcome to a Twilio Lex sample application.").build();

        Stream stream = new Stream.Builder()
                .url(pathForWebsocketsStream)
                .build();
        Connect connect = new Connect.Builder().stream(stream).build();

        VoiceResponse voiceResponse = new VoiceResponse.Builder()
                .say(say)
                .connect(connect)
                .build();

        // Render TwiML as XML
        response.setContentType("text/xml");

        try {
            response.getWriter().print(voiceResponse.toXml());
        } catch (TwiMLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        LOGGER.info("Hello World from log!");

        httpServletResponse.setContentType("text/html");

        httpServletResponse.getWriter().println("<h1>Status Lex Wait and Continue: <i style=\"color: green\">Green</i></h1>");
    }
}
