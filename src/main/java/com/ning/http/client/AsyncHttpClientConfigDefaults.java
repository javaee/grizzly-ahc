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
package com.ning.http.client;

import static com.ning.http.util.MiscUtils.getBoolean;

public final class AsyncHttpClientConfigDefaults {

    private AsyncHttpClientConfigDefaults() {
    }

    public static final String ASYNC_CLIENT = AsyncHttpClientConfig.class.getName() + ".";

    public static int defaultMaxConnections() {
        return Integer.getInteger(ASYNC_CLIENT + "maxConnections", -1);
    }

    public static int defaultMaxConnectionsPerHost() {
        return Integer.getInteger(ASYNC_CLIENT + "maxConnectionsPerHost", -1);
    }

    public static int defaultConnectTimeout() {
        return Integer.getInteger(ASYNC_CLIENT + "connectTimeout", 5 * 1000);
    }

    public static int defaultPooledConnectionIdleTimeout() {
        return Integer.getInteger(ASYNC_CLIENT + "pooledConnectionIdleTimeout", 60 * 1000);
    }

    public static int defaultReadTimeout() {
        return Integer.getInteger(ASYNC_CLIENT + "readTimeout", 60 * 1000);
    }

    public static int defaultRequestTimeout() {
        return Integer.getInteger(ASYNC_CLIENT + "requestTimeout", 60 * 1000);
    }

    public static int defaultWebSocketTimeout() {
        return Integer.getInteger(ASYNC_CLIENT + "webSocketTimeout", 15 * 60 * 1000);
    }

    public static int defaultConnectionTTL() {
        return Integer.getInteger(ASYNC_CLIENT + "connectionTTL", -1);
    }

    public static boolean defaultFollowRedirect() {
        return Boolean.getBoolean(ASYNC_CLIENT + "followRedirect");
    }

    public static int defaultMaxRedirects() {
        return Integer.getInteger(ASYNC_CLIENT + "maxRedirects", 5);
    }

    public static boolean defaultCompressionEnforced() {
        return getBoolean(ASYNC_CLIENT + "compressionEnforced", false);
    }

    public static String defaultUserAgent() {
        return System.getProperty(ASYNC_CLIENT + "userAgent", "AHC/1.0");
    }

    public static int defaultIoThreadMultiplier() {
        return Integer.getInteger(ASYNC_CLIENT + "ioThreadMultiplier", 2);
    }

    public static boolean defaultUseProxySelector() {
        return Boolean.getBoolean(ASYNC_CLIENT + "useProxySelector");
    }

    public static boolean defaultUseProxyProperties() {
        return Boolean.getBoolean(ASYNC_CLIENT + "useProxyProperties");
    }

    public static boolean defaultStrict302Handling() {
        return Boolean.getBoolean(ASYNC_CLIENT + "strict302Handling");
    }

    public static boolean defaultAllowPoolingConnections() {
        return getBoolean(ASYNC_CLIENT + "allowPoolingConnections", true);
    }

    public static boolean defaultUseRelativeURIsWithConnectProxies() {
        return getBoolean(ASYNC_CLIENT + "useRelativeURIsWithConnectProxies", true);
    }

    public static int defaultMaxRequestRetry() {
        return Integer.getInteger(ASYNC_CLIENT + "maxRequestRetry", 5);
    }

    public static boolean defaultAllowPoolingSslConnections() {
        return getBoolean(ASYNC_CLIENT + "allowPoolingSslConnections", true);
    }

    public static boolean defaultDisableUrlEncodingForBoundRequests() {
        return Boolean.getBoolean(ASYNC_CLIENT + "disableUrlEncodingForBoundRequests");
    }

    public static boolean defaultAcceptAnyCertificate() {
        return getBoolean(ASYNC_CLIENT + "acceptAnyCertificate", false);
    }

    public static Integer defaultSslSessionCacheSize() {
        return Integer.getInteger(ASYNC_CLIENT + "sslSessionCacheSize");
    }

    public static Integer defaultSslSessionTimeout() {
        return Integer.getInteger(ASYNC_CLIENT + "sslSessionTimeout");
    }

    public static String[] defaultEnabledProtocols() {
        return new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" };
    }
}
