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

import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.event.Action;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.PaintTarget;

/**
 * M05, review R1, with the explicit HTML API: the overridden getCaption() is
 * sent unchanged on the four paths. Uses API added by F2b: not compiled
 * against 7.7.17.
 */
public class ActionCaptionOverrideAsHtmlTest {

    private static Action html(Action a) {
        a.setCaptionAsHtml(true);
        return a;
    }

    @Test
    public void table() throws Exception {
        PaintTarget t = Mockito.mock(PaintTarget.class);
        ActionCaptionSanitizationTest.table(html(ActionCaptionOverrideTest.dynamic())).paintContent(t);
        ActionCaptionOverrideTest.check(ActionCaptionSanitizationTest.paintedCaptions(t), ActionCaptionOverrideTest.DYNAMIC);
    }

    @Test
    public void tree() throws Exception {
        PaintTarget t = Mockito.mock(PaintTarget.class);
        ActionCaptionSanitizationTest.tree(html(ActionCaptionOverrideTest.dynamic())).paintContent(t);
        ActionCaptionOverrideTest.check(ActionCaptionSanitizationTest.paintedCaptions(t), ActionCaptionOverrideTest.DYNAMIC);
    }

    @Test
    public void calendar() throws Exception {
        ActionCaptionOverrideTest.check(ActionCaptionSanitizationTest.calendarCaptions(html(ActionCaptionOverrideTest.dynamic())), ActionCaptionOverrideTest.DYNAMIC);
    }

    @Test
    public void shortcut() throws Exception {
        ShortcutAction a = ActionCaptionOverrideTest.dynamicShortcut();
        a.setCaptionAsHtml(true);
        PaintTarget t = Mockito.mock(PaintTarget.class);
        ActionCaptionSanitizationTest.panel(a).paintContent(t);
        ActionCaptionOverrideTest.check(ActionCaptionSanitizationTest.paintedCaptions(t), ActionCaptionOverrideTest.DYNAMIC);
    }
}
