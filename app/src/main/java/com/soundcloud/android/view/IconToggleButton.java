package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ToggleButton;


public class IconToggleButton extends ToggleButton {

    private Drawable icon;

    @SuppressWarnings("UnusedDeclaration")
    public IconToggleButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("UnusedDeclaration")
    public IconToggleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IconToggleButton, defStyle, 0);
        icon = a.getDrawable(R.styleable.IconToggleButton_itb_icon);
        a.recycle();
    }

    private Drawable getSelectableItemBackgroundDrawable() {
        int[] attrs = new int[]{R.attr.selectableItemBackground};
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs);
        Drawable d = a.getDrawable(0);
        a.recycle();
        return d;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (icon != null) {
            setBackgroundDrawable(createSizeAjustedCustomBackground(w, h));
        }

    }

    @NonNull
    private LayerDrawable createSizeAjustedCustomBackground(int w, int h) {
        // you cannot refer to selectableItemBackground in layer-list via xml, so here we go...
        final LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{
                icon,
                getSelectableItemBackgroundDrawable()});
        layerDrawable.setBounds(0,0,w,h);
        return layerDrawable;
    }
}
