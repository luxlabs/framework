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
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;

/**
 * I09: push in long polling that must keep working after the client
 * resynchronizes while another request is attempted. "start" makes a
 * background thread push a "tick" every 300 ms; "ping" increments the
 * server-side "pongs" label. The browser script drops one push message, so
 * that the client gives up waiting (5 s) and resynchronizes, and sends
 * requests while the resynchronization is delayed.
 */
@Push(value = PushMode.AUTOMATIC, transport = Transport.LONG_POLLING)
public class PushResyncUI extends LogUI {

    @Override
    protected void setup() {
        final Label tick = new Label("0");
        tick.setId("tick");
        final Label pongs = new Label("0");
        pongs.setId("pongs");
        final AtomicInteger pongCount = new AtomicInteger();
        Button start = new Button("start", e -> {
            Thread t = new Thread(() -> {
                for (int i = 1; i <= 400; i++) {
                    final int value = i;
                    try {
                        Thread.sleep(300);
                        access(() -> tick.setValue(String.valueOf(value)))
                                .get();
                    } catch (Exception ex) {
                        return;
                    }
                }
            }, "push-tick");
            t.setDaemon(true);
            t.start();
        });
        start.setId("start");
        Button ping = new Button("ping",
                e -> pongs.setValue(String.valueOf(pongCount.incrementAndGet())));
        ping.setId("ping");
        layout.addComponents(tick, pongs, start, ping);
    }
}
