
package com.soundcloud.android.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Refreshable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.collection.CollectionParams;
import com.soundcloud.android.task.collection.ReturnData;
import com.soundcloud.android.task.collection.UpdateCollectionTask;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.quickaction.QuickAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ScBaseAdapter<T extends ScModel> extends BaseAdapter implements IScAdapter {
    protected Context mContext;
    protected Content mContent;
    protected Uri mContentUri;
    protected List<T> mData;
    protected int mPage = 1;
    protected Map<Long, Drawable> mIconAnimations = new HashMap<Long, Drawable>();
    protected Set<Long> mLoadingIcons = new HashSet<Long>();
    private boolean mIsLoadingData;
    private View mProgressView;
    private String mNextHref;

    @SuppressWarnings("unchecked")
    public ScBaseAdapter(Context context, Uri uri) {
        mContext = context;
        mContent = Content.match(uri);
        mContentUri = uri;
        mData = new ArrayList<T>();
        mProgressView = View.inflate(context, R.layout.list_loading_item, null);
    }

    private List<T> getData() {
        return mData;
    }

    private void setData(List<T> data) {
        mData = data;
        notifyDataSetChanged();
    }

    public int getItemCount() {
        return mData.size();
    }

    @Override
    public boolean isEnabled(int position) {
        return !isPositionOfProgressElement(position);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public int getCount() {
        int count = mData == null ? 0 : mData.size();
        return mIsLoadingData ? count + 1 : count;
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0 && !mIsLoadingData;
    }

    @Override
    public T getItem(int location) {
        return mData.get(location);
    }

    public void setIsLoadingData(boolean isLoadingData) {
        mIsLoadingData = isLoadingData;
    }

    public void setIsLoadingData(boolean isLoadingData, boolean redrawList) {
        mIsLoadingData = isLoadingData;
        if (redrawList) {
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionOfProgressElement(position)) {
            return IGNORE_ITEM_VIEW_TYPE;
        }
        return 0;
    }

    private boolean isPositionOfProgressElement(int position) {
        return mIsLoadingData && position == mData.size();
    }

    @Override
    public long getItemId(int position) {
        Object o = getItem(position);
        if (o instanceof ScModel && ((ScModel) o).id != -1) {
            return ((ScModel) o).id;
        }
        return position;
    }

    @Override
    public View getView(int index, View row, ViewGroup parent) {
        if (isPositionOfProgressElement(index)) {
            return mProgressView;
        }

        LazyRow rowView;
        if (row == null) {
            rowView = createRow(index);
        }  else {
            rowView = (LazyRow) row;
        }
        rowView.display(index, getItem(index));
        return rowView;
    }

    protected abstract LazyRow createRow(int position);

    public void clearData() {
        clearIcons();
        mData.clear();
        mPage = 1;
    }

    public void onDestroy(){}


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

    // needed?
    @Override
    public Content getContent() {
        return mContent;
    }

    @Override
    public QuickAction getQuickActionMenu() {
        return null;
    }

    public boolean needsItems() {
        return getCount() == 0;
    }

    protected void clearIcons(){
        mIconAnimations.clear();
        mLoadingIcons.clear();
    }

    public void onResume() {
    }

    public boolean shouldRequestNextPage(int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        // if loading, subtract the loading item from total count
        boolean lastItemReached = ((mIsLoadingData? totalItemCount - 1 : totalItemCount) > 0)
                && (totalItemCount - visibleItemCount * 2 < firstVisibleItem);

        return !mIsLoadingData && lastItemReached;
    }

    public void addItems(CollectionHolder<T> newItems) {
        mData.addAll(newItems.collection);
    }

    private void addItem(T newItem) {
        getData().add(newItem);
    }

    public CollectionParams getParams(boolean refresh) {
        CollectionParams params = new CollectionParams();
        params.loadModel = mContent.resourceType;
        params.isRefresh = refresh;
        params.startIndex = refresh ? 0 : getItemCount();
        params.contentUri = mContentUri;
        return params;
    }

    public void handleTaskReturnData(ReturnData<T> data) {
        if (data.success) {
            if (data.wasRefresh) {
                onSuccessfulRefresh();
            }
            mNextHref = data.nextHref;

            addItems(data.newItems);
            checkForStaleItems();
        }
        setIsLoadingData(false, true);
    }

    protected void onSuccessfulRefresh() {
        clearData();
    }

    protected void checkForStaleItems() {
        if (!(IOUtils.isWifiConnected(mContext)) || isEmpty()) return;

        Map<Long, Track> trackUpdates = new HashMap<Long, Track>();
        Map<Long, User> userUpdates = new HashMap<Long, User>();
        for (ScModel newItem : mData) {

            if (newItem instanceof Refreshable) {
                ScModel resource = ((Refreshable) newItem).getRefreshableResource();
                if (resource != null) {
                    if (((Refreshable) newItem).isStale()) {
                        if (resource instanceof Track) {
                            trackUpdates.put(resource.id, (Track) resource);
                        } else if (resource instanceof User) {
                            userUpdates.put(resource.id, (User) resource);
                        }
                    }
                }
            }
        }
        if (trackUpdates.size() > 0) {
            UpdateCollectionTask updateCollectionTask = new UpdateCollectionTask(SoundCloudApplication.fromContext(mContext), Track.class);
            updateCollectionTask.setAdapter(this);
            updateCollectionTask.execute(trackUpdates);
        }

        if (userUpdates.size() > 0) {
            UpdateCollectionTask updateCollectionTask = new UpdateCollectionTask(SoundCloudApplication.fromContext(mContext), User.class);
            updateCollectionTask.setAdapter(this);
            updateCollectionTask.execute(userUpdates);
        }
    }

    public abstract void handleListItemClick(int position, long id);
}
