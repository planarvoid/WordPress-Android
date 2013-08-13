package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ExploreTracksCategoriesAdapter;
import com.soundcloud.android.adapter.ItemAdapter;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.fragment.behavior.AdapterViewAware;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.ItemObserver;
import com.soundcloud.android.rx.observers.PullToRefreshObserver;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class ExploreTracksCategoriesFragment extends SherlockFragment implements AdapterView.OnItemClickListener,
        AdapterViewAware<ExploreTracksCategory>, PullToRefreshBase.OnRefreshListener<ListView> {


    private final Observable<ExploreTracksCategory> mCategoriesObservable;
    private ExploreTracksCategoriesAdapter mCategoriesAdapter;
    private ItemObserver mItemObserver;
    private EmptyListView mEmptyListView;
    private int mEmptyViewStatus;
    private int mListViewID = R.id.suggested_tracks_categories_list;

    public ExploreTracksCategoriesFragment() {
        this(new ExploreTracksCategoriesAdapter(),
                new ExploreTracksOperations().getCategories().observeOn(ScSchedulers.UI_SCHEDULER));
    }

    public ExploreTracksCategoriesFragment(ExploreTracksCategoriesAdapter trackExploreAdapter,
                                           Observable<ExploreTracksCategory> exploreTracksCategoriesObservable) {
        mCategoriesAdapter = trackExploreAdapter;
        mCategoriesObservable = exploreTracksCategoriesObservable;
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItemObserver = new ItemObserver<ExploreTracksCategory, ExploreTracksCategoriesFragment>(this);
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

        PullToRefreshListView pullToRefreshListView = (PullToRefreshListView) view.findViewById(mListViewID);
        pullToRefreshListView.setOnItemClickListener(this);
        pullToRefreshListView.setAdapter(mCategoriesAdapter);
        pullToRefreshListView.setEmptyView(mEmptyListView);
        pullToRefreshListView.setOnRefreshListener(this);
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        mEmptyListView.setStatus(status);
    }

    @Override
    public ItemAdapter<ExploreTracksCategory> getAdapter() {
        return mCategoriesAdapter;
    }

    @Override
    public void onRefresh(PullToRefreshBase<ListView> refreshView) {
        ItemObserver<ExploreTracksCategory, ExploreTracksCategoriesFragment> itemObserver =
                new ItemObserver<ExploreTracksCategory, ExploreTracksCategoriesFragment>(this);
        mCategoriesObservable.subscribe(new PullToRefreshObserver<ExploreTracksCategoriesFragment, ExploreTracksCategory>(
                        this, mListViewID, itemObserver));
    }
}
