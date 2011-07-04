package com.markupartist.android.widget;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.*;
import android.widget.AbsListView.OnScrollListener;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.utils.CloudUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
    original source: https://github.com/johannilsson/android-pulltorefresh
 */


public class PullToRefreshListView extends ListView implements OnScrollListener {

    private static final int TAP_TO_REFRESH = 1;
    private static final int PULL_TO_REFRESH = 2;
    private static final int RELEASE_TO_REFRESH = 3;
    private static final int REFRESHING = 4;

    private static final int HEADER_HIDE_DURATION = 400;

    private static final String TAG = "PullToRefreshListView";

    private OnRefreshListener mOnRefreshListener;

    /**
     * Listener that will receive notifications every time the list scrolls.
     */
    private OnScrollListener mOnScrollListener;
    private LayoutInflater mInflater;

    private LinearLayout mRefreshView;
    private TextView mRefreshViewText;
    private ImageView mRefreshViewImage;
    private ProgressBar mRefreshViewProgress;
    private TextView mRefreshViewLastUpdated;

    private int mCurrentScrollState;
    private int mRefreshState;

private boolean mPushBackUp;

    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;

    protected int mRefreshViewHeight;
    private int mRefreshOriginalTopPadding;
    private int mLastMotionY;

    private View mEmptyView;

    private long mLastUpdated;

    protected View mFooterView;
    protected boolean mNeedFooterView;


    public PullToRefreshListView(Context context) {
        super(context);
        init(context);
    }

