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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.FileAssert.fail;

import org.testng.annotations.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.generators.InputStreamBodyGenerator;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;

/**
 * Test that the url fetcher is able to communicate via a proxy
 * 
 * @author dominict
 */
abstract public class ChunkingTest extends AbstractBasicTest {
    // So we can just test the returned data is the image,
    // and doesn't contain the chunked delimeters.
    public static byte[] LARGE_IMAGE_BYTES;

    static {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream instream = null;
        try {
            ClassLoader cl = ChunkingTest.class.getClassLoader();
            // override system properties
            URL url = cl.getResource("300k.png");
            File sourceFile = new File(url.toURI());
            instream = new FileInputStream(sourceFile);
            byte[] buf = new byte[8092];
            int len = 0;
            while ((len = instream.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            LARGE_IMAGE_BYTES = baos.toByteArray();
        } catch (Throwable e) {
            LARGE_IMAGE_BYTES = new byte[265495];
            Random x = new Random();
            x.nextBytes(LARGE_IMAGE_BYTES);
        }
    }

    // So we can just test the returned data is the image,
    // and doesn't contain the chunked delimeters.
    @Test()
    public void testBufferLargerThanFileWithStreamBodyGenerator() throws Throwable {
        doTestWithInputStreamBodyGenerator(new BufferedInputStream(new ByteArrayInputStream(LARGE_IMAGE_BYTES), 400000));
    }

    @Test()
    public void testBufferSmallThanFileWithStreamBodyGenerator() throws Throwable {
        doTestWithInputStreamBodyGenerator(new BufferedInputStream(new ByteArrayInputStream(LARGE_IMAGE_BYTES)));
    }

    @Test()
    public void testDirectFileWithStreamBodyGenerator() throws Throwable {
        doTestWithInputStreamBodyGenerator(new ByteArrayInputStream(LARGE_IMAGE_BYTES));
    }

    private void doTestWithInputStreamBodyGenerator(InputStream is) throws Throwable {
        AsyncHttpClientConfig.Builder bc = httpClientBuilder();

        try (AsyncHttpClient c = getAsyncHttpClient(bc.build())) {

            RequestBuilder builder = new RequestBuilder("POST");
            builder.setUrl(getTargetUrl());
            builder.setBody(new InputStreamBodyGenerator(is));

            Request r = builder.build();

            final ListenableFuture<Response> responseFuture = c.executeRequest(r);
            waitForAndAssertResponse(responseFuture);
        }
    }

    protected AsyncHttpClientConfig.Builder httpClientBuilder() {
        return new AsyncHttpClientConfig.Builder()//
                .setAllowPoolingConnections(true)//
                .setMaxConnectionsPerHost(1)//
                .setMaxConnections(1)//
                .setConnectTimeout(1000)//
                .setRequestTimeout(1000).setFollowRedirect(true);
    }

    protected void waitForAndAssertResponse(ListenableFuture<Response> responseFuture) throws InterruptedException, java.util.concurrent.ExecutionException, IOException {
        Response response = responseFuture.get();
        if (500 == response.getStatusCode()) {
            StringBuilder sb = new StringBuilder();
            sb.append("==============\n");
            sb.append("500 response from call\n");
            sb.append("Headers:" + response.getHeaders() + "\n");
            sb.append("==============\n");
            log.debug(sb.toString());
            assertEquals(response.getStatusCode(), 500, "Should have 500 status code");
            assertTrue(response.getHeader("X-Exception").contains("invalid.chunk.length"), "Should have failed due to chunking");
            fail("HARD Failing the test due to provided InputStreamBodyGenerator, chunking incorrectly:" + response.getHeader("X-Exception"));
        } else {
            assertEquals(response.getResponseBodyAsBytes(), LARGE_IMAGE_BYTES);
        }
    }
}
