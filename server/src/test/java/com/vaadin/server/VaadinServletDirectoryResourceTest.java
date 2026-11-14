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
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * 7.7.31 "Don't serve directories as static files" (M14 / I30): directories
 * under /VAADIN/, in the file system or inside a jar, are not valid static
 * resources.
 */
public class VaadinServletDirectoryResourceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private URL fileDir;
    private URL fileDirNoSlash;
    private URL fileResource;
    private URL jarDir;
    private URL jarDirNoSlash;
    private URL jarResource;

    private final VaadinServlet servlet = new VaadinServlet();

    @Before
    public void setup() throws IOException {
        File root = folder.newFolder("webroot");
        File themes = new File(root, "VAADIN/themes/mytheme");
        Assert.assertTrue(themes.mkdirs());
        File css = new File(themes, "styles.css");
        FileOutputStream out = new FileOutputStream(css);
        out.write("body{}".getBytes("UTF-8"));
        out.close();
        fileDir = new File(root, "VAADIN/themes").toURI().toURL();
        String s = fileDir.toString();
        fileDirNoSlash = new URL(s.substring(0, s.length() - 1));
        fileResource = css.toURI().toURL();

        File jar = folder.newFile("theme.jar");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar));
        jos.putNextEntry(new JarEntry("VAADIN/"));
        jos.closeEntry();
        jos.putNextEntry(new JarEntry("VAADIN/themes/"));
        jos.closeEntry();
        jos.putNextEntry(new JarEntry("VAADIN/themes/mytheme/"));
        jos.closeEntry();
        jos.putNextEntry(new JarEntry("VAADIN/themes/mytheme/styles.css"));
        jos.write("body{}".getBytes("UTF-8"));
        jos.closeEntry();
        jos.close();
        String base = "jar:" + jar.toURI().toURL() + "!/";
        jarDir = new URL(base + "VAADIN/themes/mytheme/");
        jarDirNoSlash = new URL(base + "VAADIN/themes/mytheme");
        jarResource = new URL(base + "VAADIN/themes/mytheme/styles.css");
    }

    @Test
    public void directoriesAreNotAllowed() {
        Assert.assertFalse("file directory " + fileDir,
                servlet.isAllowedVAADINResourceUrl(null, fileDir));
        Assert.assertFalse("file directory " + fileDirNoSlash,
                servlet.isAllowedVAADINResourceUrl(null, fileDirNoSlash));
        Assert.assertFalse("jar directory " + jarDir,
                servlet.isAllowedVAADINResourceUrl(null, jarDir));
        Assert.assertFalse("jar directory " + jarDirNoSlash,
                servlet.isAllowedVAADINResourceUrl(null, jarDirNoSlash));
    }

    @Test
    public void filesAreStillAllowed() {
        Assert.assertTrue(servlet.isAllowedVAADINResourceUrl(null, fileResource));
        Assert.assertTrue(servlet.isAllowedVAADINResourceUrl(null, jarResource));
    }

    @Test
    public void directoryRequestIsRefused() throws IOException,
            ServletException {
        assertRefused(fileDirNoSlash);
        assertRefused(jarDir);
    }

    private void assertRefused(final URL url) throws IOException,
            ServletException {
        VaadinServlet s = new VaadinServlet() {
            @Override
            protected URL findResourceURL(String filename) {
                return url;
            }
        };
        s.init(new MockServletConfig());
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream())
                .thenReturn(new ServletOutputStream() {
                    @Override
                    public void write(int b) {
                        body.write(b);
                    }
                });
        s.serveStaticResourcesInVAADIN("/VAADIN/themes/mytheme", request,
                response);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        Assert.assertEquals("directory content served for " + url, 0,
                body.size());
    }
}
