package com.soundcloud.android.share;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class ShareOperations {

    private static final String SHARE_TYPE = "text/plain";

    private final EventBus eventBus;
    private final TrackRepository trackRepository;

    @Inject
    public ShareOperations(EventBus eventBus,
                           TrackRepository trackRepository) {
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
    }

    public void shareTrack(Context context, final Urn trackUrn, final String screenTag) {
        trackRepository
                .track(trackUrn)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ShareSubscriber(context, screenTag));
    }

    public void share(Context context, PropertySet playable, String screenTag) {
        if (!playable.get(PlayableProperty.IS_PRIVATE)) {
            startShareActivity(context, playable);
            publishShareTracking(playable, screenTag);
        }
    }

    private void startShareActivity(Context context, PropertySet playable) {
        context.startActivity(buildShareIntent(context, playable));
    }

    private void publishShareTracking(PropertySet playable, String screen) {
        Urn urn = playable.get(EntityProperty.URN);
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromShare(screen, urn, PlayableMetadata.from(playable)));
    }

    private Intent buildShareIntent(Context context, PropertySet playable) {
        String title = playable.get(PlayableProperty.TITLE);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType(SHARE_TYPE);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, title));
        shareIntent.putExtra(Intent.EXTRA_TEXT, buildShareText(context, playable));

        return shareIntent;
    }

    private String buildShareText(Context context, PropertySet playable) {
        String title = playable.get(PlayableProperty.TITLE);
        String username = playable.getOrElse(PlayableProperty.CREATOR_NAME, Strings.EMPTY);
        String permalink = playable.get(PlayableProperty.PERMALINK_URL);

        if (Strings.isNotBlank(username)) {
            return context.getString(R.string.share_track_by_artist_on_soundcloud, title, username, permalink);
        } else {
            return context.getString(R.string.share_track_on_soundcloud, title, permalink);
        }
    }

    class ShareSubscriber extends DefaultSubscriber<PropertySet> {
        private final Context context;
        private final String screenTag;

        ShareSubscriber(Context context, String screenTag) {
            this.context = context;
            this.screenTag = screenTag;
        }

        @Override
        public void onNext(PropertySet track) {
            share(context, track, screenTag);
        }
    }

}
