package com.soundcloud.android.playback.widget;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.PropertySetFunctions;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayerWidgetController {

    public static final String ACTION_LIKE_CHANGED = BuildConfig.APPLICATION_ID + ".widgetLike";
    public static final String EXTRA_ADD_LIKE = "isLike";

    private final Context context;
    private final PlayerWidgetPresenter presenter;
    private final PlaySessionStateProvider playSessionsStateProvider;
    private final PlayQueueManager playQueueManager;
    private final TrackRepository trackRepository;
    private final EventBus eventBus;
    private final LikeOperations likeOperations;
    private final EngagementsTracking engagementsTracking;

    @Inject
    public PlayerWidgetController(Context context, PlayerWidgetPresenter presenter,
                                  PlaySessionStateProvider playSessionsStateProvider,
                                  PlayQueueManager playQueueManager, TrackRepository trackRepository,
                                  EventBus eventBus, LikeOperations likeOperations,
                                  EngagementsTracking engagementsTracking) {
        this.context = context;
        this.presenter = presenter;
        this.playSessionsStateProvider = playSessionsStateProvider;
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.eventBus = eventBus;
        this.likeOperations = likeOperations;
        this.engagementsTracking = engagementsTracking;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new TrackMetadataChangeSubscriber());
        eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber());
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlaybackStateSubscriber());
        eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new CurrentItemSubscriber());
    }

    public void update() {
        updatePlayState();
        updatePlayableInformation(PropertySet.create());
    }

    private void updatePlayState() {
        presenter.updatePlayState(context, playSessionsStateProvider.isPlaying());
    }

    private void updatePlayableInformation(PropertySet extraInfo) {
        PlayQueueItem item = playQueueManager.getCurrentPlayQueueItem();
        if (item.isAudioAd()) {
            presenter.updateForAudioAd(context);
        } else if (item.isVideoAd()) {
            presenter.updateForVideoAd(context);
        } else if (item.isTrack()) {
            trackRepository.track(item.getUrn())
                           .map(PropertySetFunctions.mergeWith(extraInfo))
                           .subscribe(new CurrentTrackSubscriber());
        } else {
            presenter.reset(context);
        }
    }

    public void handleToggleLikeAction(final boolean addLike) {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        if (currentPlayQueueItem.isTrack()) {
            final Urn currentTrackUrn = currentPlayQueueItem.getUrn();
            fireAndForget(likeOperations.toggleLike(currentTrackUrn, addLike));

            engagementsTracking.likeTrackUrn(currentTrackUrn,
                                             addLike,
                                             getEventMetadata(),
                                             playQueueManager.getCurrentPromotedSourceInfo(currentTrackUrn));
        } else {
            ErrorUtils.handleSilentException(new IllegalStateException(
                    "Tried to like a track from widget with invalid playQueue item"));
        }
    }

    private EventContextMetadata getEventMetadata() {
        return EventContextMetadata.builder()
                                   .invokerScreen(Screen.WIDGET.get())
                                   .contextScreen(playQueueManager.getScreenTag())
                                   .pageName(Screen.WIDGET.get())
                                   .pageUrn(Urn.NOT_SET)
                                   .build();
    }

    /**
     * When the user logs out, reset all widget instances
     */
    private final class CurrentUserChangedSubscriber extends DefaultSubscriber<CurrentUserChangedEvent> {
        @Override
        public void onNext(CurrentUserChangedEvent event) {
            if (event.getKind() == CurrentUserChangedEvent.USER_REMOVED) {
                presenter.reset(context);
            }
        }
    }

    private class PlaybackStateSubscriber extends DefaultSubscriber<PlayStateEvent> {
        @Override
        public void onNext(PlayStateEvent state) {
            presenter.updatePlayState(context, state.playSessionIsActive());
        }
    }

    private class CurrentTrackSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet track) {
            presenter.updateTrackInformation(context, track);
        }
    }

    private class CurrentItemSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            updatePlayableInformation(PropertySet.create());
        }
    }

    private final class TrackMetadataChangeSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(final EntityStateChangedEvent event) {
            if (!playQueueManager.isQueueEmpty() && playQueueManager.isCurrentTrack(event.getFirstUrn())) {
                updatePlayableInformation(event.getNextChangeSet());
            }
        }
    }
}
