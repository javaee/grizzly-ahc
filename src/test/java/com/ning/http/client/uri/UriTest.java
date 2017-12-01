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
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.ning.http.client.uri;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class UriTest {

    @Test
    public void testSimpleParsing() {
        Uri url = Uri.create("https://graph.facebook.com/750198471659552/accounts/test-users?method=get&access_token=750198471659552lleveCvbUu_zqBa9tkT3tcgaPh4");
        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "graph.facebook.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/750198471659552/accounts/test-users");
        assertEquals(url.getQuery(), "method=get&access_token=750198471659552lleveCvbUu_zqBa9tkT3tcgaPh4");
    }

    @Test
    public void testRootRelativeURIWithRootContext() {

        Uri context = Uri.create("https://graph.facebook.com");
        
        Uri url = Uri.create(context, "/750198471659552/accounts/test-users?method=get&access_token=750198471659552lleveCvbUu_zqBa9tkT3tcgaPh4");
        
        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "graph.facebook.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/750198471659552/accounts/test-users");
        assertEquals(url.getQuery(), "method=get&access_token=750198471659552lleveCvbUu_zqBa9tkT3tcgaPh4");
    }
    
    @Test
    public void testRootRelativeURIWithNonRootContext() {

        Uri context = Uri.create("https://graph.facebook.com/foo/bar");
        
        Uri url = Uri.create(context, "/750198471659552/accounts/test-users?method=get&access_token=750198471659552lleveCvbUu_zqBa9tkT3tcgaPh4");
        
        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "graph.facebook.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/750198471659552/accounts/test-users");
        assertEquals(url.getQuery(), "method=get&access_token=750198471659552lleveCvbUu_zqBa9tkT3tcgaPh4");
    }
    
    @Test
    public void testNonRootRelativeURIWithNonRootContext() {

        Uri context = Uri.create("https://graph.facebook.com/foo/bar");
        
        Uri url = Uri.create(context, "750198471659552/accounts/test-users?method=get&access_token=750198471659552lleveCvbUu_zqBa9tkT3tcgaPh4");
        
        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "graph.facebook.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/foo/750198471659552/accounts/test-users");
        assertEquals(url.getQuery(), "method=get&access_token=750198471659552lleveCvbUu_zqBa9tkT3tcgaPh4");
    }
    
    @Test
    public void testAbsoluteURIWithContext() {

        Uri context = Uri.create("https://hello.com/foo/bar");
        
        Uri url = Uri.create(context, "https://graph.facebook.com/750198471659552/accounts/test-users?method=get&access_token=750198471659552lleveCvbUu_zqBa9tkT3tcgaPh4");
        
        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "graph.facebook.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/750198471659552/accounts/test-users");
        assertEquals(url.getQuery(), "method=get&access_token=750198471659552lleveCvbUu_zqBa9tkT3tcgaPh4");
    }

    @Test
    public void testRelativeUriWithDots() {
        Uri context = Uri.create("https://hello.com/level1/level2/");

        Uri url = Uri.create(context, "../other/content/img.png");

        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "hello.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/level1/other/content/img.png");
        assertNull(url.getQuery());
    }

    @Test
    public void testRelativeUriWithDotsAboveRoot() {
        Uri context = Uri.create("https://hello.com/level1");

        Uri url = Uri.create(context, "../other/content/img.png");

        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "hello.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/../other/content/img.png");
        assertNull(url.getQuery());
    }

    @Test
    public void testRelativeUriWithAbsoluteDots() {
        Uri context = Uri.create("https://hello.com/level1/");

        Uri url = Uri.create(context, "/../other/content/img.png");

        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "hello.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/../other/content/img.png");
        assertNull(url.getQuery());
    }

    @Test
    public void testRelativeUriWithConsecutiveDots() {
        Uri context = Uri.create("https://hello.com/level1/level2/");

        Uri url = Uri.create(context, "../../other/content/img.png");

        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "hello.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/other/content/img.png");
        assertNull(url.getQuery());
    }

    @Test
    public void testRelativeUriWithConsecutiveDotsAboveRoot() {
        Uri context = Uri.create("https://hello.com/level1/level2");

        Uri url = Uri.create(context, "../../other/content/img.png");

        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "hello.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/../other/content/img.png");
        assertNull(url.getQuery());
    }

    @Test
    public void testRelativeUriWithAbsoluteConsecutiveDots() {
        Uri context = Uri.create("https://hello.com/level1/level2/");

        Uri url = Uri.create(context, "/../../other/content/img.png");

        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "hello.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/../../other/content/img.png");
        assertNull(url.getQuery());
    }

    @Test
    public void testRelativeUriWithConsecutiveDotsFromRoot() {
        Uri context = Uri.create("https://hello.com/");

        Uri url = Uri.create(context, "../../../other/content/img.png");

        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "hello.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/../../../other/content/img.png");
        assertNull(url.getQuery());
    }

    @Test
    public void testRelativeUriWithConsecutiveDotsFromRootResource() {
        Uri context = Uri.create("https://hello.com/level1");

        Uri url = Uri.create(context, "../../../other/content/img.png");

        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "hello.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/../../../other/content/img.png");
        assertNull(url.getQuery());
    }

    @Test
    public void testRelativeUriWithConsecutiveDotsFromSubrootResource() {
        Uri context = Uri.create("https://hello.com/level1/level2");

        Uri url = Uri.create(context, "../../../other/content/img.png");

        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "hello.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/../../other/content/img.png");
        assertNull(url.getQuery());
    }

    @Test
    public void testRelativeUriWithConsecutiveDotsFromLevel3Resource() {
        Uri context = Uri.create("https://hello.com/level1/level2/level3");

        Uri url = Uri.create(context, "../../../other/content/img.png");

        assertEquals(url.getScheme(), "https");
        assertEquals(url.getHost(), "hello.com");
        assertEquals(url.getPort(), -1);
        assertEquals(url.getPath(), "/../other/content/img.png");
        assertNull(url.getQuery());
    }
}
