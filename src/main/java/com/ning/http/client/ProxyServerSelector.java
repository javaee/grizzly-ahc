/*
 * Copyright (c) 2017-2018 Oracle and/or its affiliates. All rights reserved.
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

import com.ning.http.client.uri.Uri;

/**
 * Selector for a proxy server
 */
public interface ProxyServerSelector {

    /**
     * Select a proxy server to use for the given URI.
     *
     * @param uri The URI to select a proxy server for.
     * @return The proxy server to use, if any.  May return null.
     */
    ProxyServer select(Uri uri);

    /**
     * A selector that always selects no proxy.
     */
    static final ProxyServerSelector NO_PROXY_SELECTOR = new ProxyServerSelector() {
        public ProxyServer select(Uri uri) {
            return null;
        }
    };
}
