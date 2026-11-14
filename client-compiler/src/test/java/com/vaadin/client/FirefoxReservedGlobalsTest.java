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
package com.vaadin.client;

import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.google.gwt.dev.js.JsProtectedNames;

/**
 * REM F2c, item I04: Firefox 67+ defines a global variable called "u2f". The
 * GWT compiler must not use it as an obfuscated identifier. The compiler reads
 * the reserved browser globals with
 * {@code JsProtectedNames.class.getResource("globals/firefox25.txt")}, so the
 * list shipped in vaadin-client (before gwt-dev on the class path of
 * vaadin-client-compiler and of widgetset compilations) overrides the list
 * of gwt-dev.
 */
public class FirefoxReservedGlobalsTest {

    @Test
    public void u2fIsNotALegalIdentifier() {
        Assert.assertFalse("u2f must be a reserved global",
                JsProtectedNames.isLegalName("u2f"));
    }

    @Test
    public void firefoxGlobalsComeFromVaadinClient() {
        URL url = JsProtectedNames.class.getResource("globals/firefox25.txt");
        Assert.assertNotNull(url);
        Assert.assertFalse("firefox25.txt read from gwt-dev: " + url,
                url.toString().contains("gwt-dev"));
    }

    @Test
    public void otherGlobalsAreStillReserved() {
        // from the gwt-dev list, kept in the Vaadin list
        Assert.assertFalse(JsProtectedNames.isLegalName("window"));
        Assert.assertFalse(JsProtectedNames.isLegalName("document"));
        Assert.assertFalse(JsProtectedNames.isLegalName("XMLHttpRequest"));
    }
}
