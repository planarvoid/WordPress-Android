package com.soundcloud.android.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.rx.ScSchedulers;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Action1;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

public abstract class EndlessPagingAdapter<T> extends ScAdapter<T> implements AbsListView.OnScrollListener, Observer<T> {

    private static final int APPEND_ITEM_VIEW_TYPE = 1;
    private final int mProgressItemLayoutResId;

    private final Observable<Observable<T>> mPagingObservable;
    private Observable<T> mNextPageObservable;
    private Observer<T> mItemObserver;

    private AppendState mAppendState = AppendState.IDLE;

    protected enum AppendState {
        IDLE, LOADING, ERROR;
    }
    public EndlessPagingAdapter(Observable<Observable<T>> pageEmittingObservable, int pageSize) {
        super(pageSize);
        mPagingObservable = pageEmittingObservable;
        mProgressItemLayoutResId = R.layout.list_loading_item;
    }

    public void setDefaultItemObserver(Observer<T> itemObserver) {
        mItemObserver = itemObserver;
    }

    @Override
    public int getCount() {
        return mItems.isEmpty() ? 0 :
                mAppendState == AppendState.IDLE ? mItems.size() : mItems.size() + 1;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position)
    {
        return getItemViewType(position) != APPEND_ITEM_VIEW_TYPE || mAppendState == AppendState.ERROR;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == APPEND_ITEM_VIEW_TYPE) {
            if (convertView == null){
                convertView = View.inflate(parent.getContext(), mProgressItemLayoutResId, null);
                convertView.setBackgroundResource(R.drawable.list_selector_background);
            }
            configureAppendingLayout(convertView);
            return convertView;

        } else {
            return super.getView(position, convertView, parent);
        }
    }

    private void configureAppendingLayout(View appendingLayout) {
        switch (mAppendState){
            case LOADING:
                appendingLayout.findViewById(R.id.list_loading).setVisibility(View.VISIBLE);
                appendingLayout.findViewById(R.id.txt_list_loading_retry).setVisibility(View.GONE);
                break;
            case ERROR:
                appendingLayout.findViewById(R.id.list_loading).setVisibility(View.GONE);
                appendingLayout.findViewById(R.id.txt_list_loading_retry).setVisibility(View.VISIBLE);
                break;
            default:
                throw new IllegalStateException("Unexpected idle state with progress row");
        }
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

    public void setNewAppendState(AppendState newState) {
        mAppendState = newState;
        notifyDataSetChanged();
    }

    public Subscription subscribe() {
        return subscribeWithObserver(mItemObserver);
    }

    public Subscription subscribeWithObserver(final Observer<T> firstPageObserver) {
        return mPagingObservable.subscribe(new PagingObserver(firstPageObserver));
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

    private Subscription loadNextPage() {
        setNewAppendState(AppendState.LOADING);
        return mNextPageObservable.observeOn(ScSchedulers.UI_SCHEDULER).subscribe(mItemObserver);
    }

    public void onCompleted(){
        notifyDataSetChanged();
        setNewAppendState(AppendState.IDLE);
    }

    public void onError(Exception e){
        setNewAppendState(AppendState.ERROR);
    }

    public void onNext(T item){
        addItem(item);
    }

    private final class PagingObserver implements Action1<Observable<T>> {
        private Observer<T> firstPageObserver;

        public PagingObserver(Observer<T> firstPageObserver) {
            this.firstPageObserver = firstPageObserver;
        }

        @Override
        public void call(Observable<T> nextPageObservable) {
            if (firstPageObserver != null){
                nextPageObservable.observeOn(ScSchedulers.UI_SCHEDULER).subscribe(firstPageObserver);
                firstPageObserver = null;
            } else {
                EndlessPagingAdapter.this.mNextPageObservable = nextPageObservable;
            }
        }
    }
}
