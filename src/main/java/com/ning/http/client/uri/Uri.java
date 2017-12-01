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
package com.ning.http.client.uri;

import com.ning.http.util.MiscUtils;
import com.ning.http.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class Uri {

    public static Uri create(String originalUrl) {
        return create(null, originalUrl);
    }

    public static Uri create(Uri context, final String originalUrl) {
        UriParser parser = new UriParser();
        parser.parse(context, originalUrl);

        return new Uri(parser.scheme,//
                parser.userInfo,//
                parser.host,//
                parser.port,//
                parser.path,//
                parser.query);
    }

    private final String scheme;
    private final String userInfo;
    private final String host;
    private final int port;
    private final String query;
    private final String path;
    private String url;

    public Uri(String scheme,//
            String userInfo,//
            String host,//
            int port,//
            String path,//
            String query) {

        if (scheme == null)
            throw new NullPointerException("scheme");
        if (host == null)
            throw new NullPointerException("host");

        this.scheme = scheme;
        this.userInfo = userInfo;
        this.host = host;
        this.port = port;
        this.path = path;
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public String getPath() {
        return path;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public int getPort() {
        return port;
    }

    public String getScheme() {
        return scheme;
    }

    public String getHost() {
        return host;
    }

    public URI toJavaNetURI() throws URISyntaxException {
        return new URI(toUrl());
    }

    public String toUrl() {
        if (url == null) {
            StringBuilder sb = StringUtils.stringBuilder();
            sb.append(scheme).append("://");
            if (userInfo != null)
                sb.append(userInfo).append('@');
            sb.append(host);
            if (port != -1)
                sb.append(':').append(port);
            if (path != null)
                sb.append(path);
            if (query != null)
                sb.append('?').append(query);
            url = sb.toString();
            sb.setLength(0);
        }

        return url;
    }

    public String toRelativeUrl() {
        StringBuilder sb = StringUtils.stringBuilder();
        if (MiscUtils.isNonEmpty(path))
            sb.append(path);
        else
            sb.append('/');
        if (query != null)
            sb.append('?').append(query);

        return sb.toString();
    }

    @Override
    public String toString() {
        // for now, but might change
        return toUrl();
    }

    public Uri withNewScheme(String newScheme) {
        return new Uri(newScheme,//
                userInfo,//
                host,//
                port,//
                path,//
                query);
    }

    public Uri withNewQuery(String newQuery) {
        return new Uri(scheme,//
                userInfo,//
                host,//
                port,//
                path,//
                newQuery);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + port;
        result = prime * result + ((query == null) ? 0 : query.hashCode());
        result = prime * result + ((scheme == null) ? 0 : scheme.hashCode());
        result = prime * result + ((userInfo == null) ? 0 : userInfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Uri other = (Uri) obj;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (port != other.port)
            return false;
        if (query == null) {
            if (other.query != null)
                return false;
        } else if (!query.equals(other.query))
            return false;
        if (scheme == null) {
            if (other.scheme != null)
                return false;
        } else if (!scheme.equals(other.scheme))
            return false;
        if (userInfo == null) {
            if (other.userInfo != null)
                return false;
        } else if (!userInfo.equals(other.userInfo))
            return false;
        return true;
    }
}
