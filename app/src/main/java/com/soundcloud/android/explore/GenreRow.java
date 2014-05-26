package com.soundcloud.android.explore;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

class GenreRow extends LinearLayout {

    private TextView categoryTitle, sectionHeader;

    @SuppressWarnings("unused")
    public GenreRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        categoryTitle = (TextView) findViewById(android.R.id.text1);
        sectionHeader = (TextView) findViewById(R.id.list_section_header);
    }

    public void showSectionHeaderWithText(String text) {
        sectionHeader.setText(text.toUpperCase(Locale.getDefault()));
        sectionHeader.setVisibility(View.VISIBLE);
    }

    public void hideSectionHeader() {
        sectionHeader.setVisibility(View.GONE);
    }

    public void setDisplayName(String name) {
        categoryTitle.setText(name);
    }

}
