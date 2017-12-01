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
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ning.http.client;

import com.ning.http.client.cookie.Cookie;
import com.ning.http.client.multipart.Part;
import com.ning.http.util.UriEncoder;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Builder for a {@link Request}.
 * Warning: mutable and not thread-safe! Beware that it holds a reference on the Request instance it builds,
 * so modifying the builder will modify the request even after it has been built.
 */
public class RequestBuilder extends RequestBuilderBase<RequestBuilder> {

    public RequestBuilder() {
        super(RequestBuilder.class, "GET", false);
    }

    public RequestBuilder(String method) {
        super(RequestBuilder.class, method, false);
    }

    public RequestBuilder(String method, boolean disableUrlEncoding) {
        super(RequestBuilder.class, method, disableUrlEncoding);
    }

    public RequestBuilder(String method, UriEncoder uriEncoder) {
        super(RequestBuilder.class, method, uriEncoder);
    }

    public RequestBuilder(Request prototype) {
        super(RequestBuilder.class, prototype);
    }

    public RequestBuilder(Request prototype, UriEncoder uriEncoder) {
        super(RequestBuilder.class, prototype, uriEncoder);
    }
    
    // Note: For now we keep the delegates in place even though they are not needed
    //       since otherwise Clojure (and maybe other languages) won't be able to
    //       access these methods - see Clojure tickets 126 and 259

    @Override
    public RequestBuilder addBodyPart(Part part) {
        return super.addBodyPart(part);
    }

    @Override
    public RequestBuilder addCookie(Cookie cookie) {
        return super.addCookie(cookie);
    }

    @Override
    public RequestBuilder addHeader(String name, String value) {
        return super.addHeader(name, value);
    }

    @Override
    public RequestBuilder addFormParam(String key, String value) {
        return super.addFormParam(key, value);
    }

    @Override
    public RequestBuilder addQueryParam(String name, String value) {
        return super.addQueryParam(name, value);
    }

    @Override
    public RequestBuilder addQueryParams(List<Param> queryParams) {
        return super.addQueryParams(queryParams);
    }

    @Override
    public RequestBuilder setQueryParams(List<Param> params) {
        return super.setQueryParams(params);
    }

    @Override
    public RequestBuilder setQueryParams(Map<String, List<String>> params) {
        return super.setQueryParams(params);
    }

    @Override
    public Request build() {
        return super.build();
    }

    @Override
    public RequestBuilder setBody(byte[] data) {
        return super.setBody(data);
    }

    /**
     * Deprecated - Use setBody(new InputStreamBodyGenerator(inputStream)).
     *
     * @param stream - An {@link InputStream}
     * @return a {@link RequestBuilder}
     * @throws IllegalArgumentException
     * @see #setBody(BodyGenerator) InputStreamBodyGenerator(inputStream)
     * @see com.ning.http.client.generators.InputStreamBodyGenerator
     * @deprecated {@link #setBody(BodyGenerator)} setBody(new InputStreamBodyGenerator(inputStream))
     */
    @Override
    @Deprecated
    public RequestBuilder setBody(InputStream stream) {
        return super.setBody(stream);
    }

    @Override
    public RequestBuilder setBody(String data) {
        return super.setBody(data);
    }

    @Override
    public RequestBuilder setHeader(String name, String value) {
        return super.setHeader(name, value);
    }

    @Override
    public RequestBuilder setHeaders(FluentCaseInsensitiveStringsMap headers) {
        return super.setHeaders(headers);
    }

    @Override
    public RequestBuilder setHeaders(Map<String, Collection<String>> headers) {
        return super.setHeaders(headers);
    }

    @Override
    public RequestBuilder setFormParams(List<Param> params) {
        return super.setFormParams(params);
    }

    @Override
    public RequestBuilder setFormParams(Map<String, List<String>> params) {
        return super.setFormParams(params);
    }

    @Override
    public RequestBuilder setMethod(String method) {
        return super.setMethod(method);
    }

    @Override
    public RequestBuilder setUrl(String url) {
        return super.setUrl(url);
    }

    @Override
    public RequestBuilder setProxyServer(ProxyServer proxyServer) {
        return super.setProxyServer(proxyServer);
    }

    @Override
    public RequestBuilder setVirtualHost(String virtualHost) {
        return super.setVirtualHost(virtualHost);
    }

    @Override
    public RequestBuilder setFollowRedirects(boolean followRedirects) {
        return super.setFollowRedirects(followRedirects);
    }

    @Override
    public RequestBuilder addOrReplaceCookie(Cookie c) {
        return super.addOrReplaceCookie(c);
    }
}
