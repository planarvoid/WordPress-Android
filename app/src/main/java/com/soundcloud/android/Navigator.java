package com.soundcloud.android;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.payments.UpgradeActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.profile.LegacyProfileActivity;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.search.SearchActivity;
import org.jetbrains.annotations.NotNull;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import javax.inject.Inject;

public class Navigator {

    private static final int NO_FLAGS = 0;
    private static final int FLAGS_TOP = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TASK;

    private final FeatureFlags featureFlags;

    @Inject
    public Navigator(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    public void openUpgrade(Context activityContext) {
        activityContext.startActivity(new Intent(activityContext, UpgradeActivity.class));
    }

    public void openPlaylist(Context activityContext, Urn playlist, Screen screen) {
        activityContext.startActivity(createPlaylistIntent(playlist, screen, false));
    }

    public void openPlaylist(Context activityContext, Urn playlist, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        activityContext.startActivity(createPlaylistIntent(playlist, screen, false)
                .putExtra(PlaylistDetailActivity.EXTRA_QUERY_SOURCE_INFO, searchQuerySourceInfo));
    }

    public void openMyProfile(Context activityContext, Urn user) {
        activityContext.startActivity(createMyProfileIntent(activityContext, user));
    }

    public void openProfile(Context activityContext, Urn user) {
        activityContext.startActivity(createProfileIntent(activityContext, user));
    }

    public void openProfile(Context activityContext, Urn user, Screen screen) {
        activityContext.startActivity(createProfileIntent(activityContext, user, screen));
    }

    public void openProfile(Context activityContext, Urn user, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        activityContext.startActivity(createProfileIntent(activityContext, user, screen)
                .putExtra(LegacyProfileActivity.EXTRA_QUERY_SOURCE_INFO, searchQuerySourceInfo));
    }

    public PendingIntent openProfileFromNotification(Context context, Urn user) {
        return PendingIntent.getActivity(context,
                NO_FLAGS,
                createProfileIntent(context, user, Screen.NOTIFICATION)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK),
                NO_FLAGS);
    }

    public PendingIntent openProfileFromWidget(Context context, Urn user, int requestCode) {
        return PendingIntent.getActivity(context,
                requestCode,
                createProfileIntent(context, user, Screen.WIDGET),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public void openRecord(Context activityContext, Screen screen) {
        activityContext.startActivity(createRecordIntent(activityContext, null, screen));
    }

    public void openRecord(Context activityContext, Recording recording) {
        activityContext.startActivity(createRecordIntent(activityContext, recording));
    }

    public void openOnboarding(Context activityContext, Urn urn, Screen screen) {
        activityContext.startActivity(createOnboardingIntent(activityContext, screen, urn));
    }

    public void openStream(Context activityContext, Screen screen) {
        activityContext.startActivity(createStreamIntent(screen));
    }

    public void openLauncher(Context activityContext) {
        activityContext.startActivity(createLauncherIntent(activityContext));
    }

    public void openStreamWithExpandedPlayer(Context activityContext, Screen screen) {
        activityContext.startActivity(createStreamWithExpandedPlayerIntent(screen));
    }

    public void openWebView(Context activityContext, Uri uri) {
        activityContext.startActivity(createWebViewIntent(activityContext, uri));
    }

    public void openExplore(Context activityContext, Screen screen) {
        activityContext.startActivity(createExploreIntent(screen));
    }

    public void openSearch(Context activityContext, Uri uri, Screen screen) {
        activityContext.startActivity(createSearchIntent(activityContext, uri, screen));
    }

    public void openWhoToFollow(Context activityContext, Screen screen) {
        activityContext.startActivity(createWhoToFollowIntent(screen));
    }

    private Intent createWhoToFollowIntent(Screen screen) {
        Intent intent = new Intent(Actions.WHO_TO_FOLLOW).setFlags(FLAGS_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createSearchIntent(Context activityContext, Uri uri, Screen screen) {
        Intent intent = new Intent(activityContext, SearchActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createExploreIntent(Screen screen) {
        Intent intent = new Intent(Actions.EXPLORE).setFlags(FLAGS_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createStreamIntent(Screen screen) {
        Intent intent = new Intent(Actions.STREAM).setFlags(FLAGS_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createStreamWithExpandedPlayerIntent(Screen screen) {
        return createStreamIntent(screen)
                .putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true);
    }

    private Intent createProfileIntent(Context context, Urn user) {
        return new Intent(context, featureFlags.isEnabled(Flag.NEW_PROFILE) ? ProfileActivity.class : LegacyProfileActivity.class)
                .putExtra(LegacyProfileActivity.EXTRA_USER_URN, user);
    }

    private Intent createProfileIntent(Context context, Urn user, Screen screen) {
        Intent intent = createProfileIntent(context, user);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createMyProfileIntent(Context context, Urn user) {
        return new Intent(context, featureFlags.isEnabled(Flag.NEW_PROFILE) ? ProfileActivity.class : MeActivity.class)
                .putExtra(LegacyProfileActivity.EXTRA_USER_URN, user);
    }

    private Intent createPlaylistIntent(@NotNull Urn playlistUrn, Screen screen, boolean autoPlay) {
        Intent intent = new Intent(Actions.PLAYLIST);
        screen.addToIntent(intent);
        return intent.putExtra(PlaylistDetailActivity.EXTRA_AUTO_PLAY, autoPlay)
                .putExtra(PlaylistDetailActivity.EXTRA_URN, playlistUrn);

    }

    private Intent createRecordIntent(Context activityContext, Recording recording) {
        return new Intent(activityContext, RecordActivity.class)
                .putExtra(Recording.EXTRA, recording)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    private Intent createRecordIntent(Context activityContext, Recording recording, Screen screen) {
        Intent intent = createRecordIntent(activityContext, recording);
        screen.addToIntent(intent);
        return intent;
    }


    private Intent createLauncherIntent(Context activityContext) {
        return new Intent(activityContext, LauncherActivity.class);
    }

    private Intent createOnboardingIntent(Context activityContext, Screen screen, Urn urn) {
        Intent intent = new Intent(activityContext, OnboardActivity.class)
                .putExtra(OnboardActivity.EXTRA_URN, urn)
                .setFlags(FLAGS_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createWebViewIntent(Context activityContext, Uri uri) {
        return new Intent(activityContext, WebViewActivity.class).setData(uri);
    }
}
