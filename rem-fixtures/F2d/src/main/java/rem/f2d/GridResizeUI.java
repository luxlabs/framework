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
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Grid;

/**
 * I11 (issue #11893): a full-size Grid with 100 rows. Scrolling all the way
 * down, making the window smaller so that one row is hidden, and scrolling
 * down again must keep the rows consistent and in sequence.
 */
public class GridResizeUI extends LogUI {

    @Override
    protected void init(VaadinRequest request) {
        super.init(request);
        getContent().setSizeFull();
    }

    @Override
    protected void setup() {
        IndexedContainer container = new IndexedContainer();
        container.addContainerProperty("row", String.class, "");
        container.addContainerProperty("text", String.class, "");
        for (int i = 0; i < 100; i++) {
            Item item = container.getItem(container.addItem());
            item.getItemProperty("row").setValue("row " + i);
            item.getItemProperty("text").setValue("text of row " + i);
        }
        Grid grid = new Grid(container);
        grid.setId("grid");
        grid.setSizeFull();
        layout.setSizeFull();
        layout.setMargin(false);
        layout.addComponent(grid);
        layout.setExpandRatio(grid, 1);
    }
}
