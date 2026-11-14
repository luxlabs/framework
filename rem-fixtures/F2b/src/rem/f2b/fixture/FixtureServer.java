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
package rem.f2b.fixture;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.vaadin.server.VaadinServlet;

/**
 * Jetty with the fixture UIs. Mappings: /actions/* (ActionsUI), /grid/*
 * (GridUI), /app/* and /* (BootUI), /VAADIN/* (static resources).
 * <p>
 * Usage: FixtureServer &lt;port&gt; listens only on 127.0.0.1 and the given
 * port; {@link #local()} gives a server without sockets.
 */
public class FixtureServer {

    static ServletContextHandler context() {
        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        add(context, "/VAADIN/*", BootUI.class);
        add(context, "/actions/*", ActionsUI.class);
        add(context, "/grid/*", GridUI.class);
        add(context, "/app/*", BootUI.class);
        add(context, "/*", BootUI.class);
        return context;
    }

    private static void add(ServletContextHandler context, String mapping,
            Class<?> ui) {
        ServletHolder holder = new ServletHolder(ui.getSimpleName() + mapping,
                new VaadinServlet());
        holder.setInitParameter("UI", ui.getName());
        holder.setInitParameter("productionMode", "true");
        context.addServlet(holder, mapping);
    }

    public static Object[] local() throws Exception {
        Server server = new Server();
        LocalConnector connector = new LocalConnector(server);
        server.setConnectors(new Connector[] { connector });
        server.setHandler(context());
        server.start();
        return new Object[] { server, connector };
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        if (port < 18413 || port > 18415) {
            throw new IllegalArgumentException("port outside 18413-18415");
        }
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(port);
        server.setConnectors(new Connector[] { connector });
        server.setHandler(context());
        server.setStopAtShutdown(true);
        server.start();
        System.out.println("FIXTURE READY 127.0.0.1:" + port);
        server.join();
    }
}
