package com.soundcloud.android.fragment;

import static rx.android.AndroidObservables.fromFragment;

import com.google.common.annotations.VisibleForTesting;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ExploreTracksCategoryActivity;
import com.soundcloud.android.adapter.ExploreTracksCategoriesAdapter;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.fragment.behavior.EmptyViewAware;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.ExploreTracksCategorySection;
import com.soundcloud.android.model.Section;
import com.soundcloud.android.rx.observers.ListFragmentObserver;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class ExploreTracksCategoriesFragment extends Fragment implements AdapterView.OnItemClickListener,
        EmptyViewAware {

    private static final Func1<ExploreTracksCategories, Observable<Section<ExploreTracksCategory>>> CATEGORIES_TO_SECTIONS =
            new Func1<ExploreTracksCategories, Observable<Section<ExploreTracksCategory>>>() {
                @Override
                public Observable<Section<ExploreTracksCategory>> call(ExploreTracksCategories categories) {
                    return Observable.from(
                            new Section<ExploreTracksCategory>(ExploreTracksCategorySection.MUSIC.getTitleId(), categories.getMusic()),
                            new Section<ExploreTracksCategory>(ExploreTracksCategorySection.AUDIO.getTitleId(), categories.getAudio())
                    );
                }
            };

    private EmptyListView mEmptyListView;
    private int mEmptyViewStatus;

    private ExploreTracksOperations mTracksOperations;
    private ConnectableObservable<Section<ExploreTracksCategory>> mCategoriesObservable;
    private Subscription mSubscription = Subscriptions.empty();

    public ExploreTracksCategoriesFragment() {
        this(new ExploreTracksOperations());
    }

    @VisibleForTesting
    protected ExploreTracksCategoriesFragment(ExploreTracksOperations operations) {
        mTracksOperations = operations;
        init(buildObservable());
    }

    @VisibleForTesting
    protected ExploreTracksCategoriesFragment(ConnectableObservable<Section<ExploreTracksCategory>> observable) {
        init(observable);
    }

    private void init(ConnectableObservable<Section<ExploreTracksCategory>> observable) {
        mCategoriesObservable = observable;
    }

    private ConnectableObservable<Section<ExploreTracksCategory>> buildObservable() {
        return fromFragment(this, mTracksOperations.getCategories()).mapMany(CATEGORIES_TO_SECTIONS).replay();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_tracks_categories_fragment, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Intent intent = new Intent(getActivity(), ExploreTracksCategoryActivity.class);
        final int adjustedPosition = position - ((ListView) parent).getHeaderViewsCount();
        intent.putExtra(ExploreTracksCategory.EXTRA, getListAdapter().getItem(adjustedPosition));
        startActivity(intent);
    }



    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        mEmptyListView.setStatus(mEmptyViewStatus);
        mEmptyListView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                setEmptyViewStatus(FriendFinderFragment.Status.WAITING);
                mCategoriesObservable = buildObservable();
                mSubscription = loadCategories();
            }
        });

        ListView listview = getListView();
        listview.setOnItemClickListener(this);
        listview.setAdapter(new ExploreTracksCategoriesAdapter());
        listview.setEmptyView(mEmptyListView);
        listview.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), false, true));

        mSubscription = loadCategories();
    }

    private ListView getListView() {
        return (ListView) getView().findViewById(R.id.suggested_tracks_categories_list);
    }

    private ExploreTracksCategoriesAdapter getListAdapter() {
        return (ExploreTracksCategoriesAdapter) getListView().getAdapter();
    }

    @Override
    public void onDestroy() {
        mSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        ((ListView) getView().findViewById(R.id.suggested_tracks_categories_list)).setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        if (mEmptyListView != null) {
            mEmptyListView.setStatus(status);
        }
    }

    private Subscription loadCategories() {
        mCategoriesObservable.subscribe(getListAdapter());
        mCategoriesObservable.subscribe(new ListFragmentObserver<Section<ExploreTracksCategory>>(this));
        return mCategoriesObservable.connect();
    }

}
