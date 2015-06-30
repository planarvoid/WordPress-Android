package com.soundcloud.android.collections;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.behavior.Creation;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.collections.tasks.CollectionParams;
import com.soundcloud.android.collections.tasks.ReturnData;
import com.soundcloud.android.collections.tasks.UpdateCollectionTask;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.api.legacy.Endpoints;
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
import java.util.Map;
import java.util.Set;

@Deprecated
@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
public abstract class ScBaseAdapter<T extends ScModel> extends BaseAdapter {
    private static final Predicate<ScModel> PLAYABLE_HOLDER_PREDICATE = new Predicate<ScModel>() {
        @Override
        public boolean apply(ScModel input) {
            return input instanceof PlayableHolder &&
                    ((PlayableHolder) input).getPlayable() instanceof PublicApiTrack;
        }
    };
    protected final Content content;
    protected final Uri contentUri;
    @NotNull protected List<T> data = new ArrayList<>();
    protected boolean isLoadingData;
    protected int page;

    private View progressView;

    @SuppressWarnings("unchecked")
    public ScBaseAdapter(Uri uri) {
        content = Content.match(uri);
        contentUri = uri;
    }

    public int getItemCount() {
        return data.size();
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
        return isLoadingData ? data.size() + 1 : data.size();
    }

    @Override
    public boolean isEmpty() {
        final int count = getCount();
        return count == 0 || (count == 1 && isLoadingData);
    }

    @Override
    public T getItem(int location) {
        return data.get(location);
    }

    public
    @NotNull
    List<T> getItems() {
        return data;
    }

    public void setIsLoadingData(boolean isLoadingData) {
        this.isLoadingData = isLoadingData;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionOfProgressElement(position)) {
            return IGNORE_ITEM_VIEW_TYPE;
        }
        return 0;
    }

    protected List<Urn> toTrackUrn(List<? extends PlayableHolder> filter) {
        return Lists.transform(filter, new Function<PlayableHolder, Urn>() {
            @Override
            public Urn apply(PlayableHolder input) {
                return ((PublicApiTrack) input.getPlayable()).getUrn();
            }
        });
    }

    protected List<? extends PlayableHolder> filterPlayables(List<? extends ScModel> data) {
        return Lists.newArrayList((Iterable<? extends PlayableHolder>) Iterables.filter(data, PLAYABLE_HOLDER_PREDICATE));
    }

    protected boolean isPositionOfProgressElement(int position) {
        return isLoadingData && position == data.size();
    }

    @Override
    public long getItemId(int position) {
        if (position >= data.size()) {
            return AdapterView.INVALID_ROW_ID;
        }

        final T item = getItem(position);
        if (item.getListItemId() != -1) {
            return item.getListItemId();
        }
        return position;
    }

    @Override
    public View getView(int index, View row, ViewGroup parent) {

        if (isPositionOfProgressElement(index)) {
            if (progressView == null) {
                progressView = View.inflate(parent.getContext().getApplicationContext(), R.layout.ak_list_loading_item, null);
            }
            return progressView;
        }

        View rowView;
        if (row == null) {
            rowView = createRow(parent.getContext(), index, parent);
        } else {
            rowView = row;
        }

        bindRow(index, rowView);

        return rowView;
    }

    protected void bindRow(int index, View rowView) {
        if (rowView instanceof ListRow) {
            ((ListRow) rowView).display(index, getItem(index));
        }
    }

    protected abstract View createRow(Context context, int position, ViewGroup parent);

    public void clearData() {
        data.clear();
        page = 0;
    }

    // needed?
    public Content getContent() {
        return content;
    }

    /**
     * @return true if there's no data in the adapter, and we're not currently loading data
     */
    public boolean needsItems() {
        return getCount() == 0;
    }

    public void updateItems(Map<Urn, PublicApiResource> updatedItems) {
        notifyDataSetChanged();
    }


    public void onResume(ScActivity activity) {
        refreshCreationStamps(activity);
    }

    public void onViewCreated() {
        // hook for fragments
    }

    public void onDestroyView() {
        // hook for fragments
    }

    public void refreshCreationStamps(@NotNull Activity activity) {
        for (ScModel resource : data) {
            if (resource instanceof Creation) {
                ((Creation) resource).refreshTimeSinceCreated(activity);
            }
        }
    }

    public boolean shouldRequestNextPage(int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        // if loading, subtract the loading item from total count
        int lookAheadSize = visibleItemCount * 2;
        int itemCount = isLoadingData ? totalItemCount - 1 : totalItemCount; // size without the loading spinner
        boolean lastItemReached = itemCount > 0 && itemCount - lookAheadSize <= firstVisibleItem;

        return !isLoadingData && lastItemReached;
    }

    public void addItems(List<T> newItems) {
        data.addAll(newItems);
    }

    public CollectionParams getParams(boolean refresh) {
        CollectionParams params = new CollectionParams();
        params.loadModel = content.modelType;
        params.isRefresh = refresh;
        params.maxToLoad = Consts.LIST_PAGE_SIZE;
        params.startIndex = refresh ? 0 : page * Consts.LIST_PAGE_SIZE;
        params.contentUri = contentUri;
        return params;
    }

    public void handleTaskReturnData(ReturnData<T> data, @Nullable Activity activity) {
        if (data.success) {
            if (data.wasRefresh) {
                onSuccessfulRefresh();
            }
            page++;
            addItems(data.newItems);

            if (activity != null) {
                checkForStaleItems(activity, this.data);
            }
        }
        setIsLoadingData(false);
    }

    protected void onSuccessfulRefresh() {
        clearData();
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    protected void checkForStaleItems(@NotNull Context context, List<? extends ScModel> items) {
        if (items.isEmpty()) {
            return;
        }

        final boolean onWifi = IOUtils.isWifiConnected(context);
        Set<Long> trackUpdates = new HashSet<>();
        Set<Long> userUpdates = new HashSet<>();
        Set<Long> playlistUpdates = new HashSet<>();
        for (ScModel newItem : items) {

            if (newItem instanceof Refreshable) {
                Refreshable refreshable = (Refreshable) newItem;
                if (refreshable.isIncomplete() || (onWifi && refreshable.isStale())) {
                    Refreshable resource = refreshable.getRefreshableResource();
                    if (resource instanceof PublicApiTrack) {
                        trackUpdates.add(((PublicApiTrack) resource).getId());
                    } else if (resource instanceof PublicApiUser) {
                        userUpdates.add(((PublicApiUser) resource).getId());
                    } else if (resource instanceof PublicApiPlaylist) {
                        playlistUpdates.add(((PublicApiPlaylist) resource).getId());
                    }
                }
            }
        }
        final PublicCloudAPI api = new PublicApi(context);
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

    public abstract int handleListItemClick(Context context, int position, long id, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo);

    public interface ItemClickResults {
        int IGNORE = 0;
        int LEAVING = 1;
    }
}