    public PullToRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PullToRefreshListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }



    private void init(Context context) {
        // Load all of the animations we need in code rather than through XML
        mFlipAnimation = new RotateAnimation(0, -180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mFlipAnimation.setInterpolator(new LinearInterpolator());
        mFlipAnimation.setDuration(250);
        mFlipAnimation.setFillAfter(true);
        mReverseFlipAnimation = new RotateAnimation(-180, 0,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
        mReverseFlipAnimation.setDuration(250);
        mReverseFlipAnimation.setFillAfter(true);

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mInflater.inflate(R.layout.pull_to_refresh_header, null);

        mRefreshView = (LinearLayout) mInflater.inflate(
                R.layout.pull_to_refresh_header, null);

        mRefreshViewText =
            (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_text);
        mRefreshViewImage =
            (ImageView) mRefreshView.findViewById(R.id.pull_to_refresh_image);
        mRefreshViewProgress =
            (ProgressBar) mRefreshView.findViewById(R.id.pull_to_refresh_progress);
        mRefreshViewLastUpdated =
            (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_updated_at);

        mRefreshViewImage.setMinimumHeight(50);
        mRefreshView.setOnClickListener(new OnClickRefreshListener());
        mRefreshOriginalTopPadding = mRefreshView.getPaddingTop();

        mRefreshState = TAP_TO_REFRESH;

        addHeaderView(mRefreshView);

        mFooterView = new FrameLayout(context);
        mFooterView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 0));
        mFooterView.setBackgroundColor(0xFFFFFFFF);
        addFooterView(mFooterView);

        super.setOnScrollListener(this);

        measureView(mRefreshView);
        mRefreshViewHeight = mRefreshView.getMeasuredHeight();
    }

    @Override
    public void setEmptyView(View emptyView) {
        mEmptyView = emptyView;
        requestLayout();
    }

    @Override
    protected void onAttachedToWindow() {
        if (mRefreshState != REFRESHING) setSelection(1);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        setSelection(1);
    }

    public void setSelection(int position){
        Log.i("asdf","Set Selection " + 1);
        super.setSelection(position);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        checkHeaderVisibility(false);
    }

    @Override
    public void onTouchModeChanged(boolean isInTouchMode) {
        super.onTouchModeChanged(isInTouchMode);
        checkHeaderVisibility(false);
    }

    private void checkHeaderVisibility(final boolean smooth){
        if (getFirstVisiblePosition() == 0 && mRefreshState == TAP_TO_REFRESH) {
            post(new Runnable() {
                @Override
                public void run() {
                    if (smooth && !isInTouchMode()) {
                        scrollListBy(getChildAt(1).getTop(), HEADER_HIDE_DURATION);
                    } else {
                        setSelection(1);
                    }
                }
            });
        }
    }

    /**
     * Smoothly scroll by distance pixels over duration milliseconds.
     *
     * <p>Using reflection internally to call smoothScrollBy for API Level 8
     * otherwise scrollBy is called.
     *
     * @param distance Distance to scroll in pixels.
     * @param duration Duration of the scroll animation in milliseconds.
     */
    private void scrollListBy(int distance, int duration) {
        try {
            Method method = ListView.class.getMethod("smoothScrollBy",
                    Integer.TYPE, Integer.TYPE);
            method.invoke(this, distance + 1, duration);
        } catch (NoSuchMethodException e) {
            // If smoothScrollBy is not available (< 2.2)
        	setSelection(1);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IllegalAccessException e) {
            System.err.println("unexpected " + e);
        } catch (InvocationTargetException e) {
            System.err.println("unexpected " + e);
        }
    }


    /**
     * Set the listener that will receive notifications every time the list
     * scrolls.
     * 
     * @param l The scroll listener. 
     */
    @Override
    public void setOnScrollListener(AbsListView.OnScrollListener l) {
        mOnScrollListener = l;
    }

    /**
     * Register a callback to be invoked when this list should be refreshed.
     * 
     * @param onRefreshListener The callback to run.
     */
    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        mOnRefreshListener = onRefreshListener;
    }

    public void setLastUpdated(long lastUpdated) {
        mLastUpdated = lastUpdated;
        if (mRefreshState != TAP_TO_REFRESH) configureLastUpdated();
    }

    public long getLastUpdated() {
        return mLastUpdated;
    }

    @Override
    public ListAdapter getAdapter() {

        if (super.getAdapter() == null) return null;


        if (HeaderViewListAdapter.class.isAssignableFrom(super.getAdapter().getClass()) &&
                LazyEndlessAdapter.class.isAssignableFrom(((HeaderViewListAdapter) super.getAdapter()).getWrappedAdapter().getClass())) {
             return ((LazyEndlessAdapter)((HeaderViewListAdapter) super.getAdapter()).getWrappedAdapter()).getWrappedAdapter();

        } else if (LazyEndlessAdapter.class.isAssignableFrom(super.getAdapter().getClass())) {
            return ((LazyEndlessAdapter) super.getAdapter()).getWrappedAdapter();

        } else
            return super.getAdapter();
    }

    public LazyEndlessAdapter getWrapper() {
        if (HeaderViewListAdapter.class.isAssignableFrom(super.getAdapter().getClass()) &&
                LazyEndlessAdapter.class.isAssignableFrom(((HeaderViewListAdapter) super.getAdapter()).getWrappedAdapter().getClass())) {
             return (LazyEndlessAdapter)((HeaderViewListAdapter) super.getAdapter()).getWrappedAdapter();

        } else if (LazyEndlessAdapter.class.isAssignableFrom(super.getAdapter().getClass())) {
            return (LazyEndlessAdapter) super.getAdapter();

        } else
            return null;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (changed) {
            if (getHeight() > 0 && mEmptyView != null){
                mEmptyView.findViewById(R.id.empty_txt).getLayoutParams().height = getHeight() + 30;
                mEmptyView.invalidate();
            }
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (mPushBackUp && getFirstVisiblePosition() == 0) {

            // not enough views to fill list so pad with an empty view
            if (getLastVisiblePosition() >= getWrapper().getCount() ){
               mFooterView.getLayoutParams().height = getHeight() - (getChildAt(getWrapper().getCount()).getBottom() - getChildAt(1).getTop());
                invalidateViews();
            }
            // this has to happen after the next layout pass so that we are allowed
            post(new Runnable() {
                @Override
                public void run() {
                    scrollListBy(getChildAt(1).getTop(), HEADER_HIDE_DURATION);
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkHeaderVisibility(true);
                        }
                    }, HEADER_HIDE_DURATION);
                }
            });
            mPushBackUp = false;

        }
    }




    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (!isVerticalScrollBarEnabled()) {
                    setVerticalScrollBarEnabled(true);
                }

                if (getFirstVisiblePosition() == 0 && mRefreshState != REFRESHING) {
                    if (mRefreshView.getBottom() > mRefreshViewHeight
                            || mRefreshView.getTop() >= 0
                            && mRefreshState == RELEASE_TO_REFRESH) {
                        // Initiate the refresh
                        onRefresh();
                    } else if (mRefreshView.getBottom() < mRefreshViewHeight) {
                        // Abort refresh and scroll down below the refresh view
                        resetHeader();
                        setSelection(1);
                    }
                }
                break;
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                applyHeaderPadding(event);
                break;
        }
        return super.onTouchEvent(event);
    }

    private void applyHeaderPadding(MotionEvent ev) {

    }

    /**
     * Sets the header padding back to original size.
     */
    private void resetHeaderPadding() {
        mRefreshView.setPadding(
                mRefreshView.getPaddingLeft(),
                mRefreshOriginalTopPadding,
                mRefreshView.getPaddingRight(),
                mRefreshView.getPaddingBottom());
    }

    /**
     * Resets the header to the original state.
     */
    private void resetHeader() {
            mRefreshState = TAP_TO_REFRESH;

            resetHeaderPadding();

            // Replace refresh drawable with arrow drawable
            mRefreshViewImage.setImageResource(R.drawable.ic_pulltorefresh_arrow);
            // Clear the full rotation animation
            mRefreshViewImage.clearAnimation();
            // Hide progress bar and arrow.
            mRefreshViewImage.setVisibility(View.GONE);
            mRefreshViewProgress.setVisibility(View.GONE);
    }

    private void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(0,
                0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        // When the refresh view is completely visible, change the text to say
        // "Release to refresh..." and flip the arrow drawable.
        if (mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL
                && mRefreshState != REFRESHING) {
            if (firstVisibleItem == 0) {
                mRefreshViewImage.setVisibility(View.VISIBLE);
                if ((mRefreshView.getBottom() > mRefreshViewHeight + 20
                        || mRefreshView.getTop() >= 0)
                        && mRefreshState != RELEASE_TO_REFRESH) {
                    mRefreshViewText.setText(R.string.pull_to_refresh_release_label);
                    mRefreshViewImage.clearAnimation();
                    mRefreshViewImage.startAnimation(mFlipAnimation);
                    mRefreshState = RELEASE_TO_REFRESH;
                } else if (mRefreshView.getBottom() < mRefreshViewHeight + 20
                        && mRefreshState != PULL_TO_REFRESH) {
                    mRefreshViewText.setText(R.string.pull_to_refresh_pull_label);
                    if (mRefreshState != TAP_TO_REFRESH) {
                        mRefreshViewImage.clearAnimation();
                        mRefreshViewImage.startAnimation(mReverseFlipAnimation);
                    } else {
                        configureLastUpdated();
                    }
                    mRefreshState = PULL_TO_REFRESH;
                }
            } else {
                mRefreshViewImage.setVisibility(View.GONE);
                resetHeader();
            }
        } else if (mCurrentScrollState == SCROLL_STATE_FLING
                && firstVisibleItem == 0
                && mRefreshState != REFRESHING) {
            setSelection(1);
        }

        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(view, firstVisibleItem,
                    visibleItemCount, totalItemCount);
        }
    }

    private void configureLastUpdated(){
        // can trigger a weird layout loop if done in a different state, may need to be revisited
        if (mRefreshState == TAP_TO_REFRESH && mLastUpdated != 0) {
            mRefreshViewLastUpdated.setVisibility(View.VISIBLE);
            mRefreshViewLastUpdated.setText(getResources().getString(R.string.pull_to_refresh_last_updated,
                    CloudUtils.getElapsedTimeString(getResources(), mLastUpdated)));
        } else {
            mRefreshViewLastUpdated.setVisibility(View.GONE);
        }

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        mCurrentScrollState = scrollState;

        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    public void prepareForRefresh() {
        resetHeaderPadding();

        mRefreshViewImage.setVisibility(View.GONE);
        // We need this hack, otherwise it will keep the previous drawable.
        mRefreshViewImage.setImageDrawable(null);
        mRefreshViewProgress.setVisibility(View.VISIBLE);

        // Set refresh view text to the refreshing label
        mRefreshViewText.setText(R.string.pull_to_refresh_refreshing_label);

        configureLastUpdated();

        mRefreshState = REFRESHING;
    }

    public void onRefresh() {
        if (mRefreshState != REFRESHING) {
                prepareForRefresh();
            }
        mOnRefreshListener.onRefresh();
    }

    /**
     * Resets the list to a normal state after a refresh.
     */
    public void onRefreshComplete(boolean success) {
        if (success) mLastUpdated = System.currentTimeMillis();
        resetHeader();
        if (mRefreshView.getBottom() > 0) {
            mPushBackUp = true;
            invalidateViews();
        }
    }


    /**
     * Invoked when the refresh view is clicked on. This is mainly used when
     * there's only a few items in the list and it's not possible to drag the
     * list.
     */
    private class OnClickRefreshListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if (mRefreshState != REFRESHING) {
                onRefresh();
            }
        }

    }

    /**
     * Interface definition for a callback to be invoked when list should be
     * refreshed.
     */
    public interface OnRefreshListener {
        /**
         * Called when the list should be refreshed.
         * <p>
         * A call to {@link PullToRefreshListView #onRefreshComplete()} is
         * expected to indicate that the refresh has completed.
         */
        public boolean onRefresh();
    }
}
