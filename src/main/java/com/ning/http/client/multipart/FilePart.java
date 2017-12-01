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
 * Copyright (c) 2014 AsyncHttpClient Project. All rights reserved.
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
package com.ning.http.client.multipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

public class FilePart extends AbstractFilePart {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePart.class);

    private final File file;

    public FilePart(String name, File file) {
        this(name, file, null);
    }

    public FilePart(String name, File file, String contentType) {
        this(name, file, contentType, null);
    }

    public FilePart(String name, File file, String contentType, Charset charset) {
        this(name, file, contentType, charset, null);
    }

    public FilePart(String name, File file, String contentType, Charset charset, String fileName) {
        this(name, file, contentType, charset, fileName, null);
    }

    public FilePart(String name, File file, String contentType, Charset charset, String fileName, String contentId) {
        this(name, file, contentType, charset, fileName, contentId, null);
    }

    public FilePart(String name, File file, String contentType, Charset charset, String fileName, String contentId, String transferEncoding) {
        super(name, contentType, charset, contentId, transferEncoding);
        if (file == null)
            throw new NullPointerException("file");
        if (!file.isFile())
            throw new IllegalArgumentException("File is not a normal file " + file.getAbsolutePath());
        if (!file.canRead())
            throw new IllegalArgumentException("File is not readable " + file.getAbsolutePath());
        this.file = file;
        setFileName(fileName != null ? fileName : file.getName());
    }
    
    @Override
    protected void sendData(OutputStream out) throws IOException {
        if (getDataLength() == 0) {

            // this file contains no data, so there is nothing to send.
            // we don't want to create a zero length buffer as this will
            // cause an infinite loop when reading.
            return;
        }

        byte[] tmp = new byte[4096];
        InputStream instream = new FileInputStream(file);
        try {
            int len;
            while ((len = instream.read(tmp)) >= 0) {
                out.write(tmp, 0, len);
            }
        } finally {
            // we're done with the stream, close it
            instream.close();
        }
    }

    @Override
    protected long getDataLength() {
        return file.length();
    }

    public File getFile() {
        return file;
    }

    @Override
    public long write(WritableByteChannel target, byte[] boundary) throws IOException {
        FilePartStallHandler handler = new FilePartStallHandler(getStalledTime(), this);

        handler.start();

        int length = 0;

        length += MultipartUtils.writeBytesToChannel(target, generateFileStart(boundary));

        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel fc = raf.getChannel();

        long actualFileLength = file.length();
        long transferredFileBytes = 0;
        // FIXME why sync?
        try {
            synchronized (fc) {
                while (transferredFileBytes != actualFileLength) {
                    long written = 0;
                    
                    if (handler.isFailed()) {
                        LOGGER.debug("Stalled error");
                        throw new FileUploadStalledException();
                    }
                    try {
                        written = fc.transferTo(transferredFileBytes, actualFileLength, target);
                        if (written == 0) {
                            LOGGER.info("Waiting for writing...");
                            try {
                                fc.wait(50);
                            } catch (InterruptedException e) {
                                LOGGER.trace(e.getMessage(), e);
                            }
                        } else {
                            handler.writeHappened();
                        }
                    } catch (IOException ex) {
                        String message = ex.getMessage();

                        // http://bugs.sun.com/view_bug.do?bug_id=5103988
                        if (message != null && message.equalsIgnoreCase("Resource temporarily unavailable")) {
                            try {
                                fc.wait(1000);
                            } catch (InterruptedException e) {
                                LOGGER.trace(e.getMessage(), e);
                            }
                            LOGGER.warn("Experiencing NIO issue http://bugs.sun.com/view_bug.do?bug_id=5103988. Retrying");
                            continue;
                        } else {
                            throw ex;
                        }
                    }
                    transferredFileBytes += written;
                }
            }
        } finally {
            handler.completed();
            raf.close();
        }

        length += transferredFileBytes;
        length += MultipartUtils.writeBytesToChannel(target, generateFileEnd());

        return length;
    }
}
