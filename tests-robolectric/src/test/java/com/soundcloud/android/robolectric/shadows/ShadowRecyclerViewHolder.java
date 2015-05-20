package com.soundcloud.android.robolectric.shadows;

import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.internal.RealObject;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.lang.reflect.Field;

@Implements(RecyclerView.ViewHolder.class)
public class ShadowRecyclerViewHolder {

    @RealObject RecyclerView.ViewHolder delegate;

    public void __constructor__(View itemView) throws NoSuchFieldException, IllegalAccessException {
        final Field itemViewField = delegate.getClass().getField("itemView");
        itemViewField.setAccessible(true);
        itemViewField.set(delegate, itemView);
    }
}
