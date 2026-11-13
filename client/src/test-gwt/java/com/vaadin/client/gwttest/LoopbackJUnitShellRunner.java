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
package com.vaadin.client.gwttest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.runner.JUnitCore;

import com.google.gwt.junit.JUnitShell;

/**
 * REM F2c: runs GWTTestCase classes with HtmlUnit in development mode, with the
 * embedded web server and the code server bound to 127.0.0.1 only.
 * <p>
 * GWT 2.7 {@code JUnitShell.getUnitTestShell()} always binds to 0.0.0.0. This
 * runner creates the shared shell in the same way, through reflection, but
 * with the loopback address, before JUnit runs the tests. Ports and run style
 * come from the {@code gwt.args} system property as usual.
 * <p>
 * Usage: {@code java -Dgwt.args="-runStyle HtmlUnit:Chrome -port P -codeServerPort Q ..."
 * com.vaadin.client.gwttest.LoopbackJUnitShellRunner <test classes>}
 */
public class LoopbackJUnitShellRunner {

    private static final String LOOPBACK = "127.0.0.1";

    public static void main(String[] args) throws Exception {
        createLoopbackShell();
        JUnitCore.main(args);
    }

    private static void createLoopbackShell() throws Exception {
        Constructor<JUnitShell> constructor = JUnitShell.class
                .getDeclaredConstructor();
        constructor.setAccessible(true);
        JUnitShell shell = constructor.newInstance();
        field(JUnitShell.class, "unitTestShell").set(null, shell);
        field(JUnitShell.class, "lastLaunchFailed").setBoolean(shell, true);

        Method synthesizeArgs = JUnitShell.class
                .getDeclaredMethod("synthesizeArgs");
        synthesizeArgs.setAccessible(true);
        String[] shellArgs = (String[]) synthesizeArgs.invoke(shell);

        Class<?> argProcessorClass = Class
                .forName(JUnitShell.class.getName() + "$ArgProcessor");
        Constructor<?> argProcessorConstructor = argProcessorClass
                .getDeclaredConstructor(JUnitShell.class);
        argProcessorConstructor.setAccessible(true);
        Object argProcessor = argProcessorConstructor.newInstance(shell);
        Method processArgs = findMethod(argProcessorClass, "processArgs",
                String[].class);
        if (!(Boolean) processArgs.invoke(argProcessor,
                (Object) shellArgs)) {
            throw new IllegalStateException("Error processing shell arguments");
        }

        Object options = field(JUnitShell.class, "options").get(shell);
        findMethod(options.getClass(), "setBindAddress", String.class)
                .invoke(options, LOOPBACK);
        findMethod(options.getClass(), "setConnectAddress", String.class)
                .invoke(options, LOOPBACK);

        Method startUp = findMethod(JUnitShell.class, "startUp");
        if (!(Boolean) startUp.invoke(shell)) {
            throw new IllegalStateException("Shell failed to start");
        }
        field(JUnitShell.class, "lastLaunchFailed").setBoolean(shell, false);
    }

    private static Field field(Class<?> type, String name)
            throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                // look in the superclass
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Method findMethod(Class<?> type, String name,
            Class<?>... parameterTypes) throws NoSuchMethodException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, parameterTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                // look in the superclass
            }
        }
        throw new NoSuchMethodException(name);
    }
}
