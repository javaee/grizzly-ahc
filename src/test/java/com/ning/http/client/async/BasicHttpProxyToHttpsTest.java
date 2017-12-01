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
 * Copyright (c) 2016 AsyncHttpClient Project. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at
 *     http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.ning.http.client.async;

import static com.ning.http.client.async.BasicHttpsTest.createSSLContext;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ConnectHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test that validates that when having an HTTP proxy and trying to access an HTTPS through the proxy the
 * proxy credentials should be passed during the CONNECT request.
 */
public abstract class BasicHttpProxyToHttpsTest extends AbstractBasicTest {

    private Server server2;

    @AfterClass(alwaysRun = true)
    public void tearDownGlobal() throws Exception {
        try {
            server.stop();
        } catch (Exception e) {
            // Nothing to do
        }
        try
        {
            server2.stop();
        } catch (Exception e) {
            // Nothing to do
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownProps() throws Exception {
        System.clearProperty("javax.net.ssl.keyStore");
    }

    @BeforeClass(alwaysRun = true)
    public void setUpGlobal() throws Exception {
        // HTTP Proxy Server
        server = new Server();
        // HTTPS Server
        server2 = new Server();

        port1 = findFreePort();
        port2 = findFreePort();

        // Proxy Server configuration
        Connector listener = new SelectChannelConnector();
        listener.setHost("127.0.0.1");
        listener.setPort(port1);
        server.addConnector(listener);
        server.setHandler(configureHandler());
        server.start();

        // HTTPS Server
        SslSocketConnector connector = new SslSocketConnector();
        connector.setHost("127.0.0.1");
        connector.setPort(port2);

        ClassLoader cl = getClass().getClassLoader();

        // override system properties
        URL keystoreUrl = cl.getResource("ssltest-keystore.jks");
        String keyStoreFile = new File(keystoreUrl.toURI()).getAbsolutePath();
        connector.setKeystore(keyStoreFile);
        connector.setKeyPassword("changeit");
        connector.setKeystoreType("JKS");

        log.info("SSL keystore path: {}", keyStoreFile);

        server2.addConnector(connector);
        server2.setHandler(new AuthenticateHandler(new EchoHandler()));
        server2.start();
        log.info("Local Proxy Server (" + port1 + "), HTTPS Server (" + port2 + ") started successfully");
    }

    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new ProxyConnectHTTPHandler(new EchoHandler());
    }

    @Test
    public void httpProxyToHttpsUsePreemptiveTargetTest() throws IOException, InterruptedException, ExecutionException, NoSuchAlgorithmException {
        doTest(true);
    }

    @Test
    public void httpProxyToHttpsTargetTest() throws IOException, InterruptedException, ExecutionException, NoSuchAlgorithmException {
        doTest(false);
    }

    private void doTest(boolean usePreemptiveAuth) throws UnknownHostException, InterruptedException, ExecutionException
    {
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setSSLContext(createSSLContext(new AtomicBoolean(true))).build())) {
            Request request = new RequestBuilder("GET")
                .setProxyServer(basicProxy())
                .setUrl(getTargetUrl2())
                .setRealm(new Realm.RealmBuilder()
                              .setPrincipal("user")
                              .setPassword("passwd")
                              .setScheme(AuthScheme.BASIC)
                              .setUsePreemptiveAuth(usePreemptiveAuth)
                              .build())
                .build();
            Future<Response> responseFuture = client.executeRequest(request);
            Response response = responseFuture.get();
            Assert.assertEquals(response.getStatusCode(), HttpServletResponse.SC_OK);
            Assert.assertEquals("127.0.0.1:" + port2, response.getHeader("x-host"));
        }
    }

    private ProxyServer basicProxy() throws UnknownHostException {
        ProxyServer proxyServer = new ProxyServer("127.0.0.1", port1, "johndoe", "pass");
        proxyServer.setScheme(AuthScheme.BASIC);
        return proxyServer;
    }

    private static class ProxyConnectHTTPHandler extends ConnectHandler {

        public ProxyConnectHTTPHandler(Handler handler) {
            super(handler);
        }

        @Override
        protected boolean handleAuthentication(HttpServletRequest request, HttpServletResponse response, String address) throws ServletException, IOException
        {
            return true;
        }

        /**
         * Override this method do to the {@link ConnectHandler#handleConnect(org.eclipse.jetty.server.Request, HttpServletRequest, HttpServletResponse, String)} doesn't allow me to generate a response with
         * {@link HttpServletResponse#SC_PROXY_AUTHENTICATION_REQUIRED} neither {@link HttpServletResponse#SC_UNAUTHORIZED}.
         */
        @Override
        protected void handleConnect(org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response, String serverAddress) throws ServletException, IOException
        {
            if (!this.doHandleAuthentication(baseRequest, response)) {
                return;
            }
            // Just call super class method to establish the tunnel and avoid copy/paste.
            super.handleConnect(baseRequest, request, response, serverAddress);
        }

        public boolean doHandleAuthentication(org.eclipse.jetty.server.Request request, HttpServletResponse httpResponse) throws IOException, ServletException {
            boolean result = false;
            if ("CONNECT".equals(request.getMethod())) {
                String authorization = request.getHeader("Proxy-Authorization");
                if (authorization == null) {
                    httpResponse.setStatus(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
                    httpResponse.setHeader("Proxy-Authenticate", "Basic realm=\"Fake Realm\"");
                    result = false;
                } else if (authorization
                    .equals("Basic am9obmRvZTpwYXNz")) {
                    httpResponse.setStatus(HttpServletResponse.SC_OK, "Connection established");
                    result = true;
                } else {
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.getOutputStream().flush();
                    httpResponse.getOutputStream().close();
                    result = false;
                }
                httpResponse.getOutputStream().flush();
                httpResponse.getOutputStream().close();
                request.setHandled(true);
            }
            return result;
        }
    }

    private static class AuthenticateHandler extends AbstractHandler {

        private Handler target;

        public AuthenticateHandler(Handler target) {
            this.target = target;
        }

        @Override
        public void handle(String pathInContext, org.eclipse.jetty.server.Request request, HttpServletRequest httpRequest,
                           HttpServletResponse httpResponse) throws IOException, ServletException {
            String authorization = httpRequest.getHeader("Authorization");
            if (authorization != null && authorization.equals("Basic dXNlcjpwYXNzd2Q="))
            {
                httpResponse.addHeader("target", request.getUri().toString());
                target.handle(pathInContext, request, httpRequest, httpResponse);
            }
            else
            {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setHeader("www-authenticate", "Basic realm=\"Fake Realm\"");
                httpResponse.getOutputStream().flush();
                httpResponse.getOutputStream().close();
                request.setHandled(true);
            }

        }
    }

}
