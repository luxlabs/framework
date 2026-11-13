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
package com.vaadin.client.ui;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gwt.junit.GWTMockUtilities;
import com.vaadin.shared.VBrowserDetails;

/**
 * REM F2c, item I02: keyboard navigation in MenuBar, Slider, DateField
 * calendar, Tree and Table must use key down events on Firefox 65 and newer,
 * and key press events only on older Gecko versions.
 * <p>
 * In this module because loading the widget classes on the JVM needs the GWT
 * bridge of gwt-dev, which is not on the test class path of the client module.
 */
public class OldGeckoNavigationTest {

    private static final String FIREFOX_52 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:52.0) Gecko/20100101 Firefox/52.0";
    private static final String FIREFOX_64 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:64.0) Gecko/20100101 Firefox/64.0";
    private static final String FIREFOX_65 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:65.0) Gecko/20100101 Firefox/65.0";
    private static final String FIREFOX_67 = "Mozilla/5.0 (X11; Linux x86_64; rv:67.0) Gecko/20100101 Firefox/67.0";
    private static final String FIREFOX_128 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:128.0) Gecko/20100101 Firefox/128.0";
    private static final String CHROME_120 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String SAFARI_17 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15";
    private static final String IE_11 = "Mozilla/5.0 (Windows NT 10.0; Trident/7.0; rv:11.0) like Gecko";

    @BeforeClass
    public static void disarmGwt() {
        GWTMockUtilities.disarm();
    }

    @AfterClass
    public static void restoreGwt() {
        GWTMockUtilities.restore();
    }

    /**
     * Same values as {@code BrowserInfo.isGecko()} and
     * {@code BrowserInfo.getGeckoVersion()} for the user agent.
     */
    private static boolean useKeyPress(String userAgent) {
        VBrowserDetails details = new VBrowserDetails(userAgent);
        float geckoVersion = details.isGecko()
                ? details.getBrowserEngineVersion()
                : -1;
        return VMenuBar.isOldGeckoNavigation(details.isGecko(), geckoVersion);
    }

    @Test
    public void firefoxBefore65UsesKeyPress() {
        Assert.assertTrue("Firefox 52", useKeyPress(FIREFOX_52));
        Assert.assertTrue("Firefox 64", useKeyPress(FIREFOX_64));
    }

    @Test
    public void firefox65AndNewerUsesKeyDown() {
        Assert.assertFalse("Firefox 65", useKeyPress(FIREFOX_65));
        Assert.assertFalse("Firefox 67", useKeyPress(FIREFOX_67));
        Assert.assertFalse("Firefox 128", useKeyPress(FIREFOX_128));
    }

    @Test
    public void otherBrowsersUseKeyDown() {
        Assert.assertFalse("Chrome", useKeyPress(CHROME_120));
        Assert.assertFalse("Safari", useKeyPress(SAFARI_17));
        Assert.assertFalse("IE 11", useKeyPress(IE_11));
    }
}
