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

import static java.nio.charset.StandardCharsets.*;
import static org.testng.FileAssert.fail;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import com.ning.http.client.multipart.FilePart;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public abstract class FilePartLargeFileTest extends AbstractBasicTest {

    @Test(groups = { "standalone", "default_provider" }, enabled = true)
    public void testPutImageFile() throws Exception {
        File largeFile = getTestFile();
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setRequestTimeout(100 * 6000).build();
        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            BoundRequestBuilder rb = client.preparePut(getTargetUrl());

            rb.addBodyPart(new FilePart("test", largeFile, "application/octet-stream", UTF_8));

            Response response = rb.execute().get();
            Assert.assertEquals(200, response.getStatusCode());
        }
    }

    @Test(groups = { "standalone", "default_provider" }, enabled = true)
    public void testPutLargeTextFile() throws Exception {
        byte[] bytes = "RatherLargeFileRatherLargeFileRatherLargeFileRatherLargeFile".getBytes(UTF_16);
        long repeats = (1024 * 1024 / bytes.length) + 1;
        File largeFile = createTempFile(bytes, (int) repeats);

        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            BoundRequestBuilder rb = client.preparePut(getTargetUrl());

            rb.addBodyPart(new FilePart("test", largeFile, "application/octet-stream", UTF_8));

            Response response = rb.execute().get();
            Assert.assertEquals(200, response.getStatusCode());
        }
    }

    private static File getTestFile() {
        String testResource1 = "300k.png";

        File testResource1File = null;
        try {
            ClassLoader cl = ChunkingTest.class.getClassLoader();
            URL url = cl.getResource(testResource1);
            testResource1File = new File(url.toURI());
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            fail("unable to find " + testResource1);
        }

        return testResource1File;
    }

    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new AbstractHandler() {

            public void handle(String arg0, Request arg1, HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

                ServletInputStream in = req.getInputStream();
                byte[] b = new byte[8192];

                int count = -1;
                int total = 0;
                while ((count = in.read(b)) != -1) {
                    b = new byte[8192];
                    total += count;
                }
                System.err.println("consumed " + total + " bytes.");

                resp.setStatus(200);
                resp.addHeader("X-TRANFERED", String.valueOf(total));
                resp.getOutputStream().flush();
                resp.getOutputStream().close();

                arg1.setHandled(true);

            }
        };
    }

    private static final File TMP = new File(System.getProperty("java.io.tmpdir"), "ahc-tests-" + UUID.randomUUID().toString().substring(0, 8));

    public static File createTempFile(byte[] pattern, int repeat) throws IOException {
        TMP.mkdirs();
        TMP.deleteOnExit();
        File tmpFile = File.createTempFile("tmpfile-", ".data", TMP);
        tmpFile.deleteOnExit();
        write(pattern, repeat, tmpFile);

        return tmpFile;
    }

    public static void write(byte[] pattern, int repeat, File file) throws IOException {
        file.deleteOnExit();
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {
            for (int i = 0; i < repeat; i++) {
                out.write(pattern);
            }
        }
    }

}
