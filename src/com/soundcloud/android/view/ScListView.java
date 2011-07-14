package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.MyTracksAdapter;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
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
import java.util.ArrayList;


/*
    pull-to-refresh original source: https://github.com/johannilsson/android-pulltorefresh
 */

public class ScListView extends ListView implements AbsListView.OnScrollListener {

    private static final int MESSAGE_UPDATE_LIST_ICONS = 1;
    private static final int DELAY_SHOW_LIST_ICONS = 550;

    private static final int TAP_TO_REFRESH = 1;
    private static final int PULL_TO_REFRESH = 2;
    private static final int RELEASE_TO_REFRESH = 3;
    private static final int REFRESHING = 4;
    private static final int DONE_REFRESHING = 5;

    private static final int HEADER_HIDE_DURATION = 400;

    private static final String TAG = "ScListView";

    private ScActivity mActivity;

    private final Handler mScrollHandler = new ScrollHandler();
    private int mCurrentScrollState = SCROLL_STATE_IDLE;
    private boolean mFingerUp = true;
    private boolean mAutoScrolling;

    private LazyListListener mListener;
    private OnRefreshListener mOnRefreshListener;
    private OnScrollListener mOnScrollListener;

    private LinearLayout mRefreshView;
    private TextView mRefreshViewText;
    private ImageView mRefreshViewImage;
    private ProgressBar mRefreshViewProgress;
    private TextView mRefreshViewLastUpdated;
    private View mEmptyView, mFooterView;

    private Runnable mSelectionRunnable;
    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;

    protected int mLastY, mLastMotionY, mRefreshState, mRefreshViewHeight, mRefreshOriginalTopPadding;
    private long mLastUpdated;

    public ScListView(ScActivity activity) {
        super(activity);
        init(activity);
    }

    /**
     * @noinspection UnusedDeclaration
     */
    public ScListView(ScActivity activity, AttributeSet attrs) {
        super(activity, attrs);
        init(activity);
    }

    /**
     * @noinspection UnusedDeclaration
     */
    public ScListView(ScActivity activity, AttributeSet attrs, int defStyle) {
        super(activity, attrs, defStyle);
        init(activity);
    }

    private void init(ScActivity activity) {
        mActivity = activity;

        if (Build.VERSION.SDK_INT >= 9) {
            setOverScrollMode(OVER_SCROLL_NEVER);
        }

        // Load all of the animations we need in code rather than through XML
        mFlipAnimation = new RotateAnimation(0, -180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mFlipAnimation.setInterpolator(new LinearInterpolator());
        mFlipAnimation.setDuration(250);
        mFlipAnimation.setFillAfter(true);
        mReverseFlipAnimation = new RotateAnimation(-180, 0,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
        mReverseFlipAnimation.setDuration(250);
        mReverseFlipAnimation.setFillAfter(true);

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

        mFooterView = new FrameLayout(activity);
        mFooterView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 0));
        mFooterView.setBackgroundColor(0xFFFFFFFF);
        addFooterView(mFooterView);

        super.setOnScrollListener(this);

        measureView(mRefreshView);
        mRefreshViewHeight = mRefreshView.getMeasuredHeight();

        mEmptyView = inflater.inflate(R.layout.empty_list, null);
        mEmptyView.setBackgroundColor(0xFFFFFFFF);

        setOnItemClickListener(mOnItemClickListener);
        setOnItemLongClickListener(mOnItemLongClickListener);
        setOnItemSelectedListener(mOnItemSelectedListener);
        setOnTouchListener(new FingerTracker());
    }


    @Override
    public int getSolidColor() {
        return 0xAAFFFFFF;
    }

    public void enableLongClickListener() {
        setOnItemLongClickListener(mOnItemLongClickListener);
    }

    public void disableLongClickListener() {
        setOnItemLongClickListener(null);
    }

    public void setLazyListListener(LazyListListener listener) {
        mListener = listener;
    }

    public void setAdapter(LazyEndlessAdapter adapter, boolean refreshEnabled) {
        super.setAdapter(adapter);
        if (refreshEnabled) setOnRefreshListener(adapter);
    }

