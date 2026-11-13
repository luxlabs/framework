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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.event.Action;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.PaintTarget;

/**
 * M05, review R1: the caption sent to the client is the one returned by the
 * (overridable) getCaption(), escaped by default, on the four paths: Table,
 * Tree, Calendar and the shortcuts painted by ActionManager. Derived from the
 * probe ActionOverrideProbeTest of the F2b verifier.
 */
public class ActionCaptionOverrideTest {

    static final String DYNAMIC = "dynamic <i>translated</i> caption";
    static final String DYNAMIC_ESCAPED = "dynamic &lt;i&gt;translated&lt;/i&gt; caption";

    static Action dynamic() {
        return new Action("constructor caption") {
            @Override
            public String getCaption() {
                return DYNAMIC;
            }
        };
    }

    static ShortcutAction dynamicShortcut() {
        return new ShortcutAction("constructor caption",
                ShortcutAction.KeyCode.S, new int[0]) {
            @Override
            public String getCaption() {
                return DYNAMIC;
            }
        };
    }

    static void check(List<String> captions, String expected) {
        Assert.assertTrue("caption of the overridden getCaption() not painted ("
                + expected + "); painted=" + captions,
                captions.contains(expected));
        Assert.assertFalse(captions.contains("constructor caption"));
    }

    @Test
    public void tableUsesOverriddenCaption() throws Exception {
        PaintTarget t = Mockito.mock(PaintTarget.class);
        ActionCaptionSanitizationTest.table(dynamic()).paintContent(t);
        check(ActionCaptionSanitizationTest.paintedCaptions(t), DYNAMIC_ESCAPED);
    }

    @Test
    public void treeUsesOverriddenCaption() throws Exception {
        PaintTarget t = Mockito.mock(PaintTarget.class);
        ActionCaptionSanitizationTest.tree(dynamic()).paintContent(t);
        check(ActionCaptionSanitizationTest.paintedCaptions(t), DYNAMIC_ESCAPED);
    }

    @Test
    public void calendarUsesOverriddenCaption() throws Exception {
        check(ActionCaptionSanitizationTest.calendarCaptions(dynamic()),
                DYNAMIC_ESCAPED);
    }

    @Test
    public void shortcutUsesOverriddenCaption() throws Exception {
        PaintTarget t = Mockito.mock(PaintTarget.class);
        ActionCaptionSanitizationTest.panel(dynamicShortcut()).paintContent(t);
        check(ActionCaptionSanitizationTest.paintedCaptions(t), DYNAMIC_ESCAPED);
    }

    @Test
    public void updatedCaptionIsPainted() throws Exception {
        Action action = new Action("first");
        action.setCaption("second <b>");
        PaintTarget t = Mockito.mock(PaintTarget.class);
        ActionCaptionSanitizationTest.table(action).paintContent(t);
        check(ActionCaptionSanitizationTest.paintedCaptions(t), "second &lt;b&gt;");
    }
}
