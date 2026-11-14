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
package com.vaadin.server.communication;

import java.util.Collections;


import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;

/**
 * 7.7.25 "Clear thread local instances on connection lost in push handler"
 * (I19): the Atmosphere thread that reports a lost push connection must not
 * keep the session and the UI in its thread locals.
 */
public class PushHandlerConnectionLostTest {

    private VaadinServletService service;
    private VaadinSession session;
    private AtmosphereResourceEvent event;

    @Before
    public void setup() throws Exception {
        CurrentInstance.clearAll();
        service = Mockito.mock(VaadinServletService.class);
        session = Mockito.mock(VaadinSession.class);
        final UI ui = Mockito.mock(UI.class);
        Mockito.when(session.getUIs())
                .thenReturn(Collections.<UI> emptyList());
        Mockito.when(service.findVaadinSession(Mockito.any(VaadinRequest.class)))
                .thenAnswer(new Answer<VaadinSession>() {
                    @Override
                    public VaadinSession answer(InvocationOnMock invocation) {
                        VaadinSession.setCurrent(session);
                        return session;
                    }
                });
        Mockito.when(service.findUI(Mockito.any(VaadinRequest.class)))
                .thenAnswer(new Answer<UI>() {
                    @Override
                    public UI answer(InvocationOnMock invocation) {
                        UI.setCurrent(ui);
                        // not found by request data: as after a reload
                        return null;
                    }
                });

        event = Mockito.mock(AtmosphereResourceEvent.class);
        AtmosphereResource resource = Mockito.mock(AtmosphereResource.class);
        AtmosphereRequest request = Mockito.mock(AtmosphereRequest.class);
        Mockito.when(event.getResource()).thenReturn(resource);
        Mockito.when(resource.getRequest()).thenReturn(request);
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void threadLocalsAreClearedAfterConnectionLost() {
        new PushHandler(service).connectionLost(event);

        Assert.assertNull("VaadinSession left in the push thread",
                VaadinSession.getCurrent());
        Assert.assertNull("UI left in the push thread", UI.getCurrent());
    }

    @Test
    public void instancesOfTheCallerAreRestored() {
        VaadinService callerService = Mockito.mock(VaadinService.class);
        CurrentInstance.set(VaadinService.class, callerService);

        new PushHandler(service).connectionLost(event);

        Assert.assertSame(callerService, VaadinService.getCurrent());
        Assert.assertNull(VaadinSession.getCurrent());
        Assert.assertNull(UI.getCurrent());
    }
}
