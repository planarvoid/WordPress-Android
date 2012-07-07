
package com.soundcloud.android.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.view.ActivityRow;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.TrackInfoBar;
import com.soundcloud.android.view.UserlistRow;
import com.soundcloud.android.view.quickaction.QuickAction;
import com.soundcloud.android.view.quickaction.QuickTrackMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScBaseAdapter extends BaseAdapter implements IScAdapter {
    public static final int NOTIFY_DELAY = 300;
    protected Context mContext;
    protected Content mContent;
    protected List<Parcelable> mData;
    protected int mPage = 1;
    private QuickAction mQuickActionMenu;

    protected Map<Long, Drawable> mIconAnimations = new HashMap<Long, Drawable>();
    protected Set<Long> mLoadingIcons = new HashSet<Long>();

    private Handler mNotifyHandler = new NotifyHandler();

    @SuppressWarnings("unchecked")
    public ScBaseAdapter(Context context, Content content) {
        mContext = context;
        mContent = content;
        mData = new ArrayList<Parcelable>();

        if (Track.class.isAssignableFrom(content.resourceType) ){
            mQuickActionMenu = new QuickTrackMenu(context, this);
        }
    }

    public void setContent(Content content) {
        mContent = content;
    }

    public List<Parcelable> getData() {
        return mData;
    }

    public void setData(List<Parcelable> data) {
        mData = data;
        notifyDataSetChanged();
    }

    public int getCount() {
        return mData == null ? 0 : mData.size();
    }

    public Object getItem(int location) {
        return mData.get(location);
    }

    // TODO: make MyTracksAdapter#getData() return ALL the data
    public int positionOffset() {
        return 0;
    }

    public long getItemId(int position){
        if (position < getCount()){
            Object o = getItem(position);
            if (o instanceof Activity) {
                return ((Activity) o).created_at.getTime();
            }else if (o instanceof ScModel && ((ScModel) o).id != -1) {
                return ((ScModel) o).id;
            }
        }
            return position;
    }

    public View getView(int index, View row, ViewGroup parent) {
        LazyRow rowView = row instanceof LazyRow ? (LazyRow) row : createRow(index);
        rowView.display(index, (Parcelable) getItem(index));
        return rowView;
    }

    protected LazyRow createRow(int position){
        switch (mContent){
            case TRACK:
            case ME_SOUND_STREAM:
            case ME_EXCLUSIVE_STREAM:
            case ME_FAVORITES:
                return new TrackInfoBar(mContext,this);

            case ME_ACTIVITIES:
                return new ActivityRow(mContext,this);

            case USER:
            case ME_FOLLOWINGS:
                return new UserlistRow(mContext, this);

            case ME_FOLLOWERS:
                return new UserlistRow(mContext,this, true);

            default:
                throw new IllegalArgumentException("No row type available for content " + mContent);
        }
    }

    public void clearData() {
        clearIcons();
        mData.clear();
        mPage = 1;
    }

    public Class<?> getLoadModel() {
        return mContent.resourceType;
    }

    public void onDestroy(){}

    public void addItem(int position, Parcelable newItem) {
        getData().add(position,newItem);
    }
    public void addItem(Parcelable newItem) {
        getData().add(newItem);
    }

    public Drawable getDrawableFromId(Long id){
        return mIconAnimations.get(id);
    }

    public void assignDrawableToId(Long id, Drawable drawable){
        mIconAnimations.put(id, drawable);
    }

    public Boolean getIconNotReady(Long id){
        return mLoadingIcons.contains(id);
    }

    public void setIconNotReady(Long id){
        mLoadingIcons.add(id);
    }

    public void onEndOfList(){

    }

    @Override
    public Content getContent() {
        return mContent;
    }

    @Override
    public QuickAction getQuickActionMenu() {
        return mQuickActionMenu;
    }

    public boolean needsItems() {
        return getCount() == 0;
    }

    public void notifyDataSetChanged() {
        mNotifyHandler.removeMessages(1);
        super.notifyDataSetChanged();
    }

    public void scheduleNotifyDataSetChanged(){
        if (!mNotifyHandler.hasMessages(1)){
            mNotifyHandler.sendMessageDelayed(mNotifyHandler.obtainMessage(1), NOTIFY_DELAY);
        }
    }

    protected void clearIcons(){
        mIconAnimations.clear();
        mLoadingIcons.clear();
    }

    class NotifyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            notifyDataSetChanged();
        }
    }
}
