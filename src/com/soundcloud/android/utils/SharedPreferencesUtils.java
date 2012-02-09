package com.soundcloud.android.utils;

import android.content.SharedPreferences;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SharedPreferencesUtils {
    private static final Method sApplyMethod = findApplyMethod();
    private static Method findApplyMethod() {
        try {
            return SharedPreferences.Editor.class.getMethod("apply");
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    /**
     * Applies or commits the current editor.
     * Apply (async commit) is only available in SDK >= 9.
     * @param editor the editor to apply
     * @return if applying, true, otherwise return value of commit()
     */
    public static boolean apply(SharedPreferences.Editor editor) {
        if (sApplyMethod != null) {
            try {
                sApplyMethod.invoke(editor);
                return true;
            } catch (InvocationTargetException ignore) {
                // fall through
            } catch (IllegalAccessException ignore) {
                // fall through
            }
        }
        return editor.commit();
    }
}
