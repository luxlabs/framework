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

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;

/**
 * I56 from end to end (criterion 5): "expire" makes the HTTP session expire
 * after 1 s without requests; the servlet of this context sends a heartbeat
 * every 4 s (init parameter heartbeatInterval), so the next heartbeat finds
 * the session expired. The client must show the session expired message.
 */
public class SessionExpiryUI extends LogUI {

    @Override
    protected void setup() {
        Label ready = new Label("ready");
        ready.setId("ready");
        Button expire = new Button("expire", e -> {
            VaadinSession.getCurrent().getSession().setMaxInactiveInterval(1);
            log("session expires after 1 s of inactivity");
            Diag.event("i56 maxInactiveInterval=1");
        });
        expire.setId("expire");
        Button probe = new Button("probe", e -> log("probe"));
        probe.setId("probe");
        layout.addComponents(ready, expire, probe);
    }
}
