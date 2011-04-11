
package com.soundcloud.android.view;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.MyTracksAdapter;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Recording;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;

import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;


public class LazyListView extends ListView {

    private static final int MESSAGE_UPDATE_LIST_ICONS = 1;
    private static final int DELAY_SHOW_LIST_ICONS = 550;

    private ScActivity mActivity;

    private final Handler mScrollHandler = new ScrollHandler();
    private int mScrollState = ScScrollManager.SCROLL_STATE_IDLE;
    private boolean mFingerUp = true;

    private LazyListListener mListener;

    public LazyListView(ScActivity activity) {
        super(activity);

        mActivity = activity;

        setOnItemClickListener(mOnItemClickListener);
        setOnItemLongClickListener(mOnItemLongClickListener);
        setOnItemSelectedListener(mOnItemSelectedListener);
        setOnScrollListener(new ScScrollManager());
        setOnTouchListener(new FingerTracker());

    }

    public void enableLongClickListener(){
        setOnItemLongClickListener(mOnItemLongClickListener);
    }

    public void disableLongClickListener(){
        setOnItemLongClickListener(null);
    }

    public void setLazyListListener(LazyListListener listener){
        mListener = listener;
    }

    public int getScrollState(){
        return mScrollState;
    }

    @Override
    public ListAdapter getAdapter() {
        if (super.getAdapter() instanceof LazyEndlessAdapter) {
            return ((LazyEndlessAdapter) super.getAdapter()).getWrappedAdapter();
        } else
            return super.getAdapter();
    }

    public LazyEndlessAdapter getWrapper() {
        if (super.getAdapter() instanceof LazyEndlessAdapter) {
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
            if (((LazyBaseAdapter) list.getAdapter()).getCount() <= 0
                    || position >= ((LazyBaseAdapter) list.getAdapter()).getCount())
                return; // bad list item clicked (possibly loading item)

            if (((LazyBaseAdapter) list.getAdapter()).getItem(position) instanceof Track) {

                if (list.getAdapter() instanceof MyTracksAdapter) {
                    position -= ((MyTracksAdapter) list.getAdapter()).getPendingRecordingsCount();
                }

                if (mListener != null){
                    mListener.onTrackClick((ArrayList<Parcelable>) ((LazyBaseAdapter) list.getAdapter()).getData(), position);
                }

            } else if (((LazyBaseAdapter) list.getAdapter()).getItem(position) instanceof Event) {

                if (mListener != null){
                    mListener.onEventClick((ArrayList<Parcelable>) ((LazyBaseAdapter) list.getAdapter()).getData(), position);
                }

            } else if (((LazyBaseAdapter) list.getAdapter()).getItem(position) instanceof User) {

                if (mListener != null){
                    mListener.onUserClick((ArrayList<Parcelable>) ((LazyBaseAdapter) list.getAdapter()).getData(), position);
                }

            } else if (((LazyBaseAdapter) list.getAdapter()).getItem(position) instanceof Recording) {

                if (mListener != null){
                    mListener.onRecordingClick((Recording) ((LazyBaseAdapter) list.getAdapter()).getItem(position));
                }
            }
        }

    };

    protected AdapterView.OnItemLongClickListener mOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {

        public boolean onItemLongClick(AdapterView<?> list, View row, int position, long id) {
            if (((LazyBaseAdapter) list.getAdapter()).getData().size() <= 0
                    || position >= ((LazyBaseAdapter) list.getAdapter()).getData().size())
                return false; // bad list item clicked (possibly loading item)

            ((LazyBaseAdapter) list.getAdapter()).submenuIndex = ((LazyBaseAdapter) list.getAdapter()).animateSubmenuIndex = position;
            ((LazyBaseAdapter) list.getAdapter()).notifyDataSetChanged();
            return true;
        }

    };
    protected AdapterView.OnItemSelectedListener mOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> listView, View view, int position, long id)
        {
            if (((LazyBaseAdapter) listView.getAdapter()).submenuIndex == position)
                listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            else
                listView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        }

        public void onNothingSelected(AdapterView<?> listView)
        {
            // This happens when you start scrolling, so we need to prevent it from staying
            // in the afterDescendants mode if the EditText was focused
            listView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        }
    };

    private class ScScrollManager implements AbsListView.OnScrollListener {
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

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {

        }

    }

    private class FingerTracker implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            mFingerUp = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;
            if (mFingerUp && mScrollState != ScScrollManager.SCROLL_STATE_FLING) {
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

    private void postUpdateListIcons() {
        mActivity.pendingIconsUpdate = true;
        Handler handler = mScrollHandler;
        Message message = handler.obtainMessage(MESSAGE_UPDATE_LIST_ICONS, LazyListView.this);
        handler.removeMessages(MESSAGE_UPDATE_LIST_ICONS);
        handler.sendMessage(message);
    }

    // Define our custom Listener interface
    public interface LazyListListener {
        public abstract void onUserClick(ArrayList<Parcelable> users, int position);
        public abstract void onRecordingClick(Recording recording);
        public abstract void onTrackClick(ArrayList<Parcelable> tracks, int position);
        public abstract void onEventClick(ArrayList<Parcelable> events, int position);
        public abstract void onFling();
        public abstract void onFlingDone();
    }


}
