package com.soundcloud.android.share;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.java.strings.Strings;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class ShareOperations {

    private static final String SHARE_TYPE = "text/plain";

    private final EventTracker eventTracker;

    @Inject
    public ShareOperations(EventTracker eventTracker) {
        this.eventTracker = eventTracker;
    }

    public void share(Context context, PlayableItem playable, EventContextMetadata contextMetadata,
                      PromotedSourceInfo promotedSourceInfo) {
        if (!playable.isPrivate()) {
            share(context,
                  playable.getPermalinkUrl(),
                  contextMetadata, promotedSourceInfo,
                  EntityMetadata.from(playable));
        }
    }

    public void share(final Context context,
                      final String permalink,
                      final EventContextMetadata contextMetadata,
                      final PromotedSourceInfo promotedSourceInfo,
                      final EntityMetadata entityMetadata) {
            startShareActivity(context, entityMetadata.playableTitle, entityMetadata.creatorName, permalink);
            publishShareTracking(contextMetadata, promotedSourceInfo, entityMetadata.playableUrn, entityMetadata);
    }

    private void startShareActivity(Context context,
                                    String title,
                                    String creatorName, String permalink) {
        context.startActivity(buildShareIntent(context, title, creatorName, permalink));
    }

    private void publishShareTracking(EventContextMetadata contextMetadata, PromotedSourceInfo promotedSourceInfo, Urn urn, EntityMetadata entityMetadata) {
        eventTracker.trackEngagement(UIEvent.fromShare(
                urn,
                contextMetadata,
                promotedSourceInfo,
                entityMetadata));
    }

    private Intent buildShareIntent(Context context, String title, String creatorName, String permalink) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType(SHARE_TYPE);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, title));
        shareIntent.putExtra(Intent.EXTRA_TEXT, buildShareText(context, title, creatorName, permalink));

        return Intent.createChooser(shareIntent, context.getString(R.string.share));
    }

    private String buildShareText(Context context,
                                  String title,
                                  String username, String permalink) {

        if (Strings.isNotBlank(username)) {
            return context.getString(R.string.share_tracktitle_artist_link, title, username, permalink);
        } else {
            return context.getString(R.string.share_tracktitle_link, title, permalink);
        }
    }
}
