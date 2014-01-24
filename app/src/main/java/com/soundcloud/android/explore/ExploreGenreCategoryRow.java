package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.collections.SectionedListRow;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class ExploreGenreCategoryRow extends LinearLayout implements SectionedListRow {

    private TextView categoryTitle, sectionHeader;

    @SuppressWarnings("unused")
    public ExploreGenreCategoryRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        categoryTitle = (TextView) findViewById(android.R.id.text1);
        sectionHeader = (TextView) findViewById(R.id.list_section_header);
    }

    @Override
    public void showSectionHeaderWithText(String text) {
        sectionHeader.setText(text.toUpperCase(Locale.getDefault()));
        sectionHeader.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideSectionHeader() {
        sectionHeader.setVisibility(View.GONE);
    }

    public void setDisplayName(String name) {
        categoryTitle.setText(name);
    }

}
