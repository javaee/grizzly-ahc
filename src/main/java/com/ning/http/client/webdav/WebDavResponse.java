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
package com.ning.http.client.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.w3c.dom.Document;

import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Response;
import com.ning.http.client.cookie.Cookie;
import com.ning.http.client.uri.Uri;

/**
 * Customized {@link Response} which add support for getting the response's body as an XML document (@link WebDavResponse#getBodyAsXML}
 */
public class WebDavResponse implements Response {

    private final Response response;
    private final Document document;

    public WebDavResponse(Response response, Document document) {
        this.response = response;
        this.document = document;
    }

    public int getStatusCode() {
        return response.getStatusCode();
    }

    public String getStatusText() {
        return response.getStatusText();
    }

    @Override
    public byte[] getResponseBodyAsBytes() throws IOException {
        return response.getResponseBodyAsBytes();
    }

    public ByteBuffer getResponseBodyAsByteBuffer() throws IOException {
        return response.getResponseBodyAsByteBuffer();
    }

    public InputStream getResponseBodyAsStream() throws IOException {
        return response.getResponseBodyAsStream();
    }

    public String getResponseBodyExcerpt(int maxLength) throws IOException {
        return response.getResponseBodyExcerpt(maxLength);
    }

    public String getResponseBodyExcerpt(int maxLength, String charset) throws IOException {
        return response.getResponseBodyExcerpt(maxLength, charset);
    }

    public String getResponseBody() throws IOException {
        return response.getResponseBody();
    }

    public String getResponseBody(String charset) throws IOException {
        return response.getResponseBody(charset);
    }

    public Uri getUri() {
        return response.getUri();
    }

    public String getContentType() {
        return response.getContentType();
    }

    public String getHeader(String name) {
        return response.getHeader(name);
    }

    public List<String> getHeaders(String name) {
        return response.getHeaders(name);
    }

    public FluentCaseInsensitiveStringsMap getHeaders() {
        return response.getHeaders();
    }

    public boolean isRedirected() {
        return response.isRedirected();
    }

    public List<Cookie> getCookies() {
        return response.getCookies();
    }

    public boolean hasResponseStatus() {
        return response.hasResponseStatus();
    }

    public boolean hasResponseHeaders() {
        return response.hasResponseHeaders();
    }

    public boolean hasResponseBody() {
        return response.hasResponseBody();
    }

    public Document getBodyAsXML() {
        return document;
    }
}
