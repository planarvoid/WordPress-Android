package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;

/**
 * A ListView that maintains a header pinned at the top of the list. The pinned header can be pushed up and dissolved as
 * needed. Header logic taken from http://code.google.com/p/android-amazing-listview/source/browse/trunk/
 */
public class SectionedListView extends LazyListView {
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
                .inflate(R.layout.list_header, (FrameLayout) mSectionHeaderView);

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
        configureHeaderView(getFirstVisiblePosition());
    }


    public void configureHeaderView(int position) {
        int state;
        if (this.getHeaderViewsCount() > position){
            state = SectionedAdapter.PINNED_HEADER_GONE;
        } else {
            state = adapter.getWrappedAdapter().getPinnedHeaderState(position - getHeaderViewsCount());
        }


        switch (state) {
            case SectionedAdapter.PINNED_HEADER_GONE: {
                mSectionHeaderViewVisible = false;
                break;
            }

            case SectionedAdapter.PINNED_HEADER_VISIBLE: {
                adapter.getWrappedAdapter()
                        .configurePinnedHeader(mSectionHeaderView, position,
                                getResources().getColor(R.color.listBgHeader),
                                getResources().getColor(R.color.listTxtHeader));

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
                    int alpha;
                    if (bottom < headerHeight) {
                        y = (bottom - headerHeight);
                        alpha = 255 * (headerHeight + y) / headerHeight;
                    } else {
                        y = 0;
                        alpha = 255;
                    }
                    adapter.getWrappedAdapter()
                            .configurePinnedHeader(mSectionHeaderView, position,
                                    alpha << 24 | getResources().getColor(R.color.listBgHeader),
                                    alpha << 24 | getResources().getColor(R.color.listTxtHeader));

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
    public void setAdapter(ListAdapter adapter) {
        if (!(adapter instanceof SectionedEndlessAdapter)) {
            throw new IllegalArgumentException(SectionedListView.class.getSimpleName()
                    + " must use adapter of type " + SectionedEndlessAdapter.class.getSimpleName());
        }

        this.adapter = (SectionedEndlessAdapter) adapter;
        super.setAdapter(adapter);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (view instanceof SectionedListView && adapter != null) {
            adapter.getWrappedAdapter().onScroll(this, firstVisibleItem);
        }
    }
}