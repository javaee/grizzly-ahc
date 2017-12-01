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
 *
 */
package com.ning.http.client;

import com.ning.http.client.cookie.Cookie;
import com.ning.http.client.multipart.Part;
import com.ning.http.client.uri.Uri;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Collection;
import java.util.List;

/**
 * The Request class can be used to construct HTTP request:
 * <blockquote><pre>
 *   Request r = new RequestBuilder().setUrl("url")
 *                      .setRealm((new Realm.RealmBuilder()).setPrincipal(user)
 *                      .setPassword(admin)
 *                      .setRealmName("MyRealm")
 *                      .setScheme(Realm.AuthScheme.DIGEST).build());
 * </pre></blockquote>
 */
public interface Request {

    /**
     * Return the request's method name (GET, POST, etc.)
     *
     * @return the request's method name (GET, POST, etc.)
     */
    String getMethod();

    Uri getUri();

    String getUrl();

    /**
     * Return the InetAddress to override
     *
     * @return the InetAddress
     */
    InetAddress getInetAddress();

    InetAddress getLocalAddress();

    /**
     * Return the current set of Headers.
     *
     * @return a {@link FluentCaseInsensitiveStringsMap} contains headers.
     */
    FluentCaseInsensitiveStringsMap getHeaders();

    /**
     * Return Coookie.
     *
     * @return an unmodifiable Collection of Cookies
     */
    Collection<Cookie> getCookies();

    /**
     * Return the current request's body as a byte array
     *
     * @return a byte array of the current request's body.
     */
    byte[] getByteData();

    /**
     * @return the current request's body as a composite of byte arrays
     */
    List<byte[]> getCompositeByteData();
    
    /**
     * Return the current request's body as a string
     *
     * @return an String representation of the current request's body.
     */
    String getStringData();

    /**
     * Return the current request's body as an InputStream
     *
     * @return an InputStream representation of the current request's body.
     */
    InputStream getStreamData();

    /**
     * Return the current request's body generator.
     *
     * @return A generator for the request body.
     */
    BodyGenerator getBodyGenerator();

    /**
     * Return the current size of the content-lenght header based on the body's size.
     *
     * @return the current size of the content-lenght header based on the body's size.
     */
    long getContentLength();

    /**
     * Return the current form parameters.
     *
     * @return a {@link List<Param>} of parameters.
     */
    List<Param> getFormParams();

    /**
     * Return the current {@link Part}s
     *
     * @return the current {@link Part}s
     */
    List<Part> getParts();

    /**
     * Return the virtual host value.
     *
     * @return the virtual host value.
     */
    String getVirtualHost();

    /**
     * Return the query params.
     *
     * @return {@link List<Param>} of query string
     */
    List<Param> getQueryParams();

    /**
     * Return the {@link ProxyServer}
     *
     * @return the {@link ProxyServer}
     */
    ProxyServer getProxyServer();

    /**
     * Return the {@link Realm}
     *
     * @return the {@link Realm}
     */
    Realm getRealm();

    /**
     * Return the {@link File} to upload.
     *
     * @return the {@link File} to upload.
     */
    File getFile();

    /**
     * Return follow redirect
     *
     * @return the <tt>TRUE></tt> to follow redirect, FALSE, if NOT to follow, whatever the client config.
     * Return null if not set.
     */
    Boolean getFollowRedirect();

    /**
     * Overrides the config default value
     * @return the request timeout
     */
    int getRequestTimeout();

    /**
     * Return the HTTP Range header value, or
     *
     * @return the range header value, or 0 is not set.
     */
    long getRangeOffset();

    /**
     * Return the encoding value used when encoding the request's body.
     *
     * @return the encoding value used when encoding the request's body.
     */
    String getBodyEncoding();

    ConnectionPoolPartitioning getConnectionPoolPartitioning();

    NameResolver getNameResolver();
}
