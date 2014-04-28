package com.soundcloud.android.explore;

import static com.soundcloud.android.explore.ExploreGenresAdapter.AUDIO_SECTION;
import static com.soundcloud.android.explore.ExploreGenresAdapter.MUSIC_SECTION;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.collections.Section;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.model.ExploreGenresSections;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.rx.observers.ListFragmentSubscriber;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

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
import java.util.Arrays;

@SuppressLint("ValidFragment")
public class ExploreGenresFragment extends Fragment implements AdapterView.OnItemClickListener, EmptyViewAware {

    private EmptyListView emptyView;
    private int emptyViewStatus;

    @Inject
    EventBus eventBus;

    @Inject
    ExploreTracksOperations exploreOperations;

    @Inject
    ExploreGenresAdapter genresAdapter;

    @Inject
    ImageOperations imageOperations;

    private ConnectableObservable<Section<ExploreGenre>> genresObservable;
    private Subscription genresSubscription = Subscriptions.empty();
    private Subscription listViewSubscription = Subscriptions.empty();

    public ExploreGenresFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    protected ExploreGenresFragment(ExploreTracksOperations exploreOperations, ExploreGenresAdapter adapter,
                                    ImageOperations imageOperations, EventBus eventBus) {
        this.exploreOperations = exploreOperations;
        this.imageOperations = imageOperations;
        this.genresAdapter = adapter;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        genresObservable = prepareGenresObservable();
        genresSubscription = genresObservable.connect();
    }

    private ConnectableObservable<Section<ExploreGenre>> prepareGenresObservable() {
        final ConnectableObservable<Section<ExploreGenre>> observable = exploreOperations.getCategories()
                .mergeMap(GENRES_TO_SECTIONS)
                .observeOn(mainThread())
                .replay();
        // subscribe the adapter immediately; since we retain it, we don't go through unsubscribe/resubscribe so we
        // don't have to deal with duplication issues by loading items into it that were cached by `replay`
        observable.subscribe(genresAdapter);
        return observable;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_list, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), ExploreTracksCategoryActivity.class);
        int adjustedPosition = position - ((ListView) parent).getHeaderViewsCount();
        ExploreGenre category = genresAdapter.getItem(adjustedPosition);

        eventBus.publish(EventQueue.SCREEN_ENTERED, (String) view.getTag());

        intent.putExtra(ExploreGenre.EXPLORE_GENRE_EXTRA, category);
        intent.putExtra(ExploreTracksFragment.SCREEN_TAG_EXTRA, view.getTag().toString());
        startActivity(intent);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyView = (EmptyListView) view.findViewById(android.R.id.empty);
        emptyView.setStatus(emptyViewStatus);
        emptyView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                setEmptyViewStatus(EmptyListView.Status.WAITING);
                genresObservable = prepareGenresObservable();
                listViewSubscription = genresObservable.subscribe(
                        new ListFragmentSubscriber<Section<ExploreGenre>>(ExploreGenresFragment.this));
                genresSubscription = genresObservable.connect();
            }
        });

        ListView listview = getListView();
        listview.setOnItemClickListener(this);
        listview.setAdapter(genresAdapter);
        listview.setEmptyView(emptyView);
        listview.setOnScrollListener(imageOperations.createScrollPauseListener(false, true));

        listViewSubscription = genresObservable.subscribe(new ListFragmentSubscriber<Section<ExploreGenre>>(this));
    }

    private ListView getListView() {
        return (ListView) getView().findViewById(android.R.id.list);
    }

    @Override
    public void onDestroy() {
        genresSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        // always unsubscribe the list view subscriber when we destroy the views
        listViewSubscription.unsubscribe();
        // we keep the adapter subscribed, but detach it as a DataSetObserver from the list view
        ((ListView) getView().findViewById(android.R.id.list)).setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void setEmptyViewStatus(int status) {
        emptyViewStatus = status;
        if (emptyView != null) {
            emptyView.setStatus(status);
        }
    }

    private static final Func1<ExploreGenresSections, Observable<Section<ExploreGenre>>> GENRES_TO_SECTIONS =
            new Func1<ExploreGenresSections, Observable<Section<ExploreGenre>>>() {
                @Override
                public Observable<Section<ExploreGenre>> call(ExploreGenresSections categories) {
                    return Observable.from(Arrays.asList(
                            new Section<ExploreGenre>(MUSIC_SECTION, R.string.explore_genre_header_music, categories.getMusic()),
                            new Section<ExploreGenre>(AUDIO_SECTION, R.string.explore_genre_header_audio, categories.getAudio())));
                }
            };
}
