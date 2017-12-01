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
package com.ning.http.util;

import java.util.BitSet;

/**
 * Convenience class that encapsulates details of "percent encoding"
 * (as per RFC-3986, see [http://www.ietf.org/rfc/rfc3986.txt]).
 */
public final class UTF8UrlEncoder {

    /**
     * Encoding table used for figuring out ascii characters that must be escaped
     * (all non-Ascii characters need to be encoded anyway)
     */
    public final static BitSet RFC3986_UNRESERVED_CHARS = new BitSet(256);
    public final static BitSet RFC3986_RESERVED_CHARS = new BitSet(256);
    public final static BitSet RFC3986_SUBDELIM_CHARS = new BitSet(256);
    public final static BitSet RFC3986_PCHARS = new BitSet(256);
    public final static BitSet BUILT_PATH_UNTOUCHED_CHARS = new BitSet(256);
    public final static BitSet BUILT_QUERY_UNTOUCHED_CHARS = new BitSet(256);
    // http://www.w3.org/TR/html5/forms.html#application/x-www-form-urlencoded-encoding-algorithm
    public final static BitSet FORM_URL_ENCODED_SAFE_CHARS = new BitSet(256);

    static {
        for (int i = 'a'; i <= 'z'; ++i) {
            RFC3986_UNRESERVED_CHARS.set(i);
            FORM_URL_ENCODED_SAFE_CHARS.set(i);
        }
        for (int i = 'A'; i <= 'Z'; ++i) {
            RFC3986_UNRESERVED_CHARS.set(i);
            FORM_URL_ENCODED_SAFE_CHARS.set(i);
        }
        for (int i = '0'; i <= '9'; ++i) {
            RFC3986_UNRESERVED_CHARS.set(i);
            FORM_URL_ENCODED_SAFE_CHARS.set(i);
        }
        RFC3986_UNRESERVED_CHARS.set('-');
        RFC3986_UNRESERVED_CHARS.set('.');
        RFC3986_UNRESERVED_CHARS.set('_');
        RFC3986_UNRESERVED_CHARS.set('~');

        RFC3986_SUBDELIM_CHARS.set('!');
        RFC3986_SUBDELIM_CHARS.set('$');
        RFC3986_SUBDELIM_CHARS.set('&');
        RFC3986_SUBDELIM_CHARS.set('\'');
        RFC3986_SUBDELIM_CHARS.set('(');
        RFC3986_SUBDELIM_CHARS.set(')');
        RFC3986_SUBDELIM_CHARS.set('*');
        RFC3986_SUBDELIM_CHARS.set('+');
        RFC3986_SUBDELIM_CHARS.set(',');
        RFC3986_SUBDELIM_CHARS.set(';');
        RFC3986_SUBDELIM_CHARS.set('=');
        
        FORM_URL_ENCODED_SAFE_CHARS.set('-');
        FORM_URL_ENCODED_SAFE_CHARS.set('.');
        FORM_URL_ENCODED_SAFE_CHARS.set('_');
        FORM_URL_ENCODED_SAFE_CHARS.set('*');

        RFC3986_RESERVED_CHARS.set('!');
        RFC3986_RESERVED_CHARS.set('*');
        RFC3986_RESERVED_CHARS.set('\'');
        RFC3986_RESERVED_CHARS.set('(');
        RFC3986_RESERVED_CHARS.set(')');
        RFC3986_RESERVED_CHARS.set(';');
        RFC3986_RESERVED_CHARS.set(':');
        RFC3986_RESERVED_CHARS.set('@');
        RFC3986_RESERVED_CHARS.set('&');
        RFC3986_RESERVED_CHARS.set('=');
        RFC3986_RESERVED_CHARS.set('+');
        RFC3986_RESERVED_CHARS.set('$');
        RFC3986_RESERVED_CHARS.set(',');
        RFC3986_RESERVED_CHARS.set('/');
        RFC3986_RESERVED_CHARS.set('?');
        RFC3986_RESERVED_CHARS.set('#');
        RFC3986_RESERVED_CHARS.set('[');
        RFC3986_RESERVED_CHARS.set(']');
        
        RFC3986_PCHARS.or(RFC3986_UNRESERVED_CHARS);
        RFC3986_PCHARS.or(RFC3986_SUBDELIM_CHARS);
        RFC3986_PCHARS.set(':');
        RFC3986_PCHARS.set('@');

        BUILT_PATH_UNTOUCHED_CHARS.or(RFC3986_PCHARS);
        BUILT_PATH_UNTOUCHED_CHARS.set('%');
        BUILT_PATH_UNTOUCHED_CHARS.set('/');

        BUILT_QUERY_UNTOUCHED_CHARS.or(RFC3986_PCHARS);
        BUILT_QUERY_UNTOUCHED_CHARS.set('%');
        BUILT_QUERY_UNTOUCHED_CHARS.set('/');
        BUILT_QUERY_UNTOUCHED_CHARS.set('?');
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private UTF8UrlEncoder() {
    }

    public static String encodePath(String input) {
        StringBuilder sb = new StringBuilder(input.length() + 6);
        appendEncoded(sb, input, BUILT_PATH_UNTOUCHED_CHARS, false);
        return sb.toString();
    }
    
    public static StringBuilder encodeAndAppendQuery(StringBuilder sb, String query) {
        return appendEncoded(sb, query, BUILT_QUERY_UNTOUCHED_CHARS, false);
    }

    public static String encodeQueryElement(String input) {
        StringBuilder sb = new StringBuilder(input.length() + 6);
        encodeAndAppendQueryElement(sb, input);
        return sb.toString();
    }

    public static StringBuilder encodeAndAppendQueryElement(StringBuilder sb, CharSequence input) {
        return appendEncoded(sb, input, RFC3986_UNRESERVED_CHARS, false);
    }

    public static StringBuilder encodeAndAppendFormElement(StringBuilder sb, CharSequence input) {
        return appendEncoded(sb, input, FORM_URL_ENCODED_SAFE_CHARS, true);
    }

    private static StringBuilder appendEncoded(StringBuilder sb, CharSequence input, BitSet dontNeedEncoding, boolean encodeSpaceAsPlus) {
        int c;
        for (int i = 0; i < input.length(); i+= Character.charCount(c)) {
            c = Character.codePointAt(input, i);
            if (c <= 127)
                if (dontNeedEncoding.get(c))
                    sb.append((char) c);
                else
                    appendSingleByteEncoded(sb, c, encodeSpaceAsPlus);
            else
                appendMultiByteEncoded(sb, c);
        }
        return sb;
    }

    private final static void appendSingleByteEncoded(StringBuilder sb, int value, boolean encodeSpaceAsPlus) {

        if (value == ' ' && encodeSpaceAsPlus) {
            sb.append('+');
            return;
        }

        sb.append('%');
        sb.append(HEX[value >> 4]);
        sb.append(HEX[value & 0xF]);
    }

    private final static void appendMultiByteEncoded(StringBuilder sb, int value) {
        if (value < 0x800) {
            appendSingleByteEncoded(sb, (0xc0 | (value >> 6)), false);
            appendSingleByteEncoded(sb, (0x80 | (value & 0x3f)), false);
        } else if (value < 0x10000) {
            appendSingleByteEncoded(sb, (0xe0 | (value >> 12)), false);
            appendSingleByteEncoded(sb, (0x80 | ((value >> 6) & 0x3f)), false);
            appendSingleByteEncoded(sb, (0x80 | (value & 0x3f)), false);
        } else {
            appendSingleByteEncoded(sb, (0xf0 | (value >> 18)), false);
            appendSingleByteEncoded(sb, (0x80 | (value >> 12) & 0x3f), false);
            appendSingleByteEncoded(sb, (0x80 | (value >> 6) & 0x3f), false);
            appendSingleByteEncoded(sb, (0x80 | (value & 0x3f)), false);
        }
    }
}
