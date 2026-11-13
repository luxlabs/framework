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

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/** Base UI with a numbered log ("N. text" lines, newest last) in a Label with id "log". */
@Theme("valo")
public abstract class LogUI extends UI {

    protected final VerticalLayout layout = new VerticalLayout();
    private final Label log = new Label("", ContentMode.PREFORMATTED);
    private int count;

    @Override
    protected void init(VaadinRequest request) {
        layout.setMargin(true);
        layout.setSpacing(true);
        log.setId("log");
        setContent(layout);
        setup();
        layout.addComponent(log);
    }

    protected abstract void setup();

    public void log(String text) {
        count++;
        String value = log.getValue();
        log.setValue((value.isEmpty() ? "" : value + "\n") + count + ". " + text);
    }
}
