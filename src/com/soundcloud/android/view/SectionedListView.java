package com.soundcloud.android.view;

import android.widget.AbsListView;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;

import android.content.Context;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * A ListView that maintains a header pinned at the top of the list. The pinned header can be pushed up and dissolved as
 * needed. Header logic taken from http://code.google.com/p/android-amazing-listview/source/browse/trunk/
 */
public class SectionedListView extends ScListView {
    public static final String TAG = SectionedListView.class.getSimpleName();

    private final View mSectionHeaderView;
    private boolean mSectionHeaderViewVisible;

    private int mSectionHeaderViewWidth;
    private int mSectionHeaderViewHeight;

    private SectionedEndlessAdapter adapter;

    public SectionedListView(ScActivity activity) {
        super(activity);

        mSectionHeaderView = new FrameLayout(activity);
        mSectionHeaderView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.sectioned_list_header, (FrameLayout) mSectionHeaderView);

        setFadingEdgeLength(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mSectionHeaderView != null) {
            measureChild(mSectionHeaderView, widthMeasureSpec, heightMeasureSpec);
            mSectionHeaderViewWidth = mSectionHeaderView.getMeasuredWidth();
            mSectionHeaderViewHeight = mSectionHeaderView.getMeasuredHeight();
        }

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mSectionHeaderView.layout(0, 0, mSectionHeaderViewWidth, mSectionHeaderViewHeight);
        configureHeaderView(getRefreshableView().getFirstVisiblePosition());
    }


    public void configureHeaderView(int position) {
        int state;
        if (this.getRefreshableView().getHeaderViewsCount() > position || position - getRefreshableView().getHeaderViewsCount() == getBaseAdapter().getCount() - 1){
            state = SectionedAdapter.PINNED_HEADER_GONE;
        } else {
            state = adapter.getWrappedAdapter().getPinnedHeaderState(position - getRefreshableView().getHeaderViewsCount());
        }

        final int adjPosition = position - getRefreshableView().getHeaderViewsCount();
        switch (state) {
            case SectionedAdapter.PINNED_HEADER_GONE: {
                mSectionHeaderViewVisible = false;
                break;
            }

            case SectionedAdapter.PINNED_HEADER_VISIBLE: {
                adapter.getWrappedAdapter()
                        .configurePinnedHeader(mSectionHeaderView, adjPosition);

                if (mSectionHeaderView.getTop() != 0) {
                    mSectionHeaderView.layout(0, 0, mSectionHeaderViewWidth, mSectionHeaderViewHeight);
                }

                mSectionHeaderViewVisible = true;
                break;
            }

            case SectionedAdapter.PINNED_HEADER_PUSHED_UP: {
                View firstView = getChildAt(0);
                if (firstView != null) {
                    int bottom = firstView.getBottom();
                    int headerHeight = mSectionHeaderView.getHeight();
                    int y;
                    if (bottom < headerHeight) {
                        y = (bottom - headerHeight);
                    } else {
                        y = 0;
                    }
                    adapter.getWrappedAdapter()
                            .configurePinnedHeader(mSectionHeaderView, adjPosition);

                    if (mSectionHeaderView.getTop() != y) {
                        mSectionHeaderView.layout(0, y, mSectionHeaderViewWidth, mSectionHeaderViewHeight + y);
                    }
                    mSectionHeaderViewVisible = true;
                }
                break;
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mSectionHeaderViewVisible) {
            drawChild(canvas, mSectionHeaderView, getDrawingTime());
        }
    }

    @Override
    public void setAdapter(LazyEndlessAdapter adapter, boolean refreshEnabled) {
        if (!(adapter instanceof SectionedEndlessAdapter)) {
            throw new IllegalArgumentException(SectionedListView.class.getSimpleName()
                    + " must use adapter of type " + SectionedEndlessAdapter.class.getSimpleName());
        }

        this.adapter = (SectionedEndlessAdapter) adapter;
        super.setAdapter(adapter, refreshEnabled);
    }


    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        super.onScroll(view,firstVisibleItem,visibleItemCount,totalItemCount);
        if (this instanceof SectionedListView && adapter != null) {
            adapter.getWrappedAdapter().onScroll(this, firstVisibleItem);
        }
    }
}