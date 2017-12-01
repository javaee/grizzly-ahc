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
 * Copyright (c) Will Sargent. All rights reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A HostnameChecker proxy.
 */
public class ProxyHostnameChecker implements HostnameChecker {

    public final static byte TYPE_TLS = 1;

    private final Object checker = getHostnameChecker();

    public ProxyHostnameChecker() {
    }

    private Object getHostnameChecker() {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            @SuppressWarnings("unchecked")
            final Class<Object> hostnameCheckerClass = (Class<Object>) classLoader.loadClass("sun.security.util.HostnameChecker");
            final Method instanceMethod = hostnameCheckerClass.getMethod("getInstance", Byte.TYPE);
            return instanceMethod.invoke(null, TYPE_TLS);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public void match(String hostname, X509Certificate peerCertificate) throws CertificateException {
        try {
            final Class<?> hostnameCheckerClass = checker.getClass();
            final Method checkMethod = hostnameCheckerClass.getMethod("match", String.class, X509Certificate.class);
            checkMethod.invoke(checker, hostname, peerCertificate);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof CertificateException) {
                throw (CertificateException) t;
            } else {
                throw new IllegalStateException(e);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean match(String hostname, Principal principal) {
        try {
            final Class<?> hostnameCheckerClass = checker.getClass();
            final Method checkMethod = hostnameCheckerClass.getMethod("match", String.class, Principal.class);
            return (Boolean) checkMethod.invoke(null, hostname, principal);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

}
