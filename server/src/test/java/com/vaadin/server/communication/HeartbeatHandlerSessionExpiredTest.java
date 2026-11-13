/*
 * Part of the 7.7.17.1 maintenance fork of Vaadin Framework (REM F2d).
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
package com.vaadin.server.communication;

import java.io.IOException;

import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;

/**
 * REM F2d, item I56: an expired session must be reported to a heartbeat
 * request with 403 Forbidden (not 410 Gone), and the response must not be
 * cached.
 */
public class HeartbeatHandlerSessionExpiredTest {

    @Test
    public void expiredHeartbeatGetsForbiddenAndNoCache() throws IOException {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/HEARTBEAT/");
        VaadinResponse response = Mockito.mock(VaadinResponse.class);

        boolean handled = new HeartbeatHandler().handleSessionExpired(request,
                response);

        org.junit.Assert.assertTrue(handled);
        Mockito.verify(response).sendError(403, "Session expired");
        Mockito.verify(response, Mockito.never()).sendError(
                Mockito.eq(410), Mockito.anyString());
        Mockito.verify(response).setHeader("Cache-Control", "no-cache");
        Mockito.verify(response).setHeader("Content-Type", "text/plain");
    }

    @Test
    public void nonHeartbeatRequestIsNotHandled() throws IOException {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/UIDL/");
        VaadinResponse response = Mockito.mock(VaadinResponse.class);

        org.junit.Assert.assertFalse(new HeartbeatHandler()
                .handleSessionExpired(request, response));
        Mockito.verifyZeroInteractions(response);
    }
}
