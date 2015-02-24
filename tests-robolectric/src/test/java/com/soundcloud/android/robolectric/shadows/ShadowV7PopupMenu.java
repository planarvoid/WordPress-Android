package com.soundcloud.android.robolectric.shadows;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.tester.android.view.TestMenu;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;

@Implements(PopupMenu.class)
public class ShadowV7PopupMenu {

    private TestMenu menu = new TestMenu(Robolectric.application);

    @Implementation
    public void inflate(int id) {
        Context context = Robolectric.application;
        shadowOf(context).getResourceLoader().inflateMenu(context, id, menu);
    }

    @Implementation
    public void show() {}

    @Implementation
    public Menu getMenu() {
        return menu;
    }

}
