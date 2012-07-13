package com.soundcloud.android.view;

import android.content.res.Resources;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;

import com.google.android.imageloader.ImageLoader;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

/*
pull to refresh from : https://github.com/chrisbanes/Android-PullToRefresh/tree/7e918327cad2d217e909147d82882f50c2e3f59a
 */

public class ScListView extends PullToRefreshListView implements AbsListView.OnScrollListener, PullToRefreshBase.OnFlingListener, ImageLoader.LoadBlocker {

    @SuppressWarnings({"UnusedDeclaration"})
    private static final String TAG = "ScListView";

    private boolean mManuallyDetatched;
    private LazyListListener mListener;
    private View mEmptyView;

    public ScListView(ScListActivity activity) {
        super(activity);
        init();
    }

    /**
     * @noinspection UnusedDeclaration
     */
    public ScListView(ScListActivity activity, AttributeSet attrs) {
        super(activity, attrs);
        init();
    }

    private void init() {
        final Resources res = getResources();

        mEmptyView = new EmptyCollection(getContext());
        getRefreshableView().setFadingEdgeLength((int) (2 * res.getDisplayMetrics().density));
        getRefreshableView().setSelector(R.drawable.list_selector_background);
        getRefreshableView().setLongClickable(false);
        getRefreshableView().setScrollingCacheEnabled(false);
        getRefreshableView().setOnItemClickListener(mOnItemClickListener);

        setOnFlingListener(this);
    }

    /*
     We still have to use a custom view controlled by our adapter. Their solution didn't work at the time of integration.
     */
    public final void setCustomEmptyView(View newEmptyView) {
        mEmptyView = newEmptyView;
        configEmptyViewDimensions();
    }

    @Override
    public int getSolidColor() {
        return 0x666666;
    }
    public void setLazyListListener(LazyListListener listener) {
        mListener = listener;
    }
    public void setAdapter(LazyEndlessAdapter adapter, boolean refreshEnabled) {
        getRefreshableView().setAdapter(adapter);
        if (refreshEnabled) setOnRefreshListener(adapter);
    }

    /**
     * Get the data adapter. Could possibly be wrapped twice
     *
     * @return
     */
    public LazyBaseAdapter getBaseAdapter() {
        if (getRefreshableView().getAdapter() == null) return null;
        if (getRefreshableView().getAdapter() instanceof HeaderViewListAdapter &&
                ((HeaderViewListAdapter) getRefreshableView().getAdapter()).getWrappedAdapter() instanceof LazyEndlessAdapter) {
            return ((LazyEndlessAdapter) ((HeaderViewListAdapter) getRefreshableView().getAdapter()).getWrappedAdapter()).getWrappedAdapter();

        } else if (getRefreshableView().getAdapter() instanceof LazyEndlessAdapter) {
            return ((LazyEndlessAdapter) getRefreshableView().getAdapter()).getWrappedAdapter();

        } else
            return null;
    }

    /**
     * Get the endless adapter. Could be wrapped once by a Header/Footer Listview
     *
     * @return
     */
    public LazyEndlessAdapter getWrapper() {
        final ListAdapter adapter = getRefreshableView().getAdapter();
        if (adapter instanceof HeaderViewListAdapter &&
                ((HeaderViewListAdapter) adapter).getWrappedAdapter() instanceof LazyEndlessAdapter) {
            return (LazyEndlessAdapter) ((HeaderViewListAdapter) adapter).getWrappedAdapter();
        } else if (adapter instanceof LazyEndlessAdapter) {
            return (LazyEndlessAdapter) adapter;
        } else {
            return null;
        }
    }

    public void checkForManualDetatch(){
        if (mManuallyDetatched) onAttachedToWindow();
    }

     public void postDetach() {
        // XXX this blows up on ICS, possibly Honeycomb as well
        if (Build.VERSION.SDK_INT < 11) {
            post(new Runnable() {
                @Override
                public void run() {
                    mManuallyDetatched = true;
                    onDetachedFromWindow();
                }
            });
        }
    }

    private final AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> list, View row, int position, long id) {
            if (mListener == null) return;
            position -= getRefreshableView().getHeaderViewsCount();

            LazyEndlessAdapter wrapper = getWrapper();
            LazyBaseAdapter adp = getBaseAdapter();

            final int count = adp.getCount();
            if (count <= 0 || position < 0 || position >= count)
                return; // bad list item clicked (possibly loading item)

            Object item;
            try {
                item = adp.getItem(position);
            } catch (ArrayIndexOutOfBoundsException e) {
                // XXX sometimes throws ArrayIndexOutOfBoundsException
                throw e;
            }

            if (item instanceof Track) {
                mListener.onTrackClick(wrapper, position);
            } else if (item instanceof Activity) {
                mListener.onEventClick((EventsAdapterWrapper) wrapper, position);
            } else if (item instanceof User) {
                mListener.onUserClick((User) item);
            } else if (item instanceof Friend) {
                mListener.onUserClick(((Friend) item).user);
            } else if (item instanceof Comment) {
                mListener.onCommentClick((Comment) item);
            } else if (item instanceof Recording) {
                mListener.onRecordingClick((Recording) item);
            }
        }
    };

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            configEmptyViewDimensions();
        }
    }

    private void configEmptyViewDimensions() {
        if (getHeight() > 0 && mEmptyView != null && mEmptyView.findViewById(R.id.sizer) != null) {
        mEmptyView.findViewById(R.id.sizer).setMinimumHeight(getHeight());
        mEmptyView.findViewById(R.id.sizer).requestLayout();
        }
    }

    public View getCustomEmptyView() {
        return mEmptyView;
    }

    @Override
    public void onFling() {
        ImageLoader.get(getContext()).block(this);
    }

    @Override
    public void onFlingDone() {
        ImageLoader.get(getContext()).unblock(this);
    }

    public interface LazyListListener {
        void onEventClick(EventsAdapterWrapper wrapper, int position);
        void onTrackClick(LazyEndlessAdapter wrapper, int position);
        void onUserClick(User user);
        void onRecordingClick(Recording recording);
        void onCommentClick(Comment comment);
    }


}
