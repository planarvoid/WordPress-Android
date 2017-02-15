package com.soundcloud.android.share;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.firebase.FirebaseDynamicLinksApi;
import com.soundcloud.android.configuration.experiments.DynamicLinkSharingConfig;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.ShareDialog;
import com.soundcloud.java.strings.Strings;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import java.io.IOException;

public class SharePresenter {

    private static final String SHARE_TYPE = "text/plain";

    private final DynamicLinkSharingConfig dynamicLinkSharingConfig;
    private final EventTracker eventTracker;
    private final FirebaseDynamicLinksApi firebaseDynamicLinksApi;

    @Inject
    public SharePresenter(DynamicLinkSharingConfig dynamicLinkSharingConfig, EventTracker eventTracker, FirebaseDynamicLinksApi firebaseDynamicLinksApi) {
        this.dynamicLinkSharingConfig = dynamicLinkSharingConfig;
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
        eventTracker.trackEngagement(UIEvent.fromShareRequest(entityMetadata.playableUrn, contextMetadata, promotedSourceInfo, entityMetadata));

        if (dynamicLinkSharingConfig.isEnabled()) {
            ShareDialog shareDialog = ShareDialog.show(context);
            Subscription subscription = firebaseDynamicLinksApi.createDynamicLink(permalink).observeOn(AndroidSchedulers.mainThread()).subscribe(
                    new DefaultSubscriber<String>() {
                        @Override
                        public void onNext(String dynamicLink) {
                            String shortLink = dynamicLink + "?" + Uri.parse(permalink).getPath();
                            startShareActivity(context, entityMetadata.playableTitle, entityMetadata.creatorName, shortLink);
                            eventTracker.trackEngagement(UIEvent.fromSharePromptWithFirebaseLink(entityMetadata.playableUrn, contextMetadata, promotedSourceInfo, entityMetadata));
                            shareDialog.dismiss();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            shareDialog.dismiss();
                            if (throwable instanceof IOException) {
                                Log.e("Failed to retrieve dynamic link. Falling back to original URL.", throwable);
                                startShareActivity(context, entityMetadata.playableTitle, entityMetadata.creatorName, permalink);
                                eventTracker.trackEngagement(UIEvent.fromSharePromptWithSoundCloudLink(entityMetadata.playableUrn, contextMetadata, promotedSourceInfo, entityMetadata));
                            } else {
                                super.onError(throwable);
                            }
                        }

                        @Override
                        public void onCompleted() {
                            checkState(!shareDialog.isShowing(), "Share dialog still showing.");
                            super.onCompleted();
                        }
                    });
            shareDialog.onCancelObservable().subscribe(ignored -> {
                subscription.unsubscribe();
                eventTracker.trackEngagement(UIEvent.fromShareCancel(entityMetadata.playableUrn, contextMetadata, promotedSourceInfo, entityMetadata));
            });
        } else {
            startShareActivity(context, entityMetadata.playableTitle, entityMetadata.creatorName, permalink);
            eventTracker.trackEngagement(UIEvent.fromSharePromptWithSoundCloudLink(entityMetadata.playableUrn, contextMetadata, promotedSourceInfo, entityMetadata));
        }
    }

    public void share(Context context, ShareOptions shareOptions) {
        share(context, shareOptions.permalinkUrl(), shareOptions.eventContextMetadata(), shareOptions.promotedSourceInfo(), shareOptions.entityMetadata());
    }

    private void startShareActivity(Context context,
                                    String title,
                                    String creatorName, String permalink) {
        context.startActivity(buildShareIntent(context, title, creatorName, permalink));
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

    @AutoValue
    public abstract static class ShareOptions {

        public static ShareOptions create(String permalinkUrl,
                                          EventContextMetadata eventContextMetadata,
                                          PromotedSourceInfo promotedSourceInfo,
                                          EntityMetadata entityMetadata){
            return new AutoValue_SharePresenter_ShareOptions(permalinkUrl, eventContextMetadata, promotedSourceInfo, entityMetadata);
        }

        public abstract String permalinkUrl();
        public abstract EventContextMetadata eventContextMetadata();
        @Nullable public abstract PromotedSourceInfo promotedSourceInfo();
        public abstract EntityMetadata entityMetadata();
    }
}
