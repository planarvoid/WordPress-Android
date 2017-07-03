package com.soundcloud.android.discovery.systemplaylist;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.newforyou.NewForYouOperations;
import com.soundcloud.android.playback.ExpandPlayerSingleObserver;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.observers.LambdaSingleObserver;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.UpdatePlayingTrackObserver;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

class SystemPlaylistPresenter extends RecyclerViewPresenter<SystemPlaylist, SystemPlaylistItem> implements TrackItemRenderer.Listener, SystemPlaylistHeaderRenderer.Listener {
    static final int NUM_EXTRA_ITEMS = 1;

    private final SystemPlaylistOperations systemPlaylistOperations;
    private final NewForYouOperations newForYouOperations;
    private final SystemPlaylistAdapter adapter;
    private final PlaybackInitiator playbackInitiator;
    private final Provider<ExpandPlayerSingleObserver> expandPlayerObserverProvider;
    private final Resources resources;
    private final EventBusV2 eventBus;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final EntityItemCreator entityItemCreator;
    private final EventTracker eventTracker;
    private final TrackingStateProvider trackingStateProvider;

    private final PublishSubject<SystemPlaylist> onData = PublishSubject.create();
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean forNewForYou;
    private Urn urn;
    private WeakReference<Activity> activity;

    @Inject
    SystemPlaylistPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                            SystemPlaylistOperations systemPlaylistOperations,
                            NewForYouOperations newForYouOperations,
                            SystemPlaylistAdapterFactory adapterFactory,
                            PlaybackInitiator playbackInitiator,
                            Provider<ExpandPlayerSingleObserver> expandPlayerObserverProvider,
                            Resources resources,
                            EventBusV2 eventBus,
                            PlaySessionStateProvider playSessionStateProvider,
                            EntityItemCreator entityItemCreator,
                            EventTracker eventTracker,
                            TrackingStateProvider trackingStateProvider) {
        super(swipeRefreshAttacher, Options.list().build());

        this.systemPlaylistOperations = systemPlaylistOperations;
        this.newForYouOperations = newForYouOperations;
        this.adapter = adapterFactory.create(this, this);
        this.playbackInitiator = playbackInitiator;
        this.expandPlayerObserverProvider = expandPlayerObserverProvider;
        this.resources = resources;
        this.eventBus = eventBus;
        this.playSessionStateProvider = playSessionStateProvider;
        this.entityItemCreator = entityItemCreator;
        this.eventTracker = eventTracker;
        this.trackingStateProvider = trackingStateProvider;
    }

    @Override
    public void onAttach(Fragment fragment, Activity activity) {
        super.onAttach(fragment, activity);
        this.activity = new WeakReference<>(activity);
    }

    @Override
    public void onDetach(Fragment fragment) {
        this.activity.clear();
        super.onDetach(fragment);
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        init(fragment.getArguments());
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onDestroy(Fragment fragment) {
        disposables.clear();
        super.onDestroy(fragment);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        disposables.add(eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackObserver(adapter)));
        disposables.add(
                Observable.combineLatest(
                        ((SystemPlaylistView) fragment).onEnterScreenTimestamp(),
                        onData,
                        (timestamp, systemPlaylist) -> systemPlaylist)
                          .firstOrError()
                          .map(systemPlaylist -> {
                              if (forNewForYou) {
                                  return ScreenEvent.create(Screen.NEW_FOR_YOU);
                              } else {
                                  return ScreenEvent.createForSystemPlaylist(Screen.SYSTEM_PLAYLIST, systemPlaylist.urn(), systemPlaylist.trackingFeatureName());
                              }
                          })
                          .subscribeWith(LambdaSingleObserver.onNext(screenEvent -> eventTracker.trackScreen(screenEvent, trackingStateProvider.getLastEvent())))
        );
    }

    @Override
    protected CollectionBinding<SystemPlaylist, SystemPlaylistItem> onBuildBinding(Bundle fragmentArgs) {
        final Observable<SystemPlaylist> systemPlaylistObservable;
        if (forNewForYou) {
            systemPlaylistObservable = newForYouOperations.newForYou()
                                                          .map(newForYou -> SystemPlaylistMapper.map(resources, newForYou))
                                                          .toObservable();
        } else {
            systemPlaylistObservable = systemPlaylistOperations.fetchSystemPlaylist(urn)
                                                               .toObservable();
        }

        return buildCollectionBinding(systemPlaylistObservable);
    }

    @Override
    protected CollectionBinding<SystemPlaylist, SystemPlaylistItem> onRefreshBinding() {
        final Observable<SystemPlaylist> refreshObservable;
        if (forNewForYou) {
            refreshObservable = newForYouOperations.refreshNewForYou()
                                                   .map(newForYou -> SystemPlaylistMapper.map(resources, newForYou))
                                                   .toObservable();
        } else {
            refreshObservable = systemPlaylistOperations.fetchSystemPlaylist(urn)
                                                        .toObservable();
        }
        return buildCollectionBinding(refreshObservable);
    }

    private CollectionBinding<SystemPlaylist, SystemPlaylistItem> buildCollectionBinding(Observable<SystemPlaylist> systemPlaylistObservable) {
        return CollectionBinding.fromV2(systemPlaylistObservable.observeOn(AndroidSchedulers.mainThread()), toSystemPlaylistItems())
                                .withAdapter(adapter)
                                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @VisibleForTesting
    Function<SystemPlaylist, ? extends Iterable<SystemPlaylistItem>> toSystemPlaylistItems() {
        return systemPlaylist -> {
            onData.onNext(systemPlaylist);
            final List<SystemPlaylistItem> items = new ArrayList<>(systemPlaylist.tracks().size() + NUM_EXTRA_ITEMS);

            setTitle(systemPlaylist);
            items.add(SystemPlaylistItem.Header.create(systemPlaylist.urn(),
                                                       systemPlaylist.title(),
                                                       systemPlaylist.description(),
                                                       formatMetadata(systemPlaylist.tracks()),
                                                       formatUpdatedAt(systemPlaylist.lastUpdated()),
                                                       systemPlaylist.imageResource(),
                                                       systemPlaylist.queryUrn(),
                                                       systemPlaylist.trackingFeatureName()));

            for (Track track : systemPlaylist.tracks()) {
                final boolean isTrackPlaying = playSessionStateProvider.isCurrentlyPlaying(track.urn());

                final TrackItem.Builder trackItemBuilder = entityItemCreator.trackItem(track).toBuilder();
                if (isTrackPlaying) {
                    trackItemBuilder.isPlaying(true);
                }

                final TrackItem trackItem = trackItemBuilder.build();

                if (forNewForYou) {
                    items.add(SystemPlaylistItem.Track.createNewForYouTrack(systemPlaylist.urn(), trackItem, systemPlaylist.queryUrn(), systemPlaylist.trackingFeatureName()));
                } else {
                    items.add(SystemPlaylistItem.Track.create(systemPlaylist.urn(), trackItem, systemPlaylist.queryUrn(), systemPlaylist.trackingFeatureName()));
                }
            }

            return items;
        };
    }

    private void setTitle(SystemPlaylist systemPlaylist) {
        if (activity != null) {
            Activity activity = this.activity.get();
            if (activity != null) {
                systemPlaylist.title().ifPresent(activity::setTitle);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void init(Bundle fragmentArgs) {
        forNewForYou = fragmentArgs.getBoolean(SystemPlaylistFragment.EXTRA_FOR_NEW_FOR_YOU, false);
        urn = Urns.urnFromBundle(fragmentArgs, SystemPlaylistFragment.EXTRA_PLAYLIST_URN);
        if (!forNewForYou) {
            Preconditions.checkNotNull(urn, "Urn must not be null if not displaying NewForYou");
        }
    }

    @NonNull
    private Optional<String> formatUpdatedAt(Optional<Date> lastUpdated) {
        return lastUpdated.transform(date -> resources.getString(R.string.system_playlist_updated_at, ScTextUtils.formatTimeElapsedSince(resources, date.getTime(), true)));
    }

    private String formatMetadata(List<Track> tracks) {
        long duration = 0;

        for (Track track : tracks) {
            duration += track.fullDuration();
        }

        return resources.getString(R.string.system_playlist_duration, tracks.size(), ScTextUtils.formatTimestamp(duration, TimeUnit.MILLISECONDS));
    }

    @Override
    public void trackItemClicked(Urn urn, int adapterPosition) {
        startPlayback(adapterPosition, adapterPosition - NUM_EXTRA_ITEMS);
    }

    @Override
    public void playClicked() {
        startPlayback(0, 0);
    }

    private void startPlayback(int adapterPosition, int finalPosition) {
        disposables.add(playbackInitiator.playTracks(getTrackUrns(), finalPosition, getPlaySessionSource(adapterPosition, finalPosition))
                                         .subscribeWith(expandPlayerObserverProvider.get()));
    }

    private List<Urn> getTrackUrns() {
        final List<Urn> urns = new ArrayList<>(adapter.getItemCount() - NUM_EXTRA_ITEMS);

        for (SystemPlaylistItem systemPlaylistItem : adapter.getItems()) {
            if (systemPlaylistItem.isTrack()) {
                urns.add(((SystemPlaylistItem.Track) systemPlaylistItem).track().getUrn());
            }
        }

        return urns;
    }

    private PlaySessionSource getPlaySessionSource(int adapterPosition, int playbackPosition) {
        SystemPlaylistItem item = adapter.getItem(adapterPosition);
        if (forNewForYou) {
            return PlaySessionSource.forNewForYou(Screen.NEW_FOR_YOU.get(),
                                                  playbackPosition,
                                                  item.queryUrn().get());
        } else {
            // .kt: val count = adapter.items.filter { it.isTrack() }.count()
            int count = 0;
            for (SystemPlaylistItem systemPlaylistItem : adapter.getItems()) {
                if (systemPlaylistItem.isTrack()) {
                    count++;
                }
            }
            return PlaySessionSource.forSystemPlaylist(Screen.SYSTEM_PLAYLIST.get(),
                                                       item.trackingFeatureName(),
                                                       playbackPosition,
                                                       item.queryUrn().orNull(),
                                                       item.systemPlaylistUrn(),
                                                       count);
        }
    }

    interface SystemPlaylistView {
        Observable<Long> onEnterScreenTimestamp();
    }
}
