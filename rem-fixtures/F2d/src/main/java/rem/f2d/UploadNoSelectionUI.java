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

import java.io.ByteArrayOutputStream;

import com.vaadin.ui.Button;
import com.vaadin.ui.Upload;

/**
 * I41 (issue #10419): a non-immediate Upload must not upload anything when no
 * file has been selected, neither from its button nor from
 * Upload.submitUpload().
 */
public class UploadNoSelectionUI extends LogUI {

    @Override
    protected void setup() {
        final Upload upload = new Upload("upload", (filename, mime) -> {
            log("receiving '" + filename + "'");
            Diag.event("i41 receiving '" + filename + "'");
            return new ByteArrayOutputStream();
        });
        upload.setId("upload");
        upload.setImmediate(false);
        upload.setButtonCaption("Upload");
        upload.addFinishedListener(e -> {
            log("finished '" + e.getFilename() + "' " + e.getLength());
            Diag.event("i41 finished '" + e.getFilename() + "' " + e.getLength());
        });
        Button programmatic = new Button("programmatic",
                e -> upload.submitUpload());
        programmatic.setId("programmatic");
        layout.addComponents(upload, programmatic);
    }
}
