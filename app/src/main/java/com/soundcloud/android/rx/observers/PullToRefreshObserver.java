package com.soundcloud.android.rx.observers;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.adapter.ItemAdapter;
import com.soundcloud.android.fragment.behavior.AdapterViewAware;
import rx.android.BufferingObserver;
import rx.android.RxFragmentObserver;

import android.support.v4.app.Fragment;

/**
 * An observer to be used with a pull-to-refresh widget. It ensures that all items are
 * successfully retrieved (it's a {@link BufferingObserver}) before clearing out the adapter,
 * and then forwards calls to an underlying overserver such as a {@link PageItemObserver}.
 *
 * @param <FragmentType>
 * @param <ModelType>
 */
public class PullToRefreshObserver<FragmentType extends Fragment & AdapterViewAware<ModelType>, ModelType>
        extends BufferingObserver<ModelType> {

    private ItemAdapter<ModelType> mAdapter;

    public PullToRefreshObserver(FragmentType fragment, int ptrViewId, RxFragmentObserver<FragmentType, ModelType> delegate) {
        super(new InnerObserver<FragmentType, ModelType>(fragment, ptrViewId, delegate));
        mAdapter = fragment.getAdapter();
    }

    @Override
    public void onCompleted() {
        mAdapter.clear();
        mAdapter.notifyDataSetChanged();
        super.onCompleted();
    }

    // receives the actual observer calls from the outer buffering observer
    private static final class InnerObserver<FragmentType extends Fragment & AdapterViewAware<ModelType>, ModelType>
            extends RxFragmentObserver<FragmentType, ModelType> {

        private final int mPtrViewId;
        private RxFragmentObserver<FragmentType, ModelType> mDelegate;

        public InnerObserver(FragmentType fragment, int ptrViewId, RxFragmentObserver<FragmentType, ModelType> delegate) {
            super(fragment);
            mPtrViewId = ptrViewId;
            mDelegate = delegate;
        }

        @Override
        public void onNext(FragmentType fragment, ModelType item) {
            mDelegate.onNext(fragment, item);
        }

        @Override
        public void onCompleted(FragmentType fragment) {
            mDelegate.onCompleted(fragment);
            findPullToRefreshView(fragment).onRefreshComplete();
        }

        @Override
        public void onError(FragmentType fragment, Exception error) {
            mDelegate.onError(fragment, error);
            findPullToRefreshView(fragment).onRefreshComplete();
        }

        private PullToRefreshBase<?> findPullToRefreshView(FragmentType fragment) {
            return (PullToRefreshBase<?>) fragment.getView().findViewById(mPtrViewId);
        }
    }
}
