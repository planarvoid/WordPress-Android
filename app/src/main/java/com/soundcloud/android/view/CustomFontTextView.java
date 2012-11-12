package com.soundcloud.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;
import com.soundcloud.android.R;

import java.lang.ref.SoftReference;
import java.util.Hashtable;

public class CustomFontTextView extends TextView {
    private static final Hashtable<String, SoftReference<Typeface>> fontCache;

    static {
        fontCache = new Hashtable<String, SoftReference<Typeface>>();
    }

    private static Typeface getFont(Context context, String path) {
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

    public CustomFontTextView(Context context) {
        super(context);
    }

    public CustomFontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        applyCustomFont(context, attrs);
    }

    public CustomFontTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        applyCustomFont(context, attrs);
    }

    private void applyCustomFont(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CustomFontTextView);
        String path = array.getString(R.styleable.CustomFontTextView_custom_font);

        setFontFromPath(path);

        array.recycle();
    }

    private void setFontFromPath(String path) {
        Typeface typeface = getFont(getContext(), path);

        setTypeface(typeface);
    }
}
