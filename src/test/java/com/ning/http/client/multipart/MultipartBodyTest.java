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
package com.ning.http.client.multipart;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.annotations.Test;

import com.ning.http.client.FluentCaseInsensitiveStringsMap;

public class MultipartBodyTest {

    private static final List<Part> PARTS = new ArrayList<>();

    static {
        try {
            PARTS.add(new FilePart("filePart", getTestfile()));
        } catch (URISyntaxException e) {
            throw new ExceptionInInitializerError(e);
        }
        PARTS.add(new ByteArrayPart("baPart", "testMultiPart".getBytes(UTF_8), "application/test", UTF_8, "fileName"));
        PARTS.add(new StringPart("stringPart", "testString"));
    }

    private static File getTestfile() throws URISyntaxException {
        final ClassLoader cl = MultipartBodyTest.class.getClassLoader();
        final URL url = cl.getResource("textfile.txt");
        assertNotNull(url);
        return new File(url.toURI());
    }

    private static long MAX_MULTIPART_CONTENT_LENGTH_ESTIMATE;

    static {
        try (MultipartBody dummyBody = buildMultipart()) {
            // separator is random
            MAX_MULTIPART_CONTENT_LENGTH_ESTIMATE = dummyBody.getContentLength() + 100;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static MultipartBody buildMultipart() {
        return MultipartUtils.newMultipartBody(PARTS, new FluentCaseInsensitiveStringsMap());
    }

    @Test
    public void transferWithCopy() throws Exception {
        for (int bufferLength = 1; bufferLength < MAX_MULTIPART_CONTENT_LENGTH_ESTIMATE + 1; bufferLength++) {
            try (MultipartBody multipartBody = buildMultipart()) {
                long tranferred = transferWithCopy(multipartBody, bufferLength);
                assertEquals(tranferred, multipartBody.getContentLength());
            }
        }
    }

    @Test
    public void transferZeroCopy() throws Exception {
        for (int bufferLength = 1; bufferLength < MAX_MULTIPART_CONTENT_LENGTH_ESTIMATE + 1; bufferLength++) {
            try (MultipartBody multipartBody = buildMultipart()) {
                long tranferred = transferZeroCopy(multipartBody, bufferLength);
                assertEquals(tranferred, multipartBody.getContentLength());
            }
        }
    }

    private static long transferWithCopy(MultipartBody multipartBody, int bufferSize) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        long totalBytes = 0;
        while (true) {
            long readBytes = multipartBody.read(buffer);
            if (readBytes < 0) {
                break;
            }
            buffer.clear();
            totalBytes += readBytes;
        }
        return totalBytes;
    }

    private static long transferZeroCopy(MultipartBody multipartBody, int bufferSize) throws IOException {

        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        final AtomicLong transferred = new AtomicLong();

        WritableByteChannel mockChannel = new WritableByteChannel() {
            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public void close() throws IOException {
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                int written = src.remaining();
                transferred.set(transferred.get() + written);
                src.position(src.limit());
                return written;
            }
        };

        while (transferred.get() < multipartBody.getContentLength()) {
            multipartBody.transferTo(0, mockChannel);
            buffer.clear();
        }
        return transferred.get();
    }
}
