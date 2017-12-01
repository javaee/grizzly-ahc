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
 * Copyright (c) 2015 AsyncHttpClient Project. All rights reserved.
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
package com.ning.http.client.cookie;

import java.text.ParsePosition;
import java.util.BitSet;
import java.util.Date;

public class CookieUtil {

    private static final BitSet VALID_COOKIE_VALUE_OCTETS = validCookieValueOctets();

    private static final BitSet VALID_COOKIE_NAME_OCTETS = validCookieNameOctets();

    // cookie-octet = %x21 / %x23-2B / %x2D-3A / %x3C-5B / %x5D-7E
    // US-ASCII characters excluding CTLs, whitespace, DQUOTE, comma, semicolon, and backslash
    private static BitSet validCookieValueOctets() {
        BitSet bits = new BitSet(8);
        bits.set(0x21);
        for (int i = 0x23; i <= 0x2B; i++) {
            bits.set(i);
        }
        for (int i = 0x2D; i <= 0x3A; i++) {
            bits.set(i);
        }
        for (int i = 0x3C; i <= 0x5B; i++) {
            bits.set(i);
        }
        for (int i = 0x5D; i <= 0x7E; i++) {
            bits.set(i);
        }
        return bits;
    }

    // token = 1*<any CHAR except CTLs or separators>
    // separators = "(" | ")" | "<" | ">" | "@"
    // | "," | ";" | ":" | "\" | <">
    // | "/" | "[" | "]" | "?" | "="
    // | "{" | "}" | SP | HT
    private static BitSet validCookieNameOctets() {
        BitSet bits = new BitSet(8);
        for (int i = 32; i < 127; i++) {
            bits.set(i);
        }
        int[] separators = new int[] { '(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=', '{', '}', ' ', '\t' };
        for (int separator : separators) {
            bits.set(separator, false);
        }
        return bits;
    }

    static int firstInvalidCookieNameOctet(CharSequence cs) {
        return firstInvalidOctet(cs, VALID_COOKIE_NAME_OCTETS);
    }

    static int firstInvalidCookieValueOctet(CharSequence cs) {
        return firstInvalidOctet(cs, VALID_COOKIE_VALUE_OCTETS);
    }

    static int firstInvalidOctet(CharSequence cs, BitSet bits) {

        for (int i = 0; i < cs.length(); i++) {
            char c = cs.charAt(i);
            if (!bits.get(c)) {
                return i;
            }
        }
        return -1;
    }

    static CharSequence unwrapValue(CharSequence cs) {
        final int len = cs.length();
        if (len > 0 && cs.charAt(0) == '"') {
            if (len >= 2 && cs.charAt(len - 1) == '"') {
                // properly balanced
                return len == 2 ? "" : cs.subSequence(1, len - 1);
            } else {
                return null;
            }
        }
        return cs;
    }

    static long computeExpiresAsMaxAge(String expires) {
        if (expires != null) {
            Date expiresDate = RFC2616DateParser.get().parse(expires, new ParsePosition(0));
            if (expiresDate != null) {
                long maxAgeMillis = expiresDate.getTime() - System.currentTimeMillis();
                return maxAgeMillis / 1000 + (maxAgeMillis % 1000 != 0 ? 1 : 0);
            }
        }

        return Long.MIN_VALUE;
    }

    private CookieUtil() {
        // Unused
    }
}
