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

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

/** Same UI for the bootstrap page comparison of 7.7.17.1 and the candidate. */
public class BootUI extends UI {
    @Override
    protected void init(VaadinRequest request) {
        Label label = new Label("bootstrap fixture");
        label.setId("boot");
        setContent(label);
    }
}
