package com.soundcloud.android.share;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.firebase.FirebaseDynamicLinksApi;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.strings.Strings;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import javax.inject.Inject;
import java.io.IOException;

public class ShareOperations {

    private static final String SHARE_TYPE = "text/plain";

    private final FeatureFlags featureFlags;
    private final EventTracker eventTracker;
    private final FirebaseDynamicLinksApi firebaseDynamicLinksApi;

    @Inject
    public ShareOperations(FeatureFlags featureFlags, EventTracker eventTracker, FirebaseDynamicLinksApi firebaseDynamicLinksApi) {
        this.featureFlags = featureFlags;
        this.eventTracker = eventTracker;
        this.firebaseDynamicLinksApi = firebaseDynamicLinksApi;
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
        if (featureFlags.isEnabled(Flag.DYNAMIC_LINKS)) {
            firebaseDynamicLinksApi.createDynamicLink(permalink).observeOn(AndroidSchedulers.mainThread()).subscribe(
                    new DefaultSubscriber<String>() {
                        @Override
                        public void onNext(String dynamicLink) {
                            String shortLink = dynamicLink + "?" + Uri.parse(permalink).getPath();
                            shareAndTrack(context, shortLink, contextMetadata, promotedSourceInfo, entityMetadata);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (throwable instanceof IOException) {
                                Log.e("Failed to retrieve dynamic link. Falling back to original URL.", throwable);
                                shareAndTrack(context, permalink, contextMetadata, promotedSourceInfo, entityMetadata);
                            } else {
                                super.onError(throwable);
                            }
                        }
                    });
        } else {
            shareAndTrack(context, permalink, contextMetadata, promotedSourceInfo, entityMetadata);
        }
    }

    private void shareAndTrack(Context context, String permalink, EventContextMetadata contextMetadata, PromotedSourceInfo promotedSourceInfo, EntityMetadata entityMetadata) {
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
