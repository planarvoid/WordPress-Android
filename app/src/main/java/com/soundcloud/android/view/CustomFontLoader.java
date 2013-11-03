package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

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

    public static void applyCustomFont(Context context, TextView textView, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CustomFontTextView);
        String path = array.getString(R.styleable.CustomFontTextView_custom_font);

        if (path != null && Consts.SdkSwitches.useCustomFonts) {
            Typeface typeface = CustomFontLoader.getFont(context, path);
            textView.setTypeface(typeface);
        }

        array.recycle();
    }
}
