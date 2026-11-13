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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.vaadin.server.DefaultDeploymentConfiguration;
import com.vaadin.server.MockServletConfig;
import com.vaadin.server.MockVaadinSession;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.communication.AtmospherePushConnection.State;
import com.vaadin.ui.UI;

/**
 * REM F2d, item I34: a push and a disconnect of the same
 * {@link AtmospherePushConnection} must not run concurrently.
 * <p>
 * The framework disconnects the push connection of a detached UI in a separate
 * thread that does not hold the session lock ({@code UI.setSession(null)}),
 * while a request thread holding the session lock may be pushing to the same
 * connection.
 */
public class AtmospherePushConnectionConcurrencyTest {

    private static final long TIMEOUT_MS = 5000;

    private MockVaadinSession session;
    private UI ui;
    private AtmosphereResource resource;
    private final List<String> events = Collections
            .synchronizedList(new ArrayList<String>());
    private final List<Throwable> failures = Collections
            .synchronizedList(new ArrayList<Throwable>());

    @Before
    public void setup() throws Exception {
        VaadinServlet servlet = new VaadinServlet();
        servlet.init(new MockServletConfig());
        VaadinServletService service = new VaadinServletService(servlet,
                new DefaultDeploymentConfiguration(UI.class,
                        new Properties()));
        session = new MockVaadinSession(service);
        ui = new UI() {
            @Override
            protected void init(VaadinRequest request) {
            }
        };
        session.lock();
        try {
            session.setCommunicationManager(
                    new com.vaadin.server.LegacyCommunicationManager(session));
            ui.setSession(session);
            ui.doInit(Mockito.mock(VaadinRequest.class), 1, "foo");
            session.addUI(ui);
        } finally {
            session.unlock();
        }

        resource = Mockito.mock(AtmosphereResource.class);
        Broadcaster broadcaster = Mockito.mock(Broadcaster.class);
        Mockito.when(resource.getBroadcaster()).thenReturn(broadcaster);
        Mockito.when(broadcaster.broadcast(Mockito.anyObject(),
                Mockito.any(AtmosphereResource.class)))
                .thenAnswer(new Answer<Future<Object>>() {
                    @Override
                    public Future<Object> answer(InvocationOnMock invocation) {
                        events.add("broadcast");
                        FutureTask<Object> done = new FutureTask<Object>(
                                new Callable<Object>() {
                                    @Override
                                    public Object call() {
                                        return "sent";
                                    }
                                });
                        done.run();
                        return done;
                    }
                });
    }

    /**
     * A disconnect that starts while a push is being sent must wait for the
     * push to complete, and then close the resource. In 7.7.17 the disconnect
     * closes the resource and clears it under the running push, which then
     * fails ("Push failed", assertion or NullPointerException).
     */
    @Test
    public void disconnectDuringPushWaitsForThePush() throws Exception {
        recordClose(null, null);
        final CountDownLatch inSend = new CountDownLatch(1);
        final CountDownLatch proceed = new CountDownLatch(1);
        final AtmospherePushConnection connection = new AtmospherePushConnection(
                ui) {
            @Override
            void sendMessage(String message) {
                inSend.countDown();
                await(proceed);
                super.sendMessage(message);
            }
        };
        connection.connect(resource);

        Thread pushThread = start("push", new Runnable() {
            @Override
            public void run() {
                session.lock();
                try {
                    connection.push(true);
                } finally {
                    session.unlock();
                }
            }
        });
        Assert.assertTrue("push did not start",
                inSend.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Like the thread started by UI.setSession(null): no session lock
        Thread disconnectThread = start("disconnect", new Runnable() {
            @Override
            public void run() {
                connection.disconnect();
            }
        });
        waitUntilNotRunnable(disconnectThread);
        proceed.countDown();

        join(pushThread, disconnectThread);
        assertNoFailures();
        Assert.assertEquals(
                "push must complete before the resource is closed",
                Arrays.asList("broadcast", "close"), events);
        Assert.assertFalse(connection.isConnected());
    }

    /**
     * A push that starts while a disconnect is closing the resource must not
     * write to the resource being closed; it must be deferred to the next
     * connection instead. In 7.7.17 the push is broadcast to the resource being
     * closed and is lost.
     */
    @Test
    public void pushDuringDisconnectIsDeferred() throws Exception {
        final CountDownLatch closing = new CountDownLatch(1);
        final CountDownLatch finishClose = new CountDownLatch(1);
        recordClose(closing, finishClose);
        final AtmospherePushConnection connection = new AtmospherePushConnection(
                ui);
        connection.connect(resource);

        Thread disconnectThread = start("disconnect", new Runnable() {
            @Override
            public void run() {
                connection.disconnect();
            }
        });
        Assert.assertTrue("disconnect did not start closing",
                closing.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Thread pushThread = start("push", new Runnable() {
            @Override
            public void run() {
                session.lock();
                try {
                    connection.push(true);
                } finally {
                    session.unlock();
                }
            }
        });
        pushThread.join(TIMEOUT_MS);
        finishClose.countDown();

        join(pushThread, disconnectThread);
        assertNoFailures();
        Assert.assertFalse(
                "push was written to a resource being closed: " + events,
                events.contains("broadcast"));
        Assert.assertEquals(State.PUSH_PENDING, connection.getState());
        Assert.assertFalse(connection.isConnected());
    }

    private void recordClose(final CountDownLatch closing,
            final CountDownLatch finishClose) throws Exception {
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                if (closing != null) {
                    closing.countDown();
                    await(finishClose);
                }
                events.add("close");
                return null;
            }
        }).when(resource).close();
    }

    private Thread start(String name, final Runnable task) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (Throwable t) {
                    failures.add(t);
                }
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("latch timeout");
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void waitUntilNotRunnable(Thread thread)
            throws InterruptedException {
        long end = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < end) {
            Thread.State state = thread.getState();
            if (state == Thread.State.TERMINATED
                    || state == Thread.State.BLOCKED
                    || state == Thread.State.WAITING
                    || state == Thread.State.TIMED_WAITING) {
                return;
            }
            Thread.sleep(5);
        }
    }

    private static void join(Thread... threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.join(TIMEOUT_MS);
            Assert.assertFalse("thread " + thread.getName() + " still running",
                    thread.isAlive());
        }
    }

    private void assertNoFailures() {
        if (!failures.isEmpty()) {
            Throwable first = failures.get(0);
            StackTraceElement[] trace = first.getStackTrace();
            AssertionError error = new AssertionError(
                    "concurrent push/disconnect failed: " + failures
                            + (trace.length > 0 ? " at " + trace[0] : ""));
            error.initCause(first);
            throw error;
        }
    }
}
