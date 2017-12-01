/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package com.ning.http.client.async;

import static org.testng.Assert.assertEquals;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ning.http.client.AsyncCompletionHandlerBase;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

abstract public class MaxConnectionsInThreads extends AbstractBasicTest {

    private static URI servletEndpointUri;

    @Test(groups = { "online", "default_provider" })
    public void testMaxConnectionsWithinThreads() throws InterruptedException {

        String[] urls = new String[] { servletEndpointUri.toString(), servletEndpointUri.toString() };

        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setConnectTimeout(1000).setRequestTimeout(5000).setAllowPoolingConnections(true)//
                .setMaxConnections(1).setMaxConnectionsPerHost(1).build();

        final CountDownLatch inThreadsLatch = new CountDownLatch(2);
        final AtomicInteger failedCount = new AtomicInteger();
        
        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            for (int i = 0; i < urls.length; i++) {
                final String url = urls[i];
                Thread t = new Thread() {
                    public void run() {
                        client.prepareGet(url).execute(new AsyncCompletionHandlerBase() {
                            @Override
                            public Response onCompleted(Response response) throws Exception {
                                Response r = super.onCompleted(response);
                                inThreadsLatch.countDown();
                                return r;
                            }
                            
                            @Override
                            public void onThrowable(Throwable t) {
                                super.onThrowable(t);
                                failedCount.incrementAndGet();
                                inThreadsLatch.countDown();
                            }
                        });
                    }
                };
                t.start();
            }

            inThreadsLatch.await();

            assertEquals(failedCount.get(), 1, "Max Connections should have been reached when launching from concurrent threads");

            final CountDownLatch notInThreadsLatch = new CountDownLatch(2);
            failedCount.set(0);
            for (int i = 0; i < urls.length; i++) {
                final String url = urls[i];
                final int rank = i;
                client.prepareGet(url).execute(new AsyncCompletionHandlerBase() {
                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        Response r = super.onCompleted(response);
                        notInThreadsLatch.countDown();
                        return r;
                    }
                    
                    @Override
                    public void onThrowable(Throwable t) {
                        super.onThrowable(t);
                        failedCount.set(rank);
                        notInThreadsLatch.countDown();
                    }
                });
            }
            
            notInThreadsLatch.await();
            
            assertEquals(failedCount.get(), 1, "Max Connections should have been reached when launching from main thread");
        }
    }

    @Override
    @BeforeClass
    public void setUpGlobal() throws Exception {

        server = new Server();

        port1 = findFreePort();

        Connector listener = new SelectChannelConnector();
        listener.setHost("127.0.0.1");
        listener.setPort(port1);

        server.addConnector(listener);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new MockTimeoutHttpServlet()), "/timeout/*");

        server.start();

        String endpoint = "http://127.0.0.1:" + port1 + "/timeout/";
        servletEndpointUri = new URI(endpoint);

    }

    public String getTargetUrl() {
        String s = "http://127.0.0.1:" + port1 + "/timeout/";
        try {
            servletEndpointUri = new URI(s);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return s;
    }

    public static class MockTimeoutHttpServlet extends HttpServlet {
        private static final String contentType = "text/plain";
        public static long DEFAULT_TIMEOUT = 2000;

        public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
            res.setStatus(200);
            res.addHeader("Content-Type", contentType);
            long sleepTime = DEFAULT_TIMEOUT;
            try {
                sleepTime = Integer.parseInt(req.getParameter("timeout"));

            } catch (NumberFormatException e) {
                sleepTime = DEFAULT_TIMEOUT;
            }

            try {
                System.out.println("=======================================");
                System.out.println("Servlet is sleeping for: " + sleepTime);
                System.out.println("=======================================");
                System.out.flush();
                Thread.sleep(sleepTime);
                System.out.println("=======================================");
                System.out.println("Servlet is awake for");
                System.out.println("=======================================");
                System.out.flush();
            } catch (Exception e) {

            }

            res.setHeader("XXX", "TripleX");

            byte[] retVal = "1".getBytes();
            try (OutputStream os = res.getOutputStream()) {
                res.setContentLength(retVal.length);
                os.write(retVal);
            }
        }
    }
}
