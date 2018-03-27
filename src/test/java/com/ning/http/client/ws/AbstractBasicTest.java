/*
 * Copyright (c) 2017-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2010-2012 Sonatype, Inc. All rights reserved.
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

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.IOException;
import java.net.ServerSocket;

public abstract class AbstractBasicTest extends Server {

    protected final Logger log = LoggerFactory.getLogger(AbstractBasicTest.class);
    protected int port1;
    ServerConnector _connector;

    @AfterClass(alwaysRun = true)
    public void tearDownGlobal() throws Exception {
        stop();
    }

    protected int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    protected String getTargetUrl() {
        return String.format("ws://127.0.0.1:%d/", port1);
    }

    @BeforeClass(alwaysRun = true)
    public void setUpGlobal() throws Exception {
        port1 = findFreePort();
        _connector = new ServerConnector(getServer());
        _connector.setPort(port1);

        addConnector(_connector);
        WebSocketServlet _wsHandler = getWebSocketHandler();
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        getServer().setHandler(context);
        ServletHolder echo = new ServletHolder(_wsHandler);
        context.addServlet(echo, "/*");
        start();
        log.info("Local HTTP server started successfully");
    }

    public abstract WebSocketServlet getWebSocketHandler();

    public abstract AsyncHttpClient getAsyncHttpClient(AsyncHttpClientConfig config);

}
