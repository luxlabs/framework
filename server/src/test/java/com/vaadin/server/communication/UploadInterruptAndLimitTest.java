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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.server.LegacyCommunicationManager;
import com.vaadin.server.ServletPortletHelper;
import com.vaadin.server.StreamVariable;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.ConnectorTracker;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.tests.util.MockUI;
import com.vaadin.util.CurrentInstance;

/**
 * CVE-2025-9467 (M06), 7.7.48 "Fixed Upload interrupt issue" (I48) and the
 * server part of 7.7.43 "preventing upload if no file has been selected"
 * (I41), driven through {@link FileUploadHandler} with real multipart
 * requests, with and without Content-Length (chunked transfer).
 * <p>
 * On 7.7.17 there is no framework limit: the size check available to an
 * application is the one on the declared length in the started listener (as
 * MOS does). Where the fixed version offers {@code Upload#setMaxFileSize},
 * the test uses it (looked up by reflection so that the test also compiles
 * against 7.7.17).
 */
public class UploadInterruptAndLimitTest {

    private static final String BOUNDARY = "----REMF2bBoundary";
    private static final int UI_ID = 7;
    private static final String CONNECTOR_ID = "11";
    private static final String VARIABLE = "action";
    private static final String SECKEY = "seckey-1";
    private static final int LIMIT = 1000;

    /** Receiver that records what reaches it. */
    static class RecordingReceiver implements Upload.Receiver {
        int calls;
        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        boolean closed;

        @Override
        public OutputStream receiveUpload(String filename, String mimeType) {
            calls++;
            return new OutputStream() {
                @Override
                public void write(int b) {
                    data.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    data.write(b, off, len);
                }

                @Override
                public void close() {
                    closed = true;
                }
            };
        }
    }

    static class TestUpload extends Upload {
        StreamVariable streamVariable() {
            return getStreamVariable();
        }
    }

    private TestUpload upload;
    private RecordingReceiver receiver;
    private final List<String> events = new ArrayList<String>();
    private final List<Exception> failures = new ArrayList<Exception>();
    private VaadinSession session;
    private VaadinResponse response;

    @Before
    public void setup() throws IOException {
        upload = new TestUpload();
        CurrentInstance.clearAll();
        new MockUI().setContent(upload);
        receiver = new RecordingReceiver();
        upload.setReceiver(receiver);
        upload.addStartedListener(new Upload.StartedListener() {
            @Override
            public void uploadStarted(Upload.StartedEvent event) {
                events.add("started");
            }
        });
        upload.addSucceededListener(new Upload.SucceededListener() {
            @Override
            public void uploadSucceeded(Upload.SucceededEvent event) {
                events.add("succeeded");
            }
        });
        upload.addFailedListener(new Upload.FailedListener() {
            @Override
            public void uploadFailed(Upload.FailedEvent event) {
                events.add("failed");
                failures.add(event.getReason());
            }
        });

        session = Mockito.mock(VaadinSession.class);
        UI ui = Mockito.mock(UI.class);
        ConnectorTracker tracker = Mockito.mock(ConnectorTracker.class);
        StreamVariable sv = upload.streamVariable();
        Mockito.when(session.getUIById(UI_ID)).thenReturn(ui);
        Mockito.when(session.getCommunicationManager())
                .thenReturn(Mockito.mock(LegacyCommunicationManager.class));
        Mockito.when(ui.getConnectorTracker()).thenReturn(tracker);
        Mockito.when(tracker.getStreamVariable(CONNECTOR_ID, VARIABLE))
                .thenReturn(sv);
        Mockito.when(tracker.getSeckey(sv)).thenReturn(SECKEY);
        Mockito.when(tracker.getConnector(CONNECTOR_ID)).thenReturn(upload);
        response = Mockito.mock(VaadinResponse.class);
        Mockito.when(response.getOutputStream())
                .thenReturn(new ByteArrayOutputStream());
    }

