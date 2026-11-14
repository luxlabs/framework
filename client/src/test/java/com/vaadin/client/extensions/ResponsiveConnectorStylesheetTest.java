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
package com.vaadin.client.extensions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * REM F2c, item I55: {@code ResponsiveConnector} must not crash on style sheet
 * rules that have no {@code selectorText} (for example {@code @layer} or
 * {@code @container} blocks, whose legacy {@code type} is 0).
 * <p>
 * The JSNI body of {@code searchStylesheetForBreakPoints} is taken from the
 * source file on the class path (the client jar ships its sources for GWT),
 * the JSNI references are replaced with plain JavaScript stubs and the result
 * is run with the JDK JavaScript engine (Nashorn), without a browser.
 */
public class ResponsiveConnectorStylesheetTest {

    private static final String SOURCE = "com/vaadin/client/extensions/ResponsiveConnector.java";

    private ScriptEngine engine;

    @Before
    public void setUp() throws Exception {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        Assert.assertNotNull("Nashorn JavaScript engine not available",
                engine);
        String body = jsniBody(readSource(), "searchStylesheetForBreakPoints");
        String script = "var widthRangeCache = [], heightRangeCache = [], messages = [];\n"
                + "var browserInfo = { isIE: function() { return false; },"
                + " isEdge: function() { return false; },"
                + " isIE8: function() { return false; } };\n"
                + "function warning(m) { messages.push('warning: ' + m); }\n"
                + "function error(m) { messages.push('error: ' + m); }\n"
                + "function searchStylesheetForBreakPoints(sheet) {\n"
                + toPlainJs(body) + "\n}\n";
        engine.eval(script);
    }

    @Test
    public void ruleWithoutSelectorTextIsSkipped() throws ScriptException {
        // @layer block (type 0, no selectorText), then a regular rule
        engine.eval("searchStylesheetForBreakPoints({ cssRules: ["
                + " { type: 0, cssText: '@layer base { .a { color: red; } }' },"
                + " { type: 1, selectorText: '.card[width-range~=\"0-300px\"]' } ] });");
        Assert.assertEquals("1", engine.eval("String(widthRangeCache.length)"));
        Assert.assertEquals(".card,0,300px",
                engine.eval("String(widthRangeCache[0])"));
    }

    @Test
    public void ruleWithoutTypeAndSelectorTextIsSkipped()
            throws ScriptException {
        engine.eval("searchStylesheetForBreakPoints({ cssRules: ["
                + " { cssText: '@container (min-width: 10px) { .b { color: red; } }' },"
                + " { type: 1, selectorText: '.side[height-range~=\"100px-200px\"]' } ] });");
        Assert.assertEquals("1", engine.eval("String(heightRangeCache.length)"));
        Assert.assertEquals(".side,100px,200px",
                engine.eval("String(heightRangeCache[0])"));
    }

    @Test
    public void fontFaceRuleIsSkipped() throws ScriptException {
        engine.eval("searchStylesheetForBreakPoints({ cssRules: ["
                + " { type: 5, cssText: '@font-face { font-family: x; }' },"
                + " { type: 1, selectorText: '.c[width-range~=\"301px-\"]' } ] });");
        Assert.assertEquals("1", engine.eval("String(widthRangeCache.length)"));
    }

    private static String readSource() throws IOException {
        InputStream in = ResponsiveConnectorStylesheetTest.class
                .getClassLoader().getResourceAsStream(SOURCE);
        Assert.assertNotNull("Source not on the class path: " + SOURCE, in);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    private static String jsniBody(String source, String method) {
        Matcher m = Pattern.compile("native\\s+void\\s+" + method
                + "\\s*\\([^)]*\\)\\s*/\\*-\\{(.*?)\\}-\\*/", Pattern.DOTALL)
                .matcher(source);
        Assert.assertTrue("JSNI method not found: " + method, m.find());
        return m.group(1);
    }

    private static String toPlainJs(String jsni) {
        String js = jsni.replace("@com.vaadin.client.BrowserInfo::get()()",
                "browserInfo");
        // instance method calls: .@pkg.Class::name()()
        js = js.replaceAll("\\.@[\\w.]+::(\\w+)\\([^)]*\\)\\(", ".$1(");
        // static method calls: @pkg.Class::name(signature)(
        js = js.replaceAll("@[\\w.]+::(\\w+)\\([^)]*\\)\\(", "$1(");
        // static fields: @pkg.Class::name
        js = js.replaceAll("@[\\w.]+::(\\w+)", "$1");
        Assert.assertFalse("Unresolved JSNI reference: " + js,
                js.contains("@") && js.contains("::"));
        return js;
    }
}
