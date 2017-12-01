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
import com.ning.http.client.Realm;
import com.ning.http.client.Response;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.security.Constraint;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public abstract class AuthTimeoutTest extends AbstractBasicTest {

    private final static String user = "user";

    private final static String admin = "admin";

    public void setUpServer(String auth) throws Exception {
        server = new Server();
        Logger root = Logger.getRootLogger();
        root.setLevel(Level.DEBUG);
        root.addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));

        port1 = findFreePort();
        Connector listener = new SelectChannelConnector();

        listener.setHost("127.0.0.1");
        listener.setPort(port1);

        server.addConnector(listener);

        LoginService loginService = new HashLoginService("MyRealm", "src/test/resources/realm.properties");
        server.addBean(loginService);

        Constraint constraint = new Constraint();
        constraint.setName(auth);
        constraint.setRoles(new String[] { user, admin });
        constraint.setAuthenticate(true);

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec("/*");

        Set<String> knownRoles = new HashSet<>();
        knownRoles.add(user);
        knownRoles.add(admin);

        ConstraintSecurityHandler security = new ConstraintSecurityHandler();

        List<ConstraintMapping> cm = new ArrayList<>();
        cm.add(mapping);

        security.setConstraintMappings(cm, knownRoles);
        security.setAuthenticator(new BasicAuthenticator());
        security.setLoginService(loginService);
        security.setStrict(false);
        security.setHandler(configureHandler());

        server.setHandler(security);
        server.start();
        log.info("Local HTTP server started successfully");
    }

    private class SimpleHandler extends AbstractHandler {
        public void handle(String s, Request r, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

            // NOTE: handler sends less bytes than are given in Content-Length, which should lead to timeout

            OutputStream out = response.getOutputStream();
            if (request.getHeader("X-Content") != null) {
                String content = request.getHeader("X-Content");
                response.setHeader("Content-Length", String.valueOf(content.getBytes("UTF-8").length));
                out.write(content.substring(1).getBytes("UTF-8"));
                out.flush();
                out.close();
                return;
            }

            response.setStatus(200);
            out.flush();
            out.close();
        }
    }

    @Test(groups = { "standalone", "default_provider" }, enabled = false)
    public void basicAuthTimeoutTest() throws Exception {
        setUpServer(Constraint.__BASIC_AUTH);
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setPooledConnectionIdleTimeout(2000).setConnectTimeout(20000).setRequestTimeout(2000).build();

        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            Future<Response> f = execute(client, false);
            try {
                f.get();
                fail("expected timeout");
            } catch (Exception e) {
                inspectException(e);
            }
        }
    }

    @Test(groups = { "standalone", "default_provider" }, enabled = false)
    public void basicPreemptiveAuthTimeoutTest() throws Exception {
        setUpServer(Constraint.__BASIC_AUTH);
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setPooledConnectionIdleTimeout(2000).setConnectTimeout(20000).setRequestTimeout(2000).build();

        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            Future<Response> f = execute(client, true);
            try {
                f.get();
                fail("expected timeout");
            } catch (Exception e) {
                inspectException(e);
            }
        }
    }

    @Test(groups = { "standalone", "default_provider" }, enabled = false)
    public void digestAuthTimeoutTest() throws Exception {
        setUpServer(Constraint.__DIGEST_AUTH);
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setPooledConnectionIdleTimeout(2000).setConnectTimeout(20000).setRequestTimeout(2000).build();

        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            Future<Response> f = execute(client, false);
            try {
                f.get();
                fail("expected timeout");
            } catch (Exception e) {
                inspectException(e);
            }
        }
    }

    @Test(groups = { "standalone", "default_provider" }, enabled = false)
    public void digestPreemptiveAuthTimeoutTest() throws Exception {
        setUpServer(Constraint.__DIGEST_AUTH);
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setPooledConnectionIdleTimeout(2000).setConnectTimeout(20000).setRequestTimeout(2000).build();

        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            Future<Response> f = execute(client, true);
            f.get();
            fail("expected timeout");
        } catch (Exception e) {
            inspectException(e);
        }
    }

    @Test(groups = { "standalone", "default_provider" }, enabled = false)
    public void basicFutureAuthTimeoutTest() throws Exception {
        setUpServer(Constraint.__BASIC_AUTH);
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setPooledConnectionIdleTimeout(2000).setConnectTimeout(20000).setRequestTimeout(2000).build();

        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            Future<Response> f = execute(client, false);
            f.get(1, TimeUnit.SECONDS);
            fail("expected timeout");
        } catch (Exception e) {
            inspectException(e);
        }
    }

    @Test(groups = { "standalone", "default_provider" }, enabled = false)
    public void basicFuturePreemptiveAuthTimeoutTest() throws Exception {
        setUpServer(Constraint.__BASIC_AUTH);
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setPooledConnectionIdleTimeout(2000).setConnectTimeout(20000).setRequestTimeout(2000).build();

        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            Future<Response> f = execute(client, true);
            f.get(1, TimeUnit.SECONDS);
            fail("expected timeout");
        } catch (Exception e) {
            inspectException(e);
        }
    }

    @Test(groups = { "standalone", "default_provider" }, enabled = false)
    public void digestFutureAuthTimeoutTest() throws Exception {
        setUpServer(Constraint.__DIGEST_AUTH);
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setPooledConnectionIdleTimeout(2000).setConnectTimeout(20000).setRequestTimeout(2000).build();

        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            Future<Response> f = execute(client, false);
            f.get(1, TimeUnit.SECONDS);
            fail("expected timeout");
        } catch (Exception e) {
            inspectException(e);
        }
    }

    @Test(groups = { "standalone", "default_provider" }, enabled = false)
    public void digestFuturePreemptiveAuthTimeoutTest() throws Exception {
        setUpServer(Constraint.__DIGEST_AUTH);
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setPooledConnectionIdleTimeout(2000).setConnectTimeout(20000).setRequestTimeout(2000).build();

        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            Future<Response> f = execute(client, true);
            f.get(1, TimeUnit.SECONDS);
            fail("expected timeout");
        } catch (Exception e) {
            inspectException(e);
        }
    }

    protected void inspectException(Throwable t) {
        assertNotNull(t.getCause());
        assertEquals(t.getCause().getClass(), IOException.class);
        if (!t.getCause().getMessage().startsWith("Remotely Closed")) {
            fail();
        }
    }

    protected Future<Response> execute(AsyncHttpClient client, boolean preemptive) throws IOException {
        AsyncHttpClient.BoundRequestBuilder r = client.prepareGet(getTargetUrl()).setRealm(realm(preemptive)).setHeader("X-Content", "Test");
        Future<Response> f = r.execute();
        return f;
    }

    private Realm realm(boolean preemptive) {
        return (new Realm.RealmBuilder()).setPrincipal(user).setPassword(admin).setUsePreemptiveAuth(preemptive).build();
    }

    @Override
    protected String getTargetUrl() {
        return "http://127.0.0.1:" + port1 + "/";
    }

    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new SimpleHandler();
    }
}
