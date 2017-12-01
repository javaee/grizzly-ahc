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

import com.ning.http.client.uri.Uri;

import java.util.List;

/**
 * A class that represent the HTTP response' status line (code + text)
 */
public abstract class HttpResponseStatus {

    private final Uri uri;
    protected final AsyncHttpClientConfig config;

    public HttpResponseStatus(Uri uri, AsyncHttpClientConfig config) {
        this.uri = uri;
        this.config = config;
    }

    /**
     * Return the request {@link Uri}
     * 
     * @return the request {@link Uri}
     */
    public final Uri getUri() {
        return uri;
    }

    public AsyncHttpClientConfig getConfig() {
        return config;
    }

    /**
     * Prepare a {@link Response}
     *
     * @param headers   {@link HttpResponseHeaders}
     * @param bodyParts list of {@link HttpResponseBodyPart}
     * @return a {@link Response}
     */
    public abstract Response prepareResponse(HttpResponseHeaders headers, List<HttpResponseBodyPart> bodyParts);

    /**
     * Return the response status code
     * 
     * @return the response status code
     */
    public abstract int getStatusCode();

    /**
     * Return the response status text
     * 
     * @return the response status text
     */
    public abstract String getStatusText();

    /**
     * Protocol name from status line.
     * 
     * @return Protocol name.
     */
    public abstract String getProtocolName();

    /**
     * Protocol major version.
     * 
     * @return Major version.
     */
    public abstract int getProtocolMajorVersion();

    /**
     * Protocol minor version.
     * 
     * @return Minor version.
     */
    public abstract int getProtocolMinorVersion();

    /**
     * Full protocol name + version
     * 
     * @return protocol name + version
     */
    public abstract String getProtocolText();
}
