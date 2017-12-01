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
package com.ning.http.util;

public final class UTF8UrlDecoder {

    private UTF8UrlDecoder() {
    }

    private static StringBuilder initSb(StringBuilder sb, String s, int i, int offset, int length) {
        if (sb != null) {
            return sb;
        } else {
            int initialSbLength = length > 500 ? length / 2 : length;
            return new StringBuilder(initialSbLength).append(s, offset, i);
        }
    }

    private static int hexaDigit(char c) {
        return Character.digit(c, 16);
    }

    public static CharSequence decode(String s) {
        return decode(s, 0, s.length());
    }
    
    public static CharSequence decode(final String s, final int offset, final int length) {

        StringBuilder sb = null;
        int i = offset;
        int end = length + offset;

        while (i < end) {
            char c = s.charAt(i);
            if (c == '+') {
                sb = initSb(sb, s, i, offset, length);
                sb.append(' ');
                i++;

            } else if (c == '%') {
                if (end - i < 3) // We expect 3 chars. 0 based i vs. 1 based length!
                    throw new IllegalArgumentException("UTF8UrlDecoder: Incomplete trailing escape (%) pattern");

                int x, y;
                if ((x = hexaDigit(s.charAt(i + 1))) == -1 || (y = hexaDigit(s.charAt(i + 2))) == -1)
                    throw new IllegalArgumentException("UTF8UrlDecoder: Malformed");

                sb = initSb(sb, s, i, offset, length);
                sb.append((char) (x * 16 + y));
                i += 3;
            } else {
                if (sb != null)
                    sb.append(c);
                i++;
            }
        }

        return sb != null ? sb.toString() : new StringCharSequence(s, offset, length);
    }
}
