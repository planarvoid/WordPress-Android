package com.soundcloud.android;

import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.ads.FullScreenVideoActivity;
import com.soundcloud.android.ads.PrestitialActivity;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.collection.playhistory.PlayHistoryActivity;
import com.soundcloud.android.collection.playlists.PlaylistsActivity;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedActivity;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.record.RecordPermissionsActivity;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.discovery.systemplaylist.SystemPlaylistActivity;
import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.TrackLikesActivity;
import com.soundcloud.android.likes.TrackLikesIntentResolver;
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
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.search.SearchPremiumResultsActivity;
import com.soundcloud.android.search.SearchType;
import com.soundcloud.android.search.topresults.TopResults;
import com.soundcloud.android.search.topresults.TopResultsBucketActivity;
import com.soundcloud.android.settings.ChangeStorageLocationActivity;
import com.soundcloud.android.settings.LegalActivity;
import com.soundcloud.android.settings.OfflineSettingsActivity;
import com.soundcloud.android.settings.SettingsActivity;
import com.soundcloud.android.settings.notifications.NotificationPreferencesActivity;
import com.soundcloud.android.stations.LikedStationsActivity;
import com.soundcloud.android.stations.StationInfoActivity;
import com.soundcloud.android.upgrade.GoOnboardingActivity;
import com.soundcloud.java.optional.Optional;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount"})
public class Navigator {

    private static final int NO_FLAGS = 0;
    private static final int FLAGS_TOP = Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_CLEAR_TOP
            | Intent.FLAG_ACTIVITY_TASK_ON_HOME
            | Intent.FLAG_ACTIVITY_CLEAR_TASK;

    public static final String EXTRA_SEARCH_INTENT = "search_intent";
    public static final String EXTRA_CHECKOUT_PLAN = "checkout_plan";

    protected final EventTracker eventTracker;
    protected final FeatureFlags featureFlags;

    public Navigator(EventTracker eventTracker, FeatureFlags featureFlags) {
        this.eventTracker = eventTracker;
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

    public void openFullscreenVideoAd(Context context, Urn adUrn) {
        Intent intent = new Intent(context, FullScreenVideoActivity.class)
                .putExtra(FullScreenVideoActivity.EXTRA_AD_URN, adUrn);
        context.startActivity(intent);
    }

    public void openPrestititalAd(Context context) {
        context.startActivity(new Intent(context, PrestitialActivity.class));
    }

    public void openAdClickthrough(Context context, Uri clickUrl) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, clickUrl)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void openUpgrade(Context context, UpsellContext upsellContext) {
        context.startActivity(createConversionIntent(context, upsellContext));
    }

    public void openUpgradeOnMain(Context context, UpsellContext upsellContext) {
        TaskStackBuilder.create(context)
                        .addNextIntent(createHomeIntent(context))
                        .addNextIntent(createConversionIntent(context, upsellContext))
                        .startActivities();
    }

    public void openProductChoiceOnMain(Context context, Plan plan) {
        TaskStackBuilder.create(context)
                        .addNextIntent(createHomeIntent(context))
                        .addNextIntent(createProductChoiceIntent(context, plan))
                        .startActivities();
    }

    public void openDirectCheckout(Context context, Plan plan) {
        Intent intent = new Intent(context, WebCheckoutActivity.class);
        intent.putExtra(EXTRA_CHECKOUT_PLAN, plan);
        context.startActivity(intent);
    }

    public void openPlaylistWithAutoPlay(Context context, Urn playlist, Screen screen) {
        context.startActivity(PlaylistDetailActivity.getIntent(playlist, screen, true));
    }

    public void legacyOpenPlaylist(Context context, Urn playlist, Screen screen) {
        context.startActivity(PlaylistDetailActivity.getIntent(playlist, screen, false));
    }

    public void legacyOpenPlaylist(Context context, Urn playlist, Screen screen,
                                   SearchQuerySourceInfo queryInfo, PromotedSourceInfo promotedInfo) {
        context.startActivity(PlaylistDetailActivity.getIntent(playlist, screen, false, queryInfo, promotedInfo));
    }

