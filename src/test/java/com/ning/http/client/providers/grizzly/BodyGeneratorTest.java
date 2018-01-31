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

package com.ning.http.client.providers.grizzly;

import com.ning.http.client.Body;
import com.ning.http.client.BodyGenerator;
import com.ning.http.client.Request;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.memory.MemoryManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.ning.http.client.providers.grizzly.PayloadGenFactory.getPayloadGenerator;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;

public class BodyGeneratorTest {

    private final Request request = mock(Request.class);
    private final BodyGenerator bodyGenerator = mock(BodyGenerator.class);
    private final Body body = mock(Body.class);
    private final FilterChainContext context = mock(FilterChainContext.class, RETURNS_DEEP_STUBS);
    private final HttpRequestPacket requestPacket = mock(HttpRequestPacket.class);
    private final MemoryManager memoryManager = mock(MemoryManager.class);
    private final Buffer buffer = mock(Buffer.class);

    @BeforeMethod
    public void setUp() throws Exception {
        when(request.getBodyGenerator()).thenReturn(bodyGenerator);
        when(bodyGenerator.createBody()).thenReturn(body);
        when(context.getMemoryManager()).thenReturn(memoryManager);
        when(requestPacket.isCommitted()).thenReturn(true);
        when(memoryManager.allocate(anyInt())).thenReturn(buffer);
        when(body.read(any(ByteBuffer.class))).thenReturn(-1L);
    }

    @Test
    public void testBodyIsClosed() throws Exception {
        getPayloadGenerator(request).generate(context, request, requestPacket);
        verify(body).close();
    }

}
