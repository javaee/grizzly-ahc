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
 * Copyright (c) 2012 Sonatype, Inc. All rights reserved.
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

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RedirectTimeoutTest extends AbstractBasicTest {

    private static final String REDIRECT_PATH = "/redirectPath" ;

    private static final String FINAL_PATH = "/finalPath";

    private static final String PAYLOAD = "Ok";

    private static final String GLOBAL_REQUEST_TIMEOUT = "5000";

    private static final String REQUEST_TIMEOUT = "2000";

    private static final String TIMEOUT_ERROR_MESSAGE = "Timeout exceeded";

    private static long SLEEP_TIME ;

    private static final long DELTA = 800;

    private  AsyncHttpClientConfig clientConfig ;

    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new PostRedirectGetHandler();
    }

    @Override
    public AsyncHttpClient getAsyncHttpClient(AsyncHttpClientConfig config)
    {
        return new AsyncHttpClient(config);
    }

    @BeforeMethod
    public void setUp() throws Exception
    {
        clientConfig = new AsyncHttpClientConfig.Builder().setFollowRedirect(true).setRequestTimeout(Integer.valueOf(GLOBAL_REQUEST_TIMEOUT)).build();
    }

    @DataProvider(name = "timeout")
    public Object[][] createData1() {
        return new Object[][] {
                { GLOBAL_REQUEST_TIMEOUT },
                { REQUEST_TIMEOUT}
        };
    }

    @Test(dataProvider = "timeout")
    public void testRequestTimeout(String timeout) {
        SLEEP_TIME = Long.valueOf(timeout) * 2;
        AsyncHttpClient client = getAsyncHttpClient(clientConfig);
        Request request = new RequestBuilder("GET").setRequestTimeout(Integer.valueOf(timeout)).setUrl(getTargetUrl().concat(REDIRECT_PATH)).build();
        ListenableFuture<Response> responseFuture = client.executeRequest(request);
        assertTimeout(responseFuture, timeout);
    }

    private void assertTimeout (ListenableFuture<Response> responseFuture, String timeout) {
        try {
            responseFuture.get(Long.valueOf(timeout) + DELTA, TimeUnit.MILLISECONDS);
            fail("TimeoutException must be thrown");
        }
        catch (ExecutionException e) {
            //This exception is thrown when Grizzly AHC Client aborts the future (that is the expected)
            assertTrue(e.getMessage().contains(TIMEOUT_ERROR_MESSAGE));
        }
        catch (TimeoutException e) {
            //This exception is thrown when the future wait times out.
            fail("Future timed out so Grizzly didn't honor the given request timeout. ", e);
        }
        catch (InterruptedException e) {
            fail("InterruptedException should not be thrown ", e);
        }
    }

    public static class PostRedirectGetHandler extends AbstractHandler {
        @Override
        public void handle(String pathInContext, org.eclipse.jetty.server.Request request, HttpServletRequest httpRequest, final HttpServletResponse httpResponse) throws IOException, ServletException {
            if(httpRequest.getRequestURI().endsWith(REDIRECT_PATH)){
                httpResponse.setStatus(HttpStatus.FOUND_302);
                httpResponse.setHeader("Location", FINAL_PATH);
            }
            else if (httpRequest.getRequestURI().endsWith(FINAL_PATH)) {
                try {
                    Thread.sleep(SLEEP_TIME);
                    httpResponse.setStatus(HttpStatus.OK_200);
                    httpResponse.getOutputStream().print(PAYLOAD);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            httpResponse.getOutputStream().flush();
        }
    }
}
