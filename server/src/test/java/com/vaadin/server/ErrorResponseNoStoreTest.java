/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.server.communication.UidlRequestHandler;

/**
 * 7.7.45 "Set no-store headers for error responses" (M16 / I43): critical
 * notifications, session expired and cookies disabled responses must not be
 * stored by caches.
 */
public class ErrorResponseNoStoreTest {

    private VaadinServlet servlet;
    private VaadinServletService service;

    @Before
    public void setup() throws ServletException {
        servlet = new VaadinServlet();
        servlet.init(new MockServletConfig());
        service = servlet.getService();
    }

    private static HttpServletResponse httpResponse() throws IOException {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream())
                .thenReturn(new ServletOutputStream() {
                    @Override
                    public void write(int b) {
                        out.write(b);
                    }
                });
        return response;
    }

    private HttpServletRequest httpRequest(String pathInfo) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn(pathInfo);
        Mockito.when(request.getRequestedSessionId()).thenReturn(null);
        return request;
    }

    private static void assertNoStore(HttpServletResponse response) {
        Mockito.verify(response).setHeader("Cache-Control",
                "no-cache, no-store, must-revalidate");
        Mockito.verify(response).setHeader("Pragma", "no-cache");
        Mockito.verify(response).setHeader("Expires", "0");
    }

    @Test
    public void serviceCriticalNotification() throws IOException {
        HttpServletResponse response = httpResponse();
        service.criticalNotification(
                new VaadinServletRequest(httpRequest("/UIDL/"), service),
                new VaadinServletResponse(response, service), "c", "m", "d",
                null);
        assertNoStore(response);
    }

    @Test
    public void servletCriticalNotificationUidl() throws IOException {
        HttpServletResponse response = httpResponse();
        servlet.criticalNotification(
                new VaadinServletRequest(httpRequest("/UIDL/"), service),
                new VaadinServletResponse(response, service), "c", "m", "d",
                null);
        assertNoStore(response);
    }

    @Test
    public void servletCriticalNotificationHtml() throws IOException {
        HttpServletResponse response = httpResponse();
        servlet.criticalNotification(
                new VaadinServletRequest(httpRequest("/"), service),
                new VaadinServletResponse(response, service), "c", "m", "d",
                null);
        assertNoStore(response);
    }

    @Test
    public void uidlSessionExpired() throws IOException {
        HttpServletResponse response = httpResponse();
        new UidlRequestHandler().handleSessionExpired(
                new VaadinServletRequest(httpRequest("/UIDL/"), service),
                new VaadinServletResponse(response, service));
        assertNoStore(response);
    }

    @Test
    public void cookiesDisabled() throws IOException, ServletException {
        HttpServletResponse response = httpResponse();
        HttpServletRequest request = httpRequest("/UIDL/");
        Mockito.when(request.getMethod()).thenReturn("POST");
        Mockito.when(request.getRequestURI()).thenReturn("/UIDL/");
        Mockito.when(request.getContextPath()).thenReturn("");
        Mockito.when(request.getServletPath()).thenReturn("");
        servlet.service(request, response);
        assertNoStore(response);
    }

    @Test
    public void exceptionDuringUidlRequest() throws Exception {
        HttpServletResponse response = httpResponse();
        HttpServletRequest request = httpRequest("/UIDL/");
        VaadinServletService failing = new VaadinServletService(servlet,
                service.getDeploymentConfiguration()) {
            @Override
            public VaadinSession findVaadinSession(VaadinRequest request)
                    throws SessionExpiredException, ServiceException {
                throw new IllegalStateException("simulated failure");
            }
        };
        failing.init();
        failing.handleRequest(new VaadinServletRequest(request, failing),
                new VaadinServletResponse(response, failing));
        assertNoStore(response);
    }
}
