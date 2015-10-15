package com.soundcloud.android;

import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.discovery.DiscoveryActivity;
import com.soundcloud.android.discovery.PlaylistDiscoveryActivity;
import com.soundcloud.android.discovery.RecommendedTracksActivity;
import com.soundcloud.android.discovery.SearchResultsActivity;
import com.soundcloud.android.likes.TrackLikesActivity;
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
import com.soundcloud.android.settings.SettingsActivity;
import com.soundcloud.android.stations.ShowAllStationsActivity;

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

    public void openUpgrade(Context context) {
        context.startActivity(new Intent(context, UpgradeActivity.class));
    }

    public void openPlaylist(Context context, Urn playlist, Screen screen) {
        context.startActivity(PlaylistDetailActivity.getIntent(playlist, screen, false));
    }

    public void openPlaylist(Context context, Urn playlist, Screen screen, SearchQuerySourceInfo queryInfo, PromotedSourceInfo promotedInfo) {
        context.startActivity(PlaylistDetailActivity.getIntent(playlist, screen, false, queryInfo, promotedInfo));
    }

    public void openMyProfile(Context context, Urn user) {
        context.startActivity(createMyProfileIntent(context, user));
    }

    public void openProfile(Context context, Urn user) {
        context.startActivity(createProfileIntent(context, user));
    }

    public void openDiscovery(Context activityContext) {
        if (featureFlags.isEnabled(Flag.DISCOVERY)) {
            startActivity(activityContext, DiscoveryActivity.class);
        } else {
            startActivity(activityContext, SearchActivity.class);
        }
    }

    public void openProfile(Context context, Urn user, Screen screen) {
        context.startActivity(createProfileIntent(context, user, screen));
    }

    public void openProfile(Context context, Urn user, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        context.startActivity(createProfileIntent(context, user, screen)
                .putExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo));
    }

    public PendingIntent openProfileFromNotification(Context context, Urn user) {
        return PendingIntent.getActivity(context,
                NO_FLAGS,
                createProfileIntent(context, user, Screen.NOTIFICATION)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public PendingIntent openProfileFromWidget(Context context, Urn user, int requestCode) {
        return PendingIntent.getActivity(context,
                requestCode,
                createProfileIntent(context, user, Screen.WIDGET),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public void openActivities(Context context) {
        context.startActivity(new Intent(context, ActivitiesActivity.class));
    }

    public void openSettings(Context context) {
        context.startActivity(new Intent(context, SettingsActivity.class));
    }

    @Deprecated // use method that passes Screen, remove this after tabs
    public void openRecord(Context context) {
        context.startActivity(createRecordIntent(context, null));
    }

    public void openRecord(Context context, Screen screen) {
        context.startActivity(createRecordIntent(context, null, screen));
    }

    public void openRecord(Context context, Recording recording) {
        context.startActivity(createRecordIntent(context, recording));
    }

    public void openOnboarding(Context context, Urn deeplinkUrn, Screen screen) {
        context.startActivity(createOnboardingIntent(context, screen, deeplinkUrn));
    }

    public void openStream(Context context, Screen screen) {
        context.startActivity(createStreamIntent(screen));
    }

    public void openLauncher(Context context) {
        context.startActivity(createLauncherIntent(context));
    }

    public void openStreamWithExpandedPlayer(Context context, Screen screen) {
        context.startActivity(createStreamWithExpandedPlayerIntent(screen));
    }

    public void openWebView(Context context, Uri uri) {
        context.startActivity(createWebViewIntent(context, uri));
    }

    public void openExplore(Context context, Screen screen) {
        context.startActivity(createExploreIntent(screen));
    }

    public void openSearch(Context context, Uri uri, Screen screen) {
        context.startActivity(createSearchIntent(context, uri, screen));
    }

    public void openResolveForUrn(Context context, Urn urn) {
        context.startActivity(createResolveIntent(context, urn));
    }

    public void openRecommendation(Context context, long localSeedId) {
        context.startActivity(createRecommendationIntent(context, localSeedId));
    }

    public void openViewAllStations(Context context, int collectionType) {
        final Intent intent = new Intent(context, ShowAllStationsActivity.class);
        intent.putExtra(ShowAllStationsActivity.COLLECTION_TYPE, collectionType);
        context.startActivity(intent);
    }

    public void openTrackLikes(Context context) {
        context.startActivity(new Intent(context, TrackLikesActivity.class));
    }

    public void openSearchResults(Context context, String query) {
        context.startActivity(createSearchResultsIntent(context, query));
    }

    public void launchSearchSuggestion(Context context, Urn urn, SearchQuerySourceInfo searchQuerySourceInfo, Uri itemUri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        if (urn.isUser()) {
            intent.putExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo);
        }
        Screen.SEARCH_SUGGESTIONS.addToIntent(intent);
        context.startActivity(intent.setData(itemUri));
    }

    public void openPlaylistDiscoveryTag(Context context, String playlistTag) {
        context.startActivity(createPlaylistDiscoveryIntent(context, playlistTag));
    }

    private Intent createResolveIntent(Context context, Urn urn) {
        Intent intent = new Intent(context, ResolveActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(urn.toString()));
        intent.setFlags(FLAGS_TOP);
        return intent;
    }

    private Intent createSearchIntent(Context context, Uri uri, Screen screen) {
        Intent intent = new Intent(context, SearchActivity.class);
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

    private Intent createRecordIntent(Context context, Recording recording) {
        return new Intent(context, RecordActivity.class)
                .putExtra(Recording.EXTRA, recording)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    private Intent createRecordIntent(Context context, Recording recording, Screen screen) {
        Intent intent = createRecordIntent(context, recording);
        screen.addToIntent(intent);
        return intent;
    }


    private Intent createLauncherIntent(Context context) {
        return new Intent(context, LauncherActivity.class);
    }

    private Intent createOnboardingIntent(Context context, Screen screen, Urn deeplinkUrn) {
        Intent intent = new Intent(context, OnboardActivity.class)
                .putExtra(OnboardActivity.EXTRA_DEEPLINK_URN, deeplinkUrn)
                .setFlags(FLAGS_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createRecommendationIntent(Context context, long localSeedId) {
        return new Intent(context, RecommendedTracksActivity.class)
                .putExtra(RecommendedTracksActivity.EXTRA_LOCAL_SEED_ID, localSeedId);
    }

    private Intent createSearchResultsIntent(Context context, String searchQuery) {
        return new Intent(context, SearchResultsActivity.class)
                .putExtra(SearchResultsActivity.EXTRA_SEARCH_QUERY, searchQuery);
    }

    private Intent createPlaylistDiscoveryIntent(Context context, String playListTag) {
        return new Intent(context, PlaylistDiscoveryActivity.class)
                .putExtra(PlaylistDiscoveryActivity.EXTRA_PLAYLIST_TAG, playListTag);
    }

    private Intent createWebViewIntent(Context context, Uri uri) {
        return new Intent(context, WebViewActivity.class).setData(uri);
    }

    private void startActivity(Context activityContext, Class target) {
        activityContext.startActivity(new Intent(activityContext, target));
    }
}
