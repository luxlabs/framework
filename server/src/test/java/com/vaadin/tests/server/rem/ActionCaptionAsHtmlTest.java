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
package com.vaadin.tests.server.rem;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.event.Action;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;

/**
 * M05: API to send an action caption as HTML on purpose. Uses API added by
 * the fix: not compiled against 7.7.17.
 */
public class ActionCaptionAsHtmlTest {

    private static final String HTML = "<b>Bold</b> &amp; plain";

    @Test
    public void defaultIsText() {
        Action action = new Action("x");
        Assert.assertFalse(action.isCaptionAsHtml());
    }

    private static void assertSentUnchanged(PaintTarget target)
            throws PaintException {
        List<String> captions = ActionCaptionSanitizationTest
                .paintedCaptions(target);
        Assert.assertTrue("caption not sent unchanged: " + captions,
                captions.contains(HTML));
    }

    @Test
    public void tableCaptionAsHtml() throws PaintException {
        Action action = new Action(HTML);
        action.setCaptionAsHtml(true);
        PaintTarget target = Mockito.mock(PaintTarget.class);
        ActionCaptionSanitizationTest.table(action).paintContent(target);
        assertSentUnchanged(target);
    }

    @Test
    public void treeCaptionAsHtml() throws PaintException {
        Action action = new Action(HTML);
        action.setCaptionAsHtml(true);
        PaintTarget target = Mockito.mock(PaintTarget.class);
        ActionCaptionSanitizationTest.tree(action).paintContent(target);
        assertSentUnchanged(target);
    }

    @Test
    public void shortcutCaptionAsHtml() throws PaintException {
        ShortcutAction action = new ShortcutAction(HTML,
                ShortcutAction.KeyCode.S, new int[0]);
        action.setCaptionAsHtml(true);
        PaintTarget target = Mockito.mock(PaintTarget.class);
        ActionCaptionSanitizationTest.panel(action).paintContent(target);
        assertSentUnchanged(target);
    }

    @Test
    public void nullCaptionIsNotPainted() throws PaintException {
        PaintTarget target = Mockito.mock(PaintTarget.class);
        ActionCaptionSanitizationTest.table(new Action(null))
                .paintContent(target);
        for (String caption : ActionCaptionSanitizationTest
                .paintedCaptions(target)) {
            Assert.assertNotNull(caption);
        }
    }
}
