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
 * 7.7.54 "Made the maximum request body size configurable" (M17 / I58): the
 * UIDL and push message reader stops at the configured size instead of
 * buffering an unbounded body in memory.
 */
public class ServerRpcHandlerBodySizeTest {

    /** Endless stream of characters that counts what was read. */
    static class CountingReader extends Reader {
        final long total;
        long read;

        CountingReader(long total) {
            this.total = total;
        }

        @Override
        public int read(char[] cbuf, int off, int len) {
            if (read >= total) {
                return -1;
            }
            int n = (int) Math.min(len, total - read);
            for (int i = 0; i < n; i++) {
                cbuf[off + i] = 'x';
            }
            read += n;
            return n;
        }

        @Override
        public void close() {
        }
    }

    private static UI uiWithLimit(String limit) {
        UI ui = Mockito.mock(UI.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration conf = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(ui.getSession()).thenReturn(session);
        Mockito.when(session.getService()).thenReturn(service);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(conf);
        Mockito.when(conf.getApplicationOrSystemProperty(Mockito.anyString(),
                Mockito.anyString())).thenReturn(limit);
        return ui;
    }

    @Test
    public void bodyOverTheLimitIsRejectedWithoutReadingItAll() {
        CountingReader reader = new CountingReader(10L * 1024 * 1024);
        Throwable failure = null;
        try {
            new ServerRpcHandler().handleRpc(uiWithLimit("1000"), reader,
                    Mockito.mock(VaadinRequest.class));
        } catch (Throwable t) {
            failure = t;
        }
        Assert.assertTrue("request body read beyond the limit: " + reader.read
                + " characters", reader.read <= 1000 + 64 * 1024);
        Assert.assertNotNull(failure);
        Assert.assertTrue(String.valueOf(failure),
                failure instanceof IOException);
        Assert.assertTrue(failure.getMessage(),
                failure.getMessage().contains("1000"));
    }

    @Test
    public void bodyUnderTheLimitIsRead() throws Exception {
        // an empty message is ignored by handleRpc, so use a reader that
        // returns exactly the limit and check that no size error is raised
        CountingReader reader = new CountingReader(1000);
        try {
            new ServerRpcHandler().handleRpc(uiWithLimit("1000"), reader,
                    Mockito.mock(VaadinRequest.class));
        } catch (Exception e) {
            // the content is not valid JSON: any error but the size one is
            // expected here
            Assert.assertFalse(String.valueOf(e.getMessage()),
                    String.valueOf(e.getMessage()).contains("exceeds"));
        }
        Assert.assertEquals(1000, reader.read);
    }

    @Test
    public void defaultAcceptsLargeMessages() throws Exception {
        CountingReader reader = new CountingReader(8L * 1024 * 1024);
        UI ui = uiWithLimit(null);
        try {
            new ServerRpcHandler().handleRpc(ui, reader,
                    Mockito.mock(VaadinRequest.class));
        } catch (Exception e) {
            Assert.assertFalse(String.valueOf(e.getMessage()),
                    String.valueOf(e.getMessage()).contains("exceeds"));
        }
        Assert.assertEquals(8L * 1024 * 1024, reader.read);
    }
}
