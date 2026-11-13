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

import com.vaadin.annotations.Push;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;

/**
 * Criterion 4: push in long polling, as in the console, with many closely
 * spaced messages. "start" makes a background thread push COUNT updates of the
 * "count" label, each through UI.access, a few milliseconds apart.
 */
@Push(value = PushMode.AUTOMATIC, transport = Transport.LONG_POLLING)
public class PushRapidUI extends LogUI {

    static final int COUNT = 300;

    @Override
    protected void setup() {
        final Label count = new Label("0");
        count.setId("count");
        Button start = new Button("start", e -> {
            log("start");
            Thread t = new Thread(() -> {
                for (int i = 1; i <= COUNT; i++) {
                    final int value = i;
                    access(() -> count.setValue(String.valueOf(value)));
                    try {
                        Thread.sleep(i % 10 == 0 ? 20 : 2);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
                access(() -> log("done"));
            }, "push-rapid");
            t.setDaemon(true);
            t.start();
        });
        start.setId("start");
        Button ping = new Button("ping", e -> log("pong"));
        ping.setId("ping");
        layout.addComponents(count, start, ping);
    }
}
