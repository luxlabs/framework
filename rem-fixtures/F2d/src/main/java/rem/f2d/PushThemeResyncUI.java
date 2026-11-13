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

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.annotations.Push;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;

/**
 * I09, second scenario: a resynchronization without a missing message. A
 * theme change makes the 7.7.17 client resynchronize from inside the handling
 * of the response (UIConnector), while push messages keep arriving every
 * 100 ms and the user keeps clicking, so that requests are sent while the
 * resynchronization is ongoing.
 */
@Push(value = PushMode.AUTOMATIC, transport = Transport.LONG_POLLING)
public class PushThemeResyncUI extends LogUI {

    /** Repetition tag from the "rep" request parameter, for {@link Diag}. */
    private String rep = "none";

    @Override
    protected void init(VaadinRequest request) {
        String r = request.getParameter("rep");
        if (r == null && getPage().getLocation() != null
                && getPage().getLocation().getQuery() != null) {
            for (String kv : getPage().getLocation().getQuery().split("&")) {
                if (kv.startsWith("rep=")) {
                    r = kv.substring(4);
                }
            }
        }
        if (r != null && r.matches("[A-Za-z0-9._-]{1,40}")) {
            rep = r;
        }
        super.init(request);
    }

    @Override
    protected void setup() {
        final Label tick = new Label("0");
        tick.setId("tick");
        final Label pongs = new Label("0");
        pongs.setId("pongs");
        final AtomicInteger pongCount = new AtomicInteger();
        Button start = new Button("start", e -> {
            Thread t = new Thread(() -> {
                for (int i = 1; i <= 1200; i++) {
                    final int value = i;
                    try {
                        Thread.sleep(100);
                        access(() -> {
                            tick.setValue(String.valueOf(value));
                            Diag.record(rep, this, "tick=" + value);
                        }).get();
                    } catch (Exception ex) {
                        return;
                    }
                }
            }, "push-tick");
            t.setDaemon(true);
            t.start();
        });
        start.setId("start");
        Button ping = new Button("ping", e -> {
            pongs.setValue(String.valueOf(pongCount.incrementAndGet()));
            Diag.record(rep, this, "ping=" + pongCount.get());
        });
        ping.setId("ping");
        Button theme = new Button("theme", e -> {
            setTheme("valo".equals(getTheme()) ? "reindeer" : "valo");
            Diag.record(rep, this, "theme=" + getTheme());
        });
        theme.setId("theme");
        layout.addComponents(tick, pongs, start, ping, theme);
    }
}
