package com.soundcloud.android.view;

import static com.soundcloud.android.view.CustomFontLoader.SOUNDCLOUD_INTERSTATE_REGULAR;
import static com.soundcloud.android.view.CustomFontLoader.applyCustomFont;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class CustomFontTabLayout extends TabLayout {
    public CustomFontTabLayout(Context context) {
        super(context);
    }

    public CustomFontTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomFontTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addTab(Tab tab) {
        super.addTab(tab);

        ViewGroup mainView = (ViewGroup) getChildAt(0);
        ViewGroup tabView = (ViewGroup) mainView.getChildAt(tab.getPosition());

        int tabChildCount = tabView.getChildCount();
        for (int i = 0; i < tabChildCount; i++) {
            View tabViewChild = tabView.getChildAt(i);
            if (tabViewChild instanceof TextView) {
                applyCustomFont(getContext(), (TextView) tabViewChild, SOUNDCLOUD_INTERSTATE_REGULAR);
            }
        }
    }
}
