package com.soundcloud.android;

import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.discovery.PlaylistDiscoveryActivity;
import com.soundcloud.android.discovery.RecommendedTracksActivity;
import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.explore.ExploreActivity;
import com.soundcloud.android.likes.TrackLikesActivity;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineSettingsOnboardingActivity;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.payments.NativeConversionActivity;
import com.soundcloud.android.payments.WebConversionActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.profile.UserRepostsActivity;
import com.soundcloud.android.profile.UserTracksActivity;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.search.SearchPremiumResultsActivity;
import com.soundcloud.android.settings.LegalActivity;
import com.soundcloud.android.settings.OfflineSettingsActivity;
import com.soundcloud.android.settings.SettingsActivity;
import com.soundcloud.android.settings.notifications.NotificationPreferencesActivity;
import com.soundcloud.android.stations.ShowAllStationsActivity;
import com.soundcloud.android.upgrade.GoOnboardingActivity;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class Navigator {

    private static final int NO_FLAGS = 0;
    private static final int FLAGS_TOP = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TASK;

    public static final String EXTRA_SEARCH_INTENT = "search_intent";
    public static final String EXTRA_UPGRADE_INTENT = "upgrade_intent";

    private final FeatureFlags featureFlags;

    @Inject
    public Navigator(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    public void openHome(Context context) {
        context.startActivity(createHomeIntent(context));
    }

    public void openHomeAsRootScreen(Activity activity) {
        activity.finish();
        activity.startActivity(rootScreen(createHomeIntent(activity)));
    }

    public void launchHome(Context context, @Nullable Bundle extras) {
        final Intent homeIntent = createHomeIntent(context);
        if (extras != null) {
            homeIntent.putExtras(extras);
        }

        if (!Referrer.hasReferrer(homeIntent)) {
            Referrer.HOME_BUTTON.addToIntent(homeIntent);
        }

        if (!Screen.hasScreen(homeIntent)) {
            Screen.UNKNOWN.addToIntent(homeIntent);
        }

        context.startActivity(homeIntent);
    }

    public void openUpgrade(Context activityContext) {
        activityContext.startActivity(new Intent(activityContext, getUpgradeClass()));
    }

    protected Class getUpgradeClass() {
        return featureFlags.isEnabled(Flag.FEATURE_WEB_UPGRADE_FLOW)
                ? WebConversionActivity.class
                : NativeConversionActivity.class;
    }

    public void openUpgradeOnMain(Context context) {
        context.startActivity(createHomeIntent(context).putExtra(EXTRA_UPGRADE_INTENT, true));
    }

    public void openPlaylist(Context context, Urn playlist, Screen screen) {
        context.startActivity(PlaylistDetailActivity.getIntent(playlist, screen, false));
    }

    public void openPlaylistWithAutoPlay(Context context, Urn playlist, Screen screen) {
        context.startActivity(PlaylistDetailActivity.getIntent(playlist, screen, true));
    }

    public void openPlaylist(Context context, Urn playlist, Screen screen, SearchQuerySourceInfo queryInfo, PromotedSourceInfo promotedInfo) {
        context.startActivity(PlaylistDetailActivity.getIntent(playlist, screen, false, queryInfo, promotedInfo));
    }

    public void openProfile(Context context, Urn user) {
        context.startActivity(createProfileIntent(context, user));
    }

    public void openSearch(Activity activity) {
        startActivity(activity, SearchActivity.class);
    }

    public void openSearch(Activity activity, Intent searchIntent) {
        activity.startActivity(searchIntent);
    }

    public void openSearch(Context context, Uri uri, Screen screen) {
        final Intent searchIntent = createSearchIntentFromDeepLink(context, uri, screen);
        final Intent homeIntent = createHomeIntent(context);
        homeIntent.setAction(Actions.SEARCH);
        homeIntent.putExtra(EXTRA_SEARCH_INTENT, searchIntent);
        context.startActivity(homeIntent);
    }

    public void openSearchPremiumContentResults(Context context,
                                                String searchQuery,
                                                int searchType,
                                                List<PropertySet> premiumContentList,
                                                Optional<Link> nextHref,
                                                Urn queryUrn) {
        final ArrayList<? extends Parcelable> sourceSetList = new ArrayList<>(premiumContentList);
        final Intent intent = new Intent(context, SearchPremiumResultsActivity.class)
                .putExtra(SearchPremiumResultsActivity.EXTRA_SEARCH_QUERY, searchQuery)
                .putExtra(SearchPremiumResultsActivity.EXTRA_SEARCH_TYPE, searchType)
                .putExtra(SearchPremiumResultsActivity.EXTRA_SEARCH_QUERY_URN, queryUrn)
                .putParcelableArrayListExtra(SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_RESULTS, sourceSetList)
                .putExtra(SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_NEXT_HREF, nextHref.orNull());
        context.startActivity(intent);
    }

    public void performSearch(Context context, String query) {
        final Intent intent = new Intent(Actions.PERFORM_SEARCH)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(SearchManager.QUERY, query);

        context.startActivity(intent);
    }

    public void openUri(Context context, Uri uri) {
        context.startActivity(new Intent(Intent.ACTION_VIEW).setData(uri));
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
                createProfileIntent(context, user, Screen.WIDGET, Referrer.PLAYBACK_WIDGET),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public void openReposts(Context context, Urn user, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        context.startActivity(createRepostsIntent(context, user, screen)
                .putExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo));
    }

    public void openProfileTracks(Context context, Urn user, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        context.startActivity(createProfileTracksIntent(context, user, screen)
                .putExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo));
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

    public void openOfflineSettings(Context context) {
        context.startActivity(new Intent(context, OfflineSettingsActivity.class));
    }

    public void openNotificationPreferences(Context context) {
        context.startActivity(new Intent(context, NotificationPreferencesActivity.class));
    }

    public void openNotificationPreferencesFromDeeplink(Context context) {
        context.startActivity(new Intent(context, NotificationPreferencesActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
    }

    public void openLegal(Context context) {
        context.startActivity(new Intent(context, LegalActivity.class));
    }

    public void openHelpCenter(Context context) {
        context.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse(context.getString(R.string.url_support))));
    }

    public void openOnboarding(Context context, Urn deeplinkUrn, Screen screen) {
        context.startActivity(createOnboardingIntent(context, screen, deeplinkUrn));
    }

    public void openStream(Context context, Screen screen) {
        context.startActivity(createStreamIntent(screen));
    }

    public void openCollectionAsRootScreen(Activity activity) {
        activity.finish();
        activity.startActivity(rootScreen(new Intent(Actions.COLLECTION).setFlags(FLAGS_TOP)));
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
        context.startActivity(createExploreIntent(context, screen));
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

    public void openPlaylistDiscoveryTag(Context context, String playlistTag) {
        context.startActivity(createPlaylistDiscoveryIntent(context, playlistTag));
    }

    public void openTrackComments(Context context, Urn trackUrn) {
        context.startActivity(new Intent(context, TrackCommentsActivity.class)
                .putExtra(TrackCommentsActivity.EXTRA_COMMENTED_TRACK_URN, trackUrn));
    }

    public void openOfflineSettingsOnboarding(Context context) {
        context.startActivity(new Intent(context, OfflineSettingsOnboardingActivity.class));
    }

    private Intent createHomeIntent(Context context) {
        return new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    private Intent createResolveIntent(Context context, Urn urn) {
        Intent intent = new Intent(context, ResolveActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(urn.toString()));
        intent.setFlags(FLAGS_TOP);
        return intent;
    }

    private Intent createSearchIntentFromDeepLink(Context context, Uri uri, Screen screen) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createExploreIntent(Context context, Screen screen) {
        Intent intent = new Intent(context, ExploreActivity.class);
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

    public Intent createProfileIntent(Context context, Urn user) {
        return new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER_URN, user);
    }

    public Intent createProfileIntent(Context context, Urn user, Screen screen) {
        Intent intent = createProfileIntent(context, user);
        screen.addToIntent(intent);
        return intent;
    }

    public Intent createProfileIntent(Context context, Urn user, Screen screen, Referrer referrer) {
        Intent intent = createProfileIntent(context, user, screen);
        referrer.addToIntent(intent);
        return intent;
    }

    private Intent createRepostsIntent(Context context, Urn user, Screen screen) {
        Intent intent = new Intent(context, UserRepostsActivity.class)
                .putExtra(UserRepostsActivity.EXTRA_USER_URN, user);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createProfileTracksIntent(Context context, Urn user, Screen screen) {
        Intent intent = new Intent(context, UserTracksActivity.class)
                .putExtra(UserTracksActivity.EXTRA_USER_URN, user);
        screen.addToIntent(intent);
        return intent;
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

    public void resetForAccountUpgrade(Activity activity) {
        resetAppAndNavigateTo(activity, GoOnboardingActivity.class);
    }

    public void resetForAccountDowngrade(Activity activity) {
        resetAppAndNavigateTo(activity, GoOffboardingActivity.class);
    }

    private void resetAppAndNavigateTo(Activity activity, Class<? extends Activity> nextActivity) {
        activity.startActivity(rootScreen(new Intent(activity, nextActivity)));
        activity.finish();
    }

    public void restartApp(Activity context) {
        context.startActivity(createLaunchIntent(context));
        System.exit(0);
    }

    @NonNull
    private Intent createLaunchIntent(Activity context) {
        Intent launcherIntent = rootScreen(new Intent(context, LauncherActivity.class));
        launcherIntent.addCategory(Intent.CATEGORY_DEFAULT);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        return launcherIntent;
    }

    private Intent rootScreen(Intent intent) {
        return intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }
}
