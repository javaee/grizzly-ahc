/*
 * Copyright (c) 2017-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014 AsyncHttpClient Project. All rights reserved.
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

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ProxyServer;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Proxy usage tests.
 */
public abstract class ProxyTunnellingTest extends AbstractBasicTest {

    int port2;
    private Server server2;

    public void setUpGlobal() {
    }

    private void setUpServers(ServerConnector server2Connector) throws Exception {

        port1 = findFreePort();
        port2 = findFreePort();
        ServerConnector listener = new ServerConnector(getServer());
        listener.setHost("127.0.0.1");
        listener.setPort(port1);
        addConnector(listener);
        setHandler(new ConnectHandler());
        start();

        server2Connector.setHost("127.0.0.1");
        server2Connector.setPort(port2);

        server2.addConnector(server2Connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server2.setHandler(context);
        ServletHolder echo = new ServletHolder(getWebSocketHandler());
        context.addServlet(echo, "/*");
        server2.start();
        log.info("Local HTTP server started successfully");

    }

    private void setUpServer() throws Exception {
        server2 = new Server();
        setUpServers(new ServerConnector(server2));
    }

    private void setUpSSlServer2() throws Exception {
        server2 = new Server();
        HttpConfiguration https_config = new HttpConfiguration();
        https_config.setSecureScheme("https");
        https_config.setSecurePort(port2);
        https_config.setOutputBufferSize(32768);

        SecureRequestCustomizer src = new SecureRequestCustomizer();
        src.setStsMaxAge(2000);
        src.setStsIncludeSubDomains(true);
        https_config.addCustomizer(src);

        SslContextFactory sslContextFactory = new SslContextFactory();
        ClassLoader cl = getClass().getClassLoader();
        URL cacertsUrl = cl.getResource("ssltest-cacerts.jks");
        String trustStoreFile = new File(cacertsUrl.toURI()).getAbsolutePath();
        sslContextFactory.setTrustStorePath(trustStoreFile);
        sslContextFactory.setTrustStorePassword("changeit");
        sslContextFactory.setTrustStoreType("JKS");

        log.info("SSL certs path: {}", trustStoreFile);

        URL keystoreUrl = cl.getResource("ssltest-keystore.jks");
        String keyStoreFile = new File(keystoreUrl.toURI()).getAbsolutePath();
        sslContextFactory.setKeyStorePath(keyStoreFile);
        sslContextFactory.setKeyStorePassword("changeit");
        sslContextFactory.setKeyStoreType("JKS");

        log.info("SSL keystore path: {}", trustStoreFile);

        ServerConnector https_connector = new ServerConnector(server2,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https_config));
        https_connector.setPort(port2);
        https_connector.setIdleTimeout(500000);
        setUpServers(https_connector);
    }

    @Override
    public WebSocketServlet getWebSocketHandler() {
        return new EchoTextWebSocketServlet();
    }
    
    @AfterMethod(alwaysRun = true)
    public void tearDownGlobal() throws Exception {
        stop();
        if (server2 != null) {
            server2.stop();
        }
        server2 = null;
    }

    @Test(timeOut = 60000)
    public void echoWSText() throws Exception {
        setUpServer();
        runTest("ws");
    }

    @Test(timeOut = 60000)
    public void echoWSSText() throws Exception {
        setUpSSlServer2();
        runTest("wss");
    }

    private void runTest(String protocol) throws Exception {
        String targetUrl = String.format("%s://127.0.0.1:%d/", protocol, port2);

        ProxyServer ps = new ProxyServer(ProxyServer.Protocol.HTTP, "127.0.0.1", port1);
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setProxyServer(ps).setAcceptAnyCertificate(true).build();
        try (AsyncHttpClient asyncHttpClient = getAsyncHttpClient(config)) {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<String> text = new AtomicReference<>("");

            WebSocket websocket = asyncHttpClient.prepareGet(targetUrl).execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WebSocketTextListener() {

                @Override
                public void onMessage(String message) {
                    text.set(message);
                    latch.countDown();
                }

                @Override
                public void onOpen(WebSocket websocket) {
                }

                @Override
                public void onClose(WebSocket websocket) {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                    latch.countDown();
                }
            }).build()).get();

            websocket.sendMessage("ECHO");

            latch.await();
            assertEquals(text.get(), "ECHO");
        }
    }
}
