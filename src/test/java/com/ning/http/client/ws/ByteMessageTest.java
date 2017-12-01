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
package com.ning.http.client.ws;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketByteListener;
import com.ning.http.client.ws.WebSocketUpgradeHandler;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ByteMessageTest extends AbstractBasicTest {

    private final class EchoByteWebSocket implements org.eclipse.jetty.websocket.WebSocket, org.eclipse.jetty.websocket.WebSocket.OnBinaryMessage {

        private Connection connection;

        @Override
        public void onOpen(Connection connection) {
            this.connection = connection;
            connection.setMaxBinaryMessageSize(1000);
        }

        @Override
        public void onClose(int i, String s) {
            connection.close();
        }

        @Override
        public void onMessage(byte[] bytes, int i, int i1) {
            try {
                connection.sendMessage(bytes, i, i1);
            } catch (IOException e) {
                try {
                    connection.sendMessage("FAIL");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    public WebSocketHandler getWebSocketHandler() {
        return new WebSocketHandler() {
            @Override
            public org.eclipse.jetty.websocket.WebSocket doWebSocketConnect(HttpServletRequest httpServletRequest, String s) {
                return new EchoByteWebSocket();
            }
        };
    }

    @Test
    public void echoByte() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<byte[]> text = new AtomicReference<>(new byte[0]);

            WebSocket websocket = client.prepareGet(getTargetUrl()).execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WebSocketByteListener() {

                @Override
                public void onOpen(WebSocket websocket) {
                }

                @Override
                public void onClose(WebSocket websocket) {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                    latch.countDown();
                }

                @Override
                public void onMessage(byte[] message) {
                    text.set(message);
                    latch.countDown();
                }
            }).build()).get();

            websocket.sendMessage("ECHO".getBytes());

            latch.await();
            assertEquals(text.get(), "ECHO".getBytes());
        }
    }

    @Test
    public void echoTwoMessagesTest() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            final CountDownLatch latch = new CountDownLatch(2);
            final AtomicReference<byte[]> text = new AtomicReference<>(null);

            WebSocket websocket = client.prepareGet(getTargetUrl()).execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WebSocketByteListener() {

                @Override
                public void onOpen(WebSocket websocket) {
                }

                @Override
                public void onClose(WebSocket websocket) {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                    latch.countDown();
                }

                @Override
                public void onMessage(byte[] message) {
                    if (text.get() == null) {
                        text.set(message);
                    } else {
                        byte[] n = new byte[text.get().length + message.length];
                        System.arraycopy(text.get(), 0, n, 0, text.get().length);
                        System.arraycopy(message, 0, n, text.get().length, message.length);
                        text.set(n);
                    }
                    latch.countDown();
                }
            }).build()).get();

            websocket.sendMessage("ECHO".getBytes()).sendMessage("ECHO".getBytes());

            latch.await();
            assertEquals(text.get(), "ECHOECHO".getBytes());
        }
    }

    @Test
    public void echoOnOpenMessagesTest() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            final CountDownLatch latch = new CountDownLatch(2);
            final AtomicReference<byte[]> text = new AtomicReference<>(null);

            WebSocket websocket = client.prepareGet(getTargetUrl()).execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WebSocketByteListener() {

                @Override
                public void onOpen(WebSocket websocket) {
                    websocket.sendMessage("ECHO".getBytes()).sendMessage("ECHO".getBytes());
                }

                @Override
                public void onClose(WebSocket websocket) {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                    latch.countDown();
                }

                @Override
                public void onMessage(byte[] message) {
                    if (text.get() == null) {
                        text.set(message);
                    } else {
                        byte[] n = new byte[text.get().length + message.length];
                        System.arraycopy(text.get(), 0, n, 0, text.get().length);
                        System.arraycopy(message, 0, n, text.get().length, message.length);
                        text.set(n);
                    }
                    latch.countDown();
                }
            }).build()).get();

            latch.await();
            assertEquals(text.get(), "ECHOECHO".getBytes());
        }
    }

    public void echoFragments() throws Exception {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<byte[]> text = new AtomicReference<>(null);

            WebSocket websocket = client.prepareGet(getTargetUrl()).execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WebSocketByteListener() {

                @Override
                public void onOpen(WebSocket websocket) {
                }

                @Override
                public void onClose(WebSocket websocket) {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                    latch.countDown();
                }

                @Override
                public void onMessage(byte[] message) {
                    if (text.get() == null) {
                        text.set(message);
                    } else {
                        byte[] n = new byte[text.get().length + message.length];
                        System.arraycopy(text.get(), 0, n, 0, text.get().length);
                        System.arraycopy(message, 0, n, text.get().length, message.length);
                        text.set(n);
                    }
                    latch.countDown();
                }
            }).build()).get();
            websocket.stream("ECHO".getBytes(), false);
            websocket.stream("ECHO".getBytes(), true);
            latch.await();
            assertEquals(text.get(), "ECHOECHO".getBytes());
        }
    }
}
