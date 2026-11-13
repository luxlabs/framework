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
package rem.f2b.fixture.boot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.LaunchedURLClassLoader;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.jar.JarFile;

import com.vaadin.server.VaadinServlet;

/**
 * M14, review R3: static /VAADIN/ resources of a Spring Boot 1.5 executable
 * jar, as the console is packaged, stay served by the fail-closed check.
 * Builds a jar with the Boot layout (BOOT-INF/classes with a theme,
 * BOOT-INF/lib with the given Vaadin jars stored uncompressed), loads it with
 * the Spring Boot loader (LaunchedURLClassLoader, nested jar: URLs) and asks
 * VaadinServlet.isAllowedVAADINResourceUrl about files and directories.
 * Usage: BootNestedJarCheck &lt;output dir&gt; &lt;jar to nest&gt;...
 * Exit 0 if files are allowed and readable and directories are refused.
 */
public class BootNestedJarCheck {

    static class Probe extends VaadinServlet {
        boolean allowed(URL url) {
            return isAllowedVAADINResourceUrl(null, url);
        }
    }

    private static void dir(JarOutputStream out, String name) throws Exception {
        out.putNextEntry(new JarEntry(name));
        out.closeEntry();
    }

    private static void stored(JarOutputStream out, String name, byte[] data)
            throws Exception {
        JarEntry e = new JarEntry(name);
        e.setMethod(ZipEntry.STORED);
        e.setSize(data.length);
        e.setCompressedSize(data.length);
        CRC32 crc = new CRC32();
        crc.update(data);
        e.setCrc(crc.getValue());
        out.putNextEntry(e);
        out.write(data);
        out.closeEntry();
    }

    private static byte[] read(File f) throws Exception {
        byte[] b = new byte[(int) f.length()];
        InputStream in = new FileInputStream(f);
        int off = 0;
        while (off < b.length) {
            off += in.read(b, off, b.length - off);
        }
        in.close();
        return b;
    }

    private static long count(URL url) throws Exception {
        InputStream in = url.openStream();
        long n = 0;
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) > 0) {
            n += r;
        }
        in.close();
        return n;
    }

    public static void main(String[] args) throws Exception {
        File outDir = new File(args[0]);
        outDir.mkdirs();
        File fat = new File(outDir, "boot-app.jar");
        JarOutputStream out = new JarOutputStream(new FileOutputStream(fat));
        dir(out, "BOOT-INF/");
        dir(out, "BOOT-INF/classes/");
        dir(out, "BOOT-INF/classes/VAADIN/");
        dir(out, "BOOT-INF/classes/VAADIN/themes/");
        dir(out, "BOOT-INF/classes/VAADIN/themes/sample/");
        stored(out, "BOOT-INF/classes/VAADIN/themes/sample/styles.css",
                "/* sample */".getBytes("UTF-8"));
        dir(out, "BOOT-INF/lib/");
        for (int i = 1; i < args.length; i++) {
            File lib = new File(args[i]);
            stored(out, "BOOT-INF/lib/" + lib.getName(), read(lib));
        }
        out.close();

        JarFile.registerUrlProtocolHandler();
        JarFileArchive archive = new JarFileArchive(fat);
        List<Archive> nested = archive
                .getNestedArchives(new Archive.EntryFilter() {
                    @Override
                    public boolean matches(Archive.Entry entry) {
                        if (entry.isDirectory()) {
                            return entry.getName().equals("BOOT-INF/classes/");
                        }
                        return entry.getName().startsWith("BOOT-INF/lib/");
                    }
                });
        List<URL> urls = new ArrayList<URL>();
        for (Archive a : nested) {
            urls.add(a.getUrl());
        }
        LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
                urls.toArray(new URL[0]),
                ClassLoader.getSystemClassLoader().getParent());

        Probe probe = new Probe();
        String[] files = { "VAADIN/themes/sample/styles.css",
                "VAADIN/themes/valo/styles.css",
                "VAADIN/themes/valo/favicon.ico",
                "VAADIN/widgetsets/com.vaadin.DefaultWidgetSet/com.vaadin.DefaultWidgetSet.nocache.js" };
        String[] dirs = { "VAADIN/themes/sample/", "VAADIN/themes/sample",
                "VAADIN/themes/valo/", "VAADIN/themes/valo", "VAADIN/themes/",
                "VAADIN/widgetsets/com.vaadin.DefaultWidgetSet" };
        boolean ok = true;
        PrintWriter report = new PrintWriter(new File(outDir, "boot-nested.tsv"),
                "UTF-8");
        report.println("kind\tresource\turl\tconnection\tallowed\tbytes\tresult");
        for (String f : files) {
            URL u = loader.getResource(f);
            boolean allowed = u != null && probe.allowed(u);
            long bytes = allowed ? count(u) : -1;
            boolean good = allowed && bytes > 0;
            ok &= good;
            report.println("file\t" + f + "\t" + u + "\t"
                    + (u == null ? "-" : u.openConnection().getClass().getName())
                    + "\t" + allowed + "\t" + bytes + "\t" + (good ? "ok" : "FAIL"));
        }
        for (String d : dirs) {
            URL u = loader.getResource(d);
            boolean allowed = u != null && probe.allowed(u);
            ok &= !allowed;
            report.println("directory\t" + d + "\t" + u + "\t"
                    + (u == null ? "-" : u.openConnection().getClass().getName())
                    + "\t" + allowed + "\t-\t" + (!allowed ? "ok" : "FAIL"));
        }
        report.close();
        System.out.println(ok ? "BOOT NESTED OK" : "BOOT NESTED FAIL");
        System.exit(ok ? 0 : 1);
    }
}
