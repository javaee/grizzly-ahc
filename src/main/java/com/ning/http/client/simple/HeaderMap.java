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
 * Copyright (c) 2010 Sonatype, Inc. All rights reserved.
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

package com.ning.http.client.simple;

import com.ning.http.client.FluentCaseInsensitiveStringsMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A map containing headers with the sole purpose of being given to
 * {@link SimpleAHCTransferListener#onHeaders(String, HeaderMap)}.
 *
 * @author Benjamin Hanzelmann
 */
public class HeaderMap implements Map<String, List<String>> {

    private FluentCaseInsensitiveStringsMap headers;

    public HeaderMap(FluentCaseInsensitiveStringsMap headers) {
        this.headers = headers;
    }

    public Set<String> keySet() {
        return headers.keySet();
    }

    public Set<java.util.Map.Entry<String, List<String>>> entrySet() {
        return headers.entrySet();
    }

    public int size() {
        return headers.size();
    }

    public boolean isEmpty() {
        return headers.isEmpty();
    }

    public boolean containsKey(Object key) {
        return headers.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return headers.containsValue(value);
    }

    /**
     * @see FluentCaseInsensitiveStringsMap#getFirstValue(String)
     */
    public String getFirstValue(String key) {
        return headers.getFirstValue(key);
    }

    /**
     * @see FluentCaseInsensitiveStringsMap#getJoinedValue(String, String)
     */
    public String getJoinedValue(String key, String delimiter) {
        return headers.getJoinedValue(key, delimiter);
    }

    public List<String> get(Object key) {
        return headers.get(key);
    }

    /**
     * Only read access is supported.
     */
    public List<String> put(String key, List<String> value) {
        throw new UnsupportedOperationException("Only read access is supported.");
    }

    /**
     * Only read access is supported.
     */
    public List<String> remove(Object key) {
        throw new UnsupportedOperationException("Only read access is supported.");
    }

    /**
     * Only read access is supported.
     */
    public void putAll(Map<? extends String, ? extends List<String>> t) {
        throw new UnsupportedOperationException("Only read access is supported.");

    }

    /**
     * Only read access is supported.
     */
    public void clear() {
        throw new UnsupportedOperationException("Only read access is supported.");
    }

    /**
     * Only read access is supported.
     */
    public Collection<List<String>> values() {
        return headers.values();
    }

}
