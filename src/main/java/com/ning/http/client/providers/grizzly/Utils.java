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
 * Copyright (c) 2013-2015 Sonatype, Inc. All rights reserved.
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

package com.ning.http.client.providers.grizzly;

import com.ning.http.client.uri.Uri;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;

public class Utils {
    private static class NTLM_HOLDER {
        private static final Attribute<Boolean> IS_NTLM_DONE =
                Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(
                        "com.ning.http.client.providers.grizzly.ntlm-done");
    }
    // ------------------------------------------------------------ Constructors

    private Utils() {
    }

    // ---------------------------------------------------------- Public Methods

    public static boolean getAndSetNtlmAttempted(final Connection c) {
        final Boolean v = NTLM_HOLDER.IS_NTLM_DONE.get(c);
        if (v == null) {
            NTLM_HOLDER.IS_NTLM_DONE.set(c, Boolean.TRUE);
            return false;
        }
        
        return true;
    }
    
    public static void setNtlmEstablished(final Connection c) {
        NTLM_HOLDER.IS_NTLM_DONE.set(c, Boolean.TRUE);
    }

    public static boolean isNtlmEstablished(final Connection c) {
        return Boolean.TRUE.equals(NTLM_HOLDER.IS_NTLM_DONE.get(c));
    }
    
    public static boolean isSecure(final String uri) {
        return (uri.startsWith("https") || uri.startsWith("wss"));
    }
    
    public static boolean isSecure(final Uri uri) {
        final String scheme = uri.getScheme();
        return ("https".equals(scheme) || "wss".equals(scheme));
    }
    
    static String discoverTestName(final String defaultName) {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        final int strackTraceLen = stackTrace.length;
        
        if (stackTrace[strackTraceLen - 1].getClassName().contains("surefire")) {
            for (int i = strackTraceLen - 2; i >= 0; i--) {
                if (stackTrace[i].getClassName().contains("com.ning.http.client.async")) {
                    return "grizzly-kernel-" +
                            stackTrace[i].getClassName() + "." + stackTrace[i].getMethodName();
                }
            }
        }
        
        return defaultName;
    }    
}