    public void openPlaylist(Context context, Urn playlist, Screen screen, UIEvent event) {
        eventTracker.trackNavigation(event);

        context.startActivity(PlaylistDetailActivity.getIntent(playlist, screen, false));
    }

    public void openPlaylist(Context context, Urn playlist, Screen screen,
                             SearchQuerySourceInfo queryInfo, PromotedSourceInfo promotedInfo, UIEvent event) {
        eventTracker.trackNavigation(event);

        context.startActivity(PlaylistDetailActivity.getIntent(playlist, screen, false, queryInfo, promotedInfo));
    }

    public void openProfile(Context context, Urn user, UIEvent navigationEvent) {
        eventTracker.trackNavigation(navigationEvent);

        context.startActivity(createProfileIntent(context, user));
    }

    public void openProfile(Context context, Urn user, Screen screen, UIEvent navigationEvent) {
        eventTracker.trackNavigation(navigationEvent);

        context.startActivity(createProfileIntent(context, user, screen));
    }

    public void legacyOpenProfile(Context context, Urn user) {
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

    public void openSearchFromShortcut(Activity activity) {
        Intent intent = new Intent(activity, SearchActivity.class);
        Referrer.LAUNCHER_SHORTCUT.addToIntent(intent);
        Screen.SEARCH_MAIN.addToIntent(intent);
        activity.startActivity(intent);
    }

    public void openSearchPremiumContentResults(Context context,
                                                String searchQuery,
                                                SearchType searchType,
                                                List<Urn> premiumContentList,
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

    public void openEmail(Activity activity, String emailAddress) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { emailAddress });
        activity.startActivity(intent);
    }

    public void openExternal(Activity activity, Uri uri) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    public void legacyOpenProfile(Context context, Urn user, Screen screen) {
        context.startActivity(createProfileIntent(context, user, screen));
    }

    public void legacyOpenProfile(Context context,
                                  Urn user,
                                  Screen screen,
                                  SearchQuerySourceInfo searchQuerySourceInfo) {
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

    public static PendingIntent openProfileFromWidget(Context context, Urn user, int requestCode) {
        return PendingIntent.getActivity(context,
                                         requestCode,
                                         createProfileIntent(context, user, Screen.WIDGET, Referrer.PLAYBACK_WIDGET),
                                         PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public void openProfileReposts(Context context, Urn user, Screen screen, SearchQuerySourceInfo querySourceInfo) {
        context.startActivity(createProfileRepostsIntent(context, user, screen)
                                      .putExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, querySourceInfo));
    }

    public void openProfileTracks(Context context, Urn user, Screen screen, SearchQuerySourceInfo querySourceInfo) {
        context.startActivity(createProfileTracksIntent(context, user, screen)
                                      .putExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, querySourceInfo));
    }

    public void openProfileAlbums(Context context, Urn user, Screen screen, SearchQuerySourceInfo querySourceInfo) {
        context.startActivity(createProfileAlbumsIntent(context, user, screen)
                                      .putExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, querySourceInfo));
    }

