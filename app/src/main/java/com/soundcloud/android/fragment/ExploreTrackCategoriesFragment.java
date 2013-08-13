package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ExploreTracksCategoriesAdapter;
import com.soundcloud.android.adapter.ItemAdapter;
import com.soundcloud.android.api.ExploreTrackOperations;
import com.soundcloud.android.fragment.behavior.AdapterViewAware;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.ItemObserver;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class ExploreTrackCategoriesFragment extends SherlockFragment implements AdapterView.OnItemClickListener,
        AdapterViewAware<ExploreTracksCategories> {

    private final Observable<ExploreTracksCategories> mCategoriesObservable;
    private ExploreTracksCategoriesAdapter mCategoriesAdapter;
    private ExploreTracksCategoryObserver mItemObserver;
    private EmptyListView mEmptyListView;
    private int mEmptyViewStatus;

    public ExploreTrackCategoriesFragment() {
        this(new ExploreTracksCategoriesAdapter(),
                new ExploreTrackOperations().getCategories().observeOn(ScSchedulers.UI_SCHEDULER));
    }

    public ExploreTrackCategoriesFragment(ExploreTracksCategoriesAdapter trackExploreAdapter,
                                          Observable<ExploreTracksCategories> exploreTracksCategoriesObservable) {
        mCategoriesAdapter = trackExploreAdapter;
        mCategoriesObservable = exploreTracksCategoriesObservable;
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItemObserver = new ExploreTracksCategoryObserver(this);
        mCategoriesObservable.subscribe(mItemObserver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_tracks_categories_fragment, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        mEmptyListView.setStatus(mEmptyViewStatus);

        PullToRefreshListView pullToRefreshListView = (PullToRefreshListView) view.findViewById(R.id.suggested_tracks_categories_list);
        pullToRefreshListView.setOnItemClickListener(this);
        pullToRefreshListView.setAdapter(mCategoriesAdapter);
        pullToRefreshListView.setEmptyView(mEmptyListView);
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        mEmptyListView.setStatus(status);
    }

    @Override
    public ItemAdapter<ExploreTracksCategories> getAdapter() {
        return null;
    }

    private class ExploreTracksCategoryObserver extends ItemObserver<ExploreTracksCategories, ExploreTrackCategoriesFragment> {

        public ExploreTracksCategoryObserver(ExploreTrackCategoriesFragment fragment) {
            super(fragment);
        }

        @Override
        public void onNext(ExploreTrackCategoriesFragment fragment, ExploreTracksCategories item) {
            fragment.mCategoriesAdapter.setExploreTracksCategories(item);
        }

        @Override
        public void onCompleted(ExploreTrackCategoriesFragment fragment) {
            super.onCompleted(fragment);
            fragment.mCategoriesAdapter.notifyDataSetChanged();
        }
    }
}
