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
package com.vaadin.client.communication;

import java.lang.reflect.Field;

import org.easymock.EasyMock;
import org.junit.Test;
import org.objenesis.ObjenesisStd;

import com.google.gwt.http.client.Response;
import com.vaadin.client.ApplicationConnection;

/**
 * REM F2d, item I56 (client part): the client must treat a heartbeat answered
 * with 403 Forbidden as an expired session (the server no longer sends 410
 * Gone), showing the session expired notification and stopping the
 * application instead of trying to reconnect.
 */
public class DefaultConnectionStateHandlerSessionExpiredTest {

    @Test
    public void heartbeatForbiddenMeansSessionExpired() {
        ApplicationConnection connection = EasyMock
                .createNiceMock(ApplicationConnection.class);
        connection.showSessionExpiredError(null);
        EasyMock.expectLastCall().once();
        connection.setApplicationRunning(false);
        EasyMock.expectLastCall().once();
        EasyMock.replay(connection);

        DefaultConnectionStateHandler handler = newHandler(connection);
        handler.heartbeatInvalidStatusCode(null, response(403));

        EasyMock.verify(connection);
    }

    @Test
    public void heartbeatNotFoundIsIgnored() {
        ApplicationConnection connection = EasyMock
                .createStrictMock(ApplicationConnection.class);
        // no call expected: a closed UI is not a session expiration
        EasyMock.replay(connection);

        DefaultConnectionStateHandler handler = newHandler(connection);
        handler.heartbeatInvalidStatusCode(null, response(404));

        EasyMock.verify(connection);
    }

    /**
     * The constructor and setConnection use GWT.create and the reconnect
     * dialog, which are not available on the JVM; the status code handling
     * under test only uses the connection field, which is set directly.
     */
    private static DefaultConnectionStateHandler newHandler(
            ApplicationConnection connection) {
        DefaultConnectionStateHandler handler = (DefaultConnectionStateHandler) new ObjenesisStd()
                .newInstance(DefaultConnectionStateHandler.class);
        try {
            Field field = DefaultConnectionStateHandler.class
                    .getDeclaredField("connection");
            field.setAccessible(true);
            field.set(handler, connection);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return handler;
    }

    private static Response response(int status) {
        Response response = EasyMock.createNiceMock(Response.class);
        EasyMock.expect(response.getStatusCode()).andReturn(status)
                .anyTimes();
        EasyMock.replay(response);
        return response;
    }
}