    static byte[] multipart(String filename, int size)
            throws UnsupportedEncodingException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String head = "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"" + VARIABLE
                + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        byte[] h = head.getBytes("UTF-8");
        body.write(h, 0, h.length);
        for (int i = 0; i < size; i++) {
            body.write('x');
        }
        byte[] t = ("\r\n--" + BOUNDARY + "--\r\n").getBytes("UTF-8");
        body.write(t, 0, t.length);
        return body.toByteArray();
    }

    private void post(byte[] body, boolean withContentLength)
            throws IOException {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getPathInfo())
                .thenReturn("/" + ServletPortletHelper.UPLOAD_URL_PREFIX + UI_ID
                        + "/" + CONNECTOR_ID + "/" + VARIABLE + "/" + SECKEY);
        Mockito.when(request.getContentType())
                .thenReturn("multipart/form-data; boundary=" + BOUNDARY);
        // chunked transfer: no Content-Length header at all
        Mockito.when(request.getHeader("Content-Length")).thenReturn(
                withContentLength ? String.valueOf(body.length) : null);
        InputStream in = new ByteArrayInputStream(body);
        Mockito.when(request.getInputStream()).thenReturn(in);
        Assert.assertTrue(
                new FileUploadHandler().handleRequest(session, request, response));
    }

    /**
     * Applies a limit of {@link #LIMIT} bytes: the framework limit where it
     * exists, otherwise the application check on the declared length.
     */
    private boolean applyLimit() {
        try {
            Method m = Upload.class.getMethod("setMaxFileSize", long.class);
            m.invoke(upload, (long) LIMIT);
            return true;
        } catch (NoSuchMethodException e) {
            upload.addStartedListener(new Upload.StartedListener() {
                @Override
                public void uploadStarted(Upload.StartedEvent event) {
                    if (event.getContentLength() > LIMIT) {
                        upload.interruptUpload();
                    }
                }
            });
            return false;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void interruptInStartedListenerDeliversNothing() throws IOException {
        upload.addStartedListener(new Upload.StartedListener() {
            @Override
            public void uploadStarted(Upload.StartedEvent event) {
                upload.interruptUpload();
            }
        });
        post(multipart("a.bin", 10000), true);

        Assert.assertEquals("bytes delivered after interruptUpload()", 0,
                receiver.data.size());
        Assert.assertEquals("receiver asked for an output stream after interruptUpload()",
                0, receiver.calls);
        Assert.assertTrue(events.contains("failed"));
        Assert.assertFalse(events.contains("succeeded"));
        Assert.assertFalse(upload.isUploading());
    }

    @Test
    public void limitHoldsWithoutContentLength() throws IOException {
        boolean frameworkLimit = applyLimit();
        post(multipart("big.bin", 20000), false);
        if (frameworkLimit) {
            Assert.assertEquals(1, failures.size());
            Assert.assertEquals("FileSizeLimitExceededException",
                    failures.get(0).getClass().getSimpleName());
        }

        Assert.assertTrue("bytes over the limit delivered without Content-Length: "
                + receiver.data.size(), receiver.data.size() <= LIMIT);
        Assert.assertFalse("over-limit upload reported as succeeded",
                events.contains("succeeded"));
        Assert.assertTrue(events.contains("failed"));
        Assert.assertTrue("output stream not closed", receiver.closed);
        Assert.assertFalse(upload.isUploading());
    }

    @Test
    public void limitWithDeclaredLengthOpensNoStream() throws IOException {
        boolean frameworkLimit = applyLimit();
        post(multipart("big.bin", 20000), true);
        if (frameworkLimit) {
            Assert.assertEquals("FileSizeLimitExceededException",
                    failures.get(0).getClass().getSimpleName());
        }

        Assert.assertEquals("receiver asked for an output stream for an upload declared over the limit",
                0, receiver.calls);
        Assert.assertEquals(0, receiver.data.size());
        Assert.assertTrue(events.contains("failed"));
        Assert.assertFalse(events.contains("succeeded"));
    }

    @Test
    public void uploadUnderLimitSucceeds() throws IOException {
        applyLimit();
        post(multipart("small.bin", LIMIT), false);

        Assert.assertEquals(LIMIT, receiver.data.size());
        Assert.assertTrue(events.contains("succeeded"));
        Assert.assertFalse(events.contains("failed"));
    }

    @Test
    public void submitWithoutFileDoesNotStartUpload() throws IOException {
        post(multipart("", 0), true);

        Assert.assertEquals("receiver called for a submit without a file", 0,
                receiver.calls);
        Assert.assertFalse("upload started without a file",
                events.contains("started"));
        Assert.assertFalse(events.contains("succeeded"));
        Assert.assertFalse(upload.isUploading());
    }

    @Test
    public void cleanStreamVariableOfUnknownConnectorDoesNotFail() {
        CurrentInstance.clearAll();
        MockUI ui = new MockUI();
        Upload other = new Upload();
        ui.setContent(other);
        ConnectorTracker tracker = ui.getConnectorTracker();
        String id = other.getConnectorId();
        tracker.addStreamVariable(id, "a", Mockito.mock(StreamVariable.class));
        // a connector without stream variables (e.g. already cleaned up
        // after interruptUpload())
        tracker.cleanStreamVariable(id + "-unknown", "a");
        tracker.cleanStreamVariable(id, "a");
        tracker.cleanStreamVariable(id, "a");
    }
}
