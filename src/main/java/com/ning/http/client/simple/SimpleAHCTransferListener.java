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

package com.ning.http.client.simple;

import com.ning.http.client.SimpleAsyncHttpClient;
import com.ning.http.client.uri.Uri;

/**
 * A simple transfer listener for use with the {@link SimpleAsyncHttpClient}.
 * <p/>
 * Note: This listener does not cover requests failing before a connection is
 * established. For error handling, see
 * {@link com.ning.http.client.SimpleAsyncHttpClient.Builder#setDefaultThrowableHandler(com.ning.http.client.ThrowableHandler)}
 *
 * @author Benjamin Hanzelmann
 */
public interface SimpleAHCTransferListener {

    /**
     * This method is called after the connection status is received.
     *
     * @param url        the url for the connection.
     * @param statusCode the received status code.
     * @param statusText the received status text.
     */
    void onStatus(Uri uri, int statusCode, String statusText);

    /**
     * This method is called after the response headers are received.
     *
     * @param uri        the uri
     * @param headers the received headers, never {@code null}.
     */
    void onHeaders(Uri uri, HeaderMap headers);

    /**
     * This method is called when bytes of the responses body are received.
     *
     * @param uri        the uri
     * @param amount  the number of transferred bytes so far.
     * @param current the number of transferred bytes since the last call to this
     *                method.
     * @param total   the total number of bytes to be transferred. This is taken
     *                from the Content-Length-header and may be unspecified (-1).
     */
    void onBytesReceived(Uri uri, long amount, long current, long total);

    /**
     * This method is called when bytes are sent.
     *
     * @param uri        the uri
     * @param amount  the number of transferred bytes so far.
     * @param current the number of transferred bytes since the last call to this
     *                method.
     * @param total   the total number of bytes to be transferred. This is taken
     *                from the Content-Length-header and may be unspecified (-1).
     */
    void onBytesSent(Uri uri, long amount, long current, long total);

    /**
     * This method is called when the request is completed.
     *
     * @param uri        the uri
     * @param statusCode the received status code.
     * @param statusText the received status text.
     */
    void onCompleted(Uri uri, int statusCode, String statusText);
}

