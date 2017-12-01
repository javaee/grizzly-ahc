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
package com.ning.http.util;

/**
 * Implements the "base64" binary encoding scheme as defined by
 * <a href="http://tools.ietf.org/html/rfc2045">RFC 2045</a>.
 * <p/>
 * Portions of code here are taken from Apache Pivot
 */
public final class Base64 {
    private static final char[] lookup = new char[64];
    private static final byte[] reverseLookup = new byte[256];

    static {
        // Populate the lookup array

        for (int i = 0; i < 26; i++) {
            lookup[i] = (char) ('A' + i);
        }

        for (int i = 26, j = 0; i < 52; i++, j++) {
            lookup[i] = (char) ('a' + j);
        }

        for (int i = 52, j = 0; i < 62; i++, j++) {
            lookup[i] = (char) ('0' + j);
        }

        lookup[62] = '+';
        lookup[63] = '/';

        // Populate the reverse lookup array

        for (int i = 0; i < 256; i++) {
            reverseLookup[i] = -1;
        }

        for (int i = 'Z'; i >= 'A'; i--) {
            reverseLookup[i] = (byte) (i - 'A');
        }

        for (int i = 'z'; i >= 'a'; i--) {
            reverseLookup[i] = (byte) (i - 'a' + 26);
        }

        for (int i = '9'; i >= '0'; i--) {
            reverseLookup[i] = (byte) (i - '0' + 52);
        }

        reverseLookup['+'] = 62;
        reverseLookup['/'] = 63;
        reverseLookup['='] = 0;
    }

    /**
     * This class is not instantiable.
     */
    private Base64() {
    }

    /**
     * Encodes the specified data into a base64 string.
     *
     * @param bytes The unencoded raw data.
     */
    public static String encode(byte[] bytes) {
        // always sequence of 4 characters for each 3 bytes; padded with '='s as necessary:
        StringBuilder buf = new StringBuilder(((bytes.length + 2) / 3) * 4);

        // first, handle complete chunks (fast loop)
        int i = 0;
        for (int end = bytes.length - 2; i < end; ) {
            int chunk = ((bytes[i++] & 0xFF) << 16)
                    | ((bytes[i++] & 0xFF) << 8)
                    | (bytes[i++] & 0xFF);
            buf.append(lookup[chunk >> 18]);
            buf.append(lookup[(chunk >> 12) & 0x3F]);
            buf.append(lookup[(chunk >> 6) & 0x3F]);
            buf.append(lookup[chunk & 0x3F]);
        }

        // then leftovers, if any
        int len = bytes.length;
        if (i < len) { // 1 or 2 extra bytes?
            int chunk = ((bytes[i++] & 0xFF) << 16);
            buf.append(lookup[chunk >> 18]);
            if (i < len) { // 2 bytes
                chunk |= ((bytes[i] & 0xFF) << 8);
                buf.append(lookup[(chunk >> 12) & 0x3F]);
                buf.append(lookup[(chunk >> 6) & 0x3F]);
            } else { // 1 byte
                buf.append(lookup[(chunk >> 12) & 0x3F]);
                buf.append('=');
            }
            buf.append('=');
        }
        return buf.toString();
    }

    /**
     * Decodes the specified base64 string back into its raw data.
     *
     * @param encoded The base64 encoded string.
     */
    public static byte[] decode(String encoded) {
        int padding = 0;

        for (int i = encoded.length() - 1; encoded.charAt(i) == '='; i--) {
            padding++;
        }

        int length = encoded.length() * 6 / 8 - padding;
        byte[] bytes = new byte[length];

        for (int i = 0, index = 0, n = encoded.length(); i < n; i += 4) {
            int word = reverseLookup[encoded.charAt(i)] << 18;
            word += reverseLookup[encoded.charAt(i + 1)] << 12;
            word += reverseLookup[encoded.charAt(i + 2)] << 6;
            word += reverseLookup[encoded.charAt(i + 3)];

            for (int j = 0; j < 3 && index + j < length; j++) {
                bytes[index + j] = (byte) (word >> (8 * (2 - j)));
            }

            index += 3;
        }

        return bytes;
    }
}
