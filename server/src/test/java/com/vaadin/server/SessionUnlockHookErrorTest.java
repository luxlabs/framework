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

import java.util.concurrent.locks.ReentrantLock;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * I23, review R2 (round 2): an IllegalStateException of an unlockSession
 * override while the session is still valid propagates, as in 7.7.17; only a
 * real invalidation of the session triggers the safe release, which never
 * unlocks twice. Derived from the probe LockReleaseProbe of the F2b
 * verifier: the nine cases, with and without outer (reentrant) acquisitions.
 */
public class SessionUnlockHookErrorTest {

    static class Service extends VaadinServletService {
        final String mode;
        boolean invalid;
        int locked, unlocked;
        final VaadinSession existing = Mockito.mock(VaadinSession.class);

        Service(VaadinServlet s, DeploymentConfiguration c, String mode)
                throws ServiceException {
            super(s, c);
            this.mode = mode;
        }

        @Override
        protected void lockSession(WrappedSession s) {
            locked++;
            if (mode.equals("no-super")) {
                getSessionLock(s).lock();
            } else {
                super.lockSession(s);
            }
        }

        @Override
        protected VaadinSession getExistingSession(VaadinRequest r,
                boolean create) {
            if (mode.equals("invalid-before-unlock")) {
                invalid = true;
            }
            return existing;
        }

        @Override
        protected void unlockSession(WrappedSession s) {
            unlocked++;
            super.unlockSession(s);
            if (mode.equals("invalid-after-unlock")) {
                invalid = true;
                throw new IllegalStateException(
                        "session invalidated after unlock");
            }
            if (mode.equals("valid-hook-error")) {
                throw new IllegalStateException(
                        "cleanup hook failed while session is valid");
            }
        }
    }

    static class Servlet extends VaadinServlet {
        final String mode;

        Servlet(String mode) {
            this.mode = mode;
        }

        @Override
        protected VaadinServletService createServletService(
                DeploymentConfiguration c) throws ServiceException {
            Service s = new Service(this, c, mode);
            s.init();
            return s;
        }
    }

    private void check(String mode, int outer, boolean expectHookFailure)
            throws Exception {
        Servlet servlet = new Servlet(mode);
        servlet.init(new MockServletConfig());
        final Service service = (Service) servlet.getService();
        final ReentrantLock lock = new ReentrantLock();
        WrappedSession wrapped = Mockito.mock(WrappedSession.class);
        Mockito.when(wrapped.getAttribute(Mockito.anyString()))
                .thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock i) {
                        if (service.invalid) {
                            throw new IllegalStateException("invalidated");
                        }
                        return ((String) i.getArguments()[0]).endsWith(".lock")
                                ? lock : null;
                    }
                });
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/");
        Mockito.when(request.getService()).thenReturn(service);
        Mockito.when(request.getWrappedSession(Mockito.anyBoolean()))
                .thenReturn(wrapped);
        Mockito.when(request.getWrappedSession()).thenReturn(wrapped);
        for (int i = 0; i < outer; i++) {
            lock.lock();
        }
        Throwable caught = null;
        try {
            try {
                service.findVaadinSession(request);
            } catch (Throwable t) {
                caught = t;
            }
            Assert.assertEquals("lock hook", 1, service.locked);
            Assert.assertEquals("unlock hook", 1, service.unlocked);
            Assert.assertEquals(
                    "release exactly the one acquisition, mode=" + mode,
                    outer, lock.getHoldCount());
            if (expectHookFailure) {
                Assert.assertTrue(
                        "a valid-session hook failure was swallowed: "
                                + caught,
                        caught instanceof IllegalStateException && caught
                                .getMessage().contains("cleanup hook failed"));
            } else if (!mode.startsWith("invalid")) {
                Assert.assertNull(caught);
            }
        } finally {
            while (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            servlet.destroy();
        }
    }

    @Test
    public void normalZero() throws Exception {
        check("normal", 0, false);
    }

    @Test
    public void normalReentrant() throws Exception {
        check("normal", 2, false);
    }

    @Test
    public void noSuperHook() throws Exception {
        check("no-super", 1, false);
    }

    @Test
    public void invalidBeforeZero() throws Exception {
        check("invalid-before-unlock", 0, false);
    }

    @Test
    public void invalidBeforeReentrant() throws Exception {
        check("invalid-before-unlock", 2, false);
    }

    @Test
    public void invalidAfterZero() throws Exception {
        check("invalid-after-unlock", 0, false);
    }

    @Test
    public void invalidAfterReentrant() throws Exception {
        check("invalid-after-unlock", 2, false);
    }

    @Test
    public void validHookFailureZero() throws Exception {
        check("valid-hook-error", 0, true);
    }

    @Test
    public void validHookFailureReentrant() throws Exception {
        check("valid-hook-error", 2, true);
    }
}
