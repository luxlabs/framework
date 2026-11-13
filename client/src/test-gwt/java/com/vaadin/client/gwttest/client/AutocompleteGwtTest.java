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

import com.google.gwt.junit.client.GWTTestCase;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.ui.VFilterSelect;
import com.vaadin.client.ui.VTextualDate;

/**
 * REM F2c, item I03: Chrome ignores invalid autocomplete values, so ComboBox
 * and DateField inputs must use autocomplete="off" in Chrome to stop the
 * browser's own suggestion popup. Run with the HtmlUnit Chrome run style.
 */
public class AutocompleteGwtTest extends GWTTestCase {

    @Override
    public String getModuleName() {
        return "com.vaadin.client.gwttest.F2cGwtTestsWidgetSet";
    }

    public void testRunsAsChrome() {
        assertTrue("run with -runStyle HtmlUnit:Chrome",
                BrowserInfo.get().isChrome());
    }

    public void testComboBoxInputHasAutocompleteOff() {
        VFilterSelect comboBox = new VFilterSelect();
        assertEquals("off",
                comboBox.tb.getElement().getAttribute("autocomplete"));
    }

    public void testDateFieldInputHasAutocompleteOff() {
        VTextualDate dateField = new VTextualDate();
        assertEquals("off",
                dateField.text.getElement().getAttribute("autocomplete"));
    }
}
