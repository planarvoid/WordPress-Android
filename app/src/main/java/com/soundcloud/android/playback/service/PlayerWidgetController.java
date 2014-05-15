package com.soundcloud.android.playback.service;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A controller class which handles communication between our widget and the application layer.
 * We introduced this as an afterthought and only ported likes to be handled by this guy.
 * <p/>
 * Eventually this should process playback events as well.
 */
@Singleton
public class PlayerWidgetController {

    private final Context context;
    private final PlaybackStateProvider playbackStateProvider;
    private final PlayerAppWidgetProvider widgetProvider;
    private final SoundAssociationOperations soundAssocicationOps;
    private final EventBus eventBus;

    @Inject
    public PlayerWidgetController(Context context, PlaybackStateProvider playbackStateProvider,
                                  PlayerAppWidgetProvider widgetProvider,
                                  SoundAssociationOperations soundAssociationOps, EventBus eventBus) {
        this.context = context;
        this.playbackStateProvider = playbackStateProvider;
        this.widgetProvider = widgetProvider;
        this.soundAssocicationOps = soundAssociationOps;
        this.eventBus = eventBus;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new PlayableChangedSubscriber());
    }

    private void handleWidgetLikeAction(Intent intent) {
        final Track currentTrack = playbackStateProvider.getCurrentTrack();
        if (currentTrack != null) {
            final boolean isLike = intent.getBooleanExtra(PlaybackService.BroadcastExtras.IS_LIKE, false);
            fireAndForget(soundAssocicationOps.toggleLike(!isLike, currentTrack)
                    .observeOn(AndroidSchedulers.mainThread()));
        }
    }

    /**
     * Listens for track changes emitted from our application layer via Rx and updates the widget
     * accordingly.
     */
    private final class PlayableChangedSubscriber extends DefaultSubscriber<PlayableChangedEvent> {

        @Override
        public void onNext(PlayableChangedEvent event) {
            final Playable playable = event.getPlayable();

            if (playable.getId() == playbackStateProvider.getCurrentTrackId()) {
                widgetProvider.performUpdate(context, playable, playbackStateProvider.isSupposedToBePlaying());
            }
        }
    }

    /**
     * Handles track likes initiated from the widget's remote views.
     */
    public static class PlayerWidgetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PlaybackService.Actions.WIDGET_LIKE_CHANGED.equals(intent.getAction())) {
                final PlayerWidgetController widgetController =
                        SoundCloudApplication.getObjectGraph().get(PlayerWidgetController.class);
                widgetController.handleWidgetLikeAction(intent);
            }
        }
    }
}
