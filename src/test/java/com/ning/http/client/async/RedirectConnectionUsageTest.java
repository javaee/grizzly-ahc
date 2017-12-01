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

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpProviderConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.FileAssert.fail;

/**
 * Test for multithreaded url fetcher calls that use two separate
 * sets of ssl certificates.  This then tests that the certificate
 * settings do not clash (override each other), resulting in the
 * peer not authenticated exception
 *
 * @author dominict
 */
public abstract class RedirectConnectionUsageTest extends AbstractBasicTest{
    private String BASE_URL;

    private String servletEndpointRedirectUrl;

    @BeforeClass
    public void setUp() throws Exception {
        server = new Server();

        port1 = findFreePort();

        Connector listener = new SelectChannelConnector();
        listener.setHost("localhost");
        listener.setPort(port1);

        server.addConnector(listener);


        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new MockRedirectHttpServlet()), "/redirect/*");
        context.addServlet(new ServletHolder(new MockFullResponseHttpServlet()), "/*");

        server.start();

        BASE_URL = "http://localhost" + ":" + port1;
        servletEndpointRedirectUrl = BASE_URL + "/redirect";

    }

    @AfterClass
    public void tearDown() {
        try {
            if (server != null) {
                server.stop();
            }

        } catch (Exception e) {
            System.err.print("Error stopping servlet tester");
            e.printStackTrace();
        }
    }

    /**
     * Tests that after a redirect the final url in the response
     * reflect the redirect
     */
    @Test
    public void testGetRedirectFinalUrl() {

        AsyncHttpClientConfig.Builder bc = new AsyncHttpClientConfig.Builder()//
                .setAllowPoolingConnections(true)//
                .setMaxConnectionsPerHost(1)//
                .setMaxConnections(1)//
                .setConnectTimeout(1000)//
                .setRequestTimeout(1000)//
                .setFollowRedirect(true);

        try (AsyncHttpClient client = getAsyncHttpClient(bc.build())) {

            RequestBuilder builder = new RequestBuilder("GET");
            builder.setUrl(servletEndpointRedirectUrl);

            com.ning.http.client.Request r = builder.build();

            try {
                ListenableFuture<Response> response = client.executeRequest(r);
                Response res = null;
                res = response.get();
                assertNotNull(res.getResponseBody());
                assertEquals(BASE_URL + "/overthere", BASE_URL + "/overthere", res.getUri().toString());

            } catch (Exception e) {
                System.err.print("============");
                e.printStackTrace();
                System.err.print("============");
                System.err.flush();
                fail("Should not get here, The request threw an exception");
            }


        }
    }

    protected abstract AsyncHttpProviderConfig getProviderConfig();


    class MockRedirectHttpServlet extends HttpServlet {
        public void service(HttpServletRequest req, HttpServletResponse res)
                throws ServletException, IOException {
            res.sendRedirect("/overthere");
        }

    }

    class MockFullResponseHttpServlet extends HttpServlet {

        private static final String contentType = "text/xml";
        private static final String xml = "<?xml version=\"1.0\"?><hello date=\"%s\"></hello>";

        public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
            String xmlToReturn = String.format(xml, new Object[]{new Date().toString()});

            res.setStatus(200, "Complete, XML Being Returned");
            res.addHeader("Content-Type", contentType);
            res.addHeader("X-Method", req.getMethod());
            res.addHeader("MultiValue", "1");
            res.addHeader("MultiValue", "2");
            res.addHeader("MultiValue", "3");

            OutputStream os = res.getOutputStream();

            byte[] retVal = xmlToReturn.getBytes();
            res.setContentLength(retVal.length);
            os.write(retVal);
            os.close();
        }
    }
}
