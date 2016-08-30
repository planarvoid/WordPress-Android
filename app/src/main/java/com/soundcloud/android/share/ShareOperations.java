package com.soundcloud.android.share;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class ShareOperations {

    private static final String SHARE_TYPE = "text/plain";

    private final EventTracker tracker;

    @Inject
    public ShareOperations(EventTracker tracker) {
        this.tracker = tracker;
    }

    public void share(Context context, PropertySet playable, EventContextMetadata contextMetadata,
                      PromotedSourceInfo promotedSourceInfo) {
        if (!playable.get(PlayableProperty.IS_PRIVATE)) {
            startShareActivity(context, playable);
            publishShareTracking(playable, contextMetadata, promotedSourceInfo);
        }
    }

    private void startShareActivity(Context context, PropertySet playable) {
        context.startActivity(buildShareIntent(context, playable));
    }

    private void publishShareTracking(PropertySet playable, EventContextMetadata contextMetadata,
                                      PromotedSourceInfo promotedSourceInfo) {
        tracker.trackEngagement(UIEvent.fromShare(
                playable.get(EntityProperty.URN),
                contextMetadata,
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

        return Intent.createChooser(shareIntent, context.getString(R.string.share));
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
