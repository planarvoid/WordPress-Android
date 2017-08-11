package com.soundcloud.android.playback.widget;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventContextMetadata;
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
import com.soundcloud.android.rx.observers.LambdaMaybeObserver;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.optional.Optional;
import io.reactivex.functions.Function;

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
    private final LikeOperations likeOperations;
    private final EngagementsTracking engagementsTracking;

    @Inject
    public PlayerWidgetController(Context context,
                                  PlayerWidgetPresenter presenter,
                                  PlaySessionStateProvider playSessionsStateProvider,
                                  PlayQueueManager playQueueManager,
                                  TrackItemRepository trackItemRepository,
                                  LikeOperations likeOperations,
                                  EngagementsTracking engagementsTracking) {
        this.context = context;
        this.presenter = presenter;
        this.playSessionsStateProvider = playSessionsStateProvider;
        this.playQueueManager = playQueueManager;
        this.trackItemRepository = trackItemRepository;
        this.likeOperations = likeOperations;
        this.engagementsTracking = engagementsTracking;
    }

    public void update() {
        updatePlayState();
        updatePlayableInformation(trackItem -> trackItem);
    }

    private void updatePlayState() {
        presenter.updatePlayState(context, playSessionsStateProvider.isPlaying());
    }

    private void updatePlayableInformation(Function<TrackItem, TrackItem> updateFunction) {
        PlayQueueItem item = playQueueManager.getCurrentPlayQueueItem();
        if (item.isAudioAd()) {
            presenter.updateForAudioAd(context);
        } else if (item.isVideoAd()) {
            presenter.updateForVideoAd(context);
        } else if (item.isTrack()) {
            trackItemRepository.track(item.getUrn())
                               .filter(next -> next != null)
                               .map(updateFunction)
                               .subscribe(LambdaMaybeObserver.onNext(track -> presenter.updateTrackInformation(context, track)));
        } else {
            presenter.reset(context);
        }
    }

    public void handleToggleLikeAction(final boolean addLike) {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        if (currentPlayQueueItem.isTrack()) {
            final Urn currentTrackUrn = currentPlayQueueItem.getUrn();
            likeOperations.toggleLikeAndForget(currentTrackUrn, addLike);

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
                                   .pageName(Screen.WIDGET.get())
                                   .pageUrn(Urn.NOT_SET)
                                   .build();
    }

    void onCurrentUserChanged(CurrentUserChangedEvent event) {
        if (event.isUserRemoved()) {
            presenter.reset(context);
        }
    }

    void onPlaybackStateUpdate(PlayStateEvent state) {
        presenter.updatePlayState(context, state.playSessionIsActive());
    }

    void onCurrentItemChange(CurrentPlayQueueItemEvent event) {
        updatePlayableInformation(trackItem -> trackItem);
    }

    void onTrackMetadataChange(final TrackChangedEvent event) {
        if (!playQueueManager.isQueueEmpty()) {
            for (Track updatedTrack : event.changeMap().values()) {
                if (playQueueManager.isCurrentTrack(updatedTrack.urn())) {
                    updatePlayableInformation(track -> track.updatedWithTrack(updatedTrack));
                }
            }
        }
    }

    void onTrackLikeChange(final LikesStatusEvent event) {
        final Optional<Urn> currentItemUrn = playQueueManager.getCurrentItemUrn();
        if (currentItemUrn.isPresent() && currentItemUrn.get().isTrack()) {
            final Optional<LikesStatusEvent.LikeStatus> likeStatus = event.likeStatusForUrn(currentItemUrn.get());
            if (likeStatus.isPresent()) {
                updatePlayableInformation(track -> track.updatedWithLike(likeStatus.get()));
            }
        }
    }

    void onTrackRepostChange(final RepostsStatusEvent event) {
        final Optional<Urn> currentItemUrn = playQueueManager.getCurrentItemUrn();
        if (currentItemUrn.isPresent() && currentItemUrn.get().isTrack()) {
            final Optional<RepostsStatusEvent.RepostStatus> repostStatus = event.repostStatusForUrn(currentItemUrn.get());
            if (repostStatus.isPresent()) {
                updatePlayableInformation(track -> track.updatedWithRepost(repostStatus.get()));
            }
        }
    }
}
