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
 * I23, review R2: findOrCreateVaadinSession calls the protected
 * lockSession/unlockSession hooks, as in 7.7.17, and still releases the lock
 * when the session is invalidated while it is held. Derived from the probe
 * ProtectedHookProbeTest of the F2b verifier.
 */
public class SessionLockHooksTest {

    static class TrackingService extends VaadinServletService {
        int locked, unlocked;

        TrackingService(VaadinServlet s, DeploymentConfiguration c)
                throws ServiceException {
            super(s, c);
        }

        @Override
        protected void lockSession(WrappedSession s) {
            locked++;
            super.lockSession(s);
        }

        @Override
        protected void unlockSession(WrappedSession s) {
            unlocked++;
            super.unlockSession(s);
        }
    }

    static class TrackingServlet extends VaadinServlet {
        @Override
        protected VaadinServletService createServletService(
                DeploymentConfiguration c) throws ServiceException {
            TrackingService s = new TrackingService(this, c);
            s.init();
            return s;
        }
    }

    /**
     * @param invalidate
     *            if true, the session becomes invalid (every attribute access
     *            fails) as soon as findOrCreateVaadinSession reads anything
     *            but the lock; otherwise only that read fails
     */
    private static TrackingService run(final ReentrantLock lock,
            final boolean invalidate) throws Exception {
        TrackingServlet servlet = new TrackingServlet();
        servlet.init(new MockServletConfig());
        TrackingService service = (TrackingService) servlet.getService();
        final boolean[] invalidated = new boolean[1];
        WrappedSession session = Mockito.mock(WrappedSession.class);
        Mockito.when(session.getAttribute(Mockito.anyString()))
                .thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock i) {
                        String k = (String) i.getArguments()[0];
                        if (invalidated[0]) {
                            throw new IllegalStateException("invalidated");
                        }
                        if (k.endsWith(".lock")) {
                            return lock;
                        }
                        invalidated[0] = invalidate;
                        throw new IllegalStateException("end after locking");
                    }
                });
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/");
        Mockito.when(request.getService()).thenReturn(service);
        Mockito.when(request.getWrappedSession(Mockito.anyBoolean()))
                .thenReturn(session);
        Mockito.when(request.getWrappedSession()).thenReturn(session);
        try {
            service.findVaadinSession(request);
        } catch (Throwable expected) {
            // the probe ends the request after the lock is taken
        }
        return service;
    }

    @Test
    public void protectedHooksAreCalled() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        TrackingService service = run(lock, false);
        Assert.assertEquals("lockSession override skipped", 1, service.locked);
        Assert.assertEquals("unlockSession override skipped", 1,
                service.unlocked);
        Assert.assertFalse("real lock left held", lock.isLocked());
    }

    @Test
    public void protectedHooksAreCalledAndLockReleasedWhenInvalidated()
            throws Exception {
        ReentrantLock lock = new ReentrantLock();
        TrackingService service = run(lock, true);
        Assert.assertEquals("lockSession override skipped", 1, service.locked);
        Assert.assertEquals("unlockSession override skipped", 1,
                service.unlocked);
        Assert.assertFalse("lock left held after invalidation",
                lock.isLocked());
    }
}
