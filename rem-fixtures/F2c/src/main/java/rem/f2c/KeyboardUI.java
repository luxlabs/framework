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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.vaadin.annotations.Theme;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.InlineDateField;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Slider;
import com.vaadin.ui.Table;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;

/**
 * I02 on Chromium: keyboard navigation in MenuBar, Slider, DateField
 * calendar, Tree and Table must keep working (key down handlers).
 */
@Theme("valo")
public class KeyboardUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        CssLayout root = Log.root(this);
        final Log log = new Log(root);

        MenuBar menu = new MenuBar();
        menu.setId("menubar");
        MenuBar.MenuItem file = menu.addItem("File", null);
        file.addItem("Open", null);
        file.addItem("Save", null);
        MenuBar.MenuItem edit = menu.addItem("Edit", null);
        edit.addItem("Undo", null);
        menu.addItem("Help", null);
        root.addComponent(menu);

        final Slider slider = new Slider(0, 100);
        slider.setId("slider");
        slider.setWidth("300px");
        slider.setValue(50.0);
        slider.setImmediate(true);
        slider.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                log.add("slider=" + slider.getValue().intValue());
            }
        });
        root.addComponent(slider);

        final InlineDateField date = new InlineDateField();
        date.setId("date");
        date.setResolution(Resolution.DAY);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2020, Calendar.JANUARY, 15);
        date.setValue(c.getTime());
        date.setImmediate(true);
        date.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                log.add("date=" + new SimpleDateFormat("yyyy-MM-dd")
                        .format(date.getValue()));
            }
        });
        root.addComponent(date);

        final Tree tree = new Tree();
        tree.setId("tree");
        tree.addItem("a");
        tree.addItem("b");
        tree.addItem("c");
        tree.setImmediate(true);
        tree.select("a");
        tree.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                log.add("tree=" + tree.getValue());
            }
        });
        root.addComponent(tree);

        final Table table = new Table();
        table.setId("table");
        table.addContainerProperty("name", String.class, "");
        for (int i = 1; i <= 3; i++) {
            table.addItem(new Object[] { "r" + i }, "r" + i);
        }
        table.setPageLength(5);
        table.setSelectable(true);
        table.setImmediate(true);
        table.select("r1");
        table.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                log.add("table=" + table.getValue());
            }
        });
        root.addComponent(table);
        log.add("ready");
    }
}
