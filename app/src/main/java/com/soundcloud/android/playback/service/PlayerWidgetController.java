package com.soundcloud.android.playback.service;

import static com.soundcloud.android.rx.observers.RxObserverHelper.fireAndForget;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventBus2;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.DefaultObserver;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

/**
 * A controller class which handles communication between our widget and the application layer.
 * We introduced this as an afterthought and only ported likes to be handled by this guy.
 *
 * Eventually this should process playback events as well.
 */
public class PlayerWidgetController {

    private static PlayerWidgetController instance;

    @Inject
    Context mContext;
    @Inject
    PlaybackStateProvider mPlaybackStateProvider;
    @Inject
    PlayerAppWidgetProvider mWidgetProvider;
    @Inject
    SoundAssociationOperations mSoundAssocicationOps;
    @Inject
    EventBus2 mEventBus;

    private Subscription eventSubscription = Subscriptions.empty();

    public static PlayerWidgetController getInstance(Context context) {
        if (instance == null) {
            instance = new PlayerWidgetController(context);
        }
        return instance;
    }

    @VisibleForTesting
    PlayerWidgetController(Context context, PlaybackStateProvider playbackStateProvider, PlayerAppWidgetProvider provider,
                           EventBus2 eventBus) {
        mContext = context;
        mPlaybackStateProvider = playbackStateProvider;
        mWidgetProvider = provider;
        mEventBus = eventBus;
    }

    private PlayerWidgetController(Context context) {
        SoundCloudApplication application = (SoundCloudApplication) context.getApplicationContext();
        application.getObjectGraph().plus(new PlayerWidgetModule()).inject(this);
    }

    public void subscribe() {
        eventSubscription = mEventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new PlayableChangedObserver());
    }

    @VisibleForTesting
    void unsubscribe() {
        eventSubscription.unsubscribe();
    }

    private void handleWidgetLikeAction(Intent intent) {
        final Track currentTrack = mPlaybackStateProvider.getCurrentTrack();
        if (currentTrack != null) {
            final boolean isLike = intent.getBooleanExtra(PlaybackService.BroadcastExtras.IS_LIKE, false);
            fireAndForget(mSoundAssocicationOps.toggleLike(!isLike, currentTrack)
                    .observeOn(AndroidSchedulers.mainThread()));
        }
    }

    /**
     * Listens for track changes emitted from our application layer via Rx and updates the widget
     * accordingly.
     */
    private final class PlayableChangedObserver extends DefaultObserver<PlayableChangedEvent> {

        @Override
        public void onNext(PlayableChangedEvent event) {
            final Playable playable = event.getPlayable();

            if (playable.getId() == mPlaybackStateProvider.getCurrentTrackId()) {
                mWidgetProvider.performUpdate(mContext, playable, mPlaybackStateProvider.isSupposedToBePlaying());
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
                PlayerWidgetController.getInstance(context).handleWidgetLikeAction(intent);
            }
        }
    }
}
