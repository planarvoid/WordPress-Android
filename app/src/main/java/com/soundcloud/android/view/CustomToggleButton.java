package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.ToggleButton;
import com.soundcloud.android.R;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CustomToggleButton extends ToggleButton {
    private class CustomToggleButtonDrawable extends StateListDrawable {
        @Nullable private Drawable  mBackground;

        public CustomToggleButtonDrawable(Drawable background) {
            mBackground = background;
        }

        @Override
        public void draw(Canvas canvas) {
            final String   text    = isChecked() ? getTextOn().toString() : getTextOff().toString();
            final Paint    paint   = getPaint();
            final Drawable icon    = getCompoundDrawables()[0];
            final int      padding = getCompoundDrawablePadding();

            final int width  = CustomToggleButton.this.getWidth();
            final int height = CustomToggleButton.this.getHeight();

            if (mBackground != null) {
                mBackground.setBounds(getBounds());
                mBackground.draw(canvas);
            }

            int requiredWidth = 0;
            requiredWidth += paint.measureText(text);
            requiredWidth += icon != null ? icon.getIntrinsicWidth() + padding : 0;

            int x = (width - requiredWidth) / 2;
            if (icon != null) {
                int iconY = (height - icon.getIntrinsicHeight()) / 2;

                icon.setBounds(x, iconY, x + icon.getIntrinsicWidth(), iconY + icon.getIntrinsicHeight());
                icon.draw(canvas);

                x += icon.getIntrinsicWidth() + padding;
            }


            int textSize = (int) (paint.getTextSize() - paint.getFontMetrics().bottom);
            int textY = (int) (height - (height - textSize) / 2);
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
            return mBackground.setState(stateSet);
        }
    }

    public CustomToggleButton(Context context) {
        super(context);
    }

    public CustomToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomToggleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    {
        setBackground(new CustomToggleButtonDrawable(getBackground()));
    }


    @Override
    protected void onDraw(Canvas canvas) {

    }

}