    protected AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> list, View row, int position, long id) {
            if (list.getAdapter().getCount() <= 0
                    || position >= list.getAdapter().getCount())
                return; // bad list item clicked (possibly loading item)

            position -= getHeaderViewsCount();

            if (list.getAdapter().getItem(position) instanceof Track) {

                if (list.getAdapter() instanceof MyTracksAdapter) {
                    position -= ((MyTracksAdapter) list.getAdapter()).getPendingRecordingsCount();
                }

                if (mListener != null) {
                    mListener.onTrackClick((ArrayList<Parcelable>) ((LazyBaseAdapter) list.getAdapter()).getData(), position);
                }

            } else if (list.getAdapter().getItem(position) instanceof Event) {

                if (mListener != null) {
                    mListener.onEventClick((ArrayList<Parcelable>) ((LazyBaseAdapter) list.getAdapter()).getData(), position);
                }

            } else if (list.getAdapter().getItem(position) instanceof User || list.getAdapter().getItem(position) instanceof Friend) {

                if (mListener != null) {
                    mListener.onUserClick((ArrayList<Parcelable>) ((LazyBaseAdapter) list.getAdapter()).getData(), position);
                }

            } else if (list.getAdapter().getItem(position) instanceof Recording) {

                if (mListener != null) {
                    mListener.onRecordingClick((Recording) list.getAdapter().getItem(position));
                }
            }
        }
    };

    protected AdapterView.OnItemLongClickListener mOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {

        public boolean onItemLongClick(AdapterView<?> list, View row, int position, long id) {
            if (list.getAdapter().getCount() <= 0 || position >= list.getAdapter().getCount()){
                return false; // bad list item clicked (possibly loading item)
            }

            ((LazyBaseAdapter) list.getAdapter()).submenuIndex = ((LazyBaseAdapter) list.getAdapter()).animateSubmenuIndex = position;
            ((LazyBaseAdapter) list.getAdapter()).notifyDataSetChanged();
            return true;
        }

    };
    protected AdapterView.OnItemSelectedListener mOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> listView, View view, int position, long id) {
            if (((LazyBaseAdapter) listView.getAdapter()).submenuIndex == position) {
                listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            } else {
                listView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            }
        }

        public void onNothingSelected(AdapterView<?> listView) {
            // This happens when you start scrolling, so we need to prevent it from staying
            // in the afterDescendants mode if the EditText was focused
            listView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        }
    };


    private class FingerTracker implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            mFingerUp = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;
            if (mFingerUp && mCurrentScrollState != SCROLL_STATE_FLING) {
                //postUpdateListIcons();
            }
            return false;
        }
    }


    private class ScrollHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE_LIST_ICONS:
                    if (mListener != null)
                        mListener.onFlingDone();
                    break;
            }
        }
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
        super.onAttachedToWindow();
        checkHeaderVisibility(false);
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) {
            checkHeaderVisibility(false);
        }
    }

    public void onResume() {
        if (getWrapper() != null) {
            getWrapper().notifyDataSetChanged();
            if (getWrapper().isRefreshing()) {
                prepareForRefresh();
                if (getFirstVisiblePosition() != 0) {
                    postSelect(0, 0, false);
                }
            } else if (getWrapper().needsRefresh()) {
                onRefresh();
            } else {
                checkHeaderVisibility(false);
            }
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

    private void checkHeaderVisibility(final boolean smooth) {
        if (getFirstVisiblePosition() == 0 && mRefreshState == TAP_TO_REFRESH && mSelectionRunnable == null) {
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
     * <p/>
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

                if (mRefreshState == TAP_TO_REFRESH) { // instant set selection, must be first layout
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
                skipParentHandling = checkAndHandleTop(event, mLastMotionY);
                break;
        }

        if (skipParentHandling) {
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
            if (topPadding != mRefreshView.getPaddingTop()) {
                mRefreshView.setPadding(mRefreshView.getPaddingLeft(), topPadding,
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

        if (mCurrentScrollState == SCROLL_STATE_FLING && scrollState != SCROLL_STATE_FLING) {

            final Handler handler = mScrollHandler;
            final Message message = handler.obtainMessage(MESSAGE_UPDATE_LIST_ICONS,
                    mActivity);
            handler.removeMessages(MESSAGE_UPDATE_LIST_ICONS);
            handler.sendMessageDelayed(message, mFingerUp ? 0 : DELAY_SHOW_LIST_ICONS);

        } else if (scrollState == SCROLL_STATE_FLING) {
            mScrollHandler.removeMessages(MESSAGE_UPDATE_LIST_ICONS);
            if (mListener != null)
                mListener.onFling();
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
                Log.i("asdf", "Post Selecting " + position);
                setSelectionFromTop(position, yOffset);
                mSelectionRunnable = null;
            }
        };
        post(mSelectionRunnable);
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
         * A call to {@link ScListView #onRefreshComplete()} is
         * expected to indicate that the refresh has completed.
         */
        public void onRefresh();
    }

     public interface LazyListListener {
        void onUserClick(ArrayList<Parcelable> users, int position);
        void onRecordingClick(Recording recording);
        void onTrackClick(ArrayList<Parcelable> tracks, int position);
        void onEventClick(ArrayList<Parcelable> events, int position);
        void onFling();
        void onFlingDone();
    }

}
