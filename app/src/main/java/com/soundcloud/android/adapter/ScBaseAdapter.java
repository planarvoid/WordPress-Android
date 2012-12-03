
package com.soundcloud.android.adapter;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Refreshable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.collection.CollectionParams;
import com.soundcloud.android.task.collection.ReturnData;
import com.soundcloud.android.task.collection.UpdateCollectionTask;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.quickaction.QuickAction;
import com.soundcloud.api.Endpoints;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ScBaseAdapter<T extends ScModel> extends BaseAdapter implements IScAdapter {
    protected Context mContext;
    protected Content mContent;
    protected Uri mContentUri;
    @NotNull protected List<T> mData = new ArrayList<T>();
    protected boolean mIsLoadingData;
    protected int mPage;

    private LruCache<Long, Drawable> mIconAnimations = new LruCache<Long, Drawable>(16);
    private Set<Long> mLoadingIcons = new HashSet<Long>();
    private View mProgressView;

    @SuppressWarnings("unchecked")
    public ScBaseAdapter(Context context, Uri uri) {
        mContext = context;
        mContent = Content.match(uri);
        mContentUri = uri;
        mProgressView = View.inflate(context, R.layout.list_loading_item, null);
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
        return mIsLoadingData ? mData.size() + 1 : mData.size();
    }

    @Override
    public boolean isEmpty() {
        final int count = getCount();
        return count == 0 || (count == 1 && mIsLoadingData);
    }

    @Override
    public T getItem(int location) {
        return mData.get(location);
    }

    public void setIsLoadingData(boolean isLoadingData) {
        mIsLoadingData = isLoadingData;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionOfProgressElement(position)) {
            return IGNORE_ITEM_VIEW_TYPE;
        }
        return 0;
    }

    protected boolean isPositionOfProgressElement(int position) {
        return mIsLoadingData && position == mData.size();
    }

    @Override
    public long getItemId(int position) {
        Object o = getItem(position);
        if (o instanceof ScResource && ((ScResource) o).id != -1) {
            return ((ScResource) o).id;
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
        mPage = 0;
    }

    // not used?
    public void onDestroy() {
    }

    public Drawable getDrawableFromId(long id) {
        return mIconAnimations.get(id);
    }

    public void assignDrawableToId(long id, Drawable drawable) {
        mIconAnimations.put(id, drawable);
    }

    public boolean getIconNotReady(long id) {
        return mLoadingIcons.contains(id);
    }

    public void setIconNotReady(long id) {
        mLoadingIcons.add(id);
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
        mIconAnimations.evictAll();
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

    public CollectionParams getParams(boolean refresh) {
        CollectionParams params = new CollectionParams();
        params.loadModel = mContent.modelType;
        params.isRefresh = refresh;
        params.maxToLoad = Consts.COLLECTION_PAGE_SIZE;
        params.startIndex = refresh ? 0 : mPage * Consts.COLLECTION_PAGE_SIZE;
        params.contentUri = mContentUri;
        return params;
    }

    public void handleTaskReturnData(ReturnData<T> data) {
        if (data.success) {
            if (data.wasRefresh) {
                onSuccessfulRefresh();
            }
            mPage++;
            addItems(data.newItems);

            /*
            if (IOUtils.isWifiConnected(mContext)){
                // prefetch sound artwork
                for (ScModel model : data.newItems){
                    if (model instanceof Playable){
                        final String artworkUrl = Consts.GraphicSize.formatUriForList(mContext, ((Playable) model).getTrack().getArtwork());
                        if (!TextUtils.isEmpty(artworkUrl)) ImageLoader.get(mContext).prefetch(artworkUrl);
                    }
                }
            }
            */
            checkForStaleItems(mData);
        }
        setIsLoadingData(false);
    }

    protected void onSuccessfulRefresh() {
        clearData();
    }

    protected void checkForStaleItems(List<? extends ScModel> items) {
        if (items.isEmpty() || !IOUtils.isWifiConnected(mContext)) return;
        Set<Long> trackUpdates = new HashSet<Long>();
        Set<Long> userUpdates = new HashSet<Long>();
        for (ScModel newItem : items) {

            if (newItem instanceof Refreshable) {
                Refreshable refreshable = (Refreshable) newItem;
                if (refreshable.isStale()) {

                    ScResource resource = refreshable.getRefreshableResource();
                    if (resource instanceof Track) {
                        trackUpdates.add(resource.id);
                    } else if (resource instanceof User) {
                        userUpdates.add(resource.id);
                    }
                }
            }
        }
        final AndroidCloudAPI api = SoundCloudApplication.fromContext(mContext);
        if (!trackUpdates.isEmpty()) {
            UpdateCollectionTask task = new UpdateCollectionTask(api, Endpoints.TRACKS);
            task.setAdapter(this);
            task.executeOnThreadPool(trackUpdates);
        }

        if (!userUpdates.isEmpty()) {
            UpdateCollectionTask task = new UpdateCollectionTask(api, Endpoints.USERS);
            task.setAdapter(this);
            task.executeOnThreadPool(userUpdates);
        }
    }

    public abstract int handleListItemClick(int position, long id);

    public interface ItemClickResults{
        int IGNORE = 0;
        int LEAVING = 1;
    }
}
