/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Based on: http://developer.android.com/samples/SlidingTabsBasic/project.html
 *
 * Modified to provide fixed width tabs, set defaults and remove unnecessary features.
 */

package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import java.util.Locale;

/**
 * To be used with ViewPager to provide a tab indicator component which give constant feedback as to
 * the user's scroll progress.
 * <p>
 * To use the component, simply add it to your view hierarchy. Then in your
 * {@link android.app.Activity} or {@link android.support.v4.app.Fragment} call
 * {@link #setViewPager(ViewPager)} providing it the ViewPager this layout is being used for.
 * <p>
 */
public class SlidingTabLayout extends HorizontalScrollView {

    private static final int TITLE_OFFSET_DP = 100;
    private static final int LAYOUT_BACKGROUND = 0xFFFFFFFF;

    private static final int TAB_PADDING_VERTICAL_DP = 15;
    private static final int TAB_PADDING_HORIZONTAL_DP = 30;
    private static final int TAB_BACKGROUND = R.drawable.ak_list_selector_gray;
    private static final int TAB_TEXT_SIZE = R.dimen.tab_text_size;
    private static final int TAB_TEXT_COLOR = 0xFF666666;

    private int titleOffset;
    private int tabWidth;
    private boolean fixedTabs;

    private ViewPager viewPager;
    private ViewPager.OnPageChangeListener viewPagerPageChangeListener;

    private final SlidingTabStrip tabStrip;

    public SlidingTabLayout(Context context) {
        this(context, null);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setHorizontalScrollBarEnabled(false);
        setFillViewport(true);
        setBackgroundColor(LAYOUT_BACKGROUND);
        titleOffset = (int) (TITLE_OFFSET_DP * getResources().getDisplayMetrics().density);

        initAttributes(context, attrs);

        tabStrip = new SlidingTabStrip(context);
        addView(tabStrip, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlidingTabLayout);
        fixedTabs = a.getBoolean(R.styleable.SlidingTabLayout_fixedTabs, false);
        a.recycle();
    }

    /**
     * Set the {@link ViewPager.OnPageChangeListener}. When using {@link SlidingTabLayout} you are
     * required to set any {@link ViewPager.OnPageChangeListener} through this method. This is so
     * that the layout can update it's scroll position correctly.
     *
     * @see ViewPager#setOnPageChangeListener(ViewPager.OnPageChangeListener)
     */
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        viewPagerPageChangeListener = listener;
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content
     * (number of tabs and tab titles) does not change after this call has been made.
     */
    public void setViewPager(ViewPager viewPager) {
        tabStrip.removeAllViews();

        this.viewPager = viewPager;
        if (viewPager != null) {
            viewPager.setOnPageChangeListener(new InternalViewPagerListener());
            populateTabStrip();
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (fixedTabs) {
            tabWidth = MeasureSpec.getSize(widthMeasureSpec) / tabStrip.getChildCount();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Create a default view to be used for tabs.
     */
    protected TextView createDefaultTabView(Context context) {
        TextView textView = new TextView(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int tabWidth = fixedTabs
                        ? MeasureSpec.makeMeasureSpec(SlidingTabLayout.this.tabWidth, MeasureSpec.EXACTLY)
                        : widthMeasureSpec;
                super.onMeasure(tabWidth, heightMeasureSpec);
            }
        };
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundResource(TAB_BACKGROUND);
        textView.setTextColor(TAB_TEXT_COLOR);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(TAB_TEXT_SIZE));

        int vertical = (int) (TAB_PADDING_VERTICAL_DP * getResources().getDisplayMetrics().density);
        int horizontal = fixedTabs ? 0 : (int) (TAB_PADDING_HORIZONTAL_DP * getResources().getDisplayMetrics().density);
        textView.setPadding(horizontal, vertical, horizontal, vertical);

        return textView;
    }

    private void populateTabStrip() {
        final PagerAdapter adapter = viewPager.getAdapter();
        final View.OnClickListener tabClickListener = new TabClickListener();

        for (int i = 0; i < adapter.getCount(); i++) {
            TextView tabView = createDefaultTabView(getContext());
            String title = adapter.getPageTitle(i).toString();
            tabView.setText(title.toUpperCase(Locale.US));
            tabView.setOnClickListener(tabClickListener);
            tabStrip.addView(tabView);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (viewPager != null) {
            scrollToTab(viewPager.getCurrentItem(), 0);
        }
    }

    private void scrollToTab(int tabIndex, int positionOffset) {
        final int tabStripChildCount = tabStrip.getChildCount();
        if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
            return;
        }

        View selectedChild = tabStrip.getChildAt(tabIndex);
        if (selectedChild != null) {
            int targetScrollX = selectedChild.getLeft() + positionOffset;

            if (tabIndex > 0 || positionOffset > 0) {
                // If we're not at the first child and are mid-scroll, make sure we obey the offset
                targetScrollX -= titleOffset;
            }

            scrollTo(targetScrollX, 0);
        }
    }

    private class InternalViewPagerListener implements ViewPager.OnPageChangeListener {
        private int scrollState;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            int tabStripChildCount = tabStrip.getChildCount();
            if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
                return;
            }

            tabStrip.onViewPagerPageChanged(position, positionOffset);

            View selectedTitle = tabStrip.getChildAt(position);
            int extraOffset = (selectedTitle != null)
                    ? (int) (positionOffset * selectedTitle.getWidth())
                    : 0;
            scrollToTab(position, extraOffset);

            if (viewPagerPageChangeListener != null) {
                viewPagerPageChangeListener.onPageScrolled(position, positionOffset,
                        positionOffsetPixels);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            scrollState = state;

            if (viewPagerPageChangeListener != null) {
                viewPagerPageChangeListener.onPageScrollStateChanged(state);
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (scrollState == ViewPager.SCROLL_STATE_IDLE) {
                tabStrip.onViewPagerPageChanged(position, 0f);
                scrollToTab(position, 0);
            }

            if (viewPagerPageChangeListener != null) {
                viewPagerPageChangeListener.onPageSelected(position);
            }
        }

    }

    private class TabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            for (int i = 0; i < tabStrip.getChildCount(); i++) {
                if (v == tabStrip.getChildAt(i)) {
                    viewPager.setCurrentItem(i);
                    return;
                }
            }
        }
    }

}