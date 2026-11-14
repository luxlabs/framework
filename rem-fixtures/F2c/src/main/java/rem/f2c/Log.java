/*
 * Part of the 7.7.17.1 maintenance fork of Vaadin Framework (REM F2c).
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
package rem.f2c;

import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

/** Log shared by the fixture UIs: one line per server side event. */
final class Log {

    private final Label label = new Label("", com.vaadin.shared.ui.label.ContentMode.PREFORMATTED);
    private final StringBuilder text = new StringBuilder();

    Log(CssLayout parent) {
        label.setId("log");
        parent.addComponent(label);
    }

    void add(String line) {
        text.append(line).append('\n');
        label.setValue(text.toString());
    }

    static CssLayout root(UI ui) {
        CssLayout root = new CssLayout();
        root.setWidth("100%");
        ui.setContent(root);
        return root;
    }
}
