/*
 * Copyright (c) 2017-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2015 AsyncHttpClient Project. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at
 *     http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.ning.http.client.async;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Realm;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ConnectHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public abstract class NtlmHttpProxyToHttpsTest extends AbstractBasicTest {
  private Server server2;

  @AfterClass(alwaysRun = true)
  public void tearDownGlobal() throws Exception {
    server.stop();
    server2.stop();
  }

  @BeforeClass(alwaysRun = true)
  public void setUpGlobal() throws Exception {
    // HTTP Proxy Server
    server = new Server();
    // HTTPS Server
    server2 = new Server();

    port1 = findFreePort();
    port2 = findFreePort();

    // Proxy Server configuration
    Connector listener = new SelectChannelConnector();
    listener.setHost("127.0.0.1");
    listener.setPort(port1);
    server.addConnector(listener);
    server.setHandler(configureHandler());
    server.start();

    // HTTPS Server
    SslSocketConnector connector = new SslSocketConnector();
    connector.setHost("127.0.0.1");
    connector.setPort(port2);

    ClassLoader cl = getClass().getClassLoader();
    // override system properties
    URL keystoreUrl = cl.getResource("ssltest-keystore.jks");
    String keyStoreFile = new File(keystoreUrl.toURI()).getAbsolutePath();
    connector.setKeystore(keyStoreFile);
    connector.setKeyPassword("changeit");
    connector.setKeystoreType("JKS");

    log.info("SSL keystore path: {}", keyStoreFile);

    server2.addConnector(connector);
    server2.setHandler(new EchoHandler());
    server2.start();
    log.info("Local Proxy Server (" + port1 + "), HTTPS Server (" + port2 + ") started successfully");
  }

  @Override
  public AbstractHandler configureHandler() throws Exception {
    return new ConnectHandler(new EchoHandler()) {

      boolean authenticated = false;

      @Override
      protected void handleConnect(org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
                                             HttpServletResponse response,
                                             String serverAddress) throws ServletException, IOException {
        super.handleConnect(baseRequest, request, response, serverAddress);
        if (!authenticated) {
          response.getOutputStream().flush();
        }
      }

      @Override
      protected boolean handleAuthentication(HttpServletRequest request, HttpServletResponse response, String address) throws
        ServletException, IOException {
        String authorization = request.getHeader("Proxy-Authorization");
        response.setHeader("Content-Length", "0");
        response.setHeader("Connection", "keep-alive");
        if (authorization == null) {
          response.setStatus(407);
          response.setHeader("Proxy-Authenticate", "NTLM");
          return false;
        } else if (authorization.equals("NTLM TlRMTVNTUAABAAAAAYIIogAAAAAoAAAAAAAAACgAAAAFASgKAAAADw==")) {
          response.setStatus(407);
          response.setHeader("Proxy-Authenticate", "NTLM TlRMTVNTUAACAAAAAAAAACgAAAABggAAU3J2Tm9uY2UAAAAAAAAAAA==");
          return false;
        } else if (authorization
          .equals(
            "NTLM TlRMTVNTUAADAAAAGAAYAEgAAAAYABgAYAAAABQAFAB4AAAADAAMAIwAAAASABIAmAAAAAAAAACqAAAAAYIAAgUBKAoAAAAPrYfKbe/jRoW5xDxHeoxC1gBmfWiS5+iX4OAN4xBKG/IFPwfH3agtPEia6YnhsADTVQBSAFMAQQAtAE0ASQBOAE8AUgBaAGEAcABoAG8AZABMAGkAZwBoAHQAQwBpAHQAeQA=")) {
          response.setStatus(200);
          authenticated = true;
          return true;
        } else {
          response.setStatus(401);
          return false;
        }
      }
    };
  }

  @Test
  public void httpProxyToHttpsTargetTest() throws IOException, InterruptedException, ExecutionException,
    NoSuchAlgorithmException {
    try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build())) {
      Request
        request = new RequestBuilder("GET").setProxyServer(ntlmProxy()).setUrl(getTargetUrl2()).build();
      Future<Response> responseFuture = client.executeRequest(request);
      Response response = responseFuture.get();
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getStatusCode(), HttpServletResponse.SC_OK);
      Assert.assertEquals("127.0.0.1:" + port2, response.getHeader("x-host"));
    }
  }

  private ProxyServer ntlmProxy() throws UnknownHostException {
    ProxyServer proxyServer = new ProxyServer("127.0.0.1", port1, "Zaphod", "Beeblebrox").setNtlmDomain("Ursa-Minor");
    proxyServer.setNtlmHost("LightCity");
    proxyServer.setScheme(Realm.AuthScheme.NTLM);
    return proxyServer;
  }

}
