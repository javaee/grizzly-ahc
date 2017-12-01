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
package com.ning.http.client.async;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.testng.Assert.assertEquals;

public abstract class NonAsciiContentLengthTest extends AbstractBasicTest {

    public void setUpServer() throws Exception {
        server = new Server();
        port1 = findFreePort();
        Connector listener = new SelectChannelConnector();

        listener.setHost("127.0.0.1");
        listener.setPort(port1);
        server.addConnector(listener);
        server.setHandler(new AbstractHandler() {

            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                int MAX_BODY_SIZE = 1024; // Can only handle bodies of up to 1024 bytes.
                byte[] b = new byte[MAX_BODY_SIZE];
                int offset = 0;
                int numBytesRead;
                try (ServletInputStream is = request.getInputStream()) {
                    while ((numBytesRead = is.read(b, offset, MAX_BODY_SIZE - offset)) != -1) {
                        offset += numBytesRead;
                    }
                }
                assertEquals(request.getContentLength(), offset);
                response.setStatus(200);
                response.setCharacterEncoding(request.getCharacterEncoding());
                response.setContentLength(request.getContentLength());
                try (ServletOutputStream os = response.getOutputStream()) {
                    os.write(b, 0, offset);
                }
            }
        });
        server.start();
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testNonAsciiContentLength() throws Exception {
        setUpServer();
        execute("test");
        execute("\u4E00"); // Unicode CJK ideograph for one
    }

    protected void execute(String body) throws IOException, InterruptedException, ExecutionException {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            BoundRequestBuilder r = client.preparePost(getTargetUrl()).setBody(body).setBodyEncoding("UTF-8");
            Future<Response> f = r.execute();
            Response resp = f.get();
            assertEquals(resp.getStatusCode(), 200);
            assertEquals(body, resp.getResponseBody("UTF-8"));
        }
    }
}
