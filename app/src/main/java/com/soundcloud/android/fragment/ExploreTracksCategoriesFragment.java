package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ExploreTracksCategoryActivity;
import com.soundcloud.android.adapter.ExploreTracksCategoriesAdapter;
import com.soundcloud.android.adapter.ItemAdapter;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.fragment.behavior.AdapterViewAware;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.ExploreTracksCategorySection;
import com.soundcloud.android.model.Section;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.PullToRefreshObserver;
import com.soundcloud.android.rx.observers.ScFragmentObserver;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;
import rx.util.functions.Func1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class ExploreTracksCategoriesFragment extends SherlockFragment implements AdapterView.OnItemClickListener,
        AdapterViewAware<ExploreTracksCategory>, PullToRefreshBase.OnRefreshListener<ListView> {


    private final Observable<Section<ExploreTracksCategory>> mCategoriesObservable;
    private final ExploreTracksCategoriesAdapter mCategoriesAdapter;
    private ItemObserver mItemObserver;
    private EmptyListView mEmptyListView;
    private int mEmptyViewStatus;
    private int mListViewID = R.id.suggested_tracks_categories_list;

    public ExploreTracksCategoriesFragment() {
        this(new ExploreTracksCategoriesAdapter(),
                new ExploreTracksOperations().getCategories().observeOn(ScSchedulers.UI_SCHEDULER));
    }

    public ExploreTracksCategoriesFragment(ExploreTracksCategoriesAdapter trackExploreAdapter,
                                           Observable<ExploreTracksCategories> exploreTracksCategoriesObservable) {
        mCategoriesAdapter = trackExploreAdapter;
        mCategoriesObservable = exploreTracksCategoriesObservable.mapMany(new Func1<ExploreTracksCategories, Observable<Section<ExploreTracksCategory>>>() {
            @Override
            public Observable<Section<ExploreTracksCategory>> call(ExploreTracksCategories categories) {
                return Observable.from(
                        new Section<ExploreTracksCategory>(ExploreTracksCategorySection.MUSIC.getTitleId(), categories.getMusic()),
                        new Section<ExploreTracksCategory>(ExploreTracksCategorySection.AUDIO.getTitleId(), categories.getAudio())
                );
            }
        });
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItemObserver = new ItemObserver(this);
        mCategoriesObservable.subscribe(mItemObserver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_tracks_categories_fragment, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Intent intent = new Intent(getActivity(), ExploreTracksCategoryActivity.class);
        intent.putExtra(ExploreTracksCategory.EXTRA, mCategoriesAdapter.getItem(position));
        startActivity(intent);
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
        if (mEmptyListView != null) {
            mEmptyListView.setStatus(status);
        }
    }

    @Override
    public ItemAdapter<ExploreTracksCategory> getAdapter() {
        return mCategoriesAdapter;
    }

    @Override
    public void onRefresh(PullToRefreshBase<ListView> refreshView) {
        mCategoriesObservable.subscribe(new PullToRefreshObserver<ExploreTracksCategoriesFragment, Section<ExploreTracksCategory>>(
                        this, mListViewID, mCategoriesAdapter, mItemObserver));
    }


    public static class ItemObserver
            extends ScFragmentObserver<ExploreTracksCategoriesFragment, Section<ExploreTracksCategory>> {

        public ItemObserver(ExploreTracksCategoriesFragment fragment) {
            super(fragment);
        }

        @Override
        public void onNext(ExploreTracksCategoriesFragment fragment, Section<ExploreTracksCategory> item) {
            fragment.mCategoriesAdapter.onNext(item);
        }

        @Override
        public void onCompleted(ExploreTracksCategoriesFragment fragment) {
            fragment.setEmptyViewStatus(EmptyListView.Status.OK);
            fragment.mCategoriesAdapter.onCompleted();
        }

        @Override
        public void onError(ExploreTracksCategoriesFragment fragment, Exception error) {
            fragment.setEmptyViewStatus(EmptyListView.Status.ERROR);
            fragment.mCategoriesAdapter.onError(error);
        }
    }



}
