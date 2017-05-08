package com.soundcloud.android.utils;

import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class PermissionsHelper {

    public static void grantPermission(String permission) {
        grantPermissions(new String[]{permission});
    }

    public static void grantPermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                ParcelFileDescriptor parcelFileDescriptor = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                        "pm grant " + InstrumentationRegistry.getTargetContext().getPackageName()
                                + " " + permission);

                try {
                    // Synchronize with execution of command by reading the whole stream
                    new InputStreamReader(new FileInputStream(parcelFileDescriptor.getFileDescriptor())).read();
                } catch (IOException e) {
                    Log.e("PERMISSION:", "Failed to grant " + permission, e);
                }
            }
        }
    }

    public static void revokePermission(String permission) {
        revokePermissions(new String[]{permission});
    }

    public static void revokePermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                        "pm revoke " + InstrumentationRegistry.getTargetContext().getPackageName()
                                + " " + permission);
            }
        }
    }
}
