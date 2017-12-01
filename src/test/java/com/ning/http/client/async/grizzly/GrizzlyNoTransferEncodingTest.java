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
 * Copyright (c) 2014 Sonatype, Inc. All rights reserved.
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

package com.ning.http.client.async.grizzly;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import static org.glassfish.grizzly.http.server.NetworkListener.DEFAULT_NETWORK_HOST;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GrizzlyNoTransferEncodingTest {
    private static final String TEST_MESSAGE = "Hello World!";
    
    private HttpServer server;
    private int port;
    // ------------------------------------------------------------------- Setup


    @BeforeMethod
    public void setup() throws Exception {
        server = new HttpServer();
        final NetworkListener listener =
                new NetworkListener("server",
                                    DEFAULT_NETWORK_HOST,
                                    0);
        // disable chunking
        listener.setChunkingEnabled(false);
        server.addListener(listener);
        server.getServerConfiguration().addHttpHandler(
                new HttpHandler() {

                    @Override
                    public void service(final Request request,
                            final Response response) throws Exception {
                        response.setContentType("plain/text;charset=\"utf-8\"");
                        // flush to make sure content-length will be missed
                        response.flush();
                        
                        response.getWriter().write(TEST_MESSAGE);
                    }
                }, "/test");
        
        server.start();
        
        port = listener.getPort();
    }


    // --------------------------------------------------------------- Tear Down


    @AfterMethod
    public void tearDown() {
        server.shutdownNow();
        server = null;
    }


    // ------------------------------------------------------------ Test Methods


    @Test
    public void testNoTransferEncoding() throws Exception {
        String url = "http://localhost:" + port + "/test";

        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder()
            .setFollowRedirect(false)
            .setConnectTimeout(15000)
            .setRequestTimeout(15000)
            .setAllowPoolingConnections(false)
            .setDisableUrlEncodingForBoundedRequests(true)
            .setIOThreadMultiplier(2) // 2 is default
            .build();

        try (AsyncHttpClient client = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(config), config)) {
            Future<com.ning.http.client.Response> f = client.prepareGet(url).execute();
            com.ning.http.client.Response r = f.get(10, TimeUnit.SECONDS);
            Assert.assertEquals(TEST_MESSAGE, r.getResponseBody());
        }
    }
}
