package com.soundcloud.android.explore;

import static com.soundcloud.android.explore.GenreCellRenderer.AUDIO_SECTION;
import static com.soundcloud.android.explore.GenreCellRenderer.MUSIC_SECTION;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.ReactiveListComponent;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import javax.inject.Inject;
import java.util.Arrays;

@SuppressLint("ValidFragment")
public class ExploreGenresFragment extends LightCycleSupportFragment
        implements ReactiveListComponent<ConnectableObservable<GenreSection<ExploreGenre>>> {

    private static final Func1<ExploreGenresSections, Observable<GenreSection<ExploreGenre>>> GENRES_TO_SECTIONS =
            new Func1<ExploreGenresSections, Observable<GenreSection<ExploreGenre>>>() {
                @Override
                public Observable<GenreSection<ExploreGenre>> call(ExploreGenresSections categories) {
                    return Observable.from(Arrays.asList(
                            new GenreSection<>(MUSIC_SECTION, R.string.explore_genre_header_music, categories.getMusic()),
                            new GenreSection<>(AUDIO_SECTION, R.string.explore_genre_header_audio, categories.getAudio())));
                }
            };

    @Inject ExploreTracksOperations exploreOperations;
    @Inject ExploreGenresAdapter adapter;
    @Inject @LightCycle ListViewController listViewController;
    @Inject EventBus eventBus;

    private ConnectableObservable<GenreSection<ExploreGenre>> observable;
    private Subscription connectionSubscription = RxUtils.invalidSubscription();

    public ExploreGenresFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        listViewController.setAdapter(adapter);
    }

    @VisibleForTesting
    ExploreGenresFragment(ExploreTracksOperations exploreOperations,
                          ExploreGenresAdapter adapter,
                          ListViewController listViewController,
                          EventBus eventBus) {
        this.exploreOperations = exploreOperations;
        this.adapter = adapter;
        this.listViewController = listViewController;
        this.eventBus = eventBus;
        listViewController.setAdapter(adapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectObservable(buildObservable());
    }

    @Override
    public ConnectableObservable<GenreSection<ExploreGenre>> buildObservable() {
        final ConnectableObservable<GenreSection<ExploreGenre>> observable = exploreOperations.getCategories()
                .flatMap(GENRES_TO_SECTIONS)
                .observeOn(mainThread())
                .replay();
        observable.subscribe(new GenreSectionSubscriber());
        return observable;
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<GenreSection<ExploreGenre>> observable) {
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
        ExploreGenre genre = adapter.getItem(adjustedPosition);

        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create((String) view.getTag(), genre));

        intent.putExtra(ExploreGenre.EXPLORE_GENRE_EXTRA, genre);
        intent.putExtra(ExploreTracksFragment.SCREEN_TAG_EXTRA, view.getTag().toString());
        startActivity(intent);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listViewController.connect(this, observable);
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

    private final class GenreSectionSubscriber extends Subscriber<GenreSection<ExploreGenre>> {

        @Override
        public void onNext(GenreSection<ExploreGenre> section) {
            adapter.onNext(section.getItems());
            adapter.demarcateSection(section);
        }

        @Override
        public void onCompleted() {
            adapter.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            adapter.onError(e);
        }
    }
}
