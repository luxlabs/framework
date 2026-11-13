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

import java.io.IOException;
import java.io.Reader;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

/**
 * M17, review R2: handleRpc calls the protected getMessage(Reader) hook, as
 * in 7.7.17, and the size limit still holds on what the hook reads. Derived
 * from the probe ProtectedHookProbeTest of the F2b verifier.
 */
public class RpcGetMessageHookTest {

    static class InterceptingRpc extends ServerRpcHandler {
        int called;

        @Override
        protected String getMessage(Reader r) throws IOException {
            called++;
            return "";
        }
    }

    static class ReadingRpc extends ServerRpcHandler {
        int called;

        @Override
        protected String getMessage(Reader r) throws IOException {
            called++;
            return super.getMessage(r);
        }
    }

    @Test
    public void getMessageHookIsCalled() throws Exception {
        InterceptingRpc handler = new InterceptingRpc();
        UI ui = Mockito.mock(UI.class);
        Mockito.when(ui.getSession())
                .thenReturn(Mockito.mock(VaadinSession.class));
        Throwable error = null;
        try {
            handler.handleRpc(ui, new Reader() {
                @Override
                public int read(char[] c, int o, int l) throws IOException {
                    throw new IOException(
                            "reader should be intercepted by the override");
                }

                @Override
                public void close() {
                }
            }, Mockito.mock(VaadinRequest.class));
        } catch (Throwable t) {
            error = t;
        }
        Assert.assertEquals("getMessage(Reader) override skipped; error="
                + error, 1, handler.called);
        Assert.assertNull(error);
    }

    @Test
    public void limitHoldsThroughTheHook() throws Exception {
        UI ui = Mockito.mock(UI.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration conf = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(ui.getSession()).thenReturn(session);
        Mockito.when(session.getService()).thenReturn(service);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(conf);
        Mockito.when(conf.getApplicationOrSystemProperty(Mockito.anyString(),
                Mockito.anyString())).thenReturn("1000");
        ServerRpcHandlerBodySizeTest.CountingReader reader = new ServerRpcHandlerBodySizeTest.CountingReader(
                10L * 1024 * 1024);
        ReadingRpc handler = new ReadingRpc();
        Throwable error = null;
        try {
            handler.handleRpc(ui, reader, Mockito.mock(VaadinRequest.class));
        } catch (Throwable t) {
            error = t;
        }
        Assert.assertEquals(1, handler.called);
        Assert.assertTrue("read beyond the limit through the hook: "
                + reader.read, reader.read <= 1000 + 64 * 1024);
        Assert.assertTrue(String.valueOf(error), error instanceof IOException
                && error.getMessage().contains("exceeds"));
    }
}
