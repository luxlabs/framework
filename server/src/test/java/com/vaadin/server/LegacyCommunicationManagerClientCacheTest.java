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
package com.vaadin.server;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.server.LegacyCommunicationManager.ClientCache;
import com.vaadin.ui.UI;

/**
 * REM F2d, item I20: the {@link ClientCache} of a UI must be released when the
 * UI is detached, otherwise it leaks for the lifetime of the session.
 */
public class LegacyCommunicationManagerClientCacheTest {

    @Test
    public void clientCacheIsRemovedWhenUiIsDetached() throws Exception {
        VaadinServlet servlet = new VaadinServlet();
        servlet.init(new MockServletConfig());
        VaadinServletService service = new VaadinServletService(servlet,
                new DefaultDeploymentConfiguration(UI.class,
                        new Properties()));
        MockVaadinSession session = new MockVaadinSession(service);
        LegacyCommunicationManager manager = new LegacyCommunicationManager(
                session);

        UI ui = new UI() {
            @Override
            protected void init(VaadinRequest request) {
            }
        };

        session.lock();
        try {
            ui.setSession(session);
            ui.doInit(Mockito.mock(VaadinRequest.class), 1, "foo");
            session.addUI(ui);

            ClientCache cache = manager.getClientCache(ui);
            Assert.assertNotNull(cache);
            Assert.assertSame(cache, manager.getClientCache(ui));
            Assert.assertEquals(1, getUiToClientCache(manager).size());

            // As in a request: the detached UI is the current one
            UI.setCurrent(ui);
            session.removeUI(ui);

            Assert.assertTrue(
                    "ClientCache of a detached UI is still referenced: "
                            + getUiToClientCache(manager),
                    getUiToClientCache(manager).isEmpty());
        } finally {
            UI.setCurrent(null);
            session.unlock();
        }
    }

    private static Map<?, ?> getUiToClientCache(
            LegacyCommunicationManager manager) throws Exception {
        Field field = LegacyCommunicationManager.class
                .getDeclaredField("uiToClientCache");
        field.setAccessible(true);
        return (Map<?, ?>) field.get(manager);
    }
}
