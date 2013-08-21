package com.soundcloud.android.adapter;

import com.soundcloud.android.rx.ScSchedulers;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Action1;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

public abstract class EndlessPagingAdapter<T> extends ScAdapter<T> implements AbsListView.OnScrollListener, Observer<T> {

    private static final int PROGRESS_ITEM_VIEW_TYPE = 1;
    private final int mProgressItemLayoutResId;
    private boolean mDisplayProgressItem;

    private final Observable<Observable<T>> mPagingObservable;
    private Observable<T> mNextPageObservable;
    private Observer<T> mItemObserver;

    public EndlessPagingAdapter(Observable<Observable<T>> pageEmittingObservable, int pageSize, int progressItemLayoutResId) {
        super(pageSize);
        mPagingObservable = pageEmittingObservable;
        mProgressItemLayoutResId = progressItemLayoutResId;
    }

    @Override
    public int getCount() {
        return mItems.isEmpty() ? 0 :
                mDisplayProgressItem ? mItems.size() + 1 : mItems.size();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == PROGRESS_ITEM_VIEW_TYPE) {
            return convertView != null ? convertView : View.inflate(parent.getContext(), mProgressItemLayoutResId, null);
        } else {
            return super.getView(position, convertView, parent);
        }
    }


    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return mDisplayProgressItem && position == mItems.size() ? PROGRESS_ITEM_VIEW_TYPE : super.getItemViewType(position);
    }

    public void setDisplayProgressItem(boolean showProgressItem) {
        mDisplayProgressItem = showProgressItem;
        notifyDataSetChanged();
    }

    public boolean isDisplayingProgressItem() {
        return mDisplayProgressItem;
    }

    public boolean isFirstPage() {
        return mNextPageObservable == null;
    }

    public Subscription subscribe(final Observer<T> itemObserver) {
        mItemObserver = itemObserver;
        return mPagingObservable.subscribe(new PageObserver());
    }

    public Subscription loadNextPage() {
        setDisplayProgressItem(true);
        return mNextPageObservable.observeOn(ScSchedulers.UI_SCHEDULER).subscribe(mItemObserver);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (!isDisplayingProgressItem()) {
            int lookAheadSize = visibleItemCount * 2;
            boolean lastItemReached = totalItemCount > 0 && (totalItemCount - lookAheadSize <= firstVisibleItem);

            if (lastItemReached) {
                loadNextPage();
            }
        }
    }

    public void onCompleted(){
        notifyDataSetChanged();
        setDisplayProgressItem(false);
    }

    public void onError(Exception e){
        // todo
    }

    public void onNext(T item){
        addItem(item);
    }


    private final class PageObserver implements Action1<Observable<T>> {

        @Override
        public void call(Observable<T> nextPageObservable) {
            boolean wasFirstPage = isFirstPage();
            EndlessPagingAdapter.this.mNextPageObservable = nextPageObservable;
            if (wasFirstPage) {
                loadNextPage();
            }
        }
    }
}
