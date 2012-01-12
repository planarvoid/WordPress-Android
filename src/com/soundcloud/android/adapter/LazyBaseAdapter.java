
package com.soundcloud.android.adapter;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.soundcloud.android.view.LazyRow;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class LazyBaseAdapter extends BaseAdapter implements IScAdapter {
    public static final int NOTIFY_DELAY = 250;
    protected Context mContext;
    protected LazyEndlessAdapter mWrapper;
    protected List<Parcelable> mData;
    protected int mPage = 1;
    private Class<?> mLoadModel;

    protected Map<Integer, Drawable> mIconAnimations = new HashMap<Integer, Drawable>();
    protected Set<Integer> mLoadingIcons = new HashSet<Integer>();

    private Handler mNotifyHandler = new NotifyHandler();

    @SuppressWarnings("unchecked")
    public LazyBaseAdapter(Context context, List<? extends Parcelable> data, Class<?> model) {
        mData = (List<Parcelable>) data;
        mContext = context;
        mLoadModel = model;
    }

    public void setWrapper(LazyEndlessAdapter wrapper) {
        mWrapper = wrapper;
    }

    public LazyEndlessAdapter getWrapper() {
        return mWrapper;
    }

    public void setModel( Class<?> model ) {
        mLoadModel = model;
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

    public long getItemId(int i) {
        return i;
    }

    public View getView(int index, View row, ViewGroup parent) {
        LazyRow rowView = row instanceof LazyRow ? (LazyRow) row : createRow(index);
        // update the cell renderer, and handle selection state
        rowView.display(index, (Parcelable) getItem(index));
        return rowView;
    }

    protected abstract LazyRow createRow(int position);

    public void reset() {
        mData.clear();
        mPage = 1;
        mIconAnimations.clear();
        mLoadingIcons.clear();
    }

    public Class<?> getLoadModel() {
        return mLoadModel;
    }

    public void onPostQueryExecute() {
        if (mWrapper != null) mWrapper.onPostQueryExecute();
    }

    public void onDestroy(){}

    public void addItem(Parcelable newItem) {
        getData().add(newItem);
    }

    public Drawable getDrawableFromPosition(int position){
        return mIconAnimations.get(position);
    }

    public void assignDrawableToPosition(Integer position, Drawable drawable){
        mIconAnimations.put(position, drawable);
    }

    public Boolean getIconLoading(int position){
        return mLoadingIcons.contains(position);
    }

    public void setIconLoading(Integer position){
        mLoadingIcons.add(position);
    }

    public void onEndOfList(){

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

    class NotifyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            notifyDataSetChanged();
        }
    }
}
