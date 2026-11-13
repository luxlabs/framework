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

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;

/**
 * I11, container that changes size: a Grid with 100 rows and 12 columns
 * (horizontal scrollbar) whose height the server changes by less and by more
 * than one row ("h-…" buttons), as in the console's responsive layouts.
 */
public class GridFixedResizeUI extends LogUI {

    @Override
    protected void setup() {
        IndexedContainer container = new IndexedContainer();
        for (int c = 0; c < 12; c++) {
            container.addContainerProperty("c" + c, String.class, "");
        }
        for (int i = 0; i < 100; i++) {
            Item item = container.getItem(container.addItem());
            item.getItemProperty("c0").setValue("row " + i);
            for (int c = 1; c < 12; c++) {
                item.getItemProperty("c" + c)
                        .setValue("cell " + i + "/" + c + " with some width");
            }
        }
        final Grid grid = new Grid(container);
        grid.setId("grid");
        grid.setWidth("700px");
        grid.setHeight("420px");
        layout.addComponent(grid);
        for (final int h : new int[] { 420, 400, 380, 350, 300, 250, 420 }) {
            Button b = new Button("h-" + h, e -> {
                grid.setHeight(h + "px");
                log("height " + h);
            });
            b.setId("h-" + h);
            layout.addComponent(b);
        }
    }
}
