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
package com.vaadin.tests.server.rem;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.event.Action;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;
import com.vaadin.shared.ui.calendar.CalendarState;
import com.vaadin.ui.Calendar;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import com.vaadin.tests.util.MockUI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Component;
import com.vaadin.util.CurrentInstance;

/**
 * CVE-2025-15022 (M05 / I52): by default the caption of an {@link Action}
 * reaches the client, which renders it as HTML, as escaped text. Covers the
 * context menus of {@link Table} and {@link Tree} and the shortcut actions
 * painted by {@code ActionManager} (here through {@link Panel}) and the
 * actions of {@link Calendar}, sent in its shared state.
 */
public class ActionCaptionSanitizationTest {

    static final String HOSTILE = "<img src=x onerror=\"window.__xss=1\"> Save & 'close'";
    static final String ESCAPED = "&lt;img src=x onerror=&quot;window.__xss=1&quot;&gt; Save &amp; &#39;close&#39;";

    static class FixedHandler implements Action.Handler {
        private final Action[] actions;

        FixedHandler(Action... actions) {
            this.actions = actions;
        }

        @Override
        public Action[] getActions(Object target, Object sender) {
            return actions;
        }

        @Override
        public void handleAction(Action action, Object sender, Object target) {
        }
    }

    /** Captions painted with addAttribute("caption", String). */
    static List<String> paintedCaptions(PaintTarget target)
            throws PaintException {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(target, Mockito.atLeast(0))
                .addAttribute(Mockito.eq("caption"), captor.capture());
        return new ArrayList<String>(captor.getAllValues());
    }

    static void assertEscaped(List<String> captions) {
        Assert.assertTrue("action caption not painted: " + captions,
                captions.contains(ESCAPED) || captions.contains(HOSTILE));
        for (String caption : captions) {
            Assert.assertFalse("action caption sent to the client as HTML: "
                    + caption, caption.contains("<img"));
        }
        Assert.assertTrue(captions.contains(ESCAPED));
    }

    /** Attaches the component to a UI, as when it is painted for real. */
    static <C extends Component> C attach(C component) {
        CurrentInstance.clearAll();
        UI ui = new MockUI();
        VerticalLayout layout = new VerticalLayout();
        layout.addComponent(component);
        ui.setContent(layout);
        return component;
    }

    static Table table(Action action) {
        Table table = new Table();
        table.addContainerProperty("p", String.class, "");
        table.addItem("row");
        table.addActionHandler(new FixedHandler(action));
        return attach(table);
    }

    static Tree tree(Action action) {
        Tree tree = new Tree();
        tree.addItem("node");
        tree.addActionHandler(new FixedHandler(action));
        return attach(tree);
    }

    static Panel panel(ShortcutAction action) {
        Panel panel = new Panel();
        panel.addActionHandler(new FixedHandler(action));
        return attach(panel);
    }

    @Test
    public void tableContextMenuCaptionIsEscaped() throws PaintException {
        PaintTarget target = Mockito.mock(PaintTarget.class);
        table(new Action(HOSTILE)).paintContent(target);
        assertEscaped(paintedCaptions(target));
    }

    @Test
    public void treeContextMenuCaptionIsEscaped() throws PaintException {
        PaintTarget target = Mockito.mock(PaintTarget.class);
        tree(new Action(HOSTILE)).paintContent(target);
        assertEscaped(paintedCaptions(target));
    }

    @Test
    public void shortcutActionCaptionIsEscaped() throws PaintException {
        PaintTarget target = Mockito.mock(PaintTarget.class);
        panel(new ShortcutAction(HOSTILE, ShortcutAction.KeyCode.S,
                new int[] { ShortcutAction.ModifierKey.CTRL })).paintContent(target);
        assertEscaped(paintedCaptions(target));
        // the shortcut itself is still painted
        Mockito.verify(target).addAttribute("kc", ShortcutAction.KeyCode.S);
    }

    /** Captions of the actions that a Calendar sends in its state. */
    static List<String> calendarCaptions(Action action) throws Exception {
        Calendar calendar = attach(new Calendar());
        java.util.Date start = new java.util.GregorianCalendar(2026, 10, 13)
                .getTime();
        calendar.setStartDate(start);
        calendar.setEndDate(start);
        calendar.addActionHandler(new FixedHandler(action));
        calendar.beforeClientResponse(true);
        java.lang.reflect.Method getState = Calendar.class
                .getDeclaredMethod("getState");
        getState.setAccessible(true);
        CalendarState state = (CalendarState) getState.invoke(calendar);
        List<String> captions = new ArrayList<String>();
        Assert.assertNotNull("no calendar actions in the state", state.actions);
        for (CalendarState.Action a : state.actions) {
            captions.add(a.caption);
        }
        return captions;
    }

    @Test
    public void calendarActionCaptionIsEscaped() throws Exception {
        assertEscaped(calendarCaptions(new Action(HOSTILE)));
    }

    @Test
    public void getCaptionReturnsTheOriginalValue() {
        Assert.assertEquals(HOSTILE, new Action(HOSTILE).getCaption());
    }
}
