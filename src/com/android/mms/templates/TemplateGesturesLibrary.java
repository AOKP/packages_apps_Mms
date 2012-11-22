
package com.android.mms.templates;

import java.io.File;

import android.content.Context;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;

public class TemplateGesturesLibrary {

    private static GestureLibrary sStore;

    public static GestureLibrary getStore(Context c) {

        if (sStore == null) {
            File storeFile = new File(c.getFilesDir(), "sms_templates_gestures");
            sStore = GestureLibraries.fromFile(storeFile);
            sStore.load();
        }

        return sStore;
    }

}
