package com.markupartist.android.widget;


import com.soundcloud.android.R;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Context;
import android.graphics.Rect;
import android.opengl.Visibility;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    private static final int DONE_REFRESHING = 5;

    private static final int HEADER_HIDE_DURATION = 400;

    private static final String TAG = "PullToRefreshListView";

    private OnRefreshListener mOnRefreshListener;

    /**
     * Listener that will receive notifications every time the list scrolls.
     */
    private OnScrollListener mOnScrollListener;

    private LinearLayout mRefreshView;
    private TextView mRefreshViewText;
    private ImageView mRefreshViewImage;
    private ProgressBar mRefreshViewProgress;
    private TextView mRefreshViewLastUpdated;


    private int mLastMotionY;

    private int mCurrentScrollState;
    private int mRefreshState;
    private Runnable mSelectionRunnable;

    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;

    protected int mRefreshViewHeight;
    private int mRefreshOriginalTopPadding;

    private View mEmptyView;

    private long mLastUpdated;

    protected View mFooterView;
    private boolean mAutoScrolling;

    private int mLastY;


    public PullToRefreshListView(Context context) {
        super(context);
        init(context);
    }

    /**
     * @noinspection UnusedDeclaration
     */
    public PullToRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * @noinspection UnusedDeclaration
     */
    public PullToRefreshListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }


    private void init(Context context) {

        if (Build.VERSION.SDK_INT >= 9){
            setOverScrollMode(OVER_SCROLL_NEVER);
        }

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

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRefreshView = (LinearLayout) inflater.inflate(R.layout.pull_to_refresh_header, null);

        mRefreshViewText = (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_text);
        mRefreshViewImage = (ImageView) mRefreshView.findViewById(R.id.pull_to_refresh_image);
        mRefreshViewProgress = (ProgressBar) mRefreshView.findViewById(R.id.pull_to_refresh_progress);
        mRefreshViewLastUpdated = (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_updated_at);


        final int imgMin = (int) (40 * getResources().getDisplayMetrics().density);
        mRefreshViewImage.setMinimumHeight(imgMin);
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

        mEmptyView = inflater.inflate(R.layout.empty_list, null);
        mEmptyView.setBackgroundColor(0xFFFFFFFF);
    }

    public View getEmptyView() {
        return mEmptyView;
    }

    public void setEmptyText(CharSequence text) {
        ((TextView) mEmptyView.findViewById(R.id.empty_txt)).setText(text);
    }

    @Override
    public void setEmptyView(View emptyView) {
        mEmptyView = emptyView;
        requestLayout();
    }

    @Override
    protected void onAttachedToWindow() {
        if (mRefreshState != REFRESHING){
            setSelection(1);
        }
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        setSelection(1);
    }

    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        //Log.i("asdf", "Overscroll By " + deltaX + " " + deltaY + " " + scrollX + " " + scrollY + " " + scrollRangeX + " " + scrollRangeY + " " + maxOverScrollX + " " + maxOverScrollY + " " + isTouchEvent);
        return false;
    }

    private void checkHeaderVisibility(final boolean smooth){
        if (getFirstVisiblePosition() == 0 && mRefreshState == TAP_TO_REFRESH) {
            post(new Runnable() {
                @Override
                public void run() {
                    if (smooth && !isInTouchMode()) {
                        scrollListBy(getChildAt(1).getTop(), HEADER_HIDE_DURATION);
                    } else {
                        setSelectionFromTop(1, 0);
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
            mAutoScrolling = true;
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    mAutoScrolling = false;
                }
            }, duration);
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
            return ((LazyEndlessAdapter) ((HeaderViewListAdapter) super.getAdapter()).getWrappedAdapter()).getWrappedAdapter();

        } else if (LazyEndlessAdapter.class.isAssignableFrom(super.getAdapter().getClass())) {
            return ((LazyEndlessAdapter) super.getAdapter()).getWrappedAdapter();

        } else
            return super.getAdapter();
    }

    public LazyEndlessAdapter getWrapper() {
        if (HeaderViewListAdapter.class.isAssignableFrom(super.getAdapter().getClass()) &&
                LazyEndlessAdapter.class.isAssignableFrom(((HeaderViewListAdapter) super.getAdapter()).getWrappedAdapter().getClass())) {
            return (LazyEndlessAdapter) ((HeaderViewListAdapter) super.getAdapter()).getWrappedAdapter();

        } else if (LazyEndlessAdapter.class.isAssignableFrom(super.getAdapter().getClass())) {
            return (LazyEndlessAdapter) super.getAdapter();

        } else
            return null;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (changed) {
            if (getHeight() > 0 && mEmptyView != null) {
                mEmptyView.findViewById(R.id.empty_txt).getLayoutParams().height = getHeight() + 30;
                mEmptyView.findViewById(R.id.empty_txt).invalidate();
            }
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (getFirstVisiblePosition() == 0 && (mRefreshState == TAP_TO_REFRESH || mRefreshState == DONE_REFRESHING)) {
            // not enough views to fill list so pad with an empty view
            if (getLastVisiblePosition() >= getWrapper().getCount()) {
                mFooterView.getLayoutParams().height = getHeight() - (getChildAt(getWrapper().getCount()).getBottom() - getChildAt(1).getTop());

                if (mRefreshState == TAP_TO_REFRESH){ // instant set selection, must be first layout
                    setSelection(1);
                    return;
                }
            }

            if (mRefreshState == DONE_REFRESHING) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        scrollListBy(getChildAt(1).getTop(), HEADER_HIDE_DURATION);
                    }
                });
            }
            resetHeader();
        }

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int y = (int) event.getY();
        boolean skipParentHandling = false;
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
                        mRefreshState = REFRESHING;
                        prepareForRefresh();
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
               skipParentHandling = checkAndHandleTop(event,mLastMotionY);
                break;
        }

        if (skipParentHandling){
            return true;
        } else {
            return super.onTouchEvent(event);
        }


    }

    public boolean checkAndHandleTop(MotionEvent ev, int lastMotionY) {
        final int childCount = getChildCount();
        if (childCount == 0) return true;

        final int firstTop = getChildAt(0).getTop();
        final int spaceAbove = getListPaddingTop() - firstTop;

        final int height = getHeight() - getPaddingBottom() - getPaddingTop();
        int incrementalDeltaY = lastMotionY != Integer.MIN_VALUE ? (int) ev.getY() - lastMotionY : (int) ev.getY();

        if (Math.abs(incrementalDeltaY) >= 1) {
            final int topPadding = (int) ((incrementalDeltaY - mRefreshViewHeight) / 1.7);
            if (topPadding != mRefreshView.getPaddingTop()){
                    mRefreshView.setPadding(mRefreshView.getPaddingLeft(),topPadding,
                            mRefreshView.getPaddingRight(), mRefreshView.getPaddingBottom());
            }

        }

        if (incrementalDeltaY > 0) {
            incrementalDeltaY = Math.min(height - 1, incrementalDeltaY);
            if (getFirstVisiblePosition() == 0 && firstTop >= getFirstVisiblePosition() && incrementalDeltaY >= 0) {
                return true;
            }
        }
        return false;
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
        if (mRefreshState != TAP_TO_REFRESH) {
            mRefreshState = TAP_TO_REFRESH;

            resetHeaderPadding();

            // Set refresh view text to the pull label
            mRefreshViewText.setText(R.string.pull_to_refresh_refreshing_label);
            // Replace refresh drawable with arrow drawable
            mRefreshViewImage.setImageResource(R.drawable.ic_pulltorefresh_arrow);
            // Clear the full rotation animation
            mRefreshViewImage.clearAnimation();
            // Hide progress bar and arrow.
            mRefreshViewImage.setVisibility(View.INVISIBLE);
            mRefreshViewProgress.setVisibility(View.GONE);
        }
    }

    private void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0, p.width);
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
                if ((mRefreshView.getBottom() > mRefreshViewHeight + 20)
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
                    }
                    mRefreshState = PULL_TO_REFRESH;
                }
            } else {
                mRefreshViewImage.setVisibility(View.INVISIBLE);
                resetHeader();
            }
        } else if (mCurrentScrollState == SCROLL_STATE_FLING
                && firstVisibleItem == 0
                && mRefreshState != REFRESHING
                 && !mAutoScrolling) {
            setSelection(1);
        }

        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(view, firstVisibleItem,
                    visibleItemCount, totalItemCount);
        }
    }

    private void configureLastUpdated() {
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

        mRefreshViewImage.setVisibility(View.INVISIBLE);
        // We need this hack, otherwise it will keep the previous drawable.
        mRefreshViewImage.setImageDrawable(null);
        mRefreshViewProgress.setVisibility(View.VISIBLE);

        // Set refresh view text to the refreshing label
        mRefreshViewText.setText(R.string.pull_to_refresh_refreshing_label);

        mRefreshState = REFRESHING;
    }

    public void onRefresh() {
        if (mOnRefreshListener != null) {
            if (mRefreshState != REFRESHING) {
                prepareForRefresh();
            }
            mOnRefreshListener.onRefresh();
        }

    }

    /**
     * Resets the list to a normal state after a refresh.
     */
    public void onRefreshComplete(boolean success) {
        if (success) mLastUpdated = System.currentTimeMillis();
        if (mRefreshView.getBottom() > 0) {
            mRefreshState = DONE_REFRESHING;
            invalidateViews();
        } else {
            resetHeader();
        }
    }

    /**
     * This will help initial selections from being overwritten. Also allows us to maintain
     * list position on orientation change in UserBrowser
     */
    public void postSelect(final int position, final int yOffset, boolean override) {
        if (mSelectionRunnable != null) {
            if (override) {
                removeCallbacks(mSelectionRunnable);
            } else {
                return;
            }
        }

        mSelectionRunnable = new Runnable() {
            @Override
            public void run() {
                setSelectionFromTop(position, yOffset);
                mSelectionRunnable = null;
            }
        };
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
         * <p/>
         * A call to {@link PullToRefreshListView #onRefreshComplete()} is
         * expected to indicate that the refresh has completed.
         */
        public void onRefresh();
    }
}
