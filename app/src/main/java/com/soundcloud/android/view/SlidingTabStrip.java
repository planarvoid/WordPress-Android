/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Based on: http://developer.android.com/samples/SlidingTabsBasic/project.html
 */

package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

class SlidingTabStrip extends LinearLayout {

    private static final int DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS = 1;
    private static final byte DEFAULT_BOTTOM_BORDER_COLOR_ALPHA = 0x26;
    private static final int SELECTED_INDICATOR_THICKNESS_DIPS = 5;
    private static final int DEFAULT_SELECTED_INDICATOR_COLOR = 0xFFFF5500;

    private static final int DEFAULT_DIVIDER_THICKNESS_DIPS = 1;
    private static final int DEFAULT_DIVIDER_COLOR = 0xFFD8D8D8;
    private static final float DEFAULT_DIVIDER_HEIGHT = 0.5f;

    private final int bottomBorderThickness;
    private final Paint bottomBorderPaint;

    private final int selectedIndicatorThickness;
    private final Paint selectedIndicatorPaint;

    private final Paint dividerPaint;
    private final float dividerHeight;

    private int selectedPosition;
    private float selectionOffset;

    SlidingTabStrip(Context context) {
        this(context, null);
    }

    SlidingTabStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);

        final float density = getResources().getDisplayMetrics().density;

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorForeground, outValue, true);
        final int themeForegroundColor = outValue.data;

        int defaultBottomBorderColor = setColorAlpha(themeForegroundColor, DEFAULT_BOTTOM_BORDER_COLOR_ALPHA);

        bottomBorderThickness = (int) (DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS * density);
        bottomBorderPaint = new Paint();
        bottomBorderPaint.setColor(defaultBottomBorderColor);

        selectedIndicatorThickness = (int) (SELECTED_INDICATOR_THICKNESS_DIPS * density);
        selectedIndicatorPaint = new Paint();
        selectedIndicatorPaint.setColor(DEFAULT_SELECTED_INDICATOR_COLOR);

        dividerHeight = DEFAULT_DIVIDER_HEIGHT;
        dividerPaint = new Paint();
        dividerPaint.setColor(DEFAULT_DIVIDER_COLOR);
        dividerPaint.setStrokeWidth((int) (DEFAULT_DIVIDER_THICKNESS_DIPS * density));
    }

    void onViewPagerPageChanged(int position, float positionOffset) {
        selectedPosition = position;
        selectionOffset = positionOffset;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int height = getHeight();
        final int childCount = getChildCount();
        final int dividerHeightPx = (int) (Math.min(Math.max(0f, dividerHeight), 1f) * height);

        // Thick colored underline below the current selection
        if (childCount > 0) {
            View selectedTitle = getChildAt(selectedPosition);
            int left = selectedTitle.getLeft();
            int right = selectedTitle.getRight();

            if (selectionOffset > 0f && selectedPosition < (getChildCount() - 1)) {

                // Draw the selection partway between the tabs
                View nextTitle = getChildAt(selectedPosition + 1);
                left = (int) (selectionOffset * nextTitle.getLeft() +
                        (1.0f - selectionOffset) * left);
                right = (int) (selectionOffset * nextTitle.getRight() +
                        (1.0f - selectionOffset) * right);
            }

            canvas.drawRect(left, height - selectedIndicatorThickness, right,
                    height, selectedIndicatorPaint);
        }

        // Thin underline along the entire bottom edge
        canvas.drawRect(0, height - bottomBorderThickness, getWidth(), height, bottomBorderPaint);

        // Vertical separators between the titles
        int separatorTop = (height - dividerHeightPx) / 2;
        for (int i = 0; i < childCount - 1; i++) {
            View child = getChildAt(i);
            canvas.drawLine(child.getRight(), separatorTop, child.getRight(),
                    separatorTop + dividerHeightPx, dividerPaint);
        }
    }

    /**
     * Set the alpha value of the {@code color} to be the given {@code alpha} value.
     */
    private static int setColorAlpha(int color, byte alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

}