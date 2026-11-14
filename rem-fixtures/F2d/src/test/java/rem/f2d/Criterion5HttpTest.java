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
package rem.f2d;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * REM F2d, criterion 5: I41 and I56 from end to end over HTTP, against the
 * fixture (Jetty 9.4 and the real Vaadin servlet) on 127.0.0.1, with the
 * server parts of F2b and F2d together. The client side of both items is in
 * the Chromium scenarios of esegui.sh.
 * <p>
 * Port: system property f2d.port (default 18410).
 */
public class Criterion5HttpTest {

    private static final int PORT = Integer.getInteger("f2d.port", 18410);
    private static final String BASE = "http://127.0.0.1:" + PORT;
    private static Server server;

    private String key;
    private JsonObject state;
    private String uidl;
    private int clientId;

    @BeforeClass
    public static void startFixture() throws Exception {
        server = FixtureServer.start(PORT);
    }

    @AfterClass
    public static void stopFixture() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Before
    public void freshCookies() {
        CookieHandler.setDefault(new CookieManager());
        clientId = 0;
    }

    /**
     * I56: once the session has expired, a heartbeat gets 403 Forbidden (not
     * 410 Gone) and is not cached, and any other request that cannot create
     * a session (here a connector resource) gets 403 as well.
     */
    @Test
    public void expiredSessionAnswers403ToHeartbeatAndOtherRequests()
            throws Exception {
        init("i56");
        Response click = click("i56", connector("expire"));
        Assert.assertEquals(200, click.status);
        Thread.sleep(2500);

        Response heartbeat = request("POST", BASE + "/i56/HEARTBEAT/?v-uiId=0",
                null, null);
        Assert.assertEquals("heartbeat after expiry: " + heartbeat, 403,
                heartbeat.status);
        // Jetty's error page may widen the header (must-revalidate,no-cache,
        // no-store): what matters is that no-cache is there
        Assert.assertTrue("Cache-Control: " + heartbeat.cacheControl,
                heartbeat.cacheControl != null
                        && heartbeat.cacheControl.contains("no-cache"));

        Response resource = request("GET",
                BASE + "/i56/APP/connector/0/1/some-resource", null, null);
        Assert.assertEquals("connector resource after expiry: " + resource,
                403, resource.status);
    }

    /**
     * I56: a UIDL request after expiry still gets the "Session Expired"
     * critical notification that the client turns into the session expired
     * message (unchanged path, checked so that the 403 change does not hide
     * it).
     */
    @Test
    public void expiredSessionUidlGetsSessionExpiredNotification()
            throws Exception {
        init("i56");
        click("i56", connector("expire"));
        Thread.sleep(2500);
        Response after = click("i56", connector("probe"));
        Assert.assertEquals(200, after.status);
        Assert.assertTrue("no session expired notification: " + after.body,
                after.body.contains("Session Expired"));
    }

    /**
     * I41: a form submitted to the Upload without a selected file (empty file
     * name) reaches FileUploadHandler and Upload, but no receiver is called
     * and no upload event is fired.
     */
    @Test
    public void uploadWithoutFileStartsNothing() throws Exception {
        String before = events();
        init("i41-noselect");
        Matcher m = Pattern.compile("app://APP/UPLOAD/[^\"\\\\]+")
                .matcher(uidl.replace("\\/", "/"));
        Assert.assertTrue("upload action not found in the UIDL", m.find());
        String action = BASE + "/i41-noselect/"
                + m.group().substring("app://".length());

        String boundary = "----f2dBoundary";
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"upload_file\"; filename=\"\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n\r\n--"
                + boundary + "--\r\n";
        Response post = request("POST", action,
                "multipart/form-data; boundary=" + boundary,
                body.getBytes(StandardCharsets.ISO_8859_1));
        Assert.assertTrue("upload POST: " + post, post.status < 500);
        Thread.sleep(500);

        String added = events().substring(before.length());
        Assert.assertFalse("receiver called without a file: " + added,
                added.contains("i41 receiving"));
        Assert.assertFalse("upload finished without a file: " + added,
                added.contains("i41 finished"));
    }

    // --- helpers -----------------------------------------------------------

    private void init(String ctx) throws IOException {
        String form = "v-browserDetails=1&theme=valo&v-appId=" + ctx
                + "-1&v-sh=900&v-sw=1440&v-cw=1000&v-ch=800&v-curdate=1"
                + "&v-tzo=-60&v-dstd=60&v-rtzo=-60&v-dston=false&v-vw=1000"
                + "&v-vh=0&v-loc=" + BASE + "/" + ctx + "/&v-wn=f2d";
        Response r = request("POST", BASE + "/" + ctx + "/?v-1",
                "application/x-www-form-urlencoded",
                form.getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("init " + ctx + ": " + r, 200, r.status);
        JsonObject outer = Json.parse(r.body);
        uidl = outer.getString("uidl");
        JsonObject inner = Json.parse(uidl);
        key = inner.getString("Vaadin-Security-Key");
        state = inner.getObject("state");
    }

    private String connector(String id) {
        for (String cid : state.keys()) {
            JsonObject s = state.getObject(cid);
            if (s.hasKey("id") && id.equals(s.getString("id"))) {
                return cid;
            }
        }
        throw new AssertionError("no component with id " + id);
    }

    private Response click(String ctx, String cid) throws IOException {
        String json = "{\"csrfToken\":\"" + key + "\",\"rpc\":[[\"" + cid
                + "\",\"com.vaadin.shared.ui.button.ButtonServerRpc\",\"click\",[{"
                + "\"altKey\":false,\"button\":\"LEFT\",\"clientX\":1,\"clientY\":1,"
                + "\"ctrlKey\":false,\"metaKey\":false,\"relativeX\":1,\"relativeY\":1,"
                + "\"shiftKey\":false,\"type\":1}]]],\"syncId\":0,\"clientId\":"
                + (clientId++) + "}";
        return request("POST", BASE + "/" + ctx + "/UIDL/?v-uiId=0",
                "application/json; charset=UTF-8",
                json.getBytes(StandardCharsets.UTF_8));
    }

    private String events() throws IOException {
        return request("GET", BASE + "/i41-noselect/diag?all=1", null,
                null).body;
    }

    private static Response request(String method, String url,
            String contentType, byte[] body) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod(method);
        c.setInstanceFollowRedirects(false);
        if (contentType != null) {
            c.setRequestProperty("Content-Type", contentType);
        }
        if (body != null) {
            c.setDoOutput(true);
            OutputStream out = c.getOutputStream();
            out.write(body);
            out.close();
        }
        Response r = new Response();
        r.status = c.getResponseCode();
        r.cacheControl = c.getHeaderField("Cache-Control");
        InputStream in = r.status >= 400 ? c.getErrorStream()
                : c.getInputStream();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        if (in != null) {
            byte[] b = new byte[4096];
            int n;
            while ((n = in.read(b)) > 0) {
                buf.write(b, 0, n);
            }
            in.close();
        }
        r.body = new String(buf.toByteArray(), StandardCharsets.UTF_8);
        return r;
    }

    private static class Response {
        int status;
        String cacheControl;
        String body;

        @Override
        public String toString() {
            return status + " " + body.substring(0, Math.min(200, body.length()));
        }
    }
}
