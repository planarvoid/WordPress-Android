package com.soundcloud.android.collections;

import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.ScModel;
import com.soundcloud.android.collections.tasks.ReturnData;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

@Deprecated
@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
public abstract class ScBaseAdapter<T extends ScModel> extends BaseAdapter {
    @NotNull protected List<T> data = new ArrayList<>();
    protected boolean isLoadingData;
    protected int page;

    private View progressView;

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

    /**
     * @return true if there's no data in the adapter, and we're not currently loading data
     */
    public boolean needsItems() {
        return getCount() == 0;
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
}
