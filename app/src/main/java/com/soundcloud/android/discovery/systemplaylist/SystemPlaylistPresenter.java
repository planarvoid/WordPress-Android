package com.soundcloud.android.discovery.systemplaylist;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;

import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.newforyou.NewForYouOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

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
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final Resources resources;
    private final EventBus eventBus;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final EntityItemCreator entityItemCreator;

    private final CompositeSubscription subscription = new CompositeSubscription();
    private boolean forNewForYou;
    private Urn urn;
    private WeakReference<Activity> activity;

    @Inject
    SystemPlaylistPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                            SystemPlaylistOperations systemPlaylistOperations,
                            NewForYouOperations newForYouOperations,
                            SystemPlaylistAdapterFactory adapterFactory,
                            PlaybackInitiator playbackInitiator,
                            Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                            Resources resources,
                            EventBus eventBus,
                            PlaySessionStateProvider playSessionStateProvider,
                            EntityItemCreator entityItemCreator) {
        super(swipeRefreshAttacher, Options.list().build());

        this.systemPlaylistOperations = systemPlaylistOperations;
        this.newForYouOperations = newForYouOperations;
        this.adapter = adapterFactory.create(this, this);
        this.playbackInitiator = playbackInitiator;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.resources = resources;
        this.eventBus = eventBus;
        this.playSessionStateProvider = playSessionStateProvider;
        this.entityItemCreator = entityItemCreator;
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
        subscription.unsubscribe();
        super.onDestroy(fragment);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        subscription.add(eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackSubscriber(adapter)));
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
        final Observable<SystemPlaylist> systemPlaylistObservable;
        if (forNewForYou) {
            systemPlaylistObservable = newForYouOperations.refreshNewForYou()
                                                          .map(newForYou -> SystemPlaylistMapper.map(resources, newForYou))
                                                          .toObservable();
        } else {
            systemPlaylistObservable = systemPlaylistOperations.fetchSystemPlaylist(urn)
                                                               .toObservable();
        }
        return buildCollectionBinding(systemPlaylistObservable);
    }

    private CollectionBinding<SystemPlaylist, SystemPlaylistItem> buildCollectionBinding(Observable<SystemPlaylist> systemPlaylistObservable) {
        return CollectionBinding.from(RxJava.toV1Observable(systemPlaylistObservable.observeOn(AndroidSchedulers.mainThread())), toSystemPlaylistItems())
                                .withAdapter(adapter)
                                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @VisibleForTesting
    Func1<SystemPlaylist, ? extends Iterable<SystemPlaylistItem>> toSystemPlaylistItems() {
        return systemPlaylist -> {
            final List<SystemPlaylistItem> items = new ArrayList<>(systemPlaylist.tracks().size() + NUM_EXTRA_ITEMS);

            setTitle(systemPlaylist);
            items.add(SystemPlaylistItem.Header.create(systemPlaylist.title(),
                                                       systemPlaylist.description(),
                                                       formatMetadata(systemPlaylist.tracks()),
                                                       formatUpdatedAt(systemPlaylist.lastUpdated()),
                                                       systemPlaylist.imageResource(),
                                                       systemPlaylist.queryUrn()));

            for (Track track : systemPlaylist.tracks()) {
                final boolean isTrackPlaying = playSessionStateProvider.isCurrentlyPlaying(track.urn());

                final TrackItem.Builder trackItemBuilder = entityItemCreator.trackItem(track).toBuilder();
                if (isTrackPlaying) {
                    trackItemBuilder.isPlaying(true);
                }

                final TrackItem trackItem = trackItemBuilder.build();

                if (forNewForYou) {
                    items.add(SystemPlaylistItem.Track.createNewForYouTrack(trackItem, systemPlaylist.queryUrn()));
                } else {
                    items.add(SystemPlaylistItem.Track.create(trackItem, systemPlaylist.queryUrn()));
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
        urn = fragmentArgs.getParcelable(SystemPlaylistFragment.EXTRA_PLAYLIST_URN);
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
        subscription.add(RxJava.toV1Observable(playbackInitiator.playTracks(getTrackUrns(),
                                                                            finalPosition,
                                                                            getPlaySessionSource(adapterPosition, finalPosition)))
                               .subscribe(expandPlayerSubscriberProvider.get()));
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
        if (forNewForYou) {
            return PlaySessionSource.forNewForYou(Screen.NEW_FOR_YOU.get(),
                                                  playbackPosition,
                                                  adapter.getItem(adapterPosition).queryUrn().get());
        } else {
            return PlaySessionSource.forSystemPlaylist(Screen.SYSTEM_PLAYLIST.get());
        }
    }
}
