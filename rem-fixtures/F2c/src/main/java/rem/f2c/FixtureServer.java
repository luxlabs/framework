/*
 * Part of the 7.7.17.1 maintenance fork of Vaadin Framework (REM F2c).
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
package rem.f2c;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;

/**
 * REM F2c browser fixture: one Jetty context per scenario, bound to 127.0.0.1
 * only. Usage: {@code FixtureServer <port>}.
 */
public class FixtureServer {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(port);
        server.addConnector(connector);

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.addHandler(context("/panel", PanelUI.class, false));
        contexts.addHandler(context("/keyboard", KeyboardUI.class, false));
        contexts.addHandler(context("/combo", ComboUI.class, false));
        contexts.addHandler(context("/table", TableUI.class, false));
        contexts.addHandler(context("/grid", GridUI.class, false));
        contexts.addHandler(context("/responsive", ResponsiveUI.class, true));
        server.setHandler(contexts);
        server.setStopAtShutdown(true);
        server.start();
        System.out.println("F2C-FIXTURE-READY " + port);
        server.join();
    }

    private static ServletContextHandler context(String path,
            Class<? extends UI> ui, boolean responsiveStyles) {
        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath(path);
        VaadinServlet servlet = responsiveStyles ? new ResponsiveServlet()
                : new VaadinServlet();
        ServletHolder holder = new ServletHolder(ui.getSimpleName(), servlet);
        holder.setInitParameter("UI", ui.getName());
        holder.setInitParameter("widgetset", "com.vaadin.DefaultWidgetSet");
        holder.setInitParameter("productionMode", "false");
        context.addServlet(holder, "/*");
        return context;
    }
}