    public void openProfileLikes(Context context, Urn user, Screen screen, SearchQuerySourceInfo querySourceInfo) {
        context.startActivity(createProfileLikesIntent(context, user, screen)
                                      .putExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, querySourceInfo));
    }

    public void openProfilePlaylists(Context context, Urn user, Screen screen, SearchQuerySourceInfo querySourceInfo) {
        context.startActivity(createProfilePlaylistsIntent(context, user, screen)
                                      .putExtra(UserPlaylistsActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, querySourceInfo));
    }

    public void openSystemPlaylist(Context context, Urn urn, Screen screen) {
        context.startActivity(createSystemPlaylistIntent(context, urn, screen));
    }

    public void openActivities(Context context) {
        context.startActivity(new Intent(context, ActivitiesActivity.class));
    }

    public void openFollowers(Context context,
                              Urn userUrn,
                              SearchQuerySourceInfo searchQuerySourceInfo) {
        context.startActivity(new Intent(context, FollowersActivity.class)
                                      .putExtra(FollowersActivity.EXTRA_USER_URN, userUrn)
                                      .putExtra(FollowersActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
        );
    }

    public void openFollowings(Context context,
                               Urn userUrn,
                               SearchQuerySourceInfo searchQuerySourceInfo) {
        context.startActivity(new Intent(context, FollowingsActivity.class)
                                      .putExtra(FollowingsActivity.EXTRA_USER_URN, userUrn)
                                      .putExtra(FollowingsActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
        );
    }

    public void openBasicSettings(Context context) {
        context.startActivity(new Intent(context, SettingsActivity.class));
    }

    public void openRecord(Context context, Screen screen) {
        openRecord(context, null, screen);
    }

    public void openRecord(Context context, Recording recording) {
        openRecord(context, recording, Screen.UNKNOWN);
    }

    public void openRecord(Context context, Recording recording, Screen screen) {
        if (hasMicrophonePermission(context)) {
            context.startActivity(createRecordIntent(context, recording, screen));
        } else {
            context.startActivity(createRecordPermissionIntent(context, recording, screen));
        }
    }

    public void openSearchViewAll(Context context, String query, Optional<Urn> queryUrn, TopResults.Bucket.Kind kind, boolean isPremium) {
        Intent intent = new Intent(context, TopResultsBucketActivity.class);
        intent.putExtra(TopResultsBucketActivity.EXTRA_QUERY, query);
        if (queryUrn.isPresent()) {
            intent.putExtra(TopResultsBucketActivity.EXTRA_QUERY_URN, queryUrn.get());
        }
        intent.putExtra(TopResultsBucketActivity.EXTRA_BUCKET_KIND, kind);
        intent.putExtra(TopResultsBucketActivity.EXTRA_IS_PREMIUM, isPremium);
        context.startActivity(intent);
    }

    private boolean hasMicrophonePermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                                                 Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    public void openOfflineSettings(Context context) {
        context.startActivity(createOfflineSettingsIntent(context));
    }

    public PendingIntent createPendingOfflineSettings(Context context) {
        return PendingIntent.getActivity(context, 0, createOfflineSettingsIntent(context), PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private Intent createOfflineSettingsIntent(Context context) {
        return new Intent(context, OfflineSettingsActivity.class);
    }

    public PendingIntent createPendingChangeStorageLocation(Context context) {
        final Intent intent = new Intent(context, ChangeStorageLocationActivity.class);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public PendingIntent createPendingHomeIntent(Context context) {
        Intent intent = createHomeIntent(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public PendingIntent createPendingCollectionIntent(Context context) {
        Intent intent = createCollectionIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
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

    public void openOnboarding(Context context, Uri deepLinkUri, Screen screen) {
        context.startActivity(createOnboardingIntent(context, screen, deepLinkUri));
    }

    public void openStream(Context context, Screen screen) {
        context.startActivity(createStreamIntent(screen));
    }

    public void openCollectionAsTopScreen(Context context) {
        context.startActivity(createCollectionIntent().setFlags(FLAGS_TOP));
    }

    public void openCollectionAsRootScreen(Activity activity) {
        activity.finish();
        activity.startActivity(rootScreen(createCollectionIntent().setFlags(FLAGS_TOP)));
    }

    public void openCollection(Context context) {
        context.startActivity(createCollectionIntent());
    }

    private Intent createCollectionIntent() {
        return new Intent(Actions.COLLECTION);
    }

    public void openMore(Context context) {
        context.startActivity(new Intent(Actions.MORE).setFlags(FLAGS_TOP));
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

    public void openRemoteSignInWebView(Context context, Uri uri) {
        Intent intent = new Intent(context, RemoteSignInWebViewActivity.class).setData(uri);
        context.startActivity(intent);
    }

    public void openResolveForUri(Context context, Uri uri) {
        context.startActivity(createResolveIntent(context, uri));
    }

    public void openViewAllRecommendations(Context context) {
        context.startActivity(new Intent(context, ViewAllRecommendedTracksActivity.class));
    }

    public void openChart(Context context, Urn genre, ChartType type, ChartCategory category, String header) {
        context.startActivity(ChartActivity.createIntent(context, genre, type, category, header));
    }

    public void openAllGenres(Context context) {
        context.startActivity(new Intent(context, AllGenresActivity.class));
    }

    public void openAllGenres(Context context, ChartCategory category) {
        context.startActivity(AllGenresActivity.createIntent(context, category));
    }

    public void openLikedStations(Context context) {
        context.startActivity(new Intent(context, LikedStationsActivity.class));
    }

    public void openStationInfo(Context context,
                                Urn stationUrn,
                                Urn seedTrack,
                                DiscoverySource source,
                                UIEvent navigationEvent) {
        eventTracker.trackNavigation(navigationEvent);

        context.startActivity(new Intent(context, StationInfoActivity.class)
                                      .putExtra(StationInfoActivity.EXTRA_SOURCE, source.value())
                                      .putExtra(StationInfoActivity.EXTRA_URN, stationUrn)
                                      .putExtra(StationInfoActivity.EXTRA_SEED_URN, seedTrack));
    }

    public void legacyOpenStationInfo(Context context, Urn stationUrn, Urn seedTrack, DiscoverySource source) {
        context.startActivity(new Intent(context, StationInfoActivity.class)
                                      .putExtra(StationInfoActivity.EXTRA_SOURCE, source.value())
                                      .putExtra(StationInfoActivity.EXTRA_URN, stationUrn)
                                      .putExtra(StationInfoActivity.EXTRA_SEED_URN, seedTrack));
    }

    public void legacyOpenStationInfo(Context context, Urn stationUrn, DiscoverySource source) {
        context.startActivity(new Intent(context, StationInfoActivity.class)
                                      .putExtra(StationInfoActivity.EXTRA_SOURCE, source.value())
                                      .putExtra(StationInfoActivity.EXTRA_URN, stationUrn));
    }

    public void openTrackLikes(Context context) {
        context.startActivity(new Intent(context, TrackLikesActivity.class));
    }

    public void openTrackLikesFromShortcut(Context context, Intent source) {
        Intent intent = new Intent(context, TrackLikesActivity.class);
        intent.putExtra(TrackLikesIntentResolver.EXTRA_SOURCE, source);
        Referrer.LAUNCHER_SHORTCUT.addToIntent(intent);
        Screen.LIKES.addToIntent(intent);
        context.startActivity(intent);
    }

    public void openNewForYou(Context context) {
        // TODO (REC-1174): Is screen tracking required?
        final Intent intent = new Intent(context, SystemPlaylistActivity.class).putExtra(SystemPlaylistActivity.EXTRA_FOR_NEW_FOR_YOU, true);
        context.startActivity(intent);
    }

    public void openPlayHistory(Context context) {
        context.startActivity(new Intent(context, PlayHistoryActivity.class));
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

    public void openPlaylistsAndAlbumsCollection(Activity activity) {
        activity.startActivity(PlaylistsActivity.intentForPlaylistsAndAlbums(activity));
    }

    public void openPlaylistsCollection(Activity activity) {
        activity.startActivity(PlaylistsActivity.intentForPlaylists(activity));
    }

    public void openAlbumsCollection(Activity activity) {
        activity.startActivity(PlaylistsActivity.intentForAlbums(activity));
    }

    public void openDiscovery(Context context, Screen screen) {
        context.startActivity(createDiscoveryIntent(screen));
    }

    public void openRecentlyPlayed(Context context) {
        context.startActivity(new Intent(context, RecentlyPlayedActivity.class));
    }

    private Intent createHomeIntent(Context context) {
        return new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    private Intent createConversionIntent(Context context, UpsellContext upsellContext) {
        final Intent intent = new Intent(context, ConversionActivity.class);
        upsellContext.addTo(intent);
        return intent;
    }

    private Intent createProductChoiceIntent(Context context, Plan plan) {
        return new Intent(context, ProductChoiceActivity.class)
                .putExtra(ProductChoiceActivity.DEFAULT_PLAN, plan);
    }

    private Intent createResolveIntent(Context context, Uri uri) {
        Intent intent = new Intent(context, ResolveActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
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

    private Intent createStreamIntent(Screen screen) {
        Intent intent = new Intent(Actions.STREAM).setFlags(FLAGS_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createDiscoveryIntent(Screen screen) {
        Intent intent = new Intent(Actions.DISCOVERY).setFlags(FLAGS_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createStreamWithExpandedPlayerIntent(Screen screen) {
        return createStreamIntent(screen)
                .putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true);
    }

    public static Intent createProfileIntent(Context context, Urn user) {
        return new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER_URN, user);
    }

    private static Intent createProfileIntent(Context context, Urn user, Screen screen) {
        Intent intent = createProfileIntent(context, user);
        screen.addToIntent(intent);
        return intent;
    }

    private static Intent createProfileIntent(Context context, Urn user, Screen screen, Referrer referrer) {
        Intent intent = createProfileIntent(context, user, screen);
        referrer.addToIntent(intent);
        return intent;
    }

    public Intent createHomeIntentFromNotification(Context context) {
        final Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true);

        Screen.NOTIFICATION.addToIntent(intent);
        Referrer.PLAYBACK_NOTIFICATION.addToIntent(intent);

        return intent;
    }

    public Intent createHomeIntentFromCastExpandedController(Context context) {
        final Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return intent;
    }

    private Intent createProfileRepostsIntent(Context context, Urn user, Screen screen) {
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

    private Intent createProfileLikesIntent(Context context, Urn user, Screen screen) {
        Intent intent = new Intent(context, UserLikesActivity.class)
                .putExtra(UserLikesActivity.EXTRA_USER_URN, user);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createProfileAlbumsIntent(Context context, Urn user, Screen screen) {
        Intent intent = new Intent(context, UserAlbumsActivity.class)
                .putExtra(UserAlbumsActivity.EXTRA_USER_URN, user);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createProfilePlaylistsIntent(Context context, Urn user, Screen screen) {
        Intent intent = new Intent(context, UserPlaylistsActivity.class)
                .putExtra(UserPlaylistsActivity.EXTRA_USER_URN, user);
        screen.addToIntent(intent);
        return intent;
    }


    private Intent createSystemPlaylistIntent(Context context, Urn playlist, Screen screen) {
        Intent intent = new Intent(context, SystemPlaylistActivity.class)
                .putExtra(SystemPlaylistActivity.EXTRA_PLAYLIST_URN, playlist)
                .putExtra(SystemPlaylistActivity.EXTRA_FOR_NEW_FOR_YOU, false);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createRecordPermissionIntent(Context context, Recording recording, Screen screen) {
        Intent intent = new Intent(context, RecordPermissionsActivity.class)
                .putExtra(Recording.EXTRA, recording)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createRecordIntent(Context context, Recording recording, Screen screen) {
        Intent intent = new Intent(context, RecordActivity.class)
                .putExtra(Recording.EXTRA, recording)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        screen.addToIntent(intent);
        return intent;
    }

    private Intent createLauncherIntent(Context context) {
        return new Intent(context, LauncherActivity.class);
    }

    private Intent createOnboardingIntent(Context context, Screen screen, Uri deepLinkUri) {
        Intent intent = new Intent(context, OnboardActivity.class)
                .putExtra(OnboardActivity.EXTRA_DEEP_LINK_URI, deepLinkUri)
                .setFlags(FLAGS_TOP);
        screen.addToIntent(intent);
        return intent;
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
