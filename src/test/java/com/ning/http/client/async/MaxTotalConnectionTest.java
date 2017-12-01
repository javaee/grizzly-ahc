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
 * License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ning.http.client.async;

import static org.testng.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.ning.http.client.AsyncCompletionHandlerBase;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public abstract class MaxTotalConnectionTest extends AbstractBasicTest {
    protected final Logger log = LoggerFactory.getLogger(AbstractBasicTest.class);

    @Test(groups = { "standalone", "default_provider" })
    public void testMaxTotalConnectionsExceedingException() throws IOException {
        String[] urls = new String[] { "http://google.com", "http://github.com/" };

        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setConnectTimeout(1000)
                .setRequestTimeout(5000).setAllowPoolingConnections(false).setMaxConnections(1).setMaxConnectionsPerHost(1)
                .build();

        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            List<ListenableFuture<Response>> futures = new ArrayList<>();
            for (int i = 0; i < urls.length; i++) {
                futures.add(client.prepareGet(urls[i]).execute());
            }
            
            boolean caughtError = false;
            int i;
            for (i = 0; i < urls.length; i++) {
                try {
                    futures.get(i).get();
                } catch (Exception e) {
                    // assert that 2nd request fails, because maxTotalConnections=1
                    caughtError = true;
                    break;
                }
            }

            assertEquals(i, 1);
            assertTrue(caughtError);
        }
    }

    @Test
    public void testMaxTotalConnections() throws InterruptedException {
        String[] urls = new String[] { "http://google.com", "http://lenta.ru" };

        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setConnectTimeout(1000).setRequestTimeout(5000)
                .setAllowPoolingConnections(false).setMaxConnections(2).setMaxConnectionsPerHost(1).build();

        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicReference<String> failedUrl = new AtomicReference<>();

        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            for (String url : urls) {
                final String thisUrl = url;
                client.prepareGet(url).execute(new AsyncCompletionHandlerBase() {
                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        Response r = super.onCompleted(response);
                        latch.countDown();
                        return r;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        super.onThrowable(t);
                        failedUrl.set(thisUrl);
                        latch.countDown();
                    }
                });
            }

            latch.await();
            assertNull(failedUrl.get());
        }
    }
}
