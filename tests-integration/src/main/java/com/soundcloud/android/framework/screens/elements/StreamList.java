package com.soundcloud.android.framework.screens.elements;

import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import android.widget.RelativeLayout;

import java.util.List;

public class StreamList {
    private ViewElement root;

    public StreamList(ViewElement element) {
        root = element;
    }

    public ViewElement getItemAt(int index) {
        return listItems().get(index);
    }

    public int getItemCount() {
        return listItems().size();
    }

    private List<ViewElement> listItems() {
        return root.findElements(With.className(RelativeLayout.class));
    }
}
