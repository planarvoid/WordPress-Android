package com.soundcloud.android.paging;

import com.google.common.base.Preconditions;
import com.soundcloud.android.fragment.behavior.AdapterViewAware;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.ScFragmentObserver;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;
import rx.Subscription;

import android.support.v4.app.Fragment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AdapterViewPager<ModelType, FragmentType extends Fragment & AdapterViewAware<ModelType>> {

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

    public void startLoading(final FragmentType fragment) {
        mPagesSub = mPagesObservable.subscribe(new PageObserver(fragment));
    }

    public void setNextPageObservable(Observable<ModelType> nextPageObservable) {
        mNextPageObservable = nextPageObservable;
    }

    public void loadNextPage(final FragmentType fragment) {
        Preconditions.checkState(mNextPageObservable != null, "no next page observable, forgot to set it?");
        mLoadNextPageSub = mNextPageObservable.observeOn(ScSchedulers.UI_SCHEDULER).subscribe(new PageItemObserver(fragment));
    }

    public void unsubscribe() {
        if (mLoadNextPageSub != null) {
            mLoadNextPageSub.unsubscribe();
        }
        if (mPagesSub != null) {
            mPagesSub.unsubscribe();
        }
    }

    public final class PageItemObserver extends ScFragmentObserver<FragmentType, ModelType> {

        public PageItemObserver(FragmentType fragment) {
            super(fragment);
        }

        @Override
        public void onCompleted(FragmentType fragment) {
            fragment.getEmptyView().setStatus(EmptyListView.Status.OK);
            fragment.getAdapter().notifyDataSetChanged();
        }

        @Override
        public void onError(FragmentType fragment, Exception error) {
            fragment.getEmptyView().setStatus(EmptyListView.Status.ERROR);
        }

        @Override
        public void onNext(FragmentType fragment, ModelType item) {
            fragment.getAdapter().addItem(item);
        }
    }

    public final class PageObserver extends ScFragmentObserver<FragmentType, Observable<ModelType>> {
        public PageObserver(FragmentType fragment) {
            super(fragment);
        }

        @Override
        public void onNext(final FragmentType fragment, Observable<ModelType> nextPageObservable) {
            boolean firstPage = isFirstPage();
            setNextPageObservable(nextPageObservable);
            if (firstPage) {
                loadNextPage(fragment);
            }
        }
    }
}
