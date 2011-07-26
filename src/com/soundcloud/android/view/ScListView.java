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
import android.graphics.drawable.Drawable;
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
import android.widget.BaseAdapter;
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
import java.util.List;


/*
    pull-to-refresh original source: https://github.com/johannilsson/android-pulltorefresh
 */

public class ScListView extends ListView implements AbsListView.OnScrollListener {

    @SuppressWarnings({"UnusedDeclaration"})
    private static final String TAG = "ScListView";

    private static final int MESSAGE_UPDATE_LIST_ICONS = 1;
    private static final int DELAY_SHOW_LIST_ICONS = 550;

    private static final int TAP_TO_REFRESH = 1;
    private static final int PULL_TO_REFRESH = 2;
    private static final int RELEASE_TO_REFRESH = 3;
    private static final int REFRESHING = 4;
    private static final int DONE_REFRESHING = 5;

    private static final int HEADER_HIDE_DURATION = 400;

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
    private Drawable mPullToRefreshArrow;

    private Runnable mSelectionRunnable;
    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;

    private int mLastMotionY;
    private int mRefreshState;
    private int mRefreshViewHeight;
    private int mRefreshOriginalTopPadding;
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

        mRefreshView.setOnClickListener(new OnClickRefreshListener());
        mRefreshOriginalTopPadding = mRefreshView.getPaddingTop();

