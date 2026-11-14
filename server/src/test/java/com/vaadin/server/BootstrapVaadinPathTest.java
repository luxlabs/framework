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

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * 7.7.51 "security check to BootstrapHandler to prevent serving of /VAADIN/*
 * as a context path" (M15 / I54). When the Vaadin servlet is mapped to a
 * sub path (for example /app/*), a request to /app/VAADIN/... is not a static
 * resource for {@link VaadinServlet#serveStaticResources} and reaches the
 * {@link BootstrapHandler}, which serves the application page under /VAADIN/.
 */
public class BootstrapVaadinPathTest {

    private static boolean bootstrapHandles(String pathInfo,
            VaadinSession session) throws Exception {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn(pathInfo);
        Mockito.when(request.getService())
                .thenReturn(Mockito.mock(VaadinService.class));
        Mockito.when(session.getUIProviders())
                .thenReturn(Collections.<UIProvider> emptyList());
        return new com.vaadin.server.communication.ServletBootstrapHandler().handleRequest(session, request,
                Mockito.mock(VaadinResponse.class));
    }

    @Test
    public void vaadinPathIsNotBootstrapped() throws Exception {
        for (String path : new String[] { "/VAADIN/", "/VAADIN/foo",
                "/VAADIN/themes/valo/x.html", "/VAADIN/../VAADIN/a" }) {
            VaadinSession session = Mockito.mock(VaadinSession.class);
            Assert.assertFalse(path, bootstrapHandles(path, session));
            Mockito.verify(session, Mockito.never()).getUIProviders();
        }
    }

    @Test
    public void applicationPathsAreBootstrapped() throws Exception {
        for (String path : new String[] { "/", "/view", "/VAADINX",
                "/a/VAADIN/b" }) {
            VaadinSession session = Mockito.mock(VaadinSession.class);
            bootstrapHandles(path, session);
            Mockito.verify(session).getUIProviders();
        }
        VaadinSession session = Mockito.mock(VaadinSession.class);
        bootstrapHandles(null, session);
        Mockito.verify(session).getUIProviders();
    }
}
