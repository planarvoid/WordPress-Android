package com.soundcloud.android.share;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class ShareOperations {

    private static final String SHARE_TYPE = "text/plain";

    private final EventBus eventBus;

    @Inject
    public ShareOperations(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void share(Context context, PropertySet playable, String screenTag, String pageName, Urn pageUrn, PromotedSourceInfo promotedSourceInfo) {
        if (!playable.get(PlayableProperty.IS_PRIVATE)) {
            startShareActivity(context, playable);
            publishShareTracking(playable, screenTag, pageName, pageUrn, promotedSourceInfo);
        }
    }

    private void startShareActivity(Context context, PropertySet playable) {
        context.startActivity(buildShareIntent(context, playable));
    }

    private void publishShareTracking(PropertySet playable, String screen, String pageName, Urn pageUrn, PromotedSourceInfo promotedSourceInfo) {
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromShare(
                screen,
                pageName,
                playable.get(EntityProperty.URN),
                pageUrn,
                promotedSourceInfo,
                EntityMetadata.from(playable)));
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
            return context.getString(R.string.share_tracktitle_artist_link, title, username, permalink);
        } else {
            return context.getString(R.string.share_tracktitle_link, title, permalink);
        }
    }
}
