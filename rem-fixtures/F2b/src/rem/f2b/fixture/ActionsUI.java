/*
 * Copyright 2026 LuxLabs.
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
package rem.f2b.fixture;

import java.lang.reflect.Method;

import com.vaadin.event.Action;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/** M05 / I52: hostile captions of Table and Tree context menus and of a shortcut. */
public class ActionsUI extends UI {

    public static final String HOSTILE = "<img src=x class=xss-probe onerror=\"window.__xss=(window.__xss||0)+1\"> Save & 'close'";
    public static final String HTML = "<b id=\"api-bold\">bold</b> html";

    private final Label fired = new Label("none");

    private Action.Handler handler(final Action action) {
        return new Action.Handler() {
            @Override
            public Action[] getActions(Object target, Object sender) {
                return new Action[] { action };
            }

            @Override
            public void handleAction(Action a, Object sender, Object target) {
                fired.setValue("context:" + target);
            }
        };
    }

    /** Uses Action#setCaptionAsHtml when the fork has it (not on 7.7.17). */
    static boolean captionAsHtml(Action action) {
        try {
            Method m = Action.class.getMethod("setCaptionAsHtml", boolean.class);
            m.invoke(action, true);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);
        fired.setId("fired");
        layout.addComponent(fired);

        Table table = new Table("table");
        table.setId("table");
        table.addContainerProperty("p", String.class, "");
        table.addItem(new Object[] { "row 1" }, "r1");
        table.setPageLength(2);
        table.addActionHandler(handler(new Action(HOSTILE)));
        layout.addComponent(table);

        Action html = new Action(HTML);
        Label api = new Label(captionAsHtml(html) ? "api" : "no-api");
        api.setId("api");
        layout.addComponent(api);
        Table tableHtml = new Table("table-html");
        tableHtml.setId("table-html");
        tableHtml.addContainerProperty("p", String.class, "");
        tableHtml.addItem(new Object[] { "row html" }, "h1");
        tableHtml.setPageLength(2);
        tableHtml.addActionHandler(handler(html));
        layout.addComponent(tableHtml);

        Tree tree = new Tree("tree");
        tree.setId("tree");
        tree.addItem("node 1");
        tree.addActionHandler(handler(new Action(HOSTILE)));
        layout.addComponent(tree);

        Panel panel = new Panel("panel");
        panel.setId("panel");
        TextField field = new TextField("field in panel");
        field.setId("panel-field");
        VerticalLayout content = new VerticalLayout(field);
        content.setMargin(true);
        panel.setContent(content);
        panel.addShortcutListener(new ShortcutListener(HOSTILE, KeyCode.F8,
                new int[0]) {
            @Override
            public void handleAction(Object sender, Object target) {
                fired.setValue("shortcut");
            }
        });
        layout.addComponent(panel);
        setContent(layout);
    }
}
