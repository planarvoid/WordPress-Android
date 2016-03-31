package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.DefaultViewElement;

import android.view.View;

public abstract class AdapterElement extends DefaultViewElement {

    public AdapterElement(View view, Han driver) {
        super(view, driver);
    }

    public abstract int getItemCount();

    public abstract AdapterElement scrollToBottom();
}
