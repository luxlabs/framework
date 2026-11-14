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
/*
 * REM F2c, item I14 (finding R1 of the F2c verification): leaving an emptied
 * nullable ComboBox must still clear its selection (selected=[]), as in
 * 7.7.17, also while a filtering response is pending. Probe written by the
 * F2c verifier, kept unchanged below this comment.
 */
package com.vaadin.client.gwttest.client;
import java.util.Collections;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ui.VFilterSelect;
public class ComboClearPendingGwtTest extends GWTTestCase {
    public String getModuleName(){return "com.vaadin.client.gwttest.F2cGwtTestsWidgetSet";}
    static class RecordingConnection extends ApplicationConnection {
        boolean cleared;
        @Override public boolean hasEventListeners(Widget widget,String event){return false;}
        @Override public void updateVariable(String id,String name,String value,boolean immediate){}
        @Override public void updateVariable(String id,String name,int value,boolean immediate){}
        @Override public void updateVariable(String id,String name,String[] value,boolean immediate){if(name.equals("selected")&&value.length==0)cleared=true;}
    }
    private void check(boolean pending){
        RecordingConnection connection=new RecordingConnection();VFilterSelect combo=new VFilterSelect();
        combo.client=connection;combo.paintableId="combo";combo.allowNewItem=true;combo.nullSelectionAllowed=true;combo.selectedOptionKey="old-selection";combo.waitingForFilteringResponse=pending;combo.prompting=false;combo.tb.setText("");
        combo.onBlur(null);
        // Allow the implementation to defer selection until the response,
        // but clearing must still happen after the pending response arrives.
        if(pending&&!connection.cleared)combo.suggestionPopup.showSuggestions(Collections.<VFilterSelect.FilterSelectSuggestion>emptyList(),0,0);
        assertTrue("Clearing a selected nullable ComboBox must send an empty selection; pending="+pending,connection.cleared);
    }
    public void testClearWithoutPendingFiltering(){check(false);}
    public void testClearWithPendingFiltering(){check(true);}
}
