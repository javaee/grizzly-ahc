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
 * You may obtain a copy of the Apache License Version 2.0 at
 *     http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.ning.http.client;

import static com.ning.http.util.MiscUtils.isNonEmpty;

import com.ning.http.client.cookie.Cookie;
import com.ning.http.client.uri.Uri;
import com.ning.http.util.AsyncHttpProviderUtils;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

public abstract class ResponseBase implements Response {

    protected final HttpResponseStatus status;
    protected final HttpResponseHeaders headers;
    protected final List<HttpResponseBodyPart> bodyParts;
    private List<Cookie> cookies;

    protected ResponseBase(HttpResponseStatus status, HttpResponseHeaders headers, List<HttpResponseBodyPart> bodyParts) {
        this.bodyParts = bodyParts;
        this.headers = headers;
        this.status = status;
    }

    protected abstract List<Cookie> buildCookies();

    protected Charset calculateCharset(String charset) {

        if (charset == null) {
            String contentType = getContentType();
            if (contentType != null)
                charset = AsyncHttpProviderUtils.parseCharset(contentType); // parseCharset can return null
        }
        return charset != null ? Charset.forName(charset) : AsyncHttpProviderUtils.DEFAULT_CHARSET;
    }

    @Override
    public final int getStatusCode() {
        return status.getStatusCode();
    }

    @Override
    public final String getStatusText() {
        return status.getStatusText();
    }

    @Override
    public final Uri getUri() {
        return status.getUri();
    }

    @Override
    public final String getContentType() {
        return headers != null ? getHeader("Content-Type") : null;
    }

    @Override
    public final String getHeader(String name) {
        return headers != null ? getHeaders().getFirstValue(name) : null;
    }

    @Override
    public final List<String> getHeaders(String name) {
        return headers != null ? getHeaders().get(name) : Collections.<String> emptyList();
    }

    @Override
    public final FluentCaseInsensitiveStringsMap getHeaders() {
        return headers != null ? headers.getHeaders() : new FluentCaseInsensitiveStringsMap();
    }

    @Override
    public final boolean isRedirected() {
        switch (status.getStatusCode()) {
        case 301:
        case 302:
        case 303:
        case 307:
        case 308:
            return true;
        default:
            return false;
        }
    }

    @Override
    public List<Cookie> getCookies() {
        if (cookies == null)
            cookies = headers != null ? buildCookies() : Collections.<Cookie> emptyList();
        return cookies;

    }

    @Override
    public boolean hasResponseStatus() {
        return status != null;
    }

    @Override
    public boolean hasResponseHeaders() {
        return headers != null && isNonEmpty(headers.getHeaders());
    }

    @Override
    public boolean hasResponseBody() {
        return isNonEmpty(bodyParts);
    }
}
