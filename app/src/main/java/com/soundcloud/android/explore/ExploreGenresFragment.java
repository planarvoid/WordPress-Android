package com.soundcloud.android.explore;

import static com.soundcloud.android.explore.ExploreGenresAdapter.AUDIO_SECTION;
import static com.soundcloud.android.explore.ExploreGenresAdapter.MUSIC_SECTION;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.collections.Section;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.model.ExploreGenresSections;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.ReactiveListComponent;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

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

public class ExploreGenresFragment extends Fragment implements ReactiveListComponent<ConnectableObservable<Section<ExploreGenre>>> {

    @Inject
    EventBus eventBus;
    @Inject
    ExploreTracksOperations exploreOperations;
    @Inject
    ExploreGenresAdapter adapter;
    @Inject
    ListViewController listViewController;

    private ConnectableObservable<Section<ExploreGenre>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();

    public ExploreGenresFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectObservable(buildObservable());
    }

    @Override
    public ConnectableObservable<Section<ExploreGenre>> buildObservable() {
        final ConnectableObservable<Section<ExploreGenre>> observable = exploreOperations.getCategories()
                .mergeMap(GENRES_TO_SECTIONS)
                .observeOn(mainThread())
                .replay();
        observable.subscribe(adapter);
        return observable;
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<Section<ExploreGenre>> observable) {
        this.observable = observable;
        connectionSubscription = observable.connect();
        return connectionSubscription;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_list, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), ExploreTracksCategoryActivity.class);
        int adjustedPosition = position - ((ListView) parent).getHeaderViewsCount();
        ExploreGenre category = adapter.getItem(adjustedPosition);

        eventBus.publish(EventQueue.SCREEN_ENTERED, (String) view.getTag());

        intent.putExtra(ExploreGenre.EXPLORE_GENRE_EXTRA, category);
        intent.putExtra(ExploreTracksFragment.SCREEN_TAG_EXTRA, view.getTag().toString());
        startActivity(intent);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewController.onViewCreated(this, observable, view, adapter);
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        listViewController.onDestroyView();
        super.onDestroyView();
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
