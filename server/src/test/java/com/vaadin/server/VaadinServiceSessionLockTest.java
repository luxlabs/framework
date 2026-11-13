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
package com.vaadin.server;

import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletException;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * 7.7.27 "Prevent possible deadlock in findOrCreateVaadinSession" (I23): if
 * the HTTP session is invalidated by another thread while the session lock is
 * held, the lock must still be released.
 */
public class VaadinServiceSessionLockTest {

    @Test
    public void lockIsReleasedWhenSessionIsInvalidatedWhileLocked()
            throws ServletException {
        VaadinServlet servlet = new VaadinServlet();
        servlet.init(new MockServletConfig());
        VaadinServletService service = servlet.getService();

        final ReentrantLock lock = new ReentrantLock();
        final boolean[] invalidated = new boolean[1];
        WrappedSession session = Mockito.mock(WrappedSession.class);
        Mockito.when(session.getAttribute(Mockito.anyString()))
                .thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) {
                        String name = (String) invocation.getArguments()[0];
                        if (invalidated[0]) {
                            throw new IllegalStateException("invalidated");
                        }
                        if (name.endsWith(".lock")) {
                            return lock;
                        }
                        // another thread invalidates the HTTP session while
                        // this one holds the Vaadin session lock
                        invalidated[0] = true;
                        throw new IllegalStateException("invalidated");
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
            // the request fails because the session is gone
        }
        Assert.assertFalse(
                "session lock still held after the session was invalidated: next requests would block",
                lock.isLocked());
    }
}
