package com.soundcloud.android.playback.widget;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.events.TrackChangedEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;

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
    private final TrackItemRepository trackItemRepository;
    private final EventBus eventBus;
    private final LikeOperations likeOperations;
    private final EngagementsTracking engagementsTracking;

    @Inject
    public PlayerWidgetController(Context context,
                                  PlayerWidgetPresenter presenter,
                                  PlaySessionStateProvider playSessionsStateProvider,
                                  PlayQueueManager playQueueManager,
                                  TrackItemRepository trackItemRepository,
                                  EventBus eventBus,
                                  LikeOperations likeOperations,
                                  EngagementsTracking engagementsTracking) {
        this.context = context;
        this.presenter = presenter;
        this.playSessionsStateProvider = playSessionsStateProvider;
        this.playQueueManager = playQueueManager;
        this.trackItemRepository = trackItemRepository;
        this.eventBus = eventBus;
        this.likeOperations = likeOperations;
        this.engagementsTracking = engagementsTracking;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.TRACK_CHANGED, new TrackMetadataChangeSubscriber());
        eventBus.subscribe(EventQueue.LIKE_CHANGED, new TrackLikeChangeSubscriber());
        eventBus.subscribe(EventQueue.REPOST_CHANGED, new TrackRepostChangeSubscriber());
        eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber());
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlaybackStateSubscriber());
        eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new CurrentItemSubscriber());
    }

    public void update() {
        updatePlayState();
        updatePlayableInformation(UtilityFunctions.identity());
    }

    private void updatePlayState() {
        presenter.updatePlayState(context, playSessionsStateProvider.isPlaying());
    }

    private void updatePlayableInformation(Func1<TrackItem, TrackItem> updateFunction) {
        PlayQueueItem item = playQueueManager.getCurrentPlayQueueItem();
        if (item.isAudioAd()) {
            presenter.updateForAudioAd(context);
        } else if (item.isVideoAd()) {
            presenter.updateForVideoAd(context);
        } else if (item.isTrack()) {
            trackItemRepository.track(item.getUrn())
                           .filter(next -> next != null)
                           .map(updateFunction)
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

    private class CurrentTrackSubscriber extends DefaultSubscriber<TrackItem> {
        @Override
        public void onNext(TrackItem track) {
            presenter.updateTrackInformation(context, track);
        }
    }

    private class CurrentItemSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            updatePlayableInformation(UtilityFunctions.identity());
        }
    }

    private final class TrackMetadataChangeSubscriber extends DefaultSubscriber<TrackChangedEvent> {
        @Override
        public void onNext(final TrackChangedEvent event) {
            if (!playQueueManager.isQueueEmpty()) {
                for (Track updatedTrack : event.changeMap().values()) {
                    if (playQueueManager.isCurrentTrack(updatedTrack.urn())) {
                        updatePlayableInformation(track -> track.updatedWithTrack(updatedTrack));
                    }
                }
            }
        }
    }

    private final class TrackLikeChangeSubscriber extends DefaultSubscriber<LikesStatusEvent> {
        @Override
        public void onNext(final LikesStatusEvent event) {
            final Optional<Urn> currentItemUrn = playQueueManager.getCurrentItemUrn();
            if (currentItemUrn.isPresent() && currentItemUrn.get().isTrack()) {
                final Optional<LikesStatusEvent.LikeStatus> likeStatus = event.likeStatusForUrn(currentItemUrn.get());
                if (likeStatus.isPresent()) {
                    updatePlayableInformation(track -> track.updatedWithLike(likeStatus.get()));
                }
            }
        }
    }

    private final class TrackRepostChangeSubscriber extends DefaultSubscriber<RepostsStatusEvent> {
        @Override
        public void onNext(final RepostsStatusEvent event) {
            final Optional<Urn> currentItemUrn = playQueueManager.getCurrentItemUrn();
            if (currentItemUrn.isPresent() && currentItemUrn.get().isTrack()) {
                final Optional<RepostsStatusEvent.RepostStatus> repostStatus = event.repostStatusForUrn(currentItemUrn.get());
                if (repostStatus.isPresent()) {
                    updatePlayableInformation(track -> track.updatedWithRepost(repostStatus.get()));
                }
            }
        }
    }
}
