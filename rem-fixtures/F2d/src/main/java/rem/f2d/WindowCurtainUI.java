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
import com.vaadin.ui.Window;

/**
 * I29 (issue #10514): a Window closed while the user is dragging or resizing
 * it must not leave its dragging or resizing curtain on the page. "open"
 * opens a resizable, draggable window that the server closes 2 s later
 * through push; "probe" logs a line, to check the page is still clickable.
 */
@Push(value = PushMode.AUTOMATIC, transport = Transport.LONG_POLLING)
public class WindowCurtainUI extends LogUI {

    @Override
    protected void setup() {
        Button open = new Button("open", e -> {
            final Window w = new Window("Closing window",
                    new Label("This window closes itself in 2 seconds"));
            w.setId("closing-window");
            w.setWidth("400px");
            w.setHeight("250px");
            w.setResizable(true);
            w.setDraggable(true);
            w.center();
            addWindow(w);
            log("opened");
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    return;
                }
                access(() -> {
                    w.close();
                    log("closed");
                });
            }, "window-closer");
            t.setDaemon(true);
            t.start();
        });
        open.setId("open");
        Button probe = new Button("probe", e -> log("probe"));
        probe.setId("probe");
        layout.addComponents(open, probe);
    }
}
