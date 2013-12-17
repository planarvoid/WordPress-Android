package com.soundcloud.android.explore;

import static com.soundcloud.android.explore.ExploreGenresAdapter.AUDIO_SECTION;
import static com.soundcloud.android.explore.ExploreGenresAdapter.MUSIC_SECTION;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.associations.FriendFinderFragment;
import com.soundcloud.android.collections.Section;
import com.soundcloud.android.dagger.AndroidObservableFactory;
import com.soundcloud.android.dagger.DaggerDependencyInjector;
import com.soundcloud.android.dagger.DependencyInjector;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.model.ExploreGenresSections;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.rx.observers.ListFragmentObserver;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class ExploreGenresFragment extends Fragment implements AdapterView.OnItemClickListener, EmptyViewAware {

    private EmptyListView mEmptyListView;
    private int mEmptyViewStatus;

    @Inject
    AndroidObservableFactory mObservableFactory;

    @Inject
    ExploreGenresAdapter mGenresAdapter;

    @Inject
    ImageOperations mImageOperations;

    private Subscription mSubscription = Subscriptions.empty();
    private ConnectableObservable<Section<ExploreGenre>> mGenresObservable;

    private DependencyInjector mDependencyInjector;

    public ExploreGenresFragment() {
        this(new DaggerDependencyInjector());
    }

    @VisibleForTesting
    protected ExploreGenresFragment(DependencyInjector dependencyInjector) {
        mDependencyInjector = dependencyInjector;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDependencyInjector.inject(this);
        mGenresObservable = buildObservable(mObservableFactory.create(this));
    }

    private ConnectableObservable<Section<ExploreGenre>> buildObservable(Observable<ExploreGenresSections> observable) {
        return observable.mapMany(GENRES_TO_SECTIONS).replay();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.explore_genres_fragment, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), ExploreTracksCategoryActivity.class);
        int adjustedPosition = position - ((ListView) parent).getHeaderViewsCount();
        ExploreGenre category = mGenresAdapter.getItem(adjustedPosition);

        Event.SCREEN_ENTERED.publish(view.getTag());

        intent.putExtra(ExploreGenre.EXPLORE_GENRE_EXTRA, category);
        intent.putExtra(ExploreTracksFragment.SCREEN_TAG_EXTRA, view.getTag().toString());
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
                mGenresObservable = buildObservable(mObservableFactory.create(ExploreGenresFragment.this));
                mSubscription = loadCategories();
            }
        });

        ListView listview = getListView();
        listview.setOnItemClickListener(this);
        listview.setAdapter(mGenresAdapter);
        listview.setEmptyView(mEmptyListView);
        listview.setOnScrollListener(mImageOperations.createScrollPauseListener(false, true));

        mSubscription = loadCategories();
    }

    private ListView getListView() {
        return (ListView) getView().findViewById(R.id.explore_genres_list);
    }

    @Override
    public void onDestroy() {
        mSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        ((ListView) getView().findViewById(R.id.explore_genres_list)).setAdapter(null);
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
        mGenresObservable.subscribe(mGenresAdapter);
        mGenresObservable.subscribe(new ListFragmentObserver<Section<ExploreGenre>>(this));
        return mGenresObservable.connect();
    }

    private static final Func1<ExploreGenresSections, Observable<Section<ExploreGenre>>> GENRES_TO_SECTIONS =
            new Func1<ExploreGenresSections, Observable<Section<ExploreGenre>>>() {
                @Override
                public Observable<Section<ExploreGenre>> call(ExploreGenresSections categories) {
                    return Observable.from(
                            new Section<ExploreGenre>(MUSIC_SECTION, R.string.explore_genre_header_music, categories.getMusic()),
                            new Section<ExploreGenre>(AUDIO_SECTION, R.string.explore_genre_header_audio, categories.getAudio()));
                }
            };
}
