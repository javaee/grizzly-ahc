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

/**
 * An asynchronous handler or callback which gets invoked as soon as some data is available when
 * processing an asynchronous response.<br/>
 * Callback methods get invoked in the following order:
 * <ol>
 * <li>{@link #onStatusReceived(HttpResponseStatus)},</li>
 * <li>{@link #onHeadersReceived(HttpResponseHeaders)},</li>
 * <li>{@link #onBodyPartReceived(HttpResponseBodyPart)}, which could be invoked multiple times,</li>
 * <li>{@link #onCompleted()}, once the response has been fully read.</li>
 * </ol>
 * <p/>
 * Returning a {@link AsyncHandler.STATE#ABORT} from any of those callback methods will interrupt asynchronous response
 * processing, after that only {@link #onCompleted()} is going to be called.
 * <p/>
 * <p/>
 * AsyncHandler aren't thread safe, hence you should avoid re-using the same instance when doing concurrent requests.
 * As an exmaple, the following may produce unexpected results:
 * <blockquote><pre>
 *   AsyncHandler ah = new AsyncHandler() {....};
 *   AsyncHttpClient client = new AsyncHttpClient();
 *   client.prepareGet("http://...").execute(ah);
 *   client.prepareGet("http://...").execute(ah);
 * </pre></blockquote>
 * It is recommended to create a new instance instead.
 *
 * @param <T> Type of object returned by the {@link java.util.concurrent.Future#get}
 */
public interface AsyncHandler<T> {

    public static enum STATE {

        /**
         * Stop the processing.
         */
        ABORT,
        /**
         * Continue the processing
         */
        CONTINUE,
        /**
         * Upgrade the protocol.
         */
        UPGRADE
    }

    /**
     * Invoked when an unexpected exception occurs during the processing of the response. The exception may have been
     * produced by implementation of onXXXReceived method invocation.
     *
     * @param t a {@link Throwable}
     */
    void onThrowable(Throwable t);

    /**
     * Invoked as soon as some response body part are received. Could be invoked many times.
     *
     * @param bodyPart response's body part.
     * @return a {@link STATE} telling to CONTINUE or ABORT the current processing.
     * @throws Exception if something wrong happens
     */
    STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception;

    /**
     * Invoked as soon as the HTTP status line has been received
     *
     * @param responseStatus the status code and test of the response
     * @return a {@link STATE} telling to CONTINUE or ABORT the current processing.
     * @throws Exception if something wrong happens
     */
    STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception;

    /**
     * Invoked as soon as the HTTP headers has been received. Can potentially be invoked more than once if a broken server
     * sent trailing headers.
     *
     * @param headers the HTTP headers.
     * @return a {@link STATE} telling to CONTINUE or ABORT the current processing.
     * @throws Exception if something wrong happens
     */
    STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception;

    /**
     * Invoked once the HTTP response processing is finished.
     * <p/>
     * <p/>
     * Gets always invoked as last callback method.
     *
     * @return T Value that will be returned by the associated {@link java.util.concurrent.Future}
     * @throws Exception if something wrong happens
     */
    T onCompleted() throws Exception;
}
