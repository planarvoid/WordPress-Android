package com.soundcloud.android.playback.widget;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.track.TrackOperations;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayerWidgetController {

    private final Context context;
    private final PlayerWidgetPresenter presenter;
    private final PlaySessionController playSessionController;
    private final PlayQueueManager playQueueManager;
    private final TrackOperations trackOperations;
    private final SoundAssociationOperations soundAssociationOps;
    private final EventBus eventBus;

    @Inject
    public PlayerWidgetController(Context context, PlayerWidgetPresenter presenter,
                                  PlaySessionController playSessionController, PlayQueueManager playQueueManager,
                                  TrackOperations trackOperations, SoundAssociationOperations soundAssocicationOps, EventBus eventBus) {
        this.context = context;
        this.presenter = presenter;
        this.playSessionController = playSessionController;
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.soundAssociationOps = soundAssocicationOps;
        this.eventBus = eventBus;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new PlayableChangedSubscriber());
        eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber());
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlaybackStateSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber());
    }

    public void update() {
        updatePlayState();
        updatePlayableInformation();
    }

    private void updatePlayState() {
        presenter.performUpdate(context, playSessionController.isPlaying());
    }

    private void updatePlayableInformation() {
        long currentTrackId = playQueueManager.getCurrentTrackId();
        if (currentTrackId == Playable.NOT_SET) {
            presenter.reset(context);
        } else {
            loadTrack(currentTrackId).subscribe(new CurrentTrackSubscriber());
        }
    }

    // TODO: This method is not specific to the widget, it should be done in a more generic engagements controller
    public void handleToggleLikeAction(boolean isLiked) {
        fireAndForget(loadCurrentTrack()
                .flatMap(toggleLike(isLiked))
                .observeOn(AndroidSchedulers.mainThread()));
    }

    private Func1<Track, Observable<SoundAssociation>> toggleLike(final boolean isLiked) {
        return new Func1<Track, Observable<SoundAssociation>>() {
            @Override
            public Observable<SoundAssociation> call(Track track) {
                return soundAssociationOps.toggleLike(!isLiked, track);
            }
        };
    }

    private Observable<Track> loadCurrentTrack() {
        return loadTrack(playQueueManager.getCurrentTrackId());
    }

    private Observable<Track> loadTrack(long trackId) {
        return trackOperations.loadTrack(trackId, AndroidSchedulers.mainThread());
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
                presenter.performUpdate(context, playable);
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
            presenter.performUpdate(context, state.playSessionIsActive());
        }
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            loadCurrentTrack().subscribe(new CurrentTrackSubscriber());
        }
    }

    private class CurrentTrackSubscriber extends DefaultSubscriber<Track> {
        @Override
        public void onNext(Track track) {
            presenter.performUpdate(context, track);
        }

    }
}
