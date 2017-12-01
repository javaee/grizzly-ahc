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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AsyncHandler} augmented with an {@link #onCompleted(Response)} convenience method which gets called
 * when the {@link Response} processing is finished.  This class also implement the {@link ProgressAsyncHandler} callback,
 * all doing nothing except returning {@link com.ning.http.client.AsyncHandler.STATE#CONTINUE}
 *
 * @param <T> Type of the value that will be returned by the associated {@link java.util.concurrent.Future}
 */
public abstract class AsyncCompletionHandler<T> implements AsyncHandler<T>, ProgressAsyncHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncCompletionHandler.class);
    private final Response.ResponseBuilder builder = new Response.ResponseBuilder();

    @Override
    public STATE onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
        builder.accumulate(content);
        return STATE.CONTINUE;
    }

    @Override
    public STATE onStatusReceived(final HttpResponseStatus status) throws Exception {
        builder.reset();
        builder.accumulate(status);
        return STATE.CONTINUE;
    }

    @Override
    public STATE onHeadersReceived(final HttpResponseHeaders headers) throws Exception {
        builder.accumulate(headers);
        return STATE.CONTINUE;
    }

    @Override
    public final T onCompleted() throws Exception {
        return onCompleted(builder.build());
    }

    @Override
    public void onThrowable(Throwable t) {
        LOGGER.debug(t.getMessage(), t);
    }

    /**
     * Invoked once the HTTP response processing is finished.
     *
     * @param response The {@link Response}
     * @return T Value that will be returned by the associated {@link java.util.concurrent.Future}
     * @throws Exception if something wrong happens
     */
    abstract public T onCompleted(Response response) throws Exception;

    /**
     * Invoked when the content (a {@link java.io.File}, {@link String} or {@link java.io.FileInputStream} has been fully
     * written on the I/O socket.
     *
     * @return a {@link com.ning.http.client.AsyncHandler.STATE} telling to CONTINUE or ABORT the current processing.
     */
    @Override
    public STATE onHeaderWriteCompleted() {
        return STATE.CONTINUE;
    }

    /**
     * Invoked when the content (a {@link java.io.File}, {@link String} or {@link java.io.FileInputStream} has been fully
     * written on the I/O socket.
     *
     * @return a {@link com.ning.http.client.AsyncHandler.STATE} telling to CONTINUE or ABORT the current processing.
     */
    @Override
    public STATE onContentWriteCompleted() {
        return STATE.CONTINUE;
    }

    /**
     * Invoked when the I/O operation associated with the {@link Request} body as been progressed.
     *
     * @param amount  The amount of bytes to transfer.
     * @param current The amount of bytes transferred
     * @param total   The total number of bytes transferred
     * @return a {@link com.ning.http.client.AsyncHandler.STATE} telling to CONTINUE or ABORT the current processing.
     */
    @Override
    public STATE onContentWriteProgress(long amount, long current, long total) {
        return STATE.CONTINUE;
    }
}
