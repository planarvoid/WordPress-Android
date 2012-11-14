package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Typeface;

import java.lang.ref.SoftReference;
import java.util.Hashtable;

public class CustomFontLoader {
    private static final Hashtable<String, SoftReference<Typeface>> fontCache;

    static {
        fontCache = new Hashtable<String, SoftReference<Typeface>>();
    }

    public static Typeface getFont(Context context, String path) {
        synchronized (fontCache) {
            SoftReference<Typeface> reference = fontCache.get(path);
            Typeface typeface = reference != null ? reference.get() : null;

            if (typeface == null) {
                typeface = Typeface.createFromAsset(context.getAssets(), path);

                fontCache.put(path, new SoftReference<Typeface>(typeface));
            }

            return typeface;
        }
    }

}
