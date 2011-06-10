package com.soundcloud.android.view;

import android.content.*;
import android.graphics.*;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;

/**
 * A ListView that maintains a header pinned at the top of the list. The
 * pinned header can be pushed up and dissolved as needed.
 * Header logic taken from http://code.google.com/p/android-amazing-listview/source/browse/trunk/
 */
public class SectionedListView extends LazyListView {
	public static final String TAG = SectionedListView.class.getSimpleName();

    private final View mHeaderView;
    private boolean mHeaderViewVisible;

    private int mHeaderViewWidth;
    private int mHeaderViewHeight;

    private int mHeaderBgColor;
    private int mHeaderTextColor;


    private SectionedEndlessAdapter adapter;

    public SectionedListView(ScActivity activity) {
        super(activity);

        mHeaderView = new FrameLayout(activity);
        mHeaderView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.list_header, (FrameLayout) mHeaderView);

        setFadingEdgeLength(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mHeaderView != null){
            measureChild(mHeaderView, widthMeasureSpec, heightMeasureSpec);
            mHeaderViewWidth = mHeaderView.getMeasuredWidth();
            mHeaderViewHeight = mHeaderView.getMeasuredHeight();
        }

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mHeaderView.layout(0, 0, mHeaderViewWidth, mHeaderViewHeight);
        configureHeaderView(getFirstVisiblePosition());
    }

    public void configureHeaderView(int position) {
        int state = ((SectionedAdapter) adapter.getWrappedAdapter()).getPinnedHeaderState(position);
        switch (state) {
            case SectionedAdapter.PINNED_HEADER_GONE: {
                mHeaderViewVisible = false;
                break;
            }

            case SectionedAdapter.PINNED_HEADER_VISIBLE: {
                ((SectionedAdapter) adapter.getWrappedAdapter())
                            .configurePinnedHeader(mHeaderView, position,
                                    getResources().getColor(R.color.listBgHeader),
                                    getResources().getColor(R.color.listTxtHeader));

                if (mHeaderView.getTop() != 0) {
                    mHeaderView.layout(0, 0, mHeaderViewWidth, mHeaderViewHeight);
                }
                mHeaderViewVisible = true;
                break;
            }

            case SectionedAdapter.PINNED_HEADER_PUSHED_UP: {
                View firstView = getChildAt(0);
                if (firstView != null) {
                    int bottom = firstView.getBottom();
                    int headerHeight = mHeaderView.getHeight();
                    int y;
                    int alpha;
                    if (bottom < headerHeight) {
                        y = (bottom - headerHeight);
                        alpha = 255 * (headerHeight + y) / headerHeight;
                    } else {
                        y = 0;
                        alpha = 255;
                    }
                    ((SectionedAdapter) adapter.getWrappedAdapter())
                            .configurePinnedHeader(mHeaderView, position,
                                    alpha << 24 | getResources().getColor(R.color.listBgHeader),
                                    alpha << 24 | getResources().getColor(R.color.listTxtHeader));

                    if (mHeaderView.getTop() != y) {
                        mHeaderView.layout(0, y, mHeaderViewWidth, mHeaderViewHeight + y);
                    }
                    mHeaderViewVisible = true;
                }
                break;
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mHeaderViewVisible) {
            drawChild(canvas, mHeaderView, getDrawingTime());
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
        if (view instanceof SectionedListView && adapter != null){
             ((SectionedAdapter) adapter.getWrappedAdapter()).onScroll(this,firstVisibleItem);
         }
    }
}