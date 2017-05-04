package com.soundcloud.android.framework.viewelements;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

public class ImageViewElement {
    protected final ViewElement viewElement;
    protected final View view;

    public ImageViewElement(ViewElement element) {
        this.viewElement = element;
        this.view = element.getView();
    }

    public boolean hasDrawable(int drawableId) {
        if (view instanceof ImageView) {
            Drawable expected = view.getContext().getDrawable(drawableId);
            return ((ImageView) view).getDrawable().getConstantState().equals(expected.getConstantState());
        }
        throw new UnsupportedOperationException("View is not an image element: " + view);
    }
}
