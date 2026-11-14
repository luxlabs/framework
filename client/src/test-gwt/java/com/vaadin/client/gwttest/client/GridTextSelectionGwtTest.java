/*
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
package com.vaadin.client.gwttest.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;
import com.vaadin.client.renderers.TextRenderer;
import com.vaadin.client.widget.grid.datasources.ListDataSource;
import com.vaadin.client.widgets.Grid;
import com.vaadin.client.widgets.Grid.Column;

/**
 * REM F2c, item I46: a text selection made inside a Grid body cell survives
 * the re-rendering of its row (cell focus change, data update from the
 * server), so that the text of a single cell can be selected and copied.
 */
public class GridTextSelectionGwtTest extends GWTTestCase {

    @Override
    public String getModuleName() {
        return "com.vaadin.client.gwttest.F2cGwtTestsWidgetSet";
    }

    private static Column<String, String[]> column(final int index) {
        return new Column<String, String[]>("c" + index, new TextRenderer()) {
            @Override
            public String getValue(String[] row) {
                return row[index];
            }
        };
    }

    private static native void select(Element cell, int start, int end)
    /*-{
        var text = cell.firstChild;
        while (text && text.nodeType != 3) {
            text = text.firstChild;
        }
        var range = $doc.createRange();
        range.setStart(text, start);
        range.setEnd(text, end);
        var sel = $wnd.getSelection();
        sel.removeAllRanges();
        sel.addRange(range);
    }-*/;

    /** Selected text if the selection is inside the element, else null. */
    private static native String selectionInside(Element element)
    /*-{
        var sel = $wnd.getSelection();
        if (!sel || sel.rangeCount == 0 || sel.isCollapsed) {
            return null;
        }
        var range = sel.getRangeAt(0);
        var node = range.startContainer;
        while (node && node !== element) {
            node = node.parentNode;
        }
        if (!node) {
            return null;
        }
        return range.toString();
    }-*/;

    public void testSelectionSurvivesRowUpdate() {
        Grid<String[]> grid = new Grid<String[]>();
        grid.setWidth("600px");
        grid.setHeight("300px");
        List<String[]> rows = new ArrayList<String[]>();
        rows.add(new String[] { "Selectable words in row one", "other" });
        rows.add(new String[] { "Second row", "other" });
        ListDataSource<String[]> dataSource = new ListDataSource<String[]>(
                rows);
        grid.setDataSource(dataSource);
        grid.addColumn(column(0));
        grid.addColumn(column(1));
        RootPanel.get().add(grid);

        Element cell = grid.getElement()
                .getElementsByTagName("tbody").getItem(0)
                .getElementsByTagName("td").getItem(0);
        assertEquals("Selectable words in row one", cell.getInnerText());
        select(cell, 11, 16);
        assertEquals("words", selectionInside(cell));

        // the same data again: the row is re-rendered
        dataSource.asList().set(0,
                new String[] { "Selectable words in row one", "other" });

        Element cellAfter = grid.getElement()
                .getElementsByTagName("tbody").getItem(0)
                .getElementsByTagName("td").getItem(0);
        assertEquals("Selectable words in row one", cellAfter.getInnerText());
        assertEquals("selection after the row update", "words",
                selectionInside(cellAfter));
    }
}
