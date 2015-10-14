package com.soundcloud.android.collections;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModel;
import com.soundcloud.android.api.legacy.model.behavior.Creation;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.collections.tasks.ReturnData;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

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
        return transform(filter, new Function<PlayableHolder, Urn>() {
            @Override
            public Urn apply(PlayableHolder input) {
                return input.getPlayable().getUrn();
            }
        });
    }

    protected List<? extends PlayableHolder> filterPlayables(List<? extends ScModel> data) {
        return newArrayList((Iterable<? extends PlayableHolder>) Iterables.filter(data, PLAYABLE_HOLDER_PREDICATE));
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

    public void handleTaskReturnData(ReturnData<T> data) {
        if (data.success) {
            if (data.wasRefresh) {
                onSuccessfulRefresh();
            }
            page++;
            addItems(data.newItems);
        }
        setIsLoadingData(false);
    }

    protected void onSuccessfulRefresh() {
        clearData();
    }

    public abstract int handleListItemClick(Context context, int position, long id, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo);

    public interface ItemClickResults {
        int IGNORE = 0;
        int LEAVING = 1;
    }
}
