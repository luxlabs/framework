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

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Grid;
import com.vaadin.ui.UI;

/** M03 / I06: hostile Grid header captions shown in the column hiding toggles. */
public class GridUI extends UI {

    public static final String HOSTILE = "<img src=x class=xss-probe onerror=\"window.__xss=(window.__xss||0)+1\">Name <b>bold</b>";
    public static final String ENCODED = "&lt;img src=x class=xss-probe onerror=\"window.__xss=(window.__xss||0)+1\"&gt;Encoded";

    @Override
    protected void init(VaadinRequest request) {
        Grid grid = new Grid();
        grid.setId("grid");
        grid.addColumn("name");
        grid.addColumn("encoded");
        grid.addColumn("toggle");
        grid.addRow("a", "b", "c");
        grid.getColumn("name").setHidable(true);
        grid.getColumn("name").setHeaderCaption(HOSTILE);
        grid.getColumn("encoded").setHidable(true);
        grid.getColumn("encoded").setHeaderCaption(ENCODED);
        grid.getColumn("toggle").setHidable(true);
        grid.getColumn("toggle").setHeaderCaption("Toggle");
        grid.getColumn("toggle").setHidingToggleCaption(HOSTILE);
        setContent(grid);
    }
}
