/*
 * Copyright (c) 2017-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.ning.http.client.ws;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public abstract class RedirectTest extends AbstractBasicTest {

    protected int port2;

    // ------------------------------------------ Methods from AbstractBasicTest

    @BeforeClass
    @Override
    public void setUpGlobal() throws Exception {
        port1 = findFreePort();

        _connector = new ServerConnector(getServer());
        _connector.setPort(port1);

        addConnector(_connector);

        port2 = findFreePort();
        final ServerConnector connector2 = new ServerConnector(getServer());
        connector2.setPort(port2);
        addConnector(connector2);
        WebSocketServlet _wsHandler = getWebSocketHandler();
        HandlerList list = new HandlerList();
        list.addHandler(new HandlerWrapper() {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
                if (request.getLocalPort() == port2) {
                    httpServletResponse.sendRedirect(getTargetUrl());
                }
            }
        });

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        ServletHolder echo = new ServletHolder(_wsHandler);
        context.addServlet(echo, "/*");
        list.addHandler(context);
        setHandler(list);

        start();
        log.info("Local HTTP server started successfully");
    }

    @Override
    public WebSocketServlet getWebSocketHandler() {
        return new EchoTextWebSocketServlet();
    }

    // ------------------------------------------------------------ Test Methods

    @Test(timeOut = 60000)
    public void testRedirectToWSResource() throws Exception {
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setFollowRedirect(true).build())) {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<String> text = new AtomicReference<>("");

            WebSocket websocket = client.prepareGet(getRedirectURL()).execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WebSocketListener() {

                @Override
                public void onOpen(com.ning.http.client.ws.WebSocket websocket) {
                    text.set("OnOpen");
                    latch.countDown();
                }

                @Override
                public void onClose(com.ning.http.client.ws.WebSocket websocket) {
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                    latch.countDown();
                }
            }).build()).get();

            latch.await();
            assertEquals(text.get(), "OnOpen");
            websocket.close();
        }
    }

    // --------------------------------------------------------- Private Methods

    private String getRedirectURL() {
        return String.format("ws://127.0.0.1:%d/", port2);
    }
}
