package com.soundcloud.android.presentation;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * RecyclerView does not support dividers out of the box.
 */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    private Drawable divider;
    private int thickness;

    public DividerItemDecoration(Drawable divider, int thickness) {
        this.divider = divider;
        this.thickness = thickness;
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        if (divider == null) {
            super.onDraw(canvas, parent, state);
            return;
        }

        if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
            drawVertical(canvas, parent);
        } else {
            drawHorizontal(canvas, parent);
        }
    }

    private void drawVertical(Canvas canvas, RecyclerView parent) {
        final int left = parent.getPaddingLeft();
        final int right = parent.getWidth() - parent.getPaddingRight();
        final int childCount = parent.getChildCount();

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final int translationY = (int) child.getTranslationY();
            if (shouldRenderDivider(i, translationY != 0)) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
                final int size = thickness;
                final int top = child.getTop() + translationY - params.topMargin;
                final int bottom = top + size;
                divider.setBounds(left, top, right, bottom);
                divider.draw(canvas);
            }
        }
    }

    private boolean shouldRenderDivider(int index, boolean isAnimating) {
        return index > 0 || isAnimating;
    }

    private void drawHorizontal(Canvas c, RecyclerView parent) {
        final int top = parent.getPaddingTop();
        final int bottom = parent.getHeight() - parent.getPaddingBottom();
        final int childCount = parent.getChildCount();

        for (int i = 1; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            final int size = thickness;
            final int left = child.getLeft() - params.leftMargin;
            final int right = left + size;
            divider.setBounds(left, top, right, bottom);
            divider.draw(c);
        }
    }

    private int getOrientation(RecyclerView parent) {
        if (parent.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) parent.getLayoutManager();
            return layoutManager.getOrientation();
        } else {
            throw new IllegalStateException("DividerItemDecoration can only be used with a LinearLayoutManager.");
        }
    }

}