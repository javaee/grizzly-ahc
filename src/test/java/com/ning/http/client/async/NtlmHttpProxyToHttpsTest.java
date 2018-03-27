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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
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
    ServerConnector listener = new ServerConnector(server);
    listener.setHost("127.0.0.1");
    listener.setPort(port1);
    server.addConnector(listener);
    server.setHandler(configureHandler());
    server.start();

    // HTTPS Server
    HttpConfiguration https_config = new HttpConfiguration();
    https_config.setSecureScheme("https");
    https_config.setSecurePort(port2);
    https_config.setOutputBufferSize(32768);

    SecureRequestCustomizer src = new SecureRequestCustomizer();
    src.setStsMaxAge(2000);
    src.setStsIncludeSubDomains(true);
    https_config.addCustomizer(src);

    ClassLoader cl = getClass().getClassLoader();
    SslContextFactory sslContextFactory = new SslContextFactory();
    URL cacertsUrl = cl.getResource("ssltest-cacerts.jks");
    String trustStoreFile = new File(cacertsUrl.toURI()).getAbsolutePath();
    sslContextFactory.setTrustStorePath(trustStoreFile);
    sslContextFactory.setTrustStorePassword("changeit");
    sslContextFactory.setTrustStoreType("JKS");

    log.info("SSL certs path: {}", trustStoreFile);

    URL keystoreUrl = cl.getResource("ssltest-keystore.jks");
    String keyStoreFile = new File(keystoreUrl.toURI()).getAbsolutePath();
    sslContextFactory.setKeyStorePath(keyStoreFile);
    sslContextFactory.setKeyStorePassword("changeit");
    sslContextFactory.setKeyStoreType("JKS");

    log.info("SSL keystore path: {}", keyStoreFile);

    ServerConnector connector = new ServerConnector(server2,
            new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https_config));
    connector.setHost("127.0.0.1");
    connector.setPort(port2);

    server2.addConnector(connector);
    server2.setHandler(new EchoHandler());
    server2.start();
    log.info("Local Proxy Server (" + port1 + "), HTTPS Server (" + port2 + ") started successfully");
  }

  @Override
  public AbstractHandler configureHandler() throws Exception {
    return new ConnectHandler(new EchoHandler()) {
      AtomicInteger state = new AtomicInteger(1);
      AtomicBoolean authComplete = new AtomicBoolean(false);
      @Override
      public void handle(String pathInContext, org.eclipse.jetty.server.Request request,
                         HttpServletRequest httpRequest, HttpServletResponse httpResponse)
              throws IOException,ServletException {

        String authorization = httpRequest.getHeader("Proxy-Authorization");

        boolean asExpected = false;

        switch (state.getAndIncrement()) {

          case 1:
            if (authorization.equals("NTLM TlRMTVNTUAABAAAAAYIIogAAAAAoAAAAAAAAACgAAAAFASgKAAAADw==")) {
              httpResponse.setStatus(HttpStatus.PROXY_AUTHENTICATION_REQUIRED_407);
              httpResponse.setHeader("Proxy-Authenticate", "NTLM TlRMTVNTUAACAAAAAAAAACgAAAABggAAU3J2Tm9uY2UAAAAAAAAAAA==");
              asExpected = true;

            }
            break;

          case 2:
            if (authorization
                    .equals("NTLM TlRMTVNTUAADAAAAGAAYAEgAAAAYABgAYAAAABQAFAB4AAAADAAMAIwAAAASABIAmAAAAAAAAACqAAAAAYIAAgUBKAoAAAAPrYfKbe/jRoW5xDxHeoxC1gBmfWiS5+iX4OAN4xBKG/IFPwfH3agtPEia6YnhsADTVQBSAFMAQQAtAE0ASQBOAE8AUgBaAGEAcABoAG8AZABMAGkAZwBoAHQAQwBpAHQAeQA=")) {
              httpResponse.setStatus(HttpStatus.OK_200);
              super.handleConnect(request,httpRequest,httpResponse,request.getRequestURI());
              asExpected = true;
              authComplete.getAndSet(true);
            }
            break;
          default:
        }

        if (!asExpected) {
          httpResponse.setStatus(HttpStatus.FORBIDDEN_403);
        }
        if (authComplete.get() && HttpMethod.GET.is(httpRequest.getMethod())) {
          super.handle(pathInContext,request,httpRequest,httpResponse);
        }
        httpResponse.setContentLength(0);
        httpResponse.getOutputStream().flush();
        httpResponse.getOutputStream().close();
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
