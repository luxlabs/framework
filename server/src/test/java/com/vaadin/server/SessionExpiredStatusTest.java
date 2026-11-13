/*
 * Copyright 2026 LuxLabs.
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * 7.7.53 "403 FORBIDDEN instead of 410 GONE when the session has expired"
 * (I56, server part in {@link VaadinService}).
 */
public class SessionExpiredStatusTest {

    @Test
    public void expiredSessionAnswers403() throws ServletException,
            ServiceException, IOException {
        VaadinServlet servlet = new VaadinServlet();
        servlet.init(new MockServletConfig());
        VaadinServletService service = servlet.getService();

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/some/resource");
        HttpServletResponse response = Mockito
                .mock(HttpServletResponse.class);

        service.handleSessionExpired(new VaadinServletRequest(request, service),
                new VaadinServletResponse(response, service));

        Mockito.verify(response, Mockito.never()).sendError(
                Mockito.eq(HttpServletResponse.SC_GONE), Mockito.anyString());
        Mockito.verify(response).sendError(HttpServletResponse.SC_FORBIDDEN,
                "Session expired");
    }
}