        mPullToRefreshArrow = getResources().getDrawable(R.drawable.ic_pulltorefresh_arrow);

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

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) checkHeaderVisibility();

    }

    public void onResume() {
        if (getWrapper() != null) {
            getWrapper().notifyDataSetChanged();
        }

        if (mOnRefreshListener != null) {
            if (mOnRefreshListener.isRefreshing()) {
                prepareForRefresh();
                if (getFirstVisiblePosition() != 0) {
                    postSelect(0, 0, false);
                }
            } else if (mOnRefreshListener.needsRefresh()) {
                onRefresh();
            } else {
                checkHeaderVisibility();
            }
        }
    }

    public void onConnected(boolean isForeground) {
         if (mOnRefreshListener != null) {
             mOnRefreshListener.onConnected();
             if (isForeground && mOnRefreshListener.needsRefresh()) onRefresh();
         }
    }



    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        setSelection(1);
    }




    @Override
    public void setOnScrollListener(AbsListView.OnScrollListener l) {
        mOnScrollListener = l;
    }

    void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        mOnRefreshListener = onRefreshListener;
    }

    public void setLastUpdated(long lastUpdated) {
        mLastUpdated = lastUpdated;
        if (mRefreshState != TAP_TO_REFRESH) configureLastUpdated();
    }

    public long getLastUpdated() {
        return mLastUpdated;
    }

    /**
     * Get the data adapter. Could possibly be wrapped twice
     * @return
     */
    public LazyBaseAdapter getBaseAdapter() {
        if (super.getAdapter() == null) return null;
        if (HeaderViewListAdapter.class.isAssignableFrom(super.getAdapter().getClass()) &&
                LazyEndlessAdapter.class.isAssignableFrom(((HeaderViewListAdapter) super.getAdapter()).getWrappedAdapter().getClass())) {
            return ((LazyEndlessAdapter) ((HeaderViewListAdapter) super.getAdapter()).getWrappedAdapter()).getWrappedAdapter();

        } else if (LazyEndlessAdapter.class.isAssignableFrom(super.getAdapter().getClass())) {
            return ((LazyEndlessAdapter) super.getAdapter()).getWrappedAdapter();

        } else
            return null;
    }

    /**
     * Get the endless adapter. Could be wrapped once by a Header/Footer Listview
     * @return
     */
    public LazyEndlessAdapter getWrapper() {
        if (super.getAdapter() == null) return null;
        if (HeaderViewListAdapter.class.isAssignableFrom(super.getAdapter().getClass()) &&
                LazyEndlessAdapter.class.isAssignableFrom(((HeaderViewListAdapter) super.getAdapter()).getWrappedAdapter().getClass())) {
            return (LazyEndlessAdapter) ((HeaderViewListAdapter) super.getAdapter()).getWrappedAdapter();

        } else if (LazyEndlessAdapter.class.isAssignableFrom(super.getAdapter().getClass())) {
            return (LazyEndlessAdapter) super.getAdapter();

        } else return null;
    }

    public void prepareForRefresh() {
        resetHeaderPadding();
        configureLastUpdated();

        mRefreshViewImage.setVisibility(View.INVISIBLE);
        // We need this hack, otherwise it will keep the previous drawable.
        mRefreshViewImage.setImageDrawable(null);
        mRefreshViewProgress.setVisibility(View.VISIBLE);
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
        resetHeader();
        if (mRefreshView.getBottom() > 0) {
            mRefreshState = DONE_REFRESHING;
            invalidateViews();
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
        post(mSelectionRunnable);
    }

    private final AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> list, View row, int position, long id) {
            position -= getHeaderViewsCount();

            LazyBaseAdapter adp = list instanceof ScListView ?
                    ((ScListView) list).getBaseAdapter() : (LazyBaseAdapter) list.getAdapter();

            if (adp.getCount() <= 0 || position >= adp.getCount())
                return; // bad list item clicked (possibly loading item)

            if (adp.getItem(position) instanceof Track) {

                if (adp instanceof MyTracksAdapter) {
                    position -= ((MyTracksAdapter) adp).getPendingRecordingsCount();
                }

                if (mListener != null) {
                    mListener.onTrackClick(adp.getData(), position);
                }

            } else if (adp.getItem(position) instanceof Event) {

                if (mListener != null) {
                    mListener.onEventClick((ArrayList<Parcelable>) adp.getData(), position);
                }

            } else if (adp.getItem(position) instanceof User || adp.getItem(position) instanceof Friend) {

                if (mListener != null) {
                    mListener.onUserClick((ArrayList<Parcelable>) adp.getData(), position);
                }

            } else if (adp.getItem(position) instanceof Recording) {
                if (mListener != null) {
                    mListener.onRecordingClick((Recording) adp.getItem(position));
                }
            }
        }
    };

    private final AdapterView.OnItemLongClickListener mOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {

        public boolean onItemLongClick(AdapterView<?> list, View row, int position, long id) {
            if (list.getAdapter().getCount() <= 0 || position >= list.getAdapter().getCount() || !(list instanceof ScListView)){
                return false; // bad list item clicked (possibly loading item)
            }

            position -= getHeaderViewsCount();
            ScListView.this.getBaseAdapter().submenuIndex = ((ScListView) list).getBaseAdapter().animateSubmenuIndex = position;
            ScListView.this.getWrapper().notifyDataSetChanged();
            return true;
        }

    };
    private final AdapterView.OnItemSelectedListener mOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        checkHeaderVisibility();
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
                    } else {
                        configureLastUpdated();
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
            mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mOnScrollListener != null) mOnScrollListener.onScrollStateChanged(view, scrollState);

        if (mCurrentScrollState == SCROLL_STATE_FLING && scrollState != SCROLL_STATE_FLING) {
            final Handler handler = mScrollHandler;
            final Message message = handler.obtainMessage(MESSAGE_UPDATE_LIST_ICONS,mActivity);
            handler.removeMessages(MESSAGE_UPDATE_LIST_ICONS);
            handler.sendMessageDelayed(message, mFingerUp ? 0 : DELAY_SHOW_LIST_ICONS);

        } else if (scrollState == SCROLL_STATE_FLING) {
            mScrollHandler.removeMessages(MESSAGE_UPDATE_LIST_ICONS);
            if (mListener != null) mListener.onFling();
        }
        mCurrentScrollState = scrollState;
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
                int incrementalDeltaY = mLastMotionY != Integer.MIN_VALUE ? (int) event.getY() - mLastMotionY : (int) event.getY();
                if (mRefreshState == PULL_TO_REFRESH || mRefreshState == RELEASE_TO_REFRESH){
                    if (Math.abs(incrementalDeltaY) >= 1) {
                        final int topPadding = Math.max(mRefreshOriginalTopPadding,(int) ((incrementalDeltaY - mRefreshViewHeight) / 1.7));
                        if (topPadding != mRefreshView.getPaddingTop()) {
                            mRefreshView.setPadding(mRefreshView.getPaddingLeft(), topPadding,
                                    mRefreshView.getPaddingRight(), mRefreshView.getPaddingBottom());
                        }
                    }
                    if (getFirstVisiblePosition() == 0 && incrementalDeltaY > 0) {
                        incrementalDeltaY = Math.min(getHeight() - getPaddingBottom() - getPaddingTop() - 1, incrementalDeltaY);
                        if (getChildAt(0).getTop() >= 0 && incrementalDeltaY >= 0) {
                            return true;
                        }
                    }
                }

                break;
        }

        return super.onTouchEvent(event);

    }


    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (getFirstVisiblePosition() == 0 && (mRefreshState == TAP_TO_REFRESH || mRefreshState == DONE_REFRESHING)) {
            // not enough views to fill list so pad with an empty view
            final int lastDataPosition = getWrapper().getCount(); // data index + header
            if (getLastVisiblePosition() >= lastDataPosition) {
                mFooterView.getLayoutParams().height = getHeight() -
                        (getChildAt(lastDataPosition).getBottom() - getChildAt(1).getTop());

                if (mRefreshState == TAP_TO_REFRESH) { // instant set selection, must be first layout
                    setSelection(1);
                    return;
                }
            } else if (mFooterView.getLayoutParams().height != 0){
                mFooterView.getLayoutParams().height = 0;
            }

            if (mRefreshState == DONE_REFRESHING) {
                post(new Runnable() {
                    @Override public void run() {
                        scrollPastHeader();
                    }
                });
                resetHeader();
            } else if (!mAutoScrolling){
                setSelection(1);
            }



        } else if (getLastVisiblePosition() < getWrapper().getCount() && mFooterView.getLayoutParams().height != 0){
            mFooterView.getLayoutParams().height = 0;
        }
    }




     @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX,
                                   int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        return false; // no traditional overscrolling
    }

    private void checkHeaderVisibility() {
        if (getFirstVisiblePosition() == 0 && mRefreshState == TAP_TO_REFRESH && mSelectionRunnable == null) {
            post(new Runnable() {
                @Override
                public void run() {
                    setSelectionFromTop(1, 0);
                }
            });
        }
    }


    private void scrollPastHeader() {
        if (Build.VERSION.SDK_INT >= 8){
            smoothScrollBy(getChildAt(1).getTop() + 1, HEADER_HIDE_DURATION);
            mAutoScrolling = true;
            postDelayed(new Runnable() {
                @Override public void run() {
                    mAutoScrolling = false;
                    checkHeaderVisibility();
                }
            }, HEADER_HIDE_DURATION);
        } else {
            setSelection(1);
        }
    }

    private void resetHeaderPadding() {
        mRefreshView.setPadding(mRefreshView.getPaddingLeft(),mRefreshOriginalTopPadding,
                mRefreshView.getPaddingRight(),mRefreshView.getPaddingBottom());
    }

    private void resetHeader() {
        if (mRefreshState != TAP_TO_REFRESH) {
            mRefreshState = TAP_TO_REFRESH;

            resetHeaderPadding();
            mRefreshViewText.setText(R.string.pull_to_refresh_refreshing_label);
            mRefreshViewImage.setImageDrawable(mPullToRefreshArrow);
            mRefreshViewImage.clearAnimation();
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

    private class OnClickRefreshListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (mRefreshState != REFRESHING) {
                onRefresh();
            }
        }

    }

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

    public interface OnRefreshListener {
        void onRefresh();
        void onConnected();
        boolean needsRefresh();
        boolean isRefreshing();
    }

     public interface LazyListListener {
        void onUserClick(ArrayList<Parcelable> users, int position);
        void onRecordingClick(Recording recording);
        void onTrackClick(List<Parcelable> tracks, int position);
        void onEventClick(ArrayList<Parcelable> events, int position);
        void onFling();
        void onFlingDone();
     }

}
