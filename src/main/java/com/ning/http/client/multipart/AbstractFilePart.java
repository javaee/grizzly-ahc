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

import static java.nio.charset.StandardCharsets.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * This class is an adaptation of the Apache HttpClient implementation
 * 
 * @link http://hc.apache.org/httpclient-3.x/
 */
public abstract class AbstractFilePart extends PartBase {

    /**
     * Default content encoding of file attachments.
     */
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /**
     * Default transfer encoding of file attachments.
     */
    public static final String DEFAULT_TRANSFER_ENCODING = "binary";

    /**
     * Attachment's file name as a byte array
     */
    private static final byte[] FILE_NAME_BYTES = "; filename=".getBytes(US_ASCII);

    private long stalledTime = -1L;

    private String fileName;

    /**
     * FilePart Constructor.
     * 
     * @param name
     *            the name for this part
     * @param partSource
     *            the source for this part
     * @param contentType
     *            the content type for this part, if <code>null</code> the {@link #DEFAULT_CONTENT_TYPE default} is used
     * @param charset
     *            the charset encoding for this part
     */
    public AbstractFilePart(String name, String contentType, Charset charset, String contentId, String transferEncoding) {
        super(name,//
                contentType == null ? DEFAULT_CONTENT_TYPE : contentType,//
                charset,//
                contentId, //
                transferEncoding == null ? DEFAULT_TRANSFER_ENCODING : transferEncoding);
    }

    protected void visitDispositionHeader(PartVisitor visitor) throws IOException {
        super.visitDispositionHeader(visitor);
        if (fileName != null) {
            visitor.withBytes(FILE_NAME_BYTES);
            visitor.withByte(QUOTE_BYTE);
            visitor.withBytes(fileName.getBytes(getCharset() != null ? getCharset(): US_ASCII));
            visitor.withByte(QUOTE_BYTE);
        }
    }

    protected byte[] generateFileStart(byte[] boundary) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamPartVisitor visitor = new OutputStreamPartVisitor(out);
        visitStart(visitor, boundary);
        visitDispositionHeader(visitor);
        visitContentTypeHeader(visitor);
        visitTransferEncodingHeader(visitor);
        visitContentIdHeader(visitor);
        visitCustomHeaders(visitor);
        visitEndOfHeaders(visitor);

        return out.toByteArray();
    }

    protected byte[] generateFileEnd() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamPartVisitor visitor = new OutputStreamPartVisitor(out);
        visitEnd(visitor);
        return out.toByteArray();
    }

    public void setStalledTime(long ms) {
        stalledTime = ms;
    }

    public long getStalledTime() {
        return stalledTime;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        return new StringBuilder()//
                .append(super.toString())//
                .append(" filename=").append(fileName)//
                .toString();
    }
}
