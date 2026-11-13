/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.tests.server.component.grid;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.shared.ui.grid.GridColumnState;
import com.vaadin.shared.ui.grid.GridState;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;

/**
 * CVE-2019-25028 (M03 / I06): the header caption of a Grid column reaches the
 * client as plain text, also where the client renders it as HTML (column
 * hiding toggle, editor error message).
 */
public class GridHeaderCaptionSanitizationTest {

    private static final String HOSTILE = "<img src=x onerror=\"window.__xss=1\">Name <b>bold</b>";

    private Grid grid;
    private GridState state;

    @Before
    public void setup() throws Exception {
        IndexedContainer ds = new IndexedContainer();
        ds.addContainerProperty("name", String.class, "");
        grid = new Grid(ds);
        Method getState = Grid.class.getDeclaredMethod("getState");
        getState.setAccessible(true);
        state = (GridState) getState.invoke(grid);
    }

    private GridColumnState columnState() {
        for (GridColumnState column : state.columns) {
            if (column != null && column.headerCaption != null) {
                return column;
            }
        }
        throw new AssertionError("no column state");
    }

    @Test
    public void hostileHeaderCaptionIsSentAsText() {
        Column column = grid.getColumn("name");
        column.setHeaderCaption(HOSTILE);

        String sent = columnState().headerCaption;
        Assert.assertFalse("markup in the header caption state: " + sent,
                sent.contains("<"));
        Assert.assertEquals("Name bold", sent);
        Assert.assertEquals("Name bold", column.getHeaderCaption());
        Assert.assertEquals("Name bold", grid.getDefaultHeaderRow()
                .getCell("name").getText());
    }

    @Test
    public void encodedMarkupIsNotDecodedIntoMarkup() {
        Column column = grid.getColumn("name");
        column.setHeaderCaption("&lt;img src=x onerror=alert(1)&gt;");
        String sent = columnState().headerCaption;
        Assert.assertFalse("decoded markup in the header caption state: "
                + sent, sent.contains("<img"));
        column.setHeaderCaption("&amp;lt;img src=x onerror=alert(1)&amp;gt;");
        sent = columnState().headerCaption;
        Assert.assertFalse("decoded markup in the header caption state: "
                + sent, sent.contains("<img"));
    }

    @Test
    public void hidingToggleCaptionIsSentAsText() {
        Column column = grid.getColumn("name");
        column.setHidingToggleCaption(HOSTILE);
        Assert.assertEquals("Name bold", columnState().hidingToggleCaption);
        column.setHidingToggleCaption(null);
        Assert.assertNull(columnState().hidingToggleCaption);
    }

    @Test
    public void plainCaptionsAreUnchanged() {
        Column column = grid.getColumn("name");
        column.setHeaderCaption("Price & tax (EUR)");
        Assert.assertEquals("Price & tax (EUR)", columnState().headerCaption);
        column.setHeaderCaption("a < b");
        Assert.assertEquals("a < b", columnState().headerCaption);
        column.setHeaderCaption(null);
        Assert.assertEquals("", columnState().headerCaption);
    }
}
