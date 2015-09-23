package com.soundcloud.android.view;

import com.soundcloud.java.strings.Strings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.widget.ToggleButton;


public class CustomToggleButton extends ToggleButton {

    private class CustomToggleButtonDrawable extends StateListDrawable {
        private final Drawable background;

        public CustomToggleButtonDrawable(Drawable background) {
            this.background = background;
        }

        @Override
        public void draw(Canvas canvas) {
            final String   text    = isChecked() ? Strings.safeToString(getTextOn()) : Strings.safeToString(getTextOff());
            final Paint    paint   = getPaint();
            final Drawable icon    = getCompoundDrawables()[0];
            final int      padding = getCompoundDrawablePadding();

            final int width  = CustomToggleButton.this.getWidth();
            final int height = CustomToggleButton.this.getHeight();

            final int textWidth = (int) paint.measureText(text);

            if (background != null) {
                background.setBounds(getBounds());
                background.draw(canvas);
            }

            int requiredWidth = 0;
            requiredWidth += textWidth;
            requiredWidth += icon != null ? icon.getIntrinsicWidth() : 0;

            if (icon != null && textWidth > 0) {
                requiredWidth += padding;
            }

            int x = (width - requiredWidth) / 2;
            if (icon != null) {
                int iconY = (height - icon.getIntrinsicHeight()) / 2;

                icon.setBounds(x, iconY, x + icon.getIntrinsicWidth(), iconY + icon.getIntrinsicHeight());
                icon.draw(canvas);

                x += icon.getIntrinsicWidth() + padding;
            }


            int textSize = (int) (paint.getTextSize() - paint.getFontMetrics().bottom);
            int textY = height - (height - textSize) / 2;
            canvas.drawText(text, x, textY, paint);
        }

        @Override
        public void setAlpha(int i) { }

        @Override
        public void setColorFilter(ColorFilter colorFilter) { }

        @Override
        public int getOpacity() { return 1; }

        @Override
        public boolean setState(int[] stateSet) {
            final boolean ret = background.setState(stateSet);
            invalidate();
            return ret;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public CustomToggleButton(Context context) {
        super(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    public CustomToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("UnusedDeclaration")
    public CustomToggleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    {
        setBackgroundDrawable(new CustomToggleButtonDrawable(getBackground()));
    }


    @Override
    protected void onDraw(Canvas canvas) {
        // this line intentionally left blank

        /*
         * For some reason, custom drawing operations in this onDraw method would not show up.
         * I assume this is because of some underlying optimizations of TextView that violate
         * the contract of onDraw. To work around this without having to write my own toggle
         * button, the actual drawing is done in the CustomToggleButtonDrawable set as this
         * button's background.
         */
    }

}
