package com.soundcloud.android.view.adapters;

import static rx.android.OperatorPaged.Page;

import com.soundcloud.android.R;
import rx.Observer;
import rx.Subscription;
import rx.android.OperatorPaged;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

public class PagingItemAdapter<T extends Parcelable> extends ItemAdapter<T>
        implements AbsListView.OnScrollListener, Observer<Page<? extends Iterable<T>>> {

    private final int progressItemLayoutResId;

    private Page<? extends Iterable<T>> currentPage = OperatorPaged.emptyPage();

    private AppendState appendState = AppendState.IDLE;

    protected enum AppendState {
        IDLE, LOADING, ERROR;
    }

    public PagingItemAdapter(CellPresenter<T> cellPresenter, int pageSize) {
        this(cellPresenter, pageSize, R.layout.list_loading_item);
    }

    public PagingItemAdapter(CellPresenter<T> cellPresenter, int pageSize, int progressItemLayoutResId) {
        super(cellPresenter, pageSize);
        this.progressItemLayoutResId = progressItemLayoutResId;
    }

    @Override
    public int getCount() {
        if (items.isEmpty()) {
            return 0;
        } else {
            return appendState == AppendState.IDLE ? items.size() : items.size() + 1;
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != IGNORE_ITEM_VIEW_TYPE;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == IGNORE_ITEM_VIEW_TYPE) {
            if (convertView == null) {
                convertView = View.inflate(parent.getContext(), progressItemLayoutResId, null);
            }
            configureAppendingLayout(convertView);
            return convertView;

        } else {
            return super.getView(position, convertView, parent);
        }
    }

    private void configureAppendingLayout(final View appendingLayout) {
        switch (appendState) {
            case LOADING:
                appendingLayout.setBackgroundResource(android.R.color.transparent);
                appendingLayout.findViewById(R.id.list_loading_view).setVisibility(View.VISIBLE);
                appendingLayout.findViewById(R.id.list_loading_retry_view).setVisibility(View.GONE);
                appendingLayout.setOnClickListener(null);
                break;
            case ERROR:
                appendingLayout.setBackgroundResource(R.drawable.list_selector_gray);
                appendingLayout.findViewById(R.id.list_loading_view).setVisibility(View.GONE);
                appendingLayout.findViewById(R.id.list_loading_retry_view).setVisibility(View.VISIBLE);
                appendingLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadNextPage();
                    }
                });
                break;
            default:
                throw new IllegalStateException("Unexpected idle state with progress row");
        }
    }

    private void setNewAppendState(AppendState newState) {
        appendState = newState;
        notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return appendState != AppendState.IDLE && position == items.size() ? IGNORE_ITEM_VIEW_TYPE
                 : super.getItemViewType(position);
    }

    public Subscription loadNextPage() {
        if (currentPage.hasNextPage()) {
            setNewAppendState(AppendState.LOADING);
            return currentPage.getNextPage().observeOn(AndroidSchedulers.mainThread()).subscribe(this);
        } else {
            return Subscriptions.empty();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (appendState == AppendState.IDLE) {
            int lookAheadSize = visibleItemCount * 2;
            boolean lastItemReached = totalItemCount > 0 && (totalItemCount - lookAheadSize <= firstVisibleItem);

            if (lastItemReached) {
                loadNextPage();
            }
        }
    }

    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
        setNewAppendState(AppendState.ERROR);
    }

    @Override
    public void onNext(Page<? extends Iterable<T>> page) {
        currentPage = page;
        for (T item : page.getPagedCollection()) {
            addItem(item);
        }
        notifyDataSetChanged();
        setNewAppendState(AppendState.IDLE);
    }
}
