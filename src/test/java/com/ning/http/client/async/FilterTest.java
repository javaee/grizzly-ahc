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
package com.ning.http.client.async;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.extra.ThrottleRequestFilter;
import com.ning.http.client.filter.FilterContext;
import com.ning.http.client.filter.FilterException;
import com.ning.http.client.filter.ResponseFilter;

import org.eclipse.jetty.server.handler.AbstractHandler;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.*;

public abstract class FilterTest extends AbstractBasicTest {

    private class BasicHandler extends AbstractHandler {

        public void handle(String s, org.eclipse.jetty.server.Request r, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {

            Enumeration<?> e = httpRequest.getHeaderNames();
            String param;
            while (e.hasMoreElements()) {
                param = e.nextElement().toString();
                httpResponse.addHeader(param, httpRequest.getHeader(param));
            }

            httpResponse.setStatus(200);
            httpResponse.getOutputStream().flush();
            httpResponse.getOutputStream().close();
        }
    }

    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new BasicHandler();
    }

    @Test(groups = { "standalone", "default_provider" })
    public void basicTest() throws Throwable {
        AsyncHttpClientConfig.Builder b = new AsyncHttpClientConfig.Builder();
        b.addRequestFilter(new ThrottleRequestFilter(100));

        try (AsyncHttpClient client = getAsyncHttpClient(b.build())) {
            Response response = client.preparePost(getTargetUrl()).execute().get();
            assertNotNull(response);
            assertEquals(response.getStatusCode(), 200);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void loadThrottleTest() throws Throwable {
        AsyncHttpClientConfig.Builder b = new AsyncHttpClientConfig.Builder();
        b.addRequestFilter(new ThrottleRequestFilter(10));

        try (AsyncHttpClient client = getAsyncHttpClient(b.build())) {
            List<Future<Response>> futures = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                futures.add(client.preparePost(getTargetUrl()).execute());
            }

            for (Future<Response> f : futures) {
                Response r = f.get();
                assertNotNull(f.get());
                assertEquals(r.getStatusCode(), 200);
            }
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void maxConnectionsText() throws Throwable {
        AsyncHttpClientConfig.Builder b = new AsyncHttpClientConfig.Builder();
        b.addRequestFilter(new ThrottleRequestFilter(0, 1000));

        try (AsyncHttpClient client = getAsyncHttpClient(b.build())) {
            client.preparePost(getTargetUrl()).execute().get();
            fail("Should have timed out");
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof FilterException);
        }
    }

    public String getTargetUrl() {
        return String.format("http://127.0.0.1:%d/foo/test", port1);
    }

    @Test(groups = { "standalone", "default_provider" })
    public void basicResponseFilterTest() throws Throwable {
        AsyncHttpClientConfig.Builder b = new AsyncHttpClientConfig.Builder();
        b.addResponseFilter(new ResponseFilter() {

            public <T> FilterContext<T> filter(FilterContext<T> ctx) throws FilterException {
                return ctx;
            }

        });

        try (AsyncHttpClient client = getAsyncHttpClient(b.build())) {
            Response response = client.preparePost(getTargetUrl()).execute().get();

            assertNotNull(response);
            assertEquals(response.getStatusCode(), 200);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void replayResponseFilterTest() throws Throwable {
        AsyncHttpClientConfig.Builder b = new AsyncHttpClientConfig.Builder();
        final AtomicBoolean replay = new AtomicBoolean(true);

        b.addResponseFilter(new ResponseFilter() {

            public <T> FilterContext<T> filter(FilterContext<T> ctx) throws FilterException {

                if (replay.getAndSet(false)) {
                    Request request = new RequestBuilder(ctx.getRequest()).addHeader("X-Replay", "true").build();
                    return new FilterContext.FilterContextBuilder<T>().asyncHandler(ctx.getAsyncHandler()).request(request).replayRequest(true).build();
                }
                return ctx;
            }

        });

        try (AsyncHttpClient c = getAsyncHttpClient(b.build())) {
            Response response = c.preparePost(getTargetUrl()).execute().get();

            assertNotNull(response);
            assertEquals(response.getStatusCode(), 200);
            assertEquals(response.getHeader("X-Replay"), "true");
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void replayStatusCodeResponseFilterTest() throws Throwable {
        AsyncHttpClientConfig.Builder b = new AsyncHttpClientConfig.Builder();
        final AtomicBoolean replay = new AtomicBoolean(true);

        b.addResponseFilter(new ResponseFilter() {

            public <T> FilterContext<T> filter(FilterContext<T> ctx) throws FilterException {

                if (ctx.getResponseStatus() != null && ctx.getResponseStatus().getStatusCode() == 200 && replay.getAndSet(false)) {
                    Request request = new RequestBuilder(ctx.getRequest()).addHeader("X-Replay", "true").build();
                    return new FilterContext.FilterContextBuilder<T>().asyncHandler(ctx.getAsyncHandler()).request(request).replayRequest(true).build();
                }
                return ctx;
            }

        });

        try (AsyncHttpClient c = getAsyncHttpClient(b.build())) {
            Response response = c.preparePost(getTargetUrl()).execute().get();

            assertNotNull(response);
            assertEquals(response.getStatusCode(), 200);
            assertEquals(response.getHeader("X-Replay"), "true");
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void replayHeaderResponseFilterTest() throws Throwable {
        AsyncHttpClientConfig.Builder b = new AsyncHttpClientConfig.Builder();
        final AtomicBoolean replay = new AtomicBoolean(true);

        b.addResponseFilter(new ResponseFilter() {

            public <T> FilterContext<T> filter(FilterContext<T> ctx) throws FilterException {

                if (ctx.getResponseHeaders() != null && ctx.getResponseHeaders().getHeaders().getFirstValue("Ping").equals("Pong") && replay.getAndSet(false)) {

                    Request request = new RequestBuilder(ctx.getRequest()).addHeader("Ping", "Pong").build();
                    return new FilterContext.FilterContextBuilder<T>().asyncHandler(ctx.getAsyncHandler()).request(request).replayRequest(true).build();
                }
                return ctx;
            }

        });

        try (AsyncHttpClient c = getAsyncHttpClient(b.build())) {
            Response response = c.preparePost(getTargetUrl()).addHeader("Ping", "Pong").execute().get();

            assertNotNull(response);
            assertEquals(response.getStatusCode(), 200);
            assertEquals(response.getHeader("Ping"), "Pong");
        }
    }
}
