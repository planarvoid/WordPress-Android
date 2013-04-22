package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.Button;

public class CustomButton extends Button {
    public CustomButton(Context context) {
        super(context);
    }

    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        applyCustomFont(context, attrs);
        applyHtmlText(context, attrs);
    }

    public CustomButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        applyCustomFont(context, attrs);
        applyHtmlText(context, attrs);
    }

    private void applyCustomFont(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CustomFontTextView);
        String path = array.getString(R.styleable.CustomFontTextView_custom_font);

        if (path != null) {
            setFontFromPath(path);
        }

        array.recycle();
    }

    private void applyHtmlText(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CustomButton);
        String html = array.getString(R.styleable.CustomButton_html_text);

        if (html != null) {
            setText(Html.fromHtml(html), BufferType.SPANNABLE);
        }

        array.recycle();
    }

    private void setFontFromPath(String path) {
        Typeface typeface = CustomFontLoader.getFont(getContext(), path);
        setTypeface(typeface);
    }
}
