package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.util.Hashtable;

public class CustomFontLoader {
    private static final Hashtable<String, SoftReference<Typeface>> fontCache;

    static {
        fontCache = new Hashtable<String, SoftReference<Typeface>>();
    }

    @Nullable
    public static Typeface getFont(Context context, String path) {
        synchronized (fontCache) {
            SoftReference<Typeface> reference = fontCache.get(path);
            Typeface typeface = reference != null ? reference.get() : null;

            if (typeface == null) {
                try {
                    typeface = Typeface.createFromAsset(context.getAssets(), path);
                    fontCache.put(path, new SoftReference<Typeface>(typeface));
                } catch (RuntimeException e) {
                    Log.e(SoundCloudApplication.TAG, "Encountered exception loading the font", e);
                }
            }

            return typeface;
        }
    }

}
