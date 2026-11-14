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
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

/**
 * M17, review R4: the maxRequestBodySize limit works for every accepted
 * value, Long.MAX_VALUE included, without arithmetic overflow, and reading
 * always ends at EOF. Each test has a timeout. Derived from the probes
 * RpcExtremeLimitProbe and RpcConfiguredMaxHang of the F2b verifier.
 */
public class RpcBodyLimitBoundaryTest {

    /** Records what the getMessage(Reader) hook returns. */
    static class Recording extends ServerRpcHandler {
        String message;

        @Override
        protected String getMessage(Reader reader) throws IOException {
            message = super.getMessage(reader);
            // stop handleRpc here: the body is not a valid UIDL message
            return "";
        }
    }

    /** A UI whose deployment configuration returns the given value. */
    private static UI ui(String configured) {
        UI ui = Mockito.mock(UI.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration conf = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(ui.getSession()).thenReturn(session);
        Mockito.when(session.getService()).thenReturn(service);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(conf);
        Mockito.when(conf.getApplicationOrSystemProperty(Mockito.anyString(),
                Mockito.anyString())).thenReturn(configured);
        return ui;
    }

    private static String body(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append('x');
        }
        return sb.toString();
    }

    /** Runs handleRpc with the real limit lookup; returns the message read. */
    private static String read(long limit, String body) throws Exception {
        Recording handler = new Recording();
        handler.handleRpc(ui(String.valueOf(limit)), new StringReader(body),
                Mockito.mock(VaadinRequest.class));
        return handler.message;
    }

    private static void assertRead(long limit, String body) throws Exception {
        Assert.assertEquals("limit " + limit, body, read(limit, body));
    }

    private static void assertExceeds(long limit, String body)
            throws Exception {
        try {
            read(limit, body);
            Assert.fail("body of " + body.length()
                    + " characters accepted with limit " + limit);
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage(),
                    e.getMessage().contains("exceeds"));
        }
    }

    @Test(timeout = 5000)
    public void maxLong() throws Exception {
        assertRead(Long.MAX_VALUE, "");
        assertRead(Long.MAX_VALUE, "abcd");
        assertRead(Long.MAX_VALUE, body(200000));
    }

    @Test(timeout = 5000)
    public void maxLongMinusOne() throws Exception {
        assertRead(Long.MAX_VALUE - 1, "");
        assertRead(Long.MAX_VALUE - 1, "abcd");
    }

    @Test(timeout = 5000)
    public void zero() throws Exception {
        assertRead(0, "");
        assertExceeds(0, "a");
    }

    @Test(timeout = 5000)
    public void one() throws Exception {
        assertRead(1, "");
        assertRead(1, "a");
        assertExceeds(1, "ab");
    }

    @Test(timeout = 5000)
    public void unlimited() throws Exception {
        assertRead(-1, "");
        assertRead(-1, body(200000));
    }

    @Test(timeout = 5000)
    public void ordinary() throws Exception {
        assertRead(1000, "");
        assertRead(1000, body(1000));
        assertExceeds(1000, body(1001));
    }

    /** As the verifier probe: a single read of four characters. */
    @Test(timeout = 5000)
    public void singleReadMakesProgress() throws Exception {
        for (final long limit : new long[] { Long.MAX_VALUE,
                Long.MAX_VALUE - 1, 1000 }) {
            final int[] observed = { -99 };
            ServerRpcHandler hook = new ServerRpcHandler() {
                @Override
                protected long getMaxRequestBodySize(UI ui) {
                    return limit;
                }

                @Override
                protected String getMessage(Reader reader) throws IOException {
                    observed[0] = reader.read(new char[4], 0, 4);
                    return "";
                }
            };
            hook.handleRpc(ui(null), new StringReader("abcd"),
                    Mockito.mock(VaadinRequest.class));
            Assert.assertEquals("no progress with limit " + limit, 4,
                    observed[0]);
        }
    }
}
