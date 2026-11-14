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
import com.vaadin.event.SortEvent;
import com.vaadin.event.SortEvent.SortListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.UI;

/**
 * Grid scenarios: M03 (column hiding toggle captions), I01 (sorting after
 * enabling and disabling multi-selection twice), I15 (adding a column when a column is hidden), I46 (text selection
 * in a cell, reproduction).
 */
@Theme("valo")
public class GridUI extends UI {

    /** M03: header and hiding toggle captions with markup. */
    static final String TOGGLE_HOSTILE = "<img class=\"f2c-toggle-img\" src=\"x\" onerror=\"window.f2cToggleXss=1\"><b class=\"f2c-toggle-b\">bold</b>";

    @Override
    protected void init(VaadinRequest request) {
        CssLayout root = Log.root(this);
        final Log log = new Log(root);

        final Grid sort = new Grid();
        sort.setId("grid-sort");
        sort.setWidth("500px");
        sort.setHeight("250px");
        sort.addColumn("name", String.class);
        sort.addColumn("num", Integer.class);
        sort.addRow("Charlie", 3);
        sort.addRow("Alpha", 1);
        sort.addRow("Bravo", 2);
        sort.setSelectionMode(SelectionMode.NONE);
        sort.addSortListener(new SortListener() {
            @Override
            public void sort(SortEvent event) {
                log.add("sort=" + (event.getSortOrder().isEmpty() ? ""
                        : event.getSortOrder().get(0).getPropertyId() + " "
                                + event.getSortOrder().get(0).getDirection()));
            }
        });
        root.addComponent(sort);
        Button multi = new Button("multi", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                sort.setSelectionMode(SelectionMode.MULTI);
                log.add("mode=multi");
            }
        });
        multi.setId("mode-multi");
        root.addComponent(multi);
        Button none = new Button("none", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                sort.setSelectionMode(SelectionMode.NONE);
                log.add("mode=none");
            }
        });
        none.setId("mode-none");
        root.addComponent(none);

        final Grid cols = new Grid();
        cols.setId("grid-cols");
        cols.setWidth("500px");
        cols.setHeight("200px");
        cols.addColumn("a", String.class);
        cols.addColumn("b", String.class);
        cols.addColumn("c", String.class);
        cols.addRow("a1", "b1", "c1");
        cols.getColumn("b").setHidden(true);
        root.addComponent(cols);
        Button add = new Button("add column", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                cols.addColumn("d", String.class);
                log.add("added=d");
            }
        });
        add.setId("add-column");
        root.addComponent(add);

        final Grid toggles = new Grid();
        toggles.setId("grid-toggles");
        toggles.setWidth("500px");
        toggles.setHeight("200px");
        toggles.addColumn("x", String.class);
        toggles.addColumn("y", String.class);
        toggles.addColumn("z", String.class);
        toggles.addRow("x1", "y1", "z1");
        toggles.getColumn("x").setHeaderCaption(TOGGLE_HOSTILE)
                .setHidable(true);
        toggles.getColumn("y").setHidingToggleCaption(TOGGLE_HOSTILE)
                .setHidable(true);
        toggles.getColumn("z").setHidable(true);
        root.addComponent(toggles);

        for (SelectionMode mode : new SelectionMode[] { SelectionMode.NONE,
                SelectionMode.SINGLE }) {
            Grid text = new Grid();
            text.setId("grid-text-" + mode.name().toLowerCase());
            text.setWidth("700px");
            text.setHeight("200px");
            text.addColumn("text", String.class);
            text.addColumn("other", String.class);
            for (int i = 1; i <= 3; i++) {
                text.addRow("Selectable words in the grid row number " + i,
                        "other cell " + i);
            }
            text.setSelectionMode(mode);
            root.addComponent(text);
        }
        log.add("ready");
    }
}
