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
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;

/** I10: text selection with the mouse in a Table cell. */
@Theme("valo")
public class TableUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        CssLayout root = Log.root(this);
        Log log = new Log(root);
        Table table = new Table();
        table.setId("table-text");
        table.setWidth("700px");
        table.addContainerProperty("text", String.class, "");
        for (int i = 1; i <= 5; i++) {
            table.addItem(new Object[] {
                    "Selectable words in the table row number " + i },
                    "row" + i);
        }
        table.setPageLength(6);
        root.addComponent(table);
        log.add("ready");
    }
}
