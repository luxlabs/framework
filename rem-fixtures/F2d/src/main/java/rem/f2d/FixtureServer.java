/*
 * Part of the 7.7.17.1 maintenance fork of Vaadin Framework (REM F2d).
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
package rem.f2d;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;

/**
 * REM F2d browser fixture: one Jetty context per scenario, bound to
 * 127.0.0.1 only. Usage: {@code FixtureServer <port>}.
 */
public class FixtureServer {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        Server server = start(port);
        System.out.println("F2D-FIXTURE-READY " + port);
        server.join();
    }

    /** Starts the fixture on 127.0.0.1:port and returns the running server. */
    public static Server start(int port) throws Exception {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(port);
        server.addConnector(connector);

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.addHandler(context("/push-rapid", PushRapidUI.class));
        contexts.addHandler(context("/i09", PushResyncUI.class));
        Diag.captureVaadinPushLog();
        ServletContextHandler themeContext = context("/i09-theme",
                PushThemeResyncUI.class);
        // Diagnostics: Atmosphere interceptor of the fixture (init parameter
        // org.atmosphere.cpr.AtmosphereInterceptor), only on this context
        themeContext.getServletHandler().getServlet("PushThemeResyncUI")
                .setInitParameter("org.atmosphere.cpr.AtmosphereInterceptor",
                        DiagInterceptor.class.getName());
        themeContext.addServlet(new ServletHolder("diag", new Diag.Servlet()),
                "/diag");
        contexts.addHandler(themeContext);
        contexts.addHandler(context("/i11", GridResizeUI.class));
        contexts.addHandler(context("/i11-fixed", GridFixedResizeUI.class));
        contexts.addHandler(context("/i29", WindowCurtainUI.class));
        contexts.addHandler(context("/i41-change", UploadChangeUI.class));
        ServletContextHandler noSelect = context("/i41-noselect",
                UploadNoSelectionUI.class);
        noSelect.addServlet(new ServletHolder("diag", new Diag.Servlet()),
                "/diag");
        contexts.addHandler(noSelect);
        ServletContextHandler expiry = context("/i56", SessionExpiryUI.class);
        expiry.getServletHandler().getServlet("SessionExpiryUI")
                .setInitParameter("heartbeatInterval", "4");
        contexts.addHandler(expiry);
        server.setHandler(contexts);
        server.setStopAtShutdown(true);
        server.start();
        return server;
    }

    private static ServletContextHandler context(String path,
            Class<? extends UI> ui) {
        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath(path);
        ServletHolder holder = new ServletHolder(ui.getSimpleName(),
                new VaadinServlet());
        holder.setInitParameter("UI", ui.getName());
        holder.setInitParameter("widgetset", "com.vaadin.DefaultWidgetSet");
        holder.setInitParameter("productionMode", "false");
        holder.setAsyncSupported(true);
        // Servlet 3.0 async long polling, as on the console's servlet
        // container, instead of Atmosphere's Jetty websocket detection
        holder.setInitParameter("org.atmosphere.cpr.asyncSupport",
                "org.atmosphere.container.Servlet30CometSupport");
        context.addServlet(holder, "/*");
        return context;
    }
}
