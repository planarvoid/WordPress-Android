package com.soundcloud.android.adapter;

import com.soundcloud.android.R;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

public abstract class EndlessPagingAdapter<T> extends ScAdapter<T> implements AbsListView.OnScrollListener, Observer<T> {

    private static final int APPEND_ITEM_VIEW_TYPE = 1;
    private final int mProgressItemLayoutResId;

    private final Observable<Observable<T>> mPagingObservable;
    private Observable<T> mNextPageObservable;
    private Observer<T> mDelegateObserver;

    private AppendState mAppendState = AppendState.IDLE;

    protected enum AppendState {
        IDLE, LOADING, ERROR;
    }

    public EndlessPagingAdapter(Observable<Observable<T>> pageEmittingObservable, Observer<T> delegateObserver, int pageSize) {
        super(pageSize);
        mPagingObservable = pageEmittingObservable;
        mDelegateObserver = delegateObserver;
        mProgressItemLayoutResId = R.layout.list_loading_item;
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
        return getItemViewType(position) != APPEND_ITEM_VIEW_TYPE || mAppendState == AppendState.ERROR;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == APPEND_ITEM_VIEW_TYPE) {
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
                appendingLayout.findViewById(R.id.list_loading).setVisibility(View.VISIBLE);
                appendingLayout.findViewById(R.id.txt_list_loading_retry).setVisibility(View.GONE);
                appendingLayout.setOnClickListener(null);
                break;
            case ERROR:
                appendingLayout.setBackgroundResource(R.drawable.list_selector_background);
                appendingLayout.findViewById(R.id.list_loading).setVisibility(View.GONE);
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
        return mAppendState != AppendState.IDLE && position == mItems.size() ? APPEND_ITEM_VIEW_TYPE
                : super.getItemViewType(position);
    }

    public Subscription subscribe() {
        return subscribe(this);
    }

    public Subscription subscribe(final Observer<T> itemObserver) {
        return mPagingObservable.subscribe(new PagingObserver(itemObserver));
    }

    public Subscription loadNextPage() {
        if (mNextPageObservable != null) {
            setNewAppendState(AppendState.LOADING);
            return mNextPageObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(this);
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
        notifyDataSetChanged();
        setNewAppendState(AppendState.IDLE);
        mDelegateObserver.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        setNewAppendState(AppendState.ERROR);
        mDelegateObserver.onError(e);
    }

    @Override
    public void onNext(T item) {
        addItem(item);
        mDelegateObserver.onNext(item);
    }

    private final class PagingObserver implements Action1<Observable<T>> {
        private Observer<T> firstPageObserver;

        public PagingObserver(Observer<T> firstPageObserver) {
            this.firstPageObserver = firstPageObserver;
        }

        @Override
        public void call(Observable<T> nextPageObservable) {
            if (firstPageObserver != null) {
                nextPageObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(firstPageObserver);
                firstPageObserver = null;
            } else {
                EndlessPagingAdapter.this.mNextPageObservable = nextPageObservable;
            }
        }
    }
}
