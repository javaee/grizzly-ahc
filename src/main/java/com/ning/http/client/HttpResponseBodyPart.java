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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * A callback class used when an HTTP response body is received.
 */
public abstract class HttpResponseBodyPart {

    private final boolean last;
    private boolean closeConnection;

    public HttpResponseBodyPart(boolean last) {
        this.last = last;
    }

    /**
     * Close the underlying connection once the processing has completed. Invoking that method means the
     * underlying TCP connection will be closed as soon as the processing of the response is completed. That
     * means the underlying connection will never get pooled.
     */
    public void markUnderlyingConnectionAsToBeClosed() {
        closeConnection = true;
    }

    /**
     * Return true of the underlying connection will be closed once the response has been fully processed.
     *
     * @return true of the underlying connection will be closed once the response has been fully processed.
     */
    public boolean isUnderlyingConnectionToBeClosed() {
        return closeConnection;
    }

    /**
     * Return true if this is the last part.
     *
     * @return true if this is the last part.
     */
    public boolean isLast() {
        return last;
    }

    /**
     * Return length of this part in bytes.
     */
    public abstract int length();

    /**
     * Return the response body's part bytes received.
     *
     * @return the response body's part bytes received.
     */
    public abstract byte[] getBodyPartBytes();

    /**
     * Write the available bytes to the {@link java.io.OutputStream}
     *
     * @param outputStream
     * @return The number of bytes written
     * @throws IOException
     */
    public abstract int writeTo(OutputStream outputStream) throws IOException;

    /**
     * Return a {@link ByteBuffer} that wraps the actual bytes read from the response's chunk. The {@link ByteBuffer}
     * capacity is equal to the number of bytes available.
     *
     * @return {@link ByteBuffer}
     */
    public abstract ByteBuffer getBodyByteBuffer();
}
