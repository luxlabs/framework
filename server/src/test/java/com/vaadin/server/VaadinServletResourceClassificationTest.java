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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * M14 / I30, review R3: a static resource whose nature cannot be verified is
 * not served (fail closed). file: and jar: URLs are classified explicitly;
 * any other protocol, and any URL that cannot be classified, is refused.
 * Derived from the probe DirectoryProtocolProbeTest of the F2b verifier.
 */
public class VaadinServletResourceClassificationTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** A directory with two files, under a path with a space. */
    private File themesWithSpace() throws IOException {
        File root = folder.newFolder("raw directory");
        File dir = new File(root, "VAADIN/themes");
        Assert.assertTrue(dir.mkdirs());
        write(new File(dir, "child-a.txt"));
        write(new File(dir, "child-b.txt"));
        return dir;
    }

    private static void write(File f) throws IOException {
        FileOutputStream out = new FileOutputStream(f);
        out.write("x".getBytes("UTF-8"));
        out.close();
    }

    /** URL of a custom protocol handler backed by a real file or directory. */
    private static URL custom(String spec, File target) throws IOException {
        final URL real = target.toURI().toURL();
        return new URL(null, spec, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                final URLConnection c = real.openConnection();
                return new URLConnection(u) {
                    @Override
                    public void connect() throws IOException {
                        c.connect();
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return c.getInputStream();
                    }

                    @Override
                    public long getLastModified() {
                        return c.getLastModified();
                    }
                };
            }
        });
    }

    /** Serves the URL through serveStaticResourcesInVAADIN. */
    private static int[] serve(final URL url)
            throws IOException, ServletException {
        VaadinServlet s = new VaadinServlet() {
            @Override
            protected URL findResourceURL(String filename) {
                return url;
            }
        };
        s.init(new MockServletConfig());
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        Mockito.when(resp.getOutputStream())
                .thenReturn(new ServletOutputStream() {
                    @Override
                    public void write(int b) {
                        body.write(b);
                    }
                });
        final int[] status = { 200 };
        Mockito.doAnswer(new org.mockito.stubbing.Answer<Void>() {
            @Override
            public Void answer(org.mockito.invocation.InvocationOnMock i) {
                status[0] = (Integer) i.getArguments()[0];
                return null;
            }
        }).when(resp).setStatus(Mockito.anyInt());
        s.serveStaticResourcesInVAADIN("/VAADIN/themes/x", req, resp);
        return new int[] { status[0], body.size() };
    }

    private static void assertRefused(URL url) throws Exception {
        int[] r = serve(url);
        Assert.assertEquals("served " + r[1] + " bytes for " + url, 0, r[1]);
        Assert.assertEquals("status for " + url, 403, r[0]);
    }

    private static void assertServed(URL url, int bytes) throws Exception {
        int[] r = serve(url);
        Assert.assertEquals("status for " + url, 200, r[0]);
        Assert.assertEquals("bytes for " + url, bytes, r[1]);
    }

    @Test
    public void rawSpaceFileDirectoryIsRefused() throws Exception {
        File dir = themesWithSpace();
        URL raw = new URL("file:" + dir.getAbsolutePath());
        Assert.assertTrue(raw.getPath().contains(" "));
        assertRefused(raw);
    }

    @Test
    public void vfsDirectoryIsRefused() throws Exception {
        assertRefused(custom("vfs:/application/VAADIN/themes",
                themesWithSpace()));
    }

    @Test
    public void customJarHandlerDirectoryIsRefused() throws Exception {
        assertRefused(custom("jar:file:/application/theme.jar!/VAADIN/themes",
                themesWithSpace()));
    }

    /**
     * Documented change: a regular file reached through a protocol that the
     * servlet cannot classify (vfs:, a custom handler) is no longer served.
     */
    @Test
    public void unclassifiedProtocolFileIsRefused() throws Exception {
        File file = new File(themesWithSpace(), "child-a.txt");
        assertRefused(custom("vfs:/application/VAADIN/themes/child-a.txt",
                file));
    }

    @Test
    public void rawSpaceFileIsServed() throws Exception {
        File file = new File(themesWithSpace(), "child-a.txt");
        URL raw = new URL("file:" + file.getAbsolutePath());
        Assert.assertTrue(raw.getPath().contains(" "));
        assertServed(raw, 1);
    }

    @Test
    public void encodedSpaceFileIsServedAndDirectoryRefused()
            throws Exception {
        File dir = themesWithSpace();
        assertServed(new File(dir, "child-b.txt").toURI().toURL(), 1);
        URL encodedDir = new URL(dir.toURI().toURL().toString()
                .replaceAll("/$", ""));
        Assert.assertTrue(encodedDir.getPath().contains("%20"));
        assertRefused(encodedDir);
    }
}
