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
 * You may obtain a copy of the Apache License Version 2.0 at
 *     http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.ning.http.client.async;

import org.testng.Assert;

import com.ning.http.client.AsyncCompletionHandlerBase;
import com.ning.http.client.AsyncHandlerExtensions;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Response;

import java.net.InetAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EventCollectingHandler extends AsyncCompletionHandlerBase implements AsyncHandlerExtensions {
    public Queue<String> firedEvents = new ConcurrentLinkedQueue<>();
    private CountDownLatch completionLatch = new CountDownLatch(1);

    public void waitForCompletion() throws InterruptedException {
        if (!completionLatch.await(AbstractBasicTest.TIMEOUT, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
    }

    @Override
    public Response onCompleted(Response response) throws Exception {
        firedEvents.add("Completed");
        try {
            return super.onCompleted(response);
        } finally {
            completionLatch.countDown();
        }
    }

    @Override
    public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
        firedEvents.add("StatusReceived");
        return super.onStatusReceived(status);
    }

    @Override
    public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
        firedEvents.add("HeadersReceived");
        return super.onHeadersReceived(headers);
    }

    @Override
    public STATE onHeaderWriteCompleted() {
        firedEvents.add("HeaderWriteCompleted");
        return super.onHeaderWriteCompleted();
    }

    @Override
    public STATE onContentWriteCompleted() {
        firedEvents.add("ContentWriteCompleted");
        return super.onContentWriteCompleted();
    }

    @Override
    public void onOpenConnection() {
        firedEvents.add("OpenConnection");
    }

    @Override
    public void onConnectionOpen() {
        firedEvents.add("ConnectionOpen");
    }

    @Override
    public void onPoolConnection() {
        firedEvents.add("PoolConnection");
    }

    @Override
    public void onConnectionPooled() {
        firedEvents.add("ConnectionPooled");
    }

    @Override
    public void onSendRequest(Object request) {
        firedEvents.add("SendRequest");
    }

    @Override
    public void onRetry() {
        firedEvents.add("Retry");
    }

    @Override
    public void onDnsResolved(InetAddress address) {
        firedEvents.add("DnsResolved");
    }

    @Override
    public void onSslHandshakeCompleted() {
        firedEvents.add("SslHandshakeCompleted");
    }
}
