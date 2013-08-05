package com.soundcloud.android.paging;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.soundcloud.android.adapter.EndlessPagingAdapter;
import com.soundcloud.android.fragment.behavior.PagingAdapterViewAware;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.ScFragmentObserver;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import android.support.v4.app.Fragment;
import android.widget.AbsListView;
import android.widget.ListAdapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AdapterViewPager<ModelType, FragmentType extends Fragment & PagingAdapterViewAware<ModelType>> {

    @Nonnull
    private final Observable<Observable<ModelType>> mPagesObservable;

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

    public Subscription subscribe(final FragmentType fragment, final Observer<ModelType> itemObserver) {
        mNextPageObservable = null;
        mPagesSub = mPagesObservable.subscribe(new PageObserver(fragment, itemObserver));
        return mPagesSub;
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
    protected void loadNextPage(final EndlessPagingAdapter<ModelType> adapter, final Observer<ModelType> itemObserver) {
        adapter.setDisplayProgressItem(true);
        mLoadNextPageSub = mNextPageObservable.observeOn(ScSchedulers.UI_SCHEDULER).subscribe(itemObserver);
    }

    private final class PageObserver extends ScFragmentObserver<FragmentType, Observable<ModelType>> {

        private Observer<ModelType> mItemObserver;

        public PageObserver(FragmentType fragment, Observer<ModelType> itemObserver) {
            super(fragment);
            mItemObserver = itemObserver;
        }

        @Override
        public void onNext(final FragmentType fragment, Observable<ModelType> nextPageObservable) {
            boolean firstPage = isFirstPage();
            mNextPageObservable = nextPageObservable;
            if (firstPage) {
                loadNextPage(fragment.getAdapter(), mItemObserver);
            }
        }
    }

    public final class PageScrollListener implements AbsListView.OnScrollListener {

        private Observer<ModelType> mItemObserver;

        public PageScrollListener(Observer<ModelType> itemObserver) {
            mItemObserver = itemObserver;
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            checkArgument(view.getAdapter() instanceof EndlessPagingAdapter,
                    "The adapter view using this scroll listener must use an EndlessPagingAdapter");

            EndlessPagingAdapter<ModelType> adapter = (EndlessPagingAdapter) view.getAdapter();
            if (!adapter.isDisplayProgressItem()) {
                int lookAheadSize = visibleItemCount * 2;
                boolean lastItemReached = totalItemCount > 0 && (totalItemCount - lookAheadSize <= firstVisibleItem);

                if (lastItemReached) {
                    loadNextPage(adapter, mItemObserver);
                }
            }
        }
    }
}
