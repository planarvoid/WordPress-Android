package com.soundcloud.android.adapter;

import static rx.android.OperationPaged.Page;

import com.soundcloud.android.R;
import rx.Observer;
import rx.Subscription;
import rx.android.OperationPaged;
import rx.android.concurrency.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

public abstract class EndlessPagingAdapter<T> extends ScAdapter<T> implements AbsListView.OnScrollListener, Observer<Page<T>> {

    private final int mProgressItemLayoutResId;

    private Page<T> mCurrentPage = OperationPaged.emptyPage();

    private AppendState mAppendState = AppendState.IDLE;

    protected enum AppendState {
        IDLE, LOADING, ERROR;
    }

    public EndlessPagingAdapter(int pageSize) {
        this(pageSize, R.layout.list_loading_item);
    }

    public EndlessPagingAdapter(int pageSize, int progressItemLayoutResId) {
        super(pageSize);
        mProgressItemLayoutResId = progressItemLayoutResId;
    }

    @Override
    public int getCount() {
        if (mItems.isEmpty()) {
            return 0;
        } else {
            return mAppendState == AppendState.IDLE ? mItems.size() : mItems.size() + 1;
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != IGNORE_ITEM_VIEW_TYPE || mAppendState == AppendState.ERROR;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == IGNORE_ITEM_VIEW_TYPE) {
            if (convertView == null) {
                convertView = View.inflate(parent.getContext(), mProgressItemLayoutResId, null);
            }
            configureAppendingLayout(convertView);
            return convertView;

        } else {
            return super.getView(position, convertView, parent);
        }
    }

    private void configureAppendingLayout(final View appendingLayout) {
        switch (mAppendState) {
            case LOADING:
                appendingLayout.setBackgroundResource(android.R.color.transparent);
                appendingLayout.findViewById(R.id.loading).setVisibility(View.VISIBLE);
                appendingLayout.findViewById(R.id.txt_list_loading_retry).setVisibility(View.GONE);
                appendingLayout.setOnClickListener(null);
                break;
            case ERROR:
                appendingLayout.setBackgroundResource(R.drawable.list_selector_background);
                appendingLayout.findViewById(R.id.loading).setVisibility(View.GONE);
                appendingLayout.findViewById(R.id.txt_list_loading_retry).setVisibility(View.VISIBLE);
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
        mAppendState = newState;
        notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return mAppendState != AppendState.IDLE && position == mItems.size() ? IGNORE_ITEM_VIEW_TYPE
                : super.getItemViewType(position);
    }

    public Subscription loadNextPage() {
        if (mCurrentPage.hasNextPage()) {
            setNewAppendState(AppendState.LOADING);
            return mCurrentPage.getNextPage().observeOn(AndroidSchedulers.mainThread()).subscribe(this);
        } else {
            return Subscriptions.empty();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (mAppendState == AppendState.IDLE) {
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
        setNewAppendState(AppendState.ERROR);
    }

    @Override
    public void onNext(Page<T> page) {
        mCurrentPage = page;
        for (T item : page) {
            addItem(item);
        }
        notifyDataSetChanged();
        setNewAppendState(AppendState.IDLE);
    }
}
