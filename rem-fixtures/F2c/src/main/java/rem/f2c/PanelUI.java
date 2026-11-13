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

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;

/** I05/M04: Panel caption with hostile HTML, as text and as HTML. */
@Theme("valo")
public class PanelUI extends UI {

    static final String HOSTILE = "<b id=\"f2c-b\">bold</b><img id=\"f2c-img\" src=\"x\" onerror=\"window.f2cXss=1\">";

    @Override
    protected void init(VaadinRequest request) {
        CssLayout root = Log.root(this);
        Log log = new Log(root);
        Panel text = new Panel(HOSTILE, new Label("content"));
        text.setId("panel-text");
        text.setWidth("400px");
        root.addComponent(text);
        Panel html = new Panel("<b class=\"f2c-html\">html</b>",
                new Label("content"));
        html.setCaptionAsHtml(true);
        html.setId("panel-html");
        html.setWidth("400px");
        root.addComponent(html);
        log.add("ready");
    }
}
