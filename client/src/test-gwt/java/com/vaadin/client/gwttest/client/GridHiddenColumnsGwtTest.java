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

import java.util.Arrays;
import java.util.List;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;
import com.vaadin.client.renderers.TextRenderer;
import com.vaadin.client.widget.grid.datasources.ListDataSource;
import com.vaadin.client.widgets.Grid;
import com.vaadin.client.widgets.Grid.Column;

/**
 * REM F2c, item I15 (F2d item on a Grid.java owned by F2c): adding columns
 * when there are hidden columns must not throw IndexOutOfBoundsException.
 * Reproduction on 7.7.17 first: the release note does not say in which
 * version the regression appeared.
 */
public class GridHiddenColumnsGwtTest extends GWTTestCase {

    @Override
    public String getModuleName() {
        return "com.vaadin.client.gwttest.F2cGwtTestsWidgetSet";
    }

    private static Column<String, String[]> column(final int index) {
        Column<String, String[]> column = new Column<String, String[]>(
                "c" + index, new TextRenderer()) {
            @Override
            public String getValue(String[] row) {
                return row[index];
            }
        };
        return column;
    }

    private Grid<String[]> createGrid(int columns) {
        Grid<String[]> grid = new Grid<String[]>();
        grid.setWidth("600px");
        grid.setHeight("300px");
        List<String[]> rows = Arrays.asList(
                new String[] { "a0", "a1", "a2", "a3", "a4", "a5" },
                new String[] { "b0", "b1", "b2", "b3", "b4", "b5" });
        grid.setDataSource(new ListDataSource<String[]>(rows));
        for (int i = 0; i < columns; i++) {
            grid.addColumn(column(i));
        }
        RootPanel.get().add(grid);
        return grid;
    }

    public void testAddColumnAfterHiddenColumn() {
        Grid<String[]> grid = createGrid(3);
        grid.getColumn(1).setHidden(true);
        grid.addColumn(column(3));
        grid.addColumn(column(4), 1);
        assertEquals(5, grid.getColumnCount());
        assertEquals(4, grid.getVisibleColumns().size());
    }

    public void testAddColumnsWhenLastColumnsHidden() {
        Grid<String[]> grid = createGrid(4);
        grid.getColumn(2).setHidden(true);
        grid.getColumn(3).setHidden(true);
        grid.addColumns(column(4), column(5));
        assertEquals(6, grid.getColumnCount());
        assertEquals(4, grid.getVisibleColumns().size());
    }

    public void testAddColumnAtIndexBeforeHiddenColumns() {
        Grid<String[]> grid = createGrid(4);
        grid.getColumn(0).setHidden(true);
        grid.getColumn(3).setHidden(true);
        grid.addColumn(column(4), 0);
        grid.addColumn(column(5), 4);
        assertEquals(6, grid.getColumnCount());
        assertEquals(4, grid.getVisibleColumns().size());
    }
}
