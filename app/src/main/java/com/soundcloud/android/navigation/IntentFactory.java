package com.soundcloud.android.navigation;

import com.soundcloud.android.Actions;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.ads.FullScreenVideoActivity;
import com.soundcloud.android.ads.PrestitialActivity;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.eventlogger.DevEventLoggerMonitorActivity;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.collection.playhistory.PlayHistoryActivity;
import com.soundcloud.android.collection.playlists.PlaylistsActivity;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedActivity;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.record.RecordPermissionsActivity;
import com.soundcloud.android.creators.upload.UploadService;
import com.soundcloud.android.deeplinks.ChartDetails;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.discovery.systemplaylist.SystemPlaylistActivity;
import com.soundcloud.android.likes.TrackLikesActivity;
import com.soundcloud.android.likes.TrackLikesIntentResolver;
import com.soundcloud.android.main.DevEventLoggerMonitorReceiver;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineSettingsOnboardingActivity;
import com.soundcloud.android.olddiscovery.PlaylistDiscoveryActivity;
import com.soundcloud.android.olddiscovery.SearchActivity;
import com.soundcloud.android.olddiscovery.charts.AllGenresActivity;
import com.soundcloud.android.olddiscovery.charts.ChartActivity;
import com.soundcloud.android.olddiscovery.recommendations.ViewAllRecommendedTracksActivity;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.onboarding.auth.RemoteSignInWebViewActivity;
import com.soundcloud.android.payments.ConversionActivity;
import com.soundcloud.android.payments.ProductChoiceActivity;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.payments.WebCheckoutActivity;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.profile.FollowersActivity;
import com.soundcloud.android.profile.FollowingsActivity;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.profile.UserAlbumsActivity;
import com.soundcloud.android.profile.UserLikesActivity;
import com.soundcloud.android.profile.UserPlaylistsActivity;
import com.soundcloud.android.profile.UserRepostsActivity;
import com.soundcloud.android.profile.UserTracksActivity;
import com.soundcloud.android.search.SearchPremiumResultsActivity;
import com.soundcloud.android.search.SearchType;
import com.soundcloud.android.search.topresults.TopResultsBucketActivity;
import com.soundcloud.android.settings.LegalActivity;
import com.soundcloud.android.settings.OfflineSettingsActivity;
import com.soundcloud.android.settings.SettingsActivity;
import com.soundcloud.android.settings.notifications.NotificationPreferencesActivity;
import com.soundcloud.android.stations.LikedStationsActivity;
import com.soundcloud.android.stations.StationInfoActivity;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.optional.Optional;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.List;

@SuppressWarnings("PMD.GodClass")
public final class IntentFactory {
    static final String EXTRA_SEARCH_INTENT = "search_intent";
    public static final String EXTRA_CHECKOUT_PLAN = "checkout_plan";
    static final int FLAGS_TOP = Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_CLEAR_TOP
            | Intent.FLAG_ACTIVITY_TASK_ON_HOME
            | Intent.FLAG_ACTIVITY_CLEAR_TASK;

