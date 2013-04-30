
package com.soundcloud.android.adapter;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Creation;
import com.soundcloud.android.model.Playlist;
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
import com.soundcloud.android.view.adapter.ListRow;
import com.soundcloud.api.Endpoints;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ScBaseAdapter<T extends ScModel> extends BaseAdapter implements IScAdapter {
    protected Content mContent;
    protected Uri mContentUri;
    @NotNull protected List<T> mData = new ArrayList<T>();
    protected boolean mIsLoadingData;
    protected int mPage;

    private View mProgressView;

    @SuppressWarnings("unchecked")
    public ScBaseAdapter(Context context, Uri uri) {
        mContent = Content.match(uri);
        mContentUri = uri;
        mProgressView = View.inflate(context.getApplicationContext(), R.layout.list_loading_item, null);
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

    public @NotNull List<T> getItems() {
        return mData;
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
        if (position >= mData.size()) return AdapterView.INVALID_ROW_ID;

        T o = getItem(position);
        if (o.getListItemId() != -1) {
            return o.getListItemId();
        }
        return position;
    }

    @Override
    public View getView(int index, View row, ViewGroup parent) {

        if (isPositionOfProgressElement(index)) {
            return mProgressView;
        }

        View rowView;
        if (row == null) {
            rowView = createRow(parent.getContext(), index);
        } else {
            rowView = row;
        }

        if (rowView instanceof ListRow) {
            ((ListRow) rowView).display(index, getItem(index));
        }
        return rowView;
    }

    protected abstract View createRow(Context context, int position);

    public void clearData() {
        mData.clear();
        mPage = 0;
    }

    // needed?
    @Override
    public Content getContent() {
        return mContent;
    }

    /**
     * @return true if there's no data in the adapter, and we're not currently loading data
     */
    public boolean needsItems() {
        return getCount() == 0;
    }


    public void onResume(ScActivity activity) {
        refreshCreationStamps(activity);
    }

    public void refreshCreationStamps(@NotNull Activity activity) {
        for (ScModel resource : mData) {
            if (resource instanceof Creation) {
                ((Creation) resource).refreshTimeSinceCreated(activity);
            }
        }
    }

    public boolean shouldRequestNextPage(int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        // if loading, subtract the loading item from total count
        boolean lastItemReached = ((mIsLoadingData ? totalItemCount - 1 : totalItemCount) > 0)
                && (totalItemCount - visibleItemCount * 2 < firstVisibleItem);

        return !mIsLoadingData && lastItemReached;
    }

    public void addItems(List<T> newItems) {
        mData.addAll(newItems);
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

    public void handleTaskReturnData(ReturnData<T> data, @Nullable Activity activity) {
        if (data.success) {
            if (data.wasRefresh) {
                onSuccessfulRefresh();
            }
            mPage++;
            addItems(data.newItems);

            /*
            if (IOUtils.isWifiConnected(mContext)){
                // prefetch playable artwork
                for (ScModel model : data.newItems){
                    if (model instanceof PlayableHolder){
                        final String artworkUrl = Consts.GraphicSize.formatUriForList(mContext, ((PlayableHolder) model).getPlayable().getArtwork());
                        if (!TextUtils.isEmpty(artworkUrl)) ImageLoader.get(mContext).prefetch(artworkUrl);
                    }
                }
            }
            */

            if (activity != null) {
                checkForStaleItems(activity, mData);
            }
        }
        setIsLoadingData(false);
    }

    protected void onSuccessfulRefresh() {
        clearData();
    }

    protected void checkForStaleItems(@NotNull Context context, List<? extends ScModel> items) {
        if (items.isEmpty()) return;

        final boolean onWifi = IOUtils.isWifiConnected(context);
        Set<Long> trackUpdates = new HashSet<Long>();
        Set<Long> userUpdates = new HashSet<Long>();
        Set<Long> playlistUpdates = new HashSet<Long>();
        for (ScModel newItem : items) {

            if (newItem instanceof Refreshable) {
                Refreshable refreshable = (Refreshable) newItem;
                if (refreshable.isIncomplete() || (onWifi && refreshable.isStale())) {
                    ScResource resource = refreshable.getRefreshableResource();
                    if (resource instanceof Track) {
                        trackUpdates.add(resource.id);
                    } else if (resource instanceof User) {
                        userUpdates.add(resource.id);
                    } else if (resource instanceof Playlist) {
                        playlistUpdates.add(resource.id);
                    }
                }
            }
        }
        final AndroidCloudAPI api = SoundCloudApplication.fromContext(context);
        if (!trackUpdates.isEmpty()) {
            UpdateCollectionTask task = new UpdateCollectionTask(api, Endpoints.TRACKS, trackUpdates);
            task.setAdapter(this);
            task.executeOnThreadPool();
        }

        if (!userUpdates.isEmpty()) {
            UpdateCollectionTask task = new UpdateCollectionTask(api, Endpoints.USERS, userUpdates);
            task.setAdapter(this);
            task.executeOnThreadPool();
        }

        if (!playlistUpdates.isEmpty()) {
            UpdateCollectionTask task = new UpdateCollectionTask(api, Endpoints.PLAYLISTS, playlistUpdates);
            task.setAdapter(this);
            task.executeOnThreadPool("representation", "compact");
        }
    }

    public abstract int handleListItemClick(Context context, int position, long id);

    public interface ItemClickResults {
        int IGNORE = 0;
        int LEAVING = 1;
    }
}
