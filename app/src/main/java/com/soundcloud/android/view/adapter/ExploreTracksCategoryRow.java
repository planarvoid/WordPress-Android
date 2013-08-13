package com.soundcloud.android.view.adapter;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExploreTracksCategoryRow extends LinearLayout {

    public TextView categoryTitle, sectionHeader;

    @SuppressWarnings("unused")
    public ExploreTracksCategoryRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("unused")
    public ExploreTracksCategoryRow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        categoryTitle = (TextView) findViewById(android.R.id.text1);
        sectionHeader = (TextView) findViewById(R.id.list_section_header);
    }

    public void setDisplayName(String name) {
        categoryTitle.setText(name);
    }

    public void showSectionHeader(String text) {
        sectionHeader.setText(text);
        sectionHeader.setVisibility(View.VISIBLE);
    }

    public void hideSectionHeader() {
        sectionHeader.setVisibility(View.GONE);
    }

}
