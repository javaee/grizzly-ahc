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

import static com.ning.http.client.Realm.AuthScheme.BASIC;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Realm;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test that validates that when having an HTTP proxy and trying to access an HTTP through the proxy the
 * proxy credentials should be passed after it gets a 407 response.
 */
public abstract class BasicHttpProxyToHttpTest extends AbstractBasicTest {

    private Server server2;

    public static class ProxyHTTPHandler extends AbstractHandler {

        @Override
        public void handle(String pathInContext, org.eclipse.jetty.server.Request request, HttpServletRequest httpRequest,
                           HttpServletResponse httpResponse) throws IOException, ServletException {

            String authorization = httpRequest.getHeader("Authorization");
            String proxyAuthorization = httpRequest.getHeader("Proxy-Authorization");
            if (proxyAuthorization == null) {
                httpResponse.setStatus(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
                httpResponse.setHeader("Proxy-Authenticate", "Basic realm=\"Fake Realm\"");
            } else if (proxyAuthorization
                .equals("Basic am9obmRvZTpwYXNz") && authorization != null && authorization.equals("Basic dXNlcjpwYXNzd2Q=")) {
                httpResponse.addHeader("target", request.getUri().toString());
                httpResponse.setStatus(HttpServletResponse.SC_OK);
            } else {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setHeader("www-authenticate", "Basic realm=\"Fake Realm\"");
            }
            httpResponse.getOutputStream().flush();
            httpResponse.getOutputStream().close();
            request.setHandled(true);
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDownGlobal() throws Exception {
        server.stop();
        server2.stop();
    }

    @BeforeClass(alwaysRun = true)
    public void setUpGlobal() throws Exception {
        // HTTP Server
        server = new Server();
        // HTTP Proxy Server
        server2 = new Server();

        port1 = findFreePort();
        port2 = findFreePort();

        // HTTP Server
        Connector listener = new SelectChannelConnector();

        listener.setHost("127.0.0.1");
        listener.setPort(port1);
        server.addConnector(listener);
        server.setHandler(new EchoHandler());
        server.start();

        listener = new SelectChannelConnector();

        // Proxy Server configuration
        listener.setHost("127.0.0.1");
        listener.setPort(port2);
        server2.addConnector(listener);
        server2.setHandler(configureHandler());
        server2.start();
        log.info("Local HTTP Server (" + port1 + "), Proxy HTTP Server (" + port2 + ") started successfully");
    }


    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new ProxyHTTPHandler();
    }

    @Test
    public void httpProxyToHttpTargetUsePreemptiveAuthTest() throws IOException, InterruptedException, ExecutionException {
        doTest(true);
    }

    @Test
    public void httpProxyToHttpTargetTest() throws IOException, InterruptedException, ExecutionException {
        doTest(false);
    }

    private void doTest(boolean usePreemptiveAuth) throws UnknownHostException, InterruptedException, ExecutionException
    {
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().build())) {
            Request request = new RequestBuilder("GET").setProxyServer(basicProxy()).setUrl(getTargetUrl()).setRealm(
                new Realm.RealmBuilder().setPrincipal("user").setPassword("passwd").setScheme(BASIC).setUsePreemptiveAuth(usePreemptiveAuth).build()).build();
            Future<Response> responseFuture = client.executeRequest(request);
            Response response = responseFuture.get();
            Assert.assertEquals(response.getStatusCode(), HttpServletResponse.SC_OK);
            Assert.assertEquals(getTargetUrl(), response.getHeader("target"));
        }
    }

    private ProxyServer basicProxy() throws UnknownHostException {
        ProxyServer proxyServer = new ProxyServer("127.0.0.1", port2, "johndoe", "pass");
        proxyServer.setScheme(BASIC);
        return proxyServer;
    }
}