    static Intent createHomeIntent(Context context) {
        return new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    static Intent createConversionIntent(Context context, UpsellContext upsellContext) {
        final Intent intent = new Intent(context, ConversionActivity.class);
        upsellContext.addTo(intent);
        return intent;
    }

    static Intent createProductChoiceIntent(Context context, Plan plan) {
        return new Intent(context, ProductChoiceActivity.class)
                .putExtra(ProductChoiceActivity.DEFAULT_PLAN, plan);
    }

    static Intent createResolveIntent(Context context, Uri uri) {
        Intent intent = new Intent(context, ResolveActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.setFlags(FLAGS_TOP);
        return intent;
    }

    static Intent createSearchIntentFromDeepLink(Context context, Uri uri, Screen screen) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        screen.addToIntent(intent);
        return intent;
    }

    static Intent createStreamIntent(Screen screen) {
        Intent intent = new Intent(Actions.STREAM).setFlags(FLAGS_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    static Intent createDiscoveryIntent(Screen screen) {
        Intent intent = new Intent(Actions.DISCOVERY).setFlags(FLAGS_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    static Intent createStreamWithExpandedPlayerIntent(Screen screen) {
        return createStreamIntent(screen)
                .putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true);
    }

    public static Intent createProfileIntent(Context context, Urn user, Optional<Screen> screen, Optional<SearchQuerySourceInfo> searchQuerySourceInfo, Optional<Referrer> referrer) {
        Intent intent = new Intent(context, ProfileActivity.class)
                .putExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo.orNull());
        Urns.writeToIntent(intent, ProfileActivity.EXTRA_USER_URN, user);
        screen.ifPresent(s -> s.addToIntent(intent));
        referrer.ifPresent(r -> r.addToIntent(intent));
        return intent;
    }

    public static Intent createHomeIntentFromNotification(Context context) {
        final Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true);

        Screen.NOTIFICATION.addToIntent(intent);
        Referrer.PLAYBACK_NOTIFICATION.addToIntent(intent);

        return intent;
    }

    public static Intent createHomeIntentFromCastExpandedController(Context context) {
        final Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return intent;
    }

    static Intent createProfileRepostsIntent(Context context, Urn user, Screen screen, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return createProfileSubScreenIntent(context, user, screen, searchQuerySourceInfo, UserRepostsActivity.class, UserRepostsActivity.EXTRA_USER_URN);
    }

    static Intent createProfileTracksIntent(Context context, Urn user, Screen screen, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return createProfileSubScreenIntent(context, user, screen, searchQuerySourceInfo, UserTracksActivity.class, UserTracksActivity.EXTRA_USER_URN);
    }

    static Intent createProfileLikesIntent(Context context, Urn user, Screen screen, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return createProfileSubScreenIntent(context, user, screen, searchQuerySourceInfo, UserLikesActivity.class, UserLikesActivity.EXTRA_USER_URN);
    }

    static Intent createProfileAlbumsIntent(Context context, Urn user, Screen screen, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return createProfileSubScreenIntent(context, user, screen, searchQuerySourceInfo, UserAlbumsActivity.class, UserAlbumsActivity.EXTRA_USER_URN);
    }

    static Intent createProfilePlaylistsIntent(Context context, Urn user, Screen screen, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return createProfileSubScreenIntent(context, user, screen, searchQuerySourceInfo, UserPlaylistsActivity.class, UserPlaylistsActivity.EXTRA_USER_URN);
    }

    private static Intent createProfileSubScreenIntent(Context context,
                                                       Urn user,
                                                       Screen screen,
                                                       Optional<SearchQuerySourceInfo> searchQuerySourceInfo,
                                                       Class<? extends Activity> cls,
                                                       String userUrnKey) {
        Intent intent = new Intent(context, cls)
                .putExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo.orNull());
        Urns.writeToIntent(intent, userUrnKey, user);
        screen.addToIntent(intent);
        return intent;
    }

    static Intent createSystemPlaylistIntent(Context context, Urn playlist, Screen screen) {
        Intent intent = new Intent(context, SystemPlaylistActivity.class)
                .putExtra(SystemPlaylistActivity.EXTRA_FOR_NEW_FOR_YOU, false);
        Urns.writeToIntent(intent, SystemPlaylistActivity.EXTRA_PLAYLIST_URN, playlist);
        screen.addToIntent(intent);
        return intent;
    }

    static Intent createRecordPermissionIntent(Context context, Recording recording, Screen screen) {
        Intent intent = new Intent(context, RecordPermissionsActivity.class)
                .putExtra(Recording.EXTRA, recording)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    static Intent createRecordIntent(Context context, Recording recording, Screen screen) {
        Intent intent = new Intent(context, RecordActivity.class)
                .putExtra(Recording.EXTRA, recording)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    static Intent createLauncherIntent(Context context) {
        return new Intent(context, LauncherActivity.class);
    }

    static Intent createOnboardingIntent(Context context, Screen screen, Uri deepLinkUri) {
        Intent intent = new Intent(context, OnboardActivity.class)
                .putExtra(OnboardActivity.EXTRA_DEEP_LINK_URI, deepLinkUri)
                .setFlags(FLAGS_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    static Intent createPlaylistDiscoveryIntent(Context context, String playListTag) {
        return new Intent(context, PlaylistDiscoveryActivity.class)
                .putExtra(PlaylistDiscoveryActivity.EXTRA_PLAYLIST_TAG, playListTag);
    }

    static Intent createWebViewIntent(Context context, Uri uri) {
        return new Intent(context, WebViewActivity.class).setData(uri);
    }

    static Intent createOfflineSettingsIntent(Context context) {
        return new Intent(context, OfflineSettingsActivity.class);
    }

    static Intent createCollectionIntent() {
        return new Intent(Actions.COLLECTION);
    }

    static Intent createLaunchIntent(Activity activity) {
        Intent launcherIntent = rootScreen(new Intent(activity, LauncherActivity.class));
        launcherIntent.addCategory(Intent.CATEGORY_DEFAULT);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        return launcherIntent;
    }

    static Intent rootScreen(Intent intent) {
        return intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }

    static Intent createFullscreenVideoAdIntent(Context context, Urn adUrn) {
        return Urns.writeToIntent(new Intent(context, FullScreenVideoActivity.class), FullScreenVideoActivity.EXTRA_AD_URN, adUrn);
    }

    static Intent createPrestititalAdIntent(Context context) {
        return new Intent(context, PrestitialActivity.class);
    }

    static Intent createAdClickthroughIntent(Uri clickUrl) {
        return new Intent(Intent.ACTION_VIEW, clickUrl)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    static Intent createDirectCheckoutIntent(Context context, Plan plan) {
        return new Intent(context, WebCheckoutActivity.class).putExtra(EXTRA_CHECKOUT_PLAN, plan);
    }

    static Intent createPlaylistIntent(Urn playlist, Screen screen, boolean autoPlay) {
        return PlaylistDetailActivity.getIntent(playlist, screen, autoPlay, Optional.absent(), Optional.absent());
    }

    static Intent createPlaylistIntent(Urn playlist, Screen screen, Optional<SearchQuerySourceInfo> queryInfo, Optional<PromotedSourceInfo> promotedInfo) {
            return PlaylistDetailActivity.getIntent(playlist, screen, false, queryInfo, promotedInfo);
    }

    static Intent createSearchIntent(Activity activity) {
        return new Intent(activity, SearchActivity.class);
    }

    static Intent createSearchActionIntent(Context context, Uri uri, Screen screen) {
        final Intent searchIntent = createSearchIntentFromDeepLink(context, uri, screen);
        final Intent homeIntent = createHomeIntent(context);
        homeIntent.setAction(Actions.SEARCH);
        homeIntent.putExtra(EXTRA_SEARCH_INTENT, searchIntent);
        return homeIntent;
    }

    static Intent createSearchFromShortcutIntent(Activity activity) {
        Intent intent = createSearchIntent(activity);
        Referrer.LAUNCHER_SHORTCUT.addToIntent(intent);
        Screen.SEARCH_MAIN.addToIntent(intent);
        return intent;
    }

    static Intent createSearchPremiumContentResultsIntent(Context context, String searchQuery, SearchType searchType, List<Urn> premiumContentList, Optional<Link> nextHref, Urn queryUrn) {
        final Intent intent = new Intent(context, SearchPremiumResultsActivity.class)
                .putExtra(SearchPremiumResultsActivity.EXTRA_SEARCH_QUERY, searchQuery)
                .putExtra(SearchPremiumResultsActivity.EXTRA_SEARCH_TYPE, searchType)
                .putExtra(SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_NEXT_HREF, nextHref.orNull());
        Urns.writeToIntent(intent, SearchPremiumResultsActivity.EXTRA_SEARCH_QUERY_URN, queryUrn);
        return Urns.writeToIntent(intent, SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_RESULTS, premiumContentList);
    }

    static Intent createPerformSearchIntent(String query) {
        return new Intent(Actions.PERFORM_SEARCH)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(SearchManager.QUERY, query);
    }

    static Intent createEmailIntent(String emailAddress) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
        return intent;
    }

    static Intent createViewIntent(Uri uri) {
        return new Intent(Intent.ACTION_VIEW, uri);
    }

    static Intent createActivitiesIntent(Context context) {
        return new Intent(context, ActivitiesActivity.class);
    }

    static Intent createFollowersIntent(Context context, Urn userUrn, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        final Intent intent = new Intent(context, FollowersActivity.class)
                .putExtra(FollowersActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo.orNull());
        return Urns.writeToIntent(intent, FollowersActivity.EXTRA_USER_URN, userUrn);
    }

    static Intent createFollowingsIntent(Context context, Urn userUrn, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        final Intent intent = new Intent(context, FollowingsActivity.class)
                .putExtra(FollowingsActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo.orNull());
        return Urns.writeToIntent(intent, FollowingsActivity.EXTRA_USER_URN, userUrn);
    }

    static Intent createSettingsIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    static Intent createSearchViewAllIntent(Context context, NavigationTarget.TopResultsMetaData metaData, Optional<Urn> queryUrn) {
        Intent intent = new Intent(context, TopResultsBucketActivity.class);
        intent.putExtra(TopResultsBucketActivity.EXTRA_QUERY, metaData.query());
        Urns.writeToIntent(intent, TopResultsBucketActivity.EXTRA_QUERY_URN, queryUrn);
        intent.putExtra(TopResultsBucketActivity.EXTRA_BUCKET_KIND, metaData.kind());
        intent.putExtra(TopResultsBucketActivity.EXTRA_IS_PREMIUM, metaData.isPremium());
        return intent;
    }

    static Intent createNotificationPreferencesIntent(Context context) {
        return new Intent(context, NotificationPreferencesActivity.class);
    }

    static Intent createNotificationPreferencesFromDeeplinkIntent(Context context) {
        return createNotificationPreferencesIntent(context).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    }

    static Intent createLegalIntent(Context context) {
        return new Intent(context, LegalActivity.class);
    }

    static Intent createCollectionAsRootIntent() {
        return rootScreen(createCollectionIntent().setFlags(IntentFactory.FLAGS_TOP));
    }

    static Intent createMoreIntent() {
        return new Intent(Actions.MORE).setFlags(IntentFactory.FLAGS_TOP);
    }

    static Intent createRemoteSignInIntent(Context context, Uri uri) {
        return new Intent(context, RemoteSignInWebViewActivity.class).setData(uri);
    }

    static Intent createViewAllRecommendationsIntent(Context context) {
        return new Intent(context, ViewAllRecommendedTracksActivity.class);
    }

    static Intent createChartsIntent(Context context, ChartDetails chartDetails) {
        return ChartActivity.createIntent(context, chartDetails);
    }

    static Intent createAllGenresIntent(Context context, @Nullable ChartCategory category) {
        if (category == null) {
            return new Intent(context, AllGenresActivity.class);
        } else {
            return AllGenresActivity.createIntent(context, category);
        }
    }

    static Intent createLikedStationsIntent(Context context) {
        return new Intent(context, LikedStationsActivity.class);
    }

    static Intent createStationsInfoIntent(Context context, Urn stationUrn, Optional<Urn> seedTrack, Optional<DiscoverySource> source) {
        Intent intent = new Intent(context, StationInfoActivity.class);
        Urns.writeToIntent(intent, StationInfoActivity.EXTRA_URN, stationUrn);
        source.ifPresent(it -> intent.putExtra(StationInfoActivity.EXTRA_SOURCE, it.value()));
        Urns.writeToIntent(intent, StationInfoActivity.EXTRA_SEED_URN, seedTrack);
        return intent;
    }

    static Intent createTrackLikesIntent(Context context) {
        return new Intent(context, TrackLikesActivity.class);
    }

    static Intent createTrackLikesFromShortcutIntent(Context context, Intent source) {
        Intent intent = createTrackLikesIntent(context);
        intent.putExtra(TrackLikesIntentResolver.EXTRA_SOURCE, source);
        Referrer.LAUNCHER_SHORTCUT.addToIntent(intent);
        Screen.LIKES.addToIntent(intent);
        return intent;
    }

    static Intent createNewForYouIntent(Context context) {
        return new Intent(context, SystemPlaylistActivity.class).putExtra(SystemPlaylistActivity.EXTRA_FOR_NEW_FOR_YOU, true);
    }

    static Intent createPlayHistoryIntent(Context context) {
        return new Intent(context, PlayHistoryActivity.class);
    }

    static Intent createTrackCommentsIntent(Context context, Urn trackUrn) {
        return Urns.writeToIntent(new Intent(context, TrackCommentsActivity.class), TrackCommentsActivity.EXTRA_COMMENTED_TRACK_URN, trackUrn);
    }

    static Intent createOfflineSettingsOnboardingIntent(Context context) {
        return new Intent(context, OfflineSettingsOnboardingActivity.class);
    }

    static Intent createPlaylistsAndAlbumsCollectionIntent(Activity activity) {
        return PlaylistsActivity.intentForPlaylistsAndAlbums(activity);
    }

    static Intent createPlaylistsCollectionIntent(Activity activity) {
        return PlaylistsActivity.intentForPlaylists(activity);
    }

    static Intent createAlbumsCollectionIntent(Activity activity) {
        return PlaylistsActivity.intentForAlbums(activity);
    }

    static Intent createRecentlyPlayedIntent(Context context) {
        return new Intent(context, RecentlyPlayedActivity.class);
    }

    static Intent createDevEventLoggerMonitorReceiverIntent(Context context, boolean monitorMute) {
        return new Intent(context, DevEventLoggerMonitorReceiver.class).putExtra(DevEventLoggerMonitorReceiver.EXTRA_MONITOR_MUTE, monitorMute);
    }

    static Intent createDevEventLoggerMonitorIntent(Context context) {
        return new Intent(context, DevEventLoggerMonitorActivity.class);
    }

    static Intent getMonitorIntent(Recording recording) {
        return new Intent(Actions.UPLOAD_MONITOR).putExtra(UploadService.EXTRA_RECORDING, recording);
    }

    private IntentFactory() {
        // hidden for utility classes
    }
}
