package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import java.lang.ref.SoftReference;
import java.util.Hashtable;
import java.util.Map;

public final class CustomFontLoader {
    public static final String SOUNDCLOUD_INTERSTATE_LIGHT = "fonts/InterstateSound_Pnum-Light_fbsTfohiYZ.ttf";
    public static final String SOUNDCLOUD_INTERSTATE_LIGHT_TNUM = "fonts/InterstateSound_Tnum-Light_fbdOS9y3d6.ttf";
    public static final String SOUNDCLOUD_INTERSTATE_REGULAR = "fonts/InterstateSound_Pnum-Regular_fbFICLP0jJ.ttf";

    private static final Map<String, SoftReference<Typeface>> fontCache;

    static {
        fontCache = new Hashtable<>();
    }

    private CustomFontLoader() {
        // not to be constructed.
    }

    @Nullable
    public static Typeface getFont(Context context, String path) {
        synchronized (fontCache) {
            SoftReference<Typeface> reference = fontCache.get(path);
            Typeface typeface = reference != null ? reference.get() : null;

            if (typeface == null) {
                try {
                    typeface = Typeface.createFromAsset(context.getAssets(), path);
                    fontCache.put(path, new SoftReference<>(typeface));
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

        if (path != null) {
            Typeface typeface = CustomFontLoader.getFont(context, path);
            applyFontToView(textView, typeface);
        }

        array.recycle();
    }

    public static void applyCustomFont(Context context, TextView textView, String path) {
        Typeface typeface = CustomFontLoader.getFont(context, path);
        applyFontToView(textView, typeface);
    }

    private static void applyFontToView(TextView textView, Typeface typeface) {
        textView.setTypeface(typeface);
        textView.setPaintFlags(textView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
    }
}
