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

import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;

import com.vaadin.server.BootstrapFragmentResponse;
import com.vaadin.server.BootstrapListener;
import com.vaadin.server.BootstrapPageResponse;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinServlet;

/** Adds the I55 style sheet to the bootstrap page. */
public class ResponsiveServlet extends VaadinServlet {

    static final String CSS = "@font-face { font-family: f2cfont; src: local(\"Arial\"); }\n"
            + "@layer f2cbase { .f2c-layer { color: blue; } }\n"
            + "@container (min-width: 10px) { .f2c-container { color: green; } }\n"
            + ".f2c-resp[width-range~=\"0-300px\"] { color: rgb(255, 0, 0); }\n"
            + ".f2c-resp[width-range~=\"301px-\"] { color: rgb(0, 0, 0); }\n";

    @Override
    protected void servletInitialized() throws javax.servlet.ServletException {
        super.servletInitialized();
        getService().addSessionInitListener(new SessionInitListener() {
            @Override
            public void sessionInit(SessionInitEvent event)
                    throws ServiceException {
                event.getSession().addBootstrapListener(new BootstrapListener() {
                    @Override
                    public void modifyBootstrapFragment(
                            BootstrapFragmentResponse response) {
                        // nothing
                    }

                    @Override
                    public void modifyBootstrapPage(
                            BootstrapPageResponse response) {
                        Element style = response.getDocument().head()
                                .appendElement("style");
                        style.attr("id", "f2c-i55");
                        style.appendChild(new DataNode(CSS, ""));
                    }
                });
            }
        });
    }
}
