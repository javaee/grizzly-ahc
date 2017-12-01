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
package com.ning.http.client.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncCompletionHandlerBase;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.Response;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A {@link org.asynchttpclient.AsyncHandler} that can be used to notify a set of {@link TransferListener}
 * <p/>
 * <blockquote>
 * 
 * <pre>
 * AsyncHttpClient client = new AsyncHttpClient();
 * TransferCompletionHandler tl = new TransferCompletionHandler();
 * tl.addTransferListener(new TransferListener() {
 * <p/>
 * public void onRequestHeadersSent(FluentCaseInsensitiveStringsMap headers) {
 * }
 * <p/>
 * public void onResponseHeadersReceived(FluentCaseInsensitiveStringsMap headers) {
 * }
 * <p/>
 * public void onBytesReceived(ByteBuffer buffer) {
 * }
 * <p/>
 * public void onBytesSent(long amount, long current, long total) {
 * }
 * <p/>
 * public void onRequestResponseCompleted() {
 * }
 * <p/>
 * public void onThrowable(Throwable t) {
 * }
 * });
 * <p/>
 * Response response = httpClient.prepareGet("http://...").execute(tl).get();
 * </pre>
 * 
 * </blockquote>
 */
public class TransferCompletionHandler extends AsyncCompletionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(TransferCompletionHandler.class);
    private final ConcurrentLinkedQueue<TransferListener> listeners = new ConcurrentLinkedQueue<>();
    private final boolean accumulateResponseBytes;
    private FluentCaseInsensitiveStringsMap headers;
    private long expectedTotal;
    private long seen;

    /**
     * Create a TransferCompletionHandler that will not accumulate bytes. The resulting {@link org.asynchttpclient.Response#getResponseBody()},
     * {@link org.asynchttpclient.Response#getResponseBodyAsStream()} and {@link Response#getResponseBodyExcerpt(int)} will throw an IllegalStateException if called.
     */
    public TransferCompletionHandler() {
        this(false);
    }

    /**
     * Create a TransferCompletionHandler that can or cannot accumulate bytes and make it available when {@link org.asynchttpclient.Response#getResponseBody()} get called. The
     * default is false.
     * 
     * @param accumulateResponseBytes
     *            true to accumulates bytes in memory.
     */
    public TransferCompletionHandler(boolean accumulateResponseBytes) {
        this.accumulateResponseBytes = accumulateResponseBytes;
    }

    /**
     * Add a {@link TransferListener}
     * 
     * @param t
     *            a {@link TransferListener}
     * @return this
     */
    public TransferCompletionHandler addTransferListener(TransferListener t) {
        listeners.offer(t);
        return this;
    }

    /**
     * Remove a {@link TransferListener}
     * 
     * @param t
     *            a {@link TransferListener}
     * @return this
     */
    public TransferCompletionHandler removeTransferListener(TransferListener t) {
        listeners.remove(t);
        return this;
    }

    /**
     * Set headers to this listener.
     * 
     * @param headers
     *            {@link FluentCaseInsensitiveStringsMap}
     */
    public void headers(FluentCaseInsensitiveStringsMap headers) {
        this.headers = headers;
        // Netty 3 bug hack: last chunk is not notified, fixed in Netty 4
        String contentLength = headers.getFirstValue("Content-Length");
        if (contentLength != null)
            expectedTotal = Long.valueOf(contentLength);
    }

    @Override
    public STATE onHeadersReceived(final HttpResponseHeaders headers) throws Exception {
        fireOnHeaderReceived(headers.getHeaders());
        return super.onHeadersReceived(headers);
    }

    @Override
    public STATE onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
        STATE s = STATE.CONTINUE;
        if (accumulateResponseBytes) {
            s = super.onBodyPartReceived(content);
        }
        fireOnBytesReceived(content.getBodyPartBytes());
        return s;
    }

    @Override
    public Response onCompleted(Response response) throws Exception {
        // some chunks weren't notified, probably the last one
        if (seen < expectedTotal) {
            // do once
            fireOnBytesSent(expectedTotal - seen, expectedTotal, expectedTotal);
        }
        fireOnEnd();
        return response;
    }

    @Override
    public STATE onHeaderWriteCompleted() {
        if (headers != null) {
            fireOnHeadersSent(headers);
        }
        return STATE.CONTINUE;
    }

    @Override
    public STATE onContentWriteProgress(long amount, long current, long total) {
        seen += amount;
        fireOnBytesSent(amount, current, total);
        return STATE.CONTINUE;
    }

    @Override
    public void onThrowable(Throwable t) {
        fireOnThrowable(t);
    }

    private void fireOnHeadersSent(FluentCaseInsensitiveStringsMap headers) {
        for (TransferListener l : listeners) {
            try {
                l.onRequestHeadersSent(headers);
            } catch (Throwable t) {
                l.onThrowable(t);
            }
        }
    }

    private void fireOnHeaderReceived(FluentCaseInsensitiveStringsMap headers) {

        for (TransferListener l : listeners) {
            try {
                l.onResponseHeadersReceived(headers);
            } catch (Throwable t) {
                l.onThrowable(t);
            }
        }
    }

    private void fireOnEnd() {
        for (TransferListener l : listeners) {
            try {
                l.onRequestResponseCompleted();
            } catch (Throwable t) {
                l.onThrowable(t);
            }
        }
    }

    private void fireOnBytesReceived(byte[] b) {
        for (TransferListener l : listeners) {
            try {
                l.onBytesReceived(b);
            } catch (Throwable t) {
                l.onThrowable(t);
            }
        }
    }

    private void fireOnBytesSent(long amount, long current, long total) {
        for (TransferListener l : listeners) {
            try {
                l.onBytesSent(amount, current, total);
            } catch (Throwable t) {
                l.onThrowable(t);
            }
        }
    }

    private void fireOnThrowable(Throwable t) {
        for (TransferListener l : listeners) {
            try {
                l.onThrowable(t);
            } catch (Throwable t2) {
                logger.warn("onThrowable", t2);
            }
        }
    }
}
