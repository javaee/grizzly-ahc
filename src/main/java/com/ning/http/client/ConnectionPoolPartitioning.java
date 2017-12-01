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

import com.ning.http.client.uri.Uri;
import com.ning.http.util.AsyncHttpProviderUtils;

public interface ConnectionPoolPartitioning {

    public class ProxyPartitionKey {
        private final String proxyUrl;
        private final String targetHostBaseUrl;

        public ProxyPartitionKey(String proxyUrl, String targetHostBaseUrl) {
            this.proxyUrl = proxyUrl;
            this.targetHostBaseUrl = targetHostBaseUrl;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((proxyUrl == null) ? 0 : proxyUrl.hashCode());
            result = prime * result + ((targetHostBaseUrl == null) ? 0 : targetHostBaseUrl.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof ProxyPartitionKey))
                return false;
            ProxyPartitionKey other = (ProxyPartitionKey) obj;
            if (proxyUrl == null) {
                if (other.proxyUrl != null)
                    return false;
            } else if (!proxyUrl.equals(other.proxyUrl))
                return false;
            if (targetHostBaseUrl == null) {
                if (other.targetHostBaseUrl != null)
                    return false;
            } else if (!targetHostBaseUrl.equals(other.targetHostBaseUrl))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return new StringBuilder()//
                    .append("ProxyPartitionKey(proxyUrl=").append(proxyUrl)//
                    .append(", targetHostBaseUrl=").append(targetHostBaseUrl)//
                    .toString();
        }
    }

    Object getPartitionKey(Uri uri, ProxyServer proxyServer);

    public enum PerHostConnectionPoolPartitioning implements ConnectionPoolPartitioning {

        INSTANCE;

        public Object getPartitionKey(Uri uri, ProxyServer proxyServer) {
            String targetHostBaseUrl = AsyncHttpProviderUtils.getBaseUrl(uri);
            return proxyServer != null ? new ProxyPartitionKey(proxyServer.getUrl(), targetHostBaseUrl) : targetHostBaseUrl;
        }
    }
}
