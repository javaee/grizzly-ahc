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

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

public interface Part {

    /**
     * Carriage return/linefeed as a byte array
     */
    byte[] CRLF_BYTES = "\r\n".getBytes(US_ASCII);

    /**
     * Content dispostion as a byte
     */
    byte QUOTE_BYTE = '\"';

    /**
     * Extra characters as a byte array
     */
    byte[] EXTRA_BYTES = "--".getBytes(US_ASCII);

    /**
     * Content dispostion as a byte array
     */
    byte[] CONTENT_DISPOSITION_BYTES = "Content-Disposition: ".getBytes(US_ASCII);

    /**
     * form-data as a byte array
     */
    byte[] FORM_DATA_DISPOSITION_TYPE_BYTES = "form-data".getBytes(US_ASCII);

    /**
     * name as a byte array
     */
    byte[] NAME_BYTES = "; name=".getBytes(US_ASCII);

    /**
     * Content type header as a byte array
     */
    byte[] CONTENT_TYPE_BYTES = "Content-Type: ".getBytes(US_ASCII);

    /**
     * Content charset as a byte array
     */
    byte[] CHARSET_BYTES = "; charset=".getBytes(US_ASCII);

    /**
     * Content type header as a byte array
     */
    byte[] CONTENT_TRANSFER_ENCODING_BYTES = "Content-Transfer-Encoding: ".getBytes(US_ASCII);

    /**
     * Content type header as a byte array
     */
    byte[] CONTENT_ID_BYTES = "Content-ID: ".getBytes(US_ASCII);

    /**
     * Return the name of this part.
     * 
     * @return The name.
     */
    String getName();

    /**
     * Returns the content type of this part.
     * 
     * @return the content type, or <code>null</code> to exclude the content type header
     */
    String getContentType();

    /**
     * Return the character encoding of this part.
     * 
     * @return the character encoding, or <code>null</code> to exclude the character encoding header
     */
    Charset getCharset();

    /**
     * Return the transfer encoding of this part.
     * 
     * @return the transfer encoding, or <code>null</code> to exclude the transfer encoding header
     */
    String getTransferEncoding();

    /**
     * Return the content ID of this part.
     * 
     * @return the content ID, or <code>null</code> to exclude the content ID header
     */
    String getContentId();

    /**
     * Gets the disposition-type to be used in Content-Disposition header
     * 
     * @return the disposition-type
     */
    String getDispositionType();

    /**
     * Write all the data to the output stream. If you override this method make sure to override #length() as well
     * 
     * @param out
     *            The output stream
     * @param boundary
     *            the boundary
     * @throws IOException
     *             If an IO problem occurs.
     */
    void write(OutputStream out, byte[] boundary) throws IOException;

    /**
     * Return the full length of all the data. If you override this method make sure to override #send(OutputStream) as well
     * 
     * @return long The length.
     */
    long length(byte[] boundary);

    long write(WritableByteChannel target, byte[] boundary) throws IOException;
}
