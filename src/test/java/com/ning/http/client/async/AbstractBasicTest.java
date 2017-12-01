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

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Response;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Enumeration;

public abstract class AbstractBasicTest {
    
    public final static String TEXT_HTML_CONTENT_TYPE_WITH_UTF_8_CHARSET = "text/html; charset=UTF-8";
    
    protected final Logger log = LoggerFactory.getLogger(AbstractBasicTest.class);
    protected Server server;
    protected int port1;
    protected int port2;

    public final static int TIMEOUT = 30;

    public static class EchoHandler extends AbstractHandler {

        @Override
        public void handle(String pathInContext,
                           Request request,
                           HttpServletRequest httpRequest,
                           HttpServletResponse httpResponse) throws IOException, ServletException {

            if (httpRequest.getHeader("X-HEAD") != null) {
                httpResponse.setContentLength(1);
            }

            if (httpRequest.getHeader("X-ISO") != null) {
                httpResponse.setContentType("text/html; charset=ISO-8859-1");
            } else {
                httpResponse.setContentType(TEXT_HTML_CONTENT_TYPE_WITH_UTF_8_CHARSET);
            }

            if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
                httpResponse.addHeader("Allow","GET,HEAD,POST,OPTIONS,TRACE");
            };

            Enumeration<?> e = httpRequest.getHeaderNames();
            String param;
            while (e.hasMoreElements()) {
                param = e.nextElement().toString();

                if (param.startsWith("LockThread")) {
                    try {
                        Thread.sleep(40 * 1000);
                    } catch (InterruptedException ex) {
                    }
                }

                if (param.startsWith("X-redirect")) {
                    httpResponse.sendRedirect(httpRequest.getHeader("X-redirect"));
                    return;
                }
                if (param.startsWith("Y-")) {
                    httpResponse.addHeader(param.substring(2), httpRequest.getHeader(param));
                } else {
                    httpResponse.addHeader("X-" + param, httpRequest.getHeader(param));
                }
            }

            Enumeration<?> i = httpRequest.getParameterNames();

            StringBuilder requestBody = new StringBuilder();
            while (i.hasMoreElements()) {
                param = i.nextElement().toString();
                httpResponse.addHeader("X-" + param, httpRequest.getParameter(param));
                requestBody.append(param);
                requestBody.append("_");
            }

            String pathInfo = httpRequest.getPathInfo();
            if (pathInfo != null)
                httpResponse.addHeader("X-pathInfo", pathInfo);

            String queryString = httpRequest.getQueryString();
            if (queryString != null)
                httpResponse.addHeader("X-queryString", queryString);

            httpResponse.addHeader("X-KEEP-ALIVE", httpRequest.getRemoteAddr() + ":" + httpRequest.getRemotePort());

            javax.servlet.http.Cookie[] cs = httpRequest.getCookies();
            if (cs != null) {
                for (javax.servlet.http.Cookie c : cs) {
                    httpResponse.addCookie(c);
                }
            }

            if (requestBody.length() > 0) {
                httpResponse.getOutputStream().write(requestBody.toString().getBytes());
            }

            int size = 16384;
            if (httpRequest.getContentLength() > 0) {
                size = httpRequest.getContentLength();
            }
            byte[] bytes = new byte[size];
            if (bytes.length > 0) {
                int read = 0;
                while (read > -1) {
                    read = httpRequest.getInputStream().read(bytes);
                    if (read > 0) {
                        httpResponse.getOutputStream().write(bytes, 0, read);
                    }
                }
            }

            httpResponse.setStatus(200);
            httpResponse.getOutputStream().flush();
            httpResponse.getOutputStream().close();
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDownGlobal() throws Exception {
        server.stop();
    }

    protected int findFreePort() throws IOException {

        try (ServerSocket socket = new ServerSocket(0)){
            return socket.getLocalPort();
        }
    }

    protected String getTargetUrl() {
        return String.format("http://127.0.0.1:%d/foo/test", port1);
    }

    protected String getTargetUrl2() {
        return String.format("https://127.0.0.1:%d/foo/test", port2);
    }

    public AbstractHandler configureHandler() throws Exception {
        return new EchoHandler();
    }

    @BeforeClass(alwaysRun = true)
    public void setUpGlobal() throws Exception {
        server = new Server();

        port1 = findFreePort();
        port2 = findFreePort();

        Connector listener = new SelectChannelConnector();

        listener.setHost("127.0.0.1");
        listener.setPort(port1);

        server.addConnector(listener);

        listener = new SelectChannelConnector();
        listener.setHost("127.0.0.1");
        listener.setPort(port2);

        server.addConnector(listener);

        server.setHandler(configureHandler());
        server.start();
        log.info("Local HTTP server started successfully");
    }

    public static class AsyncCompletionHandlerAdapter extends AsyncCompletionHandler<Response> {

        @Override
        public Response onCompleted(Response response) throws Exception {
            return response;
        }

        @Override
        public void onThrowable(Throwable t) {
            t.printStackTrace();
            Assert.fail("Unexpected exception: " + t.getMessage(), t);
        }

    }

    public static class AsyncHandlerAdapter implements AsyncHandler<String> {


        @Override
        public void onThrowable(Throwable t) {
            t.printStackTrace();
            Assert.fail("Unexpected exception", t);
        }

        @Override
        public STATE onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
            return STATE.CONTINUE;
        }

        @Override
        public STATE onStatusReceived(final HttpResponseStatus responseStatus) throws Exception {
            return STATE.CONTINUE;
        }

        @Override
        public STATE onHeadersReceived(final HttpResponseHeaders headers) throws Exception {
            return STATE.CONTINUE;
        }

        @Override
        public String onCompleted() throws Exception {
            return "";
        }

    }

    public abstract AsyncHttpClient getAsyncHttpClient(AsyncHttpClientConfig config);

}
