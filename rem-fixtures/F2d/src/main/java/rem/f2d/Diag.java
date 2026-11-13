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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atmosphere.cpr.AtmosphereResource;

import com.vaadin.server.communication.PushConnection;
import com.vaadin.ui.UI;

/**
 * Diagnostics of the I09 fixture (fixture code only): for each repetition tag,
 * the server-side sync id and the state of the push connection and of its
 * long polling resource, read by reflection (the framework is not changed).
 * Lines are printed to stdout ("F2D-DIAG …") and served by {@link Servlet}
 * at {@code <context>/diag?rep=<tag>}.
 */
public final class Diag {

    private static final Map<String, List<String>> LINES = new ConcurrentHashMap<String, List<String>>();
    private static final List<String> EVENTS = new ArrayList<String>();

    /** Records a global event (Atmosphere or Vaadin log), with its time. */
    public static void event(String text) {
        String line = "t=" + System.currentTimeMillis() + " " + text;
        System.out.println("F2D-EVENT " + line);
        synchronized (EVENTS) {
            EVENTS.add(line);
        }
    }

    /**
     * Records the log records of PushHandler, PushAtmosphereHandler and
     * AtmospherePushConnection (PushHandler logs new connections and
     * connection losses with the resource uuid at FINER) as events.
     */
    public static void captureVaadinPushLog() {
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord r) {
                String msg = r.getMessage();
                Object[] ps = r.getParameters();
                if (ps != null) {
                    for (int i = 0; i < ps.length; i++) {
                        msg = msg.replace("{" + i + "}", String.valueOf(ps[i]));
                    }
                }
                event("vaadin " + r.getLevel() + " " + r.getLoggerName().replace("com.vaadin.server.communication.", "")
                        + " [" + Thread.currentThread().getName() + "] " + msg
                        + (r.getThrown() != null ? " thrown=" + r.getThrown() : ""));
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        for (String name : new String[] {
                "com.vaadin.server.communication.PushHandler",
                "com.vaadin.server.communication.PushAtmosphereHandler",
                "com.vaadin.server.communication.AtmospherePushConnection" }) {
            Logger logger = Logger.getLogger(name);
            logger.setLevel(Level.FINEST);
            logger.addHandler(handler);
        }
    }

    private Diag() {
    }

    public static void record(String rep, UI ui, String event) {
        StringBuilder b = new StringBuilder();
        b.append("t=").append(System.currentTimeMillis()).append(' ').append(event)
                .append(" syncId=").append(ui.getConnectorTracker().getCurrentSyncId());
        PushConnection pc = ui.getPushConnection();
        if (pc == null) {
            b.append(" push=none");
        } else {
            b.append(" connected=").append(pc.isConnected());
            Object state = field(pc, "state");
            Object resource = field(pc, "resource");
            b.append(" state=").append(state);
            if (resource instanceof AtmosphereResource) {
                AtmosphereResource r = (AtmosphereResource) resource;
                String uuid = r.uuid();
                b.append(" res=").append(uuid == null ? "?" : uuid)
                        .append('@').append(Integer.toHexString(System.identityHashCode(r)))
                        .append(" suspended=").append(r.isSuspended())
                        .append(" resumed=").append(r.isResumed())
                        .append(" cancelled=").append(r.isCancelled());
            } else {
                b.append(" res=null");
            }
        }
        String line = b.toString();
        System.out.println("F2D-DIAG rep=" + rep + " " + line);
        List<String> list = LINES.get(rep);
        if (list == null) {
            list = new ArrayList<String>();
            LINES.put(rep, list);
        }
        synchronized (list) {
            list.add(line);
        }
    }

    private static long time(String line) {
        try {
            return Long.parseLong(line.substring(2, line.indexOf(' ')));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static Object field(Object target, String name) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Exception e) {
                return "error:" + e;
            }
        }
        return "missing";
    }

    /** Serves the lines of one repetition as plain text. */
    public static class Servlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");
            List<String> list = LINES.get(String.valueOf(req.getParameter("rep")));
            List<String> out = new ArrayList<String>();
            long from = Long.MAX_VALUE;
            long to = Long.MIN_VALUE;
            if (list != null) {
                synchronized (list) {
                    for (String l : list) {
                        out.add(l);
                        long t = time(l);
                        from = Math.min(from, t);
                        to = Math.max(to, t);
                    }
                }
            }
            // Global events from 3 s before the first line of the repetition
            // (page load, first connect) to 500 ms after the last one
            if (from != Long.MAX_VALUE) {
                synchronized (EVENTS) {
                    for (String e : EVENTS) {
                        long t = time(e);
                        if (t >= from - 3000 && t <= to + 500) {
                            out.add(e);
                        }
                    }
                }
            }
            java.util.Collections.sort(out, new java.util.Comparator<String>() {
                @Override
                public int compare(String a, String b) {
                    return Long.compare(time(a), time(b));
                }
            });
            StringBuilder b = new StringBuilder();
            for (String l : out) {
                b.append(l).append('\n');
            }
            resp.getWriter().write(b.toString());
        }
    }
}
