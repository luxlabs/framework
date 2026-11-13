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
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

/**
 * I55: a responsive component on a page whose style sheet has rules without
 * selectorText (@layer and @container blocks) and a @font-face rule; the
 * styles are added by {@link ResponsiveServlet}.
 */
@Theme("valo")
public class ResponsiveUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        CssLayout root = Log.root(this);
        Log log = new Log(root);
        Label label = new Label("responsive");
        label.setId("resp");
        label.addStyleName("f2c-resp");
        label.setWidth("200px");
        Responsive.makeResponsive(label);
        root.addComponent(label);
        log.add("ready");
    }
}
