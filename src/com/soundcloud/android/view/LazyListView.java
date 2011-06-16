package com.soundcloud.android.view;

import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.MyTracksAdapter;
import com.soundcloud.android.model.*;

import java.util.ArrayList;


public class LazyListView extends ListView implements AbsListView.OnScrollListener {

    private static final int MESSAGE_UPDATE_LIST_ICONS = 1;
    private static final int DELAY_SHOW_LIST_ICONS = 550;

    private ScActivity mActivity;

    private final Handler mScrollHandler = new ScrollHandler();
    private int mScrollState = SCROLL_STATE_IDLE;
    private boolean mFingerUp = true;

    private LazyListListener mListener;

    public LazyListView(ScActivity activity) {
        super(activity);

        mActivity = activity;

        setOnItemClickListener(mOnItemClickListener);
        setOnItemLongClickListener(mOnItemLongClickListener);
        setOnItemSelectedListener(mOnItemSelectedListener);
        setOnScrollListener(this);
        setOnTouchListener(new FingerTracker());
    }

    @Override
    public int getSolidColor() {
        return 0xAA000000;
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
    protected void layoutChildren() {
        try {
            super.layoutChildren();
        } catch (Exception ignored) {

        }
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
            if (list.getAdapter().getCount() <= 0
                    || position >= list.getAdapter().getCount())
                return false; // bad list item clicked (possibly loading item)

            ((LazyBaseAdapter) list.getAdapter()).submenuIndex = ((LazyBaseAdapter) list.getAdapter()).animateSubmenuIndex = position;
            ((LazyBaseAdapter) list.getAdapter()).notifyDataSetChanged();
            return true;
        }

    };
    protected AdapterView.OnItemSelectedListener mOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> listView, View view, int position, long id) {
            if (((LazyBaseAdapter) listView.getAdapter()).submenuIndex == position)
                listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            else
                listView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        }

        public void onNothingSelected(AdapterView<?> listView) {
            // This happens when you start scrolling, so we need to prevent it from staying
            // in the afterDescendants mode if the EditText was focused
            listView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        }
    };

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mScrollState == SCROLL_STATE_FLING && scrollState != SCROLL_STATE_FLING) {

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
        mScrollState = scrollState;
    }

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }

    private class FingerTracker implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            mFingerUp = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;
            if (mFingerUp && mScrollState != SCROLL_STATE_FLING) {
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

    public interface LazyListListener {
        void onUserClick(ArrayList<Parcelable> users, int position);
        void onRecordingClick(Recording recording);
        void onTrackClick(ArrayList<Parcelable> tracks, int position);
        void onEventClick(ArrayList<Parcelable> events, int position);
        void onFling();
        void onFlingDone();
    }
}
