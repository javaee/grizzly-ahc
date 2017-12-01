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
