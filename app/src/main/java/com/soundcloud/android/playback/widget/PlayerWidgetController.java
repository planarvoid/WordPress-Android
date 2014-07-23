package com.soundcloud.android.playback.widget;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.LegacyTrackOperations;
import com.soundcloud.android.tracks.TrackUrn;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayerWidgetController {

    public static final String ACTION_LIKE_CHANGED = "com.soundcloud.android.widgetLike";
    public static final String EXTRA_IS_LIKE = "isLike";

    private final Context context;
    private final PlayerWidgetPresenter presenter;
    private final PlaySessionStateProvider playSessionsStateProvider;
    private final PlayQueueManager playQueueManager;
    private final LegacyTrackOperations trackOperations;
    private final SoundAssociationOperations soundAssociationOps;
    private final EventBus eventBus;

    @Inject
    public PlayerWidgetController(Context context, PlayerWidgetPresenter presenter,
                                  PlaySessionStateProvider playSessionsStateProvider, PlayQueueManager playQueueManager,
                                  LegacyTrackOperations trackOperations, SoundAssociationOperations soundAssocicationOps, EventBus eventBus) {
        this.context = context;
        this.presenter = presenter;
        this.playSessionsStateProvider = playSessionsStateProvider;
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.soundAssociationOps = soundAssocicationOps;
        this.eventBus = eventBus;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new PlayableChangedSubscriber());
        eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber());
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlaybackStateSubscriber());

        eventBus.queue(EventQueue.PLAY_QUEUE_TRACK).subscribe(new PlayQueueTrackSubscriber());
    }

    public void update() {
        updatePlayState();
        updatePlayableInformation();
    }

    private void updatePlayState() {
        presenter.updatePlayState(context, playSessionsStateProvider.isPlaying());
    }

    private void updatePlayableInformation() {
        TrackUrn currentTrackUrn = playQueueManager.getCurrentTrackUrn();
        if (TrackUrn.NOT_SET.equals(currentTrackUrn)) {
            presenter.reset(context);
        } else {
            loadTrack(currentTrackUrn).subscribe(new CurrentTrackSubscriber());
        }
    }

    // TODO: This method is not specific to the widget, it should be done in a more generic engagements controller
    public void handleToggleLikeAction(boolean isLiked) {
        fireAndForget(loadCurrentTrack()
                .flatMap(toggleLike(isLiked))
                .observeOn(AndroidSchedulers.mainThread()));
    }

    private Func1<PublicApiTrack, Observable<SoundAssociation>> toggleLike(final boolean isLiked) {
        return new Func1<PublicApiTrack, Observable<SoundAssociation>>() {
            @Override
            public Observable<SoundAssociation> call(PublicApiTrack track) {
                return soundAssociationOps.toggleLike(!isLiked, track);
            }
        };
    }

    private Observable<PublicApiTrack> loadCurrentTrack() {
        return loadTrack(playQueueManager.getCurrentTrackUrn());
    }

    private Observable<PublicApiTrack> loadTrack(TrackUrn trackUrn) {
        return trackOperations.loadTrack(trackUrn.numericId, AndroidSchedulers.mainThread());
    }

    /**
     * Listens for track changes emitted from our application layer via Rx and updates the widget
     * accordingly.
     */
    private final class PlayableChangedSubscriber extends DefaultSubscriber<PlayableChangedEvent> {
        @Override
        public void onNext(PlayableChangedEvent event) {
            final Playable playable = event.getPlayable();

            if (playable.getId() == playQueueManager.getCurrentTrackId()) {
                presenter.updatePlayableInformation(context, playable);
            }
        }
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

    private class PlaybackStateSubscriber extends DefaultSubscriber<Playa.StateTransition> {
        @Override
        public void onNext(Playa.StateTransition state) {
            presenter.updatePlayState(context, state.playSessionIsActive());
        }
    }

    private class PlayQueueTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent event) {
            loadTrack(event.getCurrentTrackUrn()).subscribe(new CurrentTrackSubscriber());
        }
    }

    private class CurrentTrackSubscriber extends DefaultSubscriber<PublicApiTrack> {
        @Override
        public void onNext(PublicApiTrack track) {
            presenter.updatePlayableInformation(context, track);
        }

    }
}
