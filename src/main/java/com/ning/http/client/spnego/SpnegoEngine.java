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
 * Copyright (c) 2010-2015 Sonatype, Inc. All rights reserved.
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
/*
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package com.ning.http.client.spnego;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.util.Base64;

import java.io.IOException;

/**
 * SPNEGO (Simple and Protected GSSAPI Negotiation Mechanism) authentication
 * scheme.
 *
 * @since 4.1
 */
public class SpnegoEngine {

    private static final String SPNEGO_OID = "1.3.6.1.5.5.2";
    private static final String KERBEROS_OID = "1.2.840.113554.1.2.2";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SpnegoTokenGenerator spnegoGenerator;

    public static final SpnegoEngine INSTANCE = new SpnegoEngine();
    
    public SpnegoEngine(final SpnegoTokenGenerator spnegoGenerator) {
        this.spnegoGenerator = spnegoGenerator;
    }

    public SpnegoEngine() {
        this(null);
    }

    public String generateToken(String server) throws Throwable {

        try {
            log.debug("init {}", server);
            /* Using the SPNEGO OID is the correct method.
             * Kerberos v5 works for IIS but not JBoss. Unwrapping
             * the initial token when using SPNEGO OID looks like what is
             * described here...
             *
             * http://msdn.microsoft.com/en-us/library/ms995330.aspx
             *
             * Another helpful URL...
             *
             * http://publib.boulder.ibm.com/infocenter/wasinfo/v7r0/index.jsp?topic=/com.ibm.websphere.express.doc/info/exp/ae/tsec_SPNEGO_token.html
             *
             * Unfortunately SPNEGO is JRE >=1.6.
             */

            GSSContext gssContext = null;
            byte[] token = null; // base64 decoded challenge
            Oid negotiationOid = null;

            /** Try SPNEGO by default, fall back to Kerberos later if error */
            negotiationOid = new Oid(SPNEGO_OID);

            boolean tryKerberos = false;
            try {
                GSSManager manager = GSSManager.getInstance();
                GSSName serverName = manager.createName("HTTP@" + server, GSSName.NT_HOSTBASED_SERVICE);
                gssContext = manager.createContext(
                        serverName.canonicalize(negotiationOid), negotiationOid, null,
                        GSSContext.DEFAULT_LIFETIME);
                gssContext.requestMutualAuth(true);
                gssContext.requestCredDeleg(true);
            } catch (GSSException ex) {
                log.error("generateToken", ex);
                // BAD MECH means we are likely to be using 1.5, fall back to Kerberos MECH.
                // Rethrow any other exception.
                if (ex.getMajor() == GSSException.BAD_MECH) {
                    log.debug("GSSException BAD_MECH, retry with Kerberos MECH");
                    tryKerberos = true;
                } else {
                    throw ex;
                }

            }
            if (tryKerberos) {
                /* Kerberos v5 GSS-API mechanism defined in RFC 1964.*/
                log.debug("Using Kerberos MECH {}", KERBEROS_OID);
                negotiationOid = new Oid(KERBEROS_OID);
                GSSManager manager = GSSManager.getInstance();
                GSSName serverName = manager.createName("HTTP@" + server, GSSName.NT_HOSTBASED_SERVICE);
                gssContext = manager.createContext(
                        serverName.canonicalize(negotiationOid), negotiationOid, null,
                        GSSContext.DEFAULT_LIFETIME);
                gssContext.requestMutualAuth(true);
                gssContext.requestCredDeleg(true);
            }

            // TODO suspicious: this will always be null because no value has been assigned before. Assign directly?
            if (token == null) {
                token = new byte[0];
            }

            token = gssContext.initSecContext(token, 0, token.length);
            if (token == null) {
                throw new Exception("GSS security context initialization failed");
            }

            /*
             * IIS accepts Kerberos and SPNEGO tokens. Some other servers Jboss, Glassfish?
             * seem to only accept SPNEGO. Below wraps Kerberos into SPNEGO token.
             */
            if (spnegoGenerator != null && negotiationOid.toString().equals(KERBEROS_OID)) {
                token = spnegoGenerator.generateSpnegoDERObject(token);
            }

            gssContext.dispose();

            String tokenstr = new String(Base64.encode(token));
            log.debug("Sending response '{}' back to the server", tokenstr);

            return tokenstr;
        } catch (GSSException gsse) {
            log.error("generateToken", gsse);
            if (gsse.getMajor() == GSSException.DEFECTIVE_CREDENTIAL
                    || gsse.getMajor() == GSSException.CREDENTIALS_EXPIRED)
                throw new Exception(gsse.getMessage(), gsse);
            if (gsse.getMajor() == GSSException.NO_CRED)
                throw new Exception(gsse.getMessage(), gsse);
            if (gsse.getMajor() == GSSException.DEFECTIVE_TOKEN
                    || gsse.getMajor() == GSSException.DUPLICATE_TOKEN
                    || gsse.getMajor() == GSSException.OLD_TOKEN)
                throw new Exception(gsse.getMessage(), gsse);
            // other error
            throw new Exception(gsse.getMessage());
        } catch (IOException ex) {
            throw new Exception(ex.getMessage());
        }
    }
}
