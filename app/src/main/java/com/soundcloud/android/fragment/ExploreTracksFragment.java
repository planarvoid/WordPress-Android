package com.soundcloud.android.fragment;

import static rx.android.OperationPaged.Page;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ExploreTracksAdapter;
import com.soundcloud.android.adapter.ItemAdapter;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.fragment.behavior.EmptyViewAware;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.SuggestedTracksCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.ListFragmentObserver;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.EmptyListView;
import org.jetbrains.annotations.NotNull;
import rx.Observer;
import rx.Subscription;
import rx.android.BufferingObserver;
import rx.android.RxFragmentObserver;
import rx.android.concurrency.AndroidSchedulers;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

public class ExploreTracksFragment extends SherlockFragment implements AdapterView.OnItemClickListener,
        EmptyViewAware, PullToRefreshBase.OnRefreshListener<GridView> {

    private static final int GRID_VIEW_ID = R.id.suggested_tracks_grid;
    private EmptyListView mEmptyListView;
    private ExploreTracksAdapter mExploreTracksAdapter;
    private Subscription mSubscription = Subscriptions.empty();
    private ExploreTracksObserver mObserver;

    private int mEmptyViewStatus = EmptyListView.Status.WAITING;
    private PlayUtils mPlayUtils;
    private ConnectableObservable<Page<SuggestedTracksCollection>> mSuggestedTracksObservable;

    public static ExploreTracksFragment fromCategory(ExploreTracksCategory category) {
        final ExploreTracksFragment exploreTracksFragment = new ExploreTracksFragment();
        Bundle args = new Bundle();
        args.putParcelable(ExploreTracksCategory.EXTRA, category);
        exploreTracksFragment.setArguments(args);
        return exploreTracksFragment;
    }

    public ExploreTracksFragment() {
        this(null);
    }

    protected ExploreTracksFragment(ExploreTracksAdapter adapter) {
        mExploreTracksAdapter = adapter;
        setRetainInstance(true);
        mPlayUtils = new PlayUtils();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mObserver = new ExploreTracksObserver(this);
        mSuggestedTracksObservable = buildSuggestedTracksObservable();
    }

    private ConnectableObservable<Page<SuggestedTracksCollection>> buildSuggestedTracksObservable() {
        final ExploreTracksCategory category = getArguments().getParcelable(ExploreTracksCategory.EXTRA);
        return new ExploreTracksOperations().getSuggestedTracks(category)
                .observeOn(AndroidSchedulers.mainThread())
                .replay();
    }

    private void loadTrackSuggestions(ConnectableObservable<Page<SuggestedTracksCollection>> observable, Observer<Page<SuggestedTracksCollection>> fragmentObserver) {
        setEmptyViewStatus(EmptyListView.Status.WAITING);
        final ConnectableObservable<Page<SuggestedTracksCollection>> suggestedTracks = observable;
        suggestedTracks.subscribe(mExploreTracksAdapter);
        suggestedTracks.subscribe(fragmentObserver);
        mSubscription = suggestedTracks.connect();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_tracks_fragment, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Track track = new Track(mExploreTracksAdapter.getItem(position));
        mPlayUtils.playExploreTrack(getActivity(), track, mObserver.getLastExploreTag());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mExploreTracksAdapter = new ExploreTracksAdapter();
        mEmptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        mEmptyListView.setStatus(mEmptyViewStatus);
        mEmptyListView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                loadTrackSuggestions(mSuggestedTracksObservable, mObserver);
            }
        });

        PullToRefreshGridView ptrGridView = (PullToRefreshGridView) view.findViewById(GRID_VIEW_ID);
        ptrGridView.setOnRefreshListener(this);
        GridView gridView = ptrGridView.getRefreshableView();
        gridView.setOnItemClickListener(this);
        gridView.setAdapter(mExploreTracksAdapter);
        gridView.setEmptyView(mEmptyListView);

        // make sure this is called /after/ setAdapter, since the listener requires an EndlessPagingAdapter to be set
        gridView.setOnScrollListener(new AbsListViewParallaxer(new PauseOnScrollListener(ImageLoader.getInstance(), false, true, mExploreTracksAdapter)));

        loadTrackSuggestions(mSuggestedTracksObservable, mObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscription.unsubscribe();
    }

    @Override
    public void onRefresh(PullToRefreshBase<GridView> refreshView) {
        final ConnectableObservable<Page<SuggestedTracksCollection>> refreshObservable = buildSuggestedTracksObservable();
        loadTrackSuggestions(refreshObservable, new PullToRefreshObserver(this, mExploreTracksAdapter, refreshObservable));
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        if (mEmptyListView != null) {
            mEmptyListView.setStatus(status);
        }
    }

    private void onRefreshComplete(@NotNull ConnectableObservable<Page<SuggestedTracksCollection>> newObservable){
        ((PullToRefreshGridView) getView().findViewById(GRID_VIEW_ID)).onRefreshComplete();
        mSuggestedTracksObservable = newObservable;
    }

    private void onRefreshFailed(){
        ((PullToRefreshGridView) getView().findViewById(GRID_VIEW_ID)).onRefreshComplete();
    }

    private static class ExploreTracksObserver extends ListFragmentObserver<ExploreTracksFragment, Page<SuggestedTracksCollection>>{

        private String mLastExploreTag;

        private ExploreTracksObserver(ExploreTracksFragment fragment) {
            super(fragment);
        }

        @Override
        public void onNext(ExploreTracksFragment fragment, Page<SuggestedTracksCollection> element) {
            super.onNext(fragment, element);
            mLastExploreTag = element.getPagedCollection().getTrackingTag();
        }

        private String getLastExploreTag() {
            return mLastExploreTag;
        }
    }

    /**
     * TODO: REPLACE ME
     * Turn this into an RX operator if possible
     */
    private static class PullToRefreshObserver extends BufferingObserver<Page<SuggestedTracksCollection>> {

        private ItemAdapter<?> mAdapter;

        public PullToRefreshObserver(ExploreTracksFragment fragment, ExploreTracksAdapter adapter,
                                     ConnectableObservable<Page<SuggestedTracksCollection>> observable) {
            super(new InnerObserver(fragment, adapter, observable));
            mAdapter = adapter;
        }

        @Override
        public void onCompleted() {
            mAdapter.clear();
            mAdapter.notifyDataSetChanged();
            super.onCompleted();
        }

        // receives the actual observer calls from the outer buffering observer
        private static final class InnerObserver extends RxFragmentObserver<ExploreTracksFragment, Page<SuggestedTracksCollection>> {

            private ExploreTracksAdapter mDelegate;
            private ConnectableObservable<Page<SuggestedTracksCollection>> mNewObservable;

            public InnerObserver(ExploreTracksFragment fragment, ExploreTracksAdapter adapter,
                                 ConnectableObservable<Page<SuggestedTracksCollection>> observable) {
                super(fragment);
                mDelegate = adapter;
                mNewObservable = observable;
            }

            @Override
            public void onNext(ExploreTracksFragment fragment, Page<SuggestedTracksCollection> item) {
                mDelegate.onNext(item);
            }

            @Override
            public void onCompleted(ExploreTracksFragment fragment) {
                mDelegate.onCompleted();
                fragment.onRefreshComplete(mNewObservable);
            }

            @Override
            public void onError(ExploreTracksFragment fragment, Throwable error) {
                mDelegate.onError(error);
                fragment.onRefreshFailed();
            }
        }
    }
}
