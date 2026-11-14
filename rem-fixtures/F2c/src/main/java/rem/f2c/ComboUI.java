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

import com.vaadin.annotations.Theme;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.AbstractSelect.NewItemHandler;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PopupDateField;
import com.vaadin.ui.UI;

/**
 * ComboBox scenarios: I03 (autocomplete of ComboBox and DateField), I44 and I40 (popup opened to the
 * left at the right edge), I14 (fast typing and tab, reproduction), I42
 * (popup position when the page scrolls, reproduction).
 */
@Theme("valo")
public class ComboUI extends UI {

    private Log log;

    @Override
    protected void init(VaadinRequest request) {
        CssLayout root = Log.root(this);
        log = new Log(root);

        root.addComponent(combo("combo-auto", false));
        PopupDateField dateAuto = new PopupDateField();
        dateAuto.setId("date-auto");
        root.addComponent(dateAuto);

        AbsoluteLayout edge = new AbsoluteLayout();
        edge.setId("edge");
        edge.setWidth("100%");
        edge.setHeight("60px");
        ComboBox right = new ComboBox();
        right.setId("combo-right");
        right.setWidth("120px");
        right.setImmediate(true);
        for (int i = 1; i <= 30; i++) {
            right.addItem(String.format(
                    "Very long item caption number %02d for a wide popup", i));
        }
        edge.addComponent(right, "right: 0px; top: 10px;");
        root.addComponent(edge);

        root.addComponent(combo("combo-sel", false));
        root.addComponent(combo("combo-new", true));

        Label spacer = new Label("spacer");
        spacer.setHeight("200px");
        root.addComponent(spacer);
        root.addComponent(combo("combo-scroll", false));
        Label tail = new Label("tail");
        tail.setHeight("3000px");
        root.addComponent(tail);
        log.add("ready");
    }

    private ComboBox combo(final String id, boolean newItems) {
        final ComboBox combo = new ComboBox();
        combo.setId(id);
        combo.setImmediate(true);
        for (String item : new String[] { "Alpha", "Beta", "Gamma", "Delta",
                "Epsilon", "Zeta", "Eta", "Theta" }) {
            combo.addItem(item);
        }
        if (newItems) {
            combo.setNewItemsAllowed(true);
            combo.setNewItemHandler(new NewItemHandler() {
                @Override
                public void addNewItem(String newItemCaption) {
                    combo.addItem(newItemCaption);
                    combo.setValue(newItemCaption);
                    log.add(id + "-new=" + newItemCaption);
                }
            });
        }
        combo.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                log.add(id + "=" + combo.getValue());
            }
        });
        return combo;
    }
}
