package com.soundcloud.android.paging;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.soundcloud.android.fragment.behavior.PagingAdapterViewAware;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.ScFragmentObserver;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import android.support.v4.app.Fragment;
import android.widget.AbsListView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AdapterViewPager<ModelType, FragmentType extends Fragment & PagingAdapterViewAware<ModelType>> {

    @Nonnull
    private final Observable<Observable<ModelType>> mPagesObservable;

    private Observer<ModelType> mItemObserver;
    private Observer<ModelType> mFirstPageItemObserver;

    @Nullable
    private Observable<ModelType> mNextPageObservable;
    @Nullable
    private Subscription mLoadNextPageSub;
    @Nullable
    private Subscription mPagesSub;

    public AdapterViewPager(@Nonnull Observable<Observable<ModelType>> pagesObservable) {
        mPagesObservable = pagesObservable;
    }

    public boolean isFirstPage() {
        return mNextPageObservable == null;
    }

    public void startLoading(final FragmentType fragment, final Observer<ModelType> itemObserver) {
        startLoading(fragment, itemObserver, itemObserver);
    }

    public void startLoading(final FragmentType fragment,
                             final Observer<ModelType> itemObserver,
                             final Observer<ModelType> firstPageItemObserver) {
        mNextPageObservable = null;
        mItemObserver = itemObserver;
        mFirstPageItemObserver = firstPageItemObserver;
        mPagesSub = mPagesObservable.subscribe(new PageObserver(fragment));
    }

    public void unsubscribe() {
        if (mLoadNextPageSub != null) {
            mLoadNextPageSub.unsubscribe();
        }
        if (mPagesSub != null) {
            mPagesSub.unsubscribe();
        }
    }

    @VisibleForTesting
    protected void loadNextPage(final FragmentType fragment, final Observer<ModelType> itemObserver) {
        Preconditions.checkState(mNextPageObservable != null, "no next page observable, forgot to set it?");
        fragment.getAdapter().setDisplayProgressItem(true);
        mLoadNextPageSub = mNextPageObservable.observeOn(ScSchedulers.UI_SCHEDULER).subscribe(itemObserver);
    }

    private final class PageObserver extends ScFragmentObserver<FragmentType, Observable<ModelType>> {
        public PageObserver(FragmentType fragment) {
            super(fragment);
        }

        @Override
        public void onNext(final FragmentType fragment, Observable<ModelType> nextPageObservable) {
            boolean firstPage = isFirstPage();
            mNextPageObservable = nextPageObservable;
            if (firstPage) {
                loadNextPage(fragment, mFirstPageItemObserver);
            }
        }
    }

    public final class PageScrollListener implements AbsListView.OnScrollListener {

        private FragmentType mFragment;


        public PageScrollListener(FragmentType fragment) {
            this.mFragment = fragment;
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (!mFragment.getAdapter().isDisplayProgressItem()) {
                int lookAheadSize = visibleItemCount * 2;
                boolean lastItemReached = totalItemCount > 0 && (totalItemCount - lookAheadSize <= firstVisibleItem);

                if (lastItemReached) {
                    loadNextPage(mFragment, mItemObserver);
                }
            }
        }
    }
}
