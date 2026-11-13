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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.junit.client.GWTTestCase;
import com.vaadin.client.ui.VPanel;

/**
 * REM F2c, items I05 and M04: the Panel caption is set as text unless
 * captionAsHtml is true.
 */
public class VPanelCaptionGwtTest extends GWTTestCase {

    private static final String HOSTILE = "<b id=\"f2c-x\">bold</b><img src=\"x\" onerror=\"window.f2cXss=1\">";

    @Override
    public String getModuleName() {
        return "com.vaadin.client.gwttest.F2cGwtTests";
    }

    private static Element captionText(VPanel panel) {
        return panel.captionNode.getFirstChildElement();
    }

    /**
     * Text of the caption, from its child text nodes (HtmlUnit implements
     * neither innerText nor textContent).
     */
    private static String textContent(Element element) {
        StringBuilder text = new StringBuilder();
        for (Node child = element.getFirstChild(); child != null; child = child
                .getNextSibling()) {
            if (child.getNodeType() == Node.TEXT_NODE) {
                text.append(child.getNodeValue());
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                text.append(textContent(Element.as(child)));
            }
        }
        return text.toString();
    }

    public void testCaptionAsTextCreatesNoElements() {
        VPanel panel = new VPanel();
        panel.setCaption(HOSTILE, false);
        Element caption = captionText(panel);
        assertEquals("elements created from the caption", 0,
                caption.getElementsByTagName("*").getLength());
        assertEquals(HOSTILE, textContent(caption));
    }

    public void testCaptionAsHtmlKeepsMarkup() {
        VPanel panel = new VPanel();
        panel.setCaption("<b>bold</b>", true);
        Element caption = captionText(panel);
        assertEquals(1, caption.getElementsByTagName("b").getLength());
        assertEquals("bold", textContent(caption));
    }

    public void testSwitchingFromHtmlToTextRemovesMarkup() {
        VPanel panel = new VPanel();
        panel.setCaption("<b>bold</b>", true);
        panel.setCaption("<b>bold</b>", false);
        Element caption = captionText(panel);
        assertEquals(0, caption.getElementsByTagName("b").getLength());
        assertEquals("<b>bold</b>", textContent(caption));
    }
}
