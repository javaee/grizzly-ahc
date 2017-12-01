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
package com.ning.http.client.extra;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.filter.FilterContext;
import com.ning.http.client.filter.FilterException;
import com.ning.http.client.filter.RequestFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link com.ning.http.client.filter.RequestFilter} throttles requests and block when the number of permits is reached, waiting for
 * the response to arrives before executing the next request.
 */
public class ThrottleRequestFilter implements RequestFilter {
    private final static Logger LOGGER = LoggerFactory.getLogger(ThrottleRequestFilter.class);
    private final Semaphore available;
    private final int maxWait;

    public ThrottleRequestFilter(int maxConnections) {
        this(maxConnections, Integer.MAX_VALUE);
    }

    public ThrottleRequestFilter(int maxConnections, int maxWait) {
        this.maxWait = maxWait;
        available = new Semaphore(maxConnections, true);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public FilterContext filter(FilterContext ctx) throws FilterException {

        try {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Current Throttling Status {}", available.availablePermits());

            if (!available.tryAcquire(maxWait, TimeUnit.MILLISECONDS)) {
                throw new FilterException(
                        String.format("No slot available for processing Request %s with AsyncHandler %s",
                                ctx.getRequest(), ctx.getAsyncHandler()));
            }
        } catch (InterruptedException e) {
            throw new FilterException(
                    String.format("Interrupted Request %s with AsyncHandler %s", ctx.getRequest(), ctx.getAsyncHandler()));
        }

        return new FilterContext.FilterContextBuilder(ctx).asyncHandler(new AsyncHandlerWrapper(ctx.getAsyncHandler())).build();
    }

    private class AsyncHandlerWrapper<T> implements AsyncHandler<T> {

        private final AsyncHandler<T> asyncHandler;
        private final AtomicBoolean complete = new AtomicBoolean(false);

        public AsyncHandlerWrapper(AsyncHandler<T> asyncHandler) {
            this.asyncHandler = asyncHandler;
        }

        private void complete() {
            if (complete.compareAndSet(false, true))
                available.release();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Current Throttling Status after onThrowable {}", available.availablePermits());
            }
        }
        
        @Override
        public void onThrowable(Throwable t) {
            try {
                asyncHandler.onThrowable(t);
            } finally {
                complete();
            }
        }

        @Override
        public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
            return asyncHandler.onBodyPartReceived(bodyPart);
        }

        @Override
        public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
            return asyncHandler.onStatusReceived(responseStatus);
        }

        @Override
        public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
            return asyncHandler.onHeadersReceived(headers);
        }

        @Override
        public T onCompleted() throws Exception {
            try {
                return asyncHandler.onCompleted();
            } finally {
                complete();
            }
        }
    }
}
