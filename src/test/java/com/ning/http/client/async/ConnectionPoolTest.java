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
 */
package com.ning.http.client.async;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncCompletionHandlerBase;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;

public abstract class ConnectionPoolTest extends AbstractBasicTest {
    protected final Logger log = LoggerFactory.getLogger(AbstractBasicTest.class);

    @Test(groups = { "standalone", "default_provider" })
    public void testMaxTotalConnections() {
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setAllowPoolingConnections(true).setMaxConnections(1).build())) {
            String url = getTargetUrl();
            int i;
            Exception exception = null;
            for (i = 0; i < 3; i++) {
                try {
                    log.info("{} requesting url [{}]...", i, url);
                    Response response = client.prepareGet(url).execute().get();
                    log.info("{} response [{}].", i, response);
                } catch (Exception ex) {
                    exception = ex;
                }
            }
            assertNull(exception);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testMaxTotalConnectionsException() throws IOException {
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setAllowPoolingConnections(true).setMaxConnections(1).build())) {
            String url = getTargetUrl();
            
            List<ListenableFuture<Response>> futures = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                log.info("{} requesting url [{}]...", i, url);
                futures.add(client.prepareGet(url).execute());
            }
            
            Exception exception = null;
            for (ListenableFuture<Response> future : futures) {
                try {
                    future.get();
                } catch (Exception ex) {
                    exception = ex;
                    break;
                }
            }

            assertNotNull(exception);
            assertNotNull(exception.getCause());
            assertEquals(exception.getCause().getMessage(), "Too many connections 1");
        }
    }

    @Test(groups = { "standalone", "default_provider", "async" }, enabled = true, invocationCount = 10, alwaysRun = true)
    public void asyncDoGetKeepAliveHandlerTest_channelClosedDoesNotFail() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            // Use a l in case the assert fail
            final CountDownLatch l = new CountDownLatch(2);

            final Map<String, Boolean> remoteAddresses = new ConcurrentHashMap<>();

            AsyncCompletionHandler<Response> handler = new AsyncCompletionHandlerAdapter() {

                @Override
                public Response onCompleted(Response response) throws Exception {
                    System.out.println("ON COMPLETED INVOKED " + response.getHeader("X-KEEP-ALIVE"));
                    try {
                        assertEquals(response.getStatusCode(), 200);
                        remoteAddresses.put(response.getHeader("X-KEEP-ALIVE"), true);
                    } finally {
                        l.countDown();
                    }
                    return response;
                }
            };

            client.prepareGet(getTargetUrl()).execute(handler).get();
            server.stop();
            server.start();
            client.prepareGet(getTargetUrl()).execute(handler);

            if (!l.await(TIMEOUT, TimeUnit.SECONDS)) {
                Assert.fail("Timed out");
            }

            assertEquals(remoteAddresses.size(), 2);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void multipleMaxConnectionOpenTest() throws Throwable {
        AsyncHttpClientConfig cg = new AsyncHttpClientConfig.Builder().setAllowPoolingConnections(true).setConnectTimeout(5000).setMaxConnections(1).build();
        try (AsyncHttpClient client = getAsyncHttpClient(cg)) {
            String body = "hello there";

            // once
            Response response = client.preparePost(getTargetUrl()).setBody(body).execute().get(TIMEOUT, TimeUnit.SECONDS);

            assertEquals(response.getResponseBody(), body);

            // twice
            Exception exception = null;
            try {
                client.preparePost(String.format("http://127.0.0.1:%d/foo/test", port2)).setBody(body).execute().get(TIMEOUT, TimeUnit.SECONDS);
                fail("Should throw exception. Too many connections issued.");
            } catch (Exception ex) {
                ex.printStackTrace();
                exception = ex;
            }
            assertNotNull(exception);
            assertNotNull(exception.getCause());
            assertEquals(exception.getCause().getMessage(), "Too many connections 1");
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void multipleMaxConnectionOpenTestWithQuery() throws Throwable {
        AsyncHttpClientConfig cg = new AsyncHttpClientConfig.Builder().setAllowPoolingConnections(true).setConnectTimeout(5000).setMaxConnections(1).build();
        try (AsyncHttpClient client = getAsyncHttpClient(cg)) {
            String body = "hello there";

            // once
            Response response = client.preparePost(getTargetUrl() + "?foo=bar").setBody(body).execute().get(TIMEOUT, TimeUnit.SECONDS);

            assertEquals(response.getResponseBody(), "foo_" + body);

            // twice
            Exception exception = null;
            try {
                response = client.preparePost(getTargetUrl()).setBody(body).execute().get(TIMEOUT, TimeUnit.SECONDS);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            assertNull(exception);
            assertNotNull(response);
            assertEquals(response.getStatusCode(), 200);
        }
    }

    /**
     * This test just make sure the hack used to catch disconnected channel under win7 doesn't throw any exception. The onComplete method must be only called once.
     * 
     * @throws Throwable
     *             if something wrong happens.
     */
    @Test(groups = { "standalone", "default_provider" })
    public void win7DisconnectTest() throws Throwable {
        final AtomicInteger count = new AtomicInteger(0);

        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            AsyncCompletionHandler<Response> handler = new AsyncCompletionHandlerAdapter() {

                @Override
                public Response onCompleted(Response response) throws Exception {

                    count.incrementAndGet();
                    StackTraceElement e = new StackTraceElement("sun.nio.ch.SocketDispatcher", "read0", null, -1);
                    IOException t = new IOException();
                    t.setStackTrace(new StackTraceElement[] { e });
                    throw t;
                }
            };

            try {
                client.prepareGet(getTargetUrl()).execute(handler).get();
                fail("Must have received an exception");
            } catch (ExecutionException ex) {
                assertNotNull(ex);
                assertNotNull(ex.getCause());
                assertEquals(ex.getCause().getClass(), IOException.class);
                assertEquals(count.get(), 1);
            }
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void asyncHandlerOnThrowableTest() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            final AtomicInteger count = new AtomicInteger();
            final String THIS_IS_NOT_FOR_YOU = "This is not for you";
            final CountDownLatch latch = new CountDownLatch(16);
            for (int i = 0; i < 16; i++) {
                client.prepareGet(getTargetUrl()).execute(new AsyncCompletionHandlerBase() {
                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        throw new Exception(THIS_IS_NOT_FOR_YOU);
                    }
                });

                client.prepareGet(getTargetUrl()).execute(new AsyncCompletionHandlerBase() {
                    /* @Override */
                    public void onThrowable(Throwable t) {
                        if (t.getMessage() != null && t.getMessage().equalsIgnoreCase(THIS_IS_NOT_FOR_YOU)) {
                            count.incrementAndGet();
                        }
                    }

                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        latch.countDown();
                        return response;
                    }
                });
            }
            latch.await(TIMEOUT, TimeUnit.SECONDS);
            assertEquals(count.get(), 0);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void nonPoolableConnectionReleaseSemaphoresTest() throws Throwable {

        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder()
        .setMaxConnections(6)
        .setMaxConnectionsPerHost(3)
        .build();

        Request request = new RequestBuilder().setUrl(getTargetUrl()).setHeader("Connection", "close").build();

        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            client.executeRequest(request).get();
            Thread.sleep(1000);
            client.executeRequest(request).get();
            Thread.sleep(1000);
            client.executeRequest(request).get();
            Thread.sleep(1000);
            client.executeRequest(request).get();
        }
    }
}
