package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
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
import rx.android.concurrency.AndroidSchedulers;
import rx.util.functions.Func1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class ExploreTracksCategoriesFragment extends SherlockFragment implements AdapterView.OnItemClickListener,
        EmptyViewAware {


    private Observable<Section<ExploreTracksCategory>> mCategoriesObservable;
    private ExploreTracksCategoriesAdapter mCategoriesAdapter;
    private EmptyListView mEmptyListView;
    private int mEmptyViewStatus;
    private int mListViewID = R.id.suggested_tracks_categories_list;

    public ExploreTracksCategoriesFragment() {
        ListFragmentObserver<ExploreTracksCategoriesFragment, Section<ExploreTracksCategory>> observer =
                new ListFragmentObserver<ExploreTracksCategoriesFragment, Section<ExploreTracksCategory>>(this);

        init(new ExploreTracksCategoriesAdapter(observer),
                new ExploreTracksOperations().getCategories().observeOn(AndroidSchedulers.mainThread()));
    }

    protected ExploreTracksCategoriesFragment(ExploreTracksCategoriesAdapter adapter,
                                              Observable<ExploreTracksCategories> observable) {
        init(adapter, observable);
    }

    private void init(ExploreTracksCategoriesAdapter adapter, Observable<ExploreTracksCategories> observable) {
        mCategoriesAdapter = adapter;
        mCategoriesObservable = observable.mapMany(new Func1<ExploreTracksCategories, Observable<Section<ExploreTracksCategory>>>() {
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
        loadCategories();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_tracks_categories_fragment, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Intent intent = new Intent(getActivity(), ExploreTracksCategoryActivity.class);
        final int adjustedPosition = position - ((ListView) parent).getHeaderViewsCount();
        intent.putExtra(ExploreTracksCategory.EXTRA, mCategoriesAdapter.getItem(adjustedPosition));
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
                loadCategories();
            }
        });

        ListView listview = (ListView) view.findViewById(mListViewID);
        listview.setOnItemClickListener(this);
        listview.setAdapter(mCategoriesAdapter);
        listview.setEmptyView(mEmptyListView);
        listview.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), false, true));
    }

    @Override
    public void onDestroyView() {
        ((ListView) getView().findViewById(mListViewID)).setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        if (mEmptyListView != null) {
            mEmptyListView.setStatus(status);
        }
    }

    private void loadCategories() {
        mCategoriesObservable.subscribe(mCategoriesAdapter);
    }

}
