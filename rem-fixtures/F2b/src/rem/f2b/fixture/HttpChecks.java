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
package rem.f2b.fixture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;

/**
 * Checks without browser and without sockets (Jetty LocalConnector):
 * bootstrap pages, static resources, M14 directories, M15 /VAADIN paths.
 * Usage: HttpChecks &lt;output directory&gt;. Writes results.tsv and the
 * bodies of the bootstrap pages.
 */
public class HttpChecks {

    private static LocalConnector connector;

    /** Minimal HTTP/1.0 response: status, headers, body. */
    static class Response {
        int status;
        final Map<String, String> headers = new LinkedHashMap<String, String>();
        byte[] body;

        String get(String name) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue();
                }
            }
            return null;
        }
    }

    static Response get(String path) throws Exception {
        String raw = "GET " + path + " HTTP/1.0\r\nHost: 127.0.0.1\r\n"
                + "User-Agent: Mozilla/5.0 (Macintosh) Chrome/120.0 Safari/537.36\r\n"
                + "\r\n";
        ByteBuffer buffer = connector.getResponse(
                ByteBuffer.wrap(raw.getBytes(StandardCharsets.ISO_8859_1)));
        byte[] all = new byte[buffer.remaining()];
        buffer.get(all);
        int split = -1;
        for (int i = 0; i + 3 < all.length; i++) {
            if (all[i] == '\r' && all[i + 1] == '\n' && all[i + 2] == '\r'
                    && all[i + 3] == '\n') {
                split = i;
                break;
            }
        }
        if (split < 0) {
            throw new IllegalStateException("no response for " + path);
        }
        String head = new String(all, 0, split, StandardCharsets.ISO_8859_1);
        String[] lines = head.split("\r\n");
        Response r = new Response();
        r.status = Integer.parseInt(lines[0].split(" ")[1]);
        for (int i = 1; i < lines.length; i++) {
            int c = lines[i].indexOf(':');
            if (c > 0) {
                r.headers.put(lines[i].substring(0, c).trim(),
                        lines[i].substring(c + 1).trim());
            }
        }
        r.body = new byte[all.length - split - 4];
        System.arraycopy(all, split + 4, r.body, 0, r.body.length);
        return r;
    }

    static String sha256(byte[] data) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : d) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        Object[] local = FixtureServer.local();
        Server server = (Server) local[0];
        connector = (LocalConnector) local[1];
        PrintWriter results = new PrintWriter(new File(out, "results.tsv"),
                "UTF-8");
        results.println("path\tstatus\tbytes\tsha256\tcache-control");
        String[] paths = { "/", "/boot/", "/actions/", "/grid/",
                "/VAADIN/vaadinBootstrap.js",
                "/VAADIN/themes/valo/favicon.ico",
                "/VAADIN/themes/valo/styles.css",
                "/VAADIN/widgetsets/com.vaadin.DefaultWidgetSet/com.vaadin.DefaultWidgetSet.nocache.js",
                // M14: directories inside the theme and widgetset jars
                "/VAADIN/themes/", "/VAADIN/themes/valo",
                "/VAADIN/themes/valo/", "/VAADIN/widgetsets/",
                // M15: /VAADIN under a servlet mapped to a sub path
                "/app/VAADIN/", "/app/VAADIN/foo", "/app/VAADIN/themes/valo/x.html",
                "/app/", "/app/view" };
        try {
            for (String path : paths) {
                Response r = get(path);
                byte[] body = r.body;
                if (body == null) {
                    body = new byte[0];
                }
                results.println(path + "\t" + r.status + "\t"
                        + body.length + "\t" + sha256(body) + "\t"
                        + r.get("Cache-Control"));
                String name = path.replaceAll("[^A-Za-z0-9]+", "_");
                if (r.status == 200 && (path.endsWith("/")
                        || path.startsWith("/app/"))) {
                    FileOutputStream f = new FileOutputStream(
                            new File(out, "body" + name + ".html"));
                    f.write(body);
                    f.close();
                }
            }
        } finally {
            results.close();
            server.stop();
        }
    }
}
