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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;
import com.vaadin.client.renderers.TextRenderer;
import com.vaadin.client.widget.grid.datasources.ListDataSource;
import com.vaadin.client.widgets.Grid;
import com.vaadin.client.widgets.Grid.Column;

/**
 * REM F2c, item M03 client side (CVE-2019-25028): the column hiding toggles in
 * the Grid sidebar show the hiding toggle caption, or the header caption, as
 * text. The column API of the client Grid has no way to declare it as HTML.
 */
public class GridHidingToggleCaptionGwtTest extends GWTTestCase {

    private static final String HOSTILE = "<img id=\"f2c-toggle-img\" src=\"x\" onerror=\"window.f2cToggleXss=1\"><b>bold</b>";

    @Override
    public String getModuleName() {
        return "com.vaadin.client.gwttest.F2cGwtTestsWidgetSet";
    }

    private static Column<String, String[]> column(final int index,
            String caption) {
        Column<String, String[]> column = new Column<String, String[]>(
                caption, new TextRenderer()) {
            @Override
            public String getValue(String[] row) {
                return row[index];
            }
        };
        return column;
    }

    private Grid<String[]> grid() {
        Grid<String[]> grid = new Grid<String[]>();
        grid.setWidth("600px");
        grid.setHeight("300px");
        grid.setDataSource(new ListDataSource<String[]>(
                Arrays.asList(new String[][] { { "a", "b" } })));
        RootPanel.get().add(grid);
        return grid;
    }

    private static Element toggleCaption(Grid<?> grid, int index) {
        NodeList<Element> spans = grid.getSidebarMenu().getElement()
                .getElementsByTagName("span");
        int found = 0;
        for (int i = 0; i < spans.getLength(); i++) {
            Element span = spans.getItem(i);
            String cls = span.getClassName();
            if ("v-on".equals(cls) || "v-off".equals(cls)) {
                if (found++ == index) {
                    return span;
                }
            }
        }
        return null;
    }

    private static int markup(Element e) {
        return e.getElementsByTagName("img").getLength()
                + e.getElementsByTagName("b").getLength();
    }

    public void testHeaderCaptionIsText() {
        Grid<String[]> grid = grid();
        Column<String, String[]> c = column(0, HOSTILE);
        grid.addColumn(c);
        c.setHidable(true);
        Element caption = toggleCaption(grid, 0);
        assertNotNull("hiding toggle not found", caption);
        assertEquals("elements created from the caption", 0, markup(caption));
        assertTrue(caption.getInnerHTML(),
                caption.getInnerHTML().contains("&lt;img"));
    }

    public void testHidingToggleCaptionIsText() {
        Grid<String[]> grid = grid();
        Column<String, String[]> c = column(0, "plain");
        c.setHidingToggleCaption(HOSTILE);
        grid.addColumn(c);
        c.setHidable(true);
        Element caption = toggleCaption(grid, 0);
        assertNotNull("hiding toggle not found", caption);
        assertEquals("elements created from the caption", 0, markup(caption));
    }

    public void testToggleUpdatedAfterHidingIsText() {
        Grid<String[]> grid = grid();
        Column<String, String[]> c = column(0, HOSTILE);
        grid.addColumn(c);
        c.setHidable(true);
        grid.addColumn(column(1, "other"));
        c.setHidden(true);
        Element caption = toggleCaption(grid, 0);
        assertNotNull("hiding toggle not found", caption);
        assertEquals("v-off", caption.getClassName());
        assertEquals("elements created from the caption", 0, markup(caption));
    }

    public void testPlainCaptionUnchanged() {
        Grid<String[]> grid = grid();
        Column<String, String[]> c = column(0, "Name & surname");
        grid.addColumn(c);
        c.setHidable(true);
        Element caption = toggleCaption(grid, 0);
        assertNotNull("hiding toggle not found", caption);
        assertEquals("<div>name &amp; surname</div>",
                caption.getInnerHTML().toLowerCase());
    }
}
