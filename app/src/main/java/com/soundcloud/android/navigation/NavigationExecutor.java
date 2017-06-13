package com.soundcloud.android.navigation;

import static com.soundcloud.android.navigation.IntentFactory.addSearchQuerySource;
import static com.soundcloud.android.navigation.IntentFactory.createAdClickthroughIntent;
import static com.soundcloud.android.navigation.IntentFactory.createAlbumsCollectionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createAllGenresIntent;
import static com.soundcloud.android.navigation.IntentFactory.createChartsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createCollectionAsRootIntent;
import static com.soundcloud.android.navigation.IntentFactory.createCollectionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createConversionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createDirectCheckoutIntent;
import static com.soundcloud.android.navigation.IntentFactory.createDiscoveryIntent;
import static com.soundcloud.android.navigation.IntentFactory.createEmailIntent;
import static com.soundcloud.android.navigation.IntentFactory.createFullscreenVideoAdIntent;
import static com.soundcloud.android.navigation.IntentFactory.createHomeIntent;
import static com.soundcloud.android.navigation.IntentFactory.createLaunchIntent;
import static com.soundcloud.android.navigation.IntentFactory.createLauncherIntent;
import static com.soundcloud.android.navigation.IntentFactory.createLegalIntent;
import static com.soundcloud.android.navigation.IntentFactory.createLikedStationsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createMoreIntent;
import static com.soundcloud.android.navigation.IntentFactory.createNewForYouIntent;
import static com.soundcloud.android.navigation.IntentFactory.createNotificationPreferencesFromDeeplinkIntent;
import static com.soundcloud.android.navigation.IntentFactory.createNotificationPreferencesIntent;
import static com.soundcloud.android.navigation.IntentFactory.createOfflineSettingsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createOfflineSettingsOnboardingIntent;
import static com.soundcloud.android.navigation.IntentFactory.createOnboardingIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPerformSearchIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlayHistoryIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlaylistDiscoveryIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlaylistIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlaylistsAndAlbumsCollectionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlaylistsCollectionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPrestititalAdIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProductChoiceIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileAlbumsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileLikesIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfilePlaylistsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileRepostsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileTracksIntent;
import static com.soundcloud.android.navigation.IntentFactory.createRecentlyPlayedIntent;
import static com.soundcloud.android.navigation.IntentFactory.createRecordIntent;
import static com.soundcloud.android.navigation.IntentFactory.createRecordPermissionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createRemoteSignInIntent;
import static com.soundcloud.android.navigation.IntentFactory.createResolveIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSearchActionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSearchFromShortcutIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSearchIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSearchPremiumContentResultsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSearchViewAllIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSettingsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createStationsInfoIntent;
import static com.soundcloud.android.navigation.IntentFactory.createStreamIntent;
import static com.soundcloud.android.navigation.IntentFactory.createStreamWithExpandedPlayerIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSystemPlaylistIntent;
import static com.soundcloud.android.navigation.IntentFactory.createTrackCommentsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createTrackLikesFromShortcutIntent;
import static com.soundcloud.android.navigation.IntentFactory.createTrackLikesIntent;
import static com.soundcloud.android.navigation.IntentFactory.createViewAllRecommendationsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createViewIntent;
import static com.soundcloud.android.navigation.IntentFactory.createWebViewIntent;
import static com.soundcloud.android.navigation.IntentFactory.rootScreen;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.search.SearchType;
import com.soundcloud.android.upgrade.GoOnboardingActivity;
import com.soundcloud.java.optional.Optional;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import java.util.List;

/**
 * This class should not be used directly to navigate within the app, since some requests might be a blocking operation.
 * All navigation requests should be executed through {@link Navigator} with a proper {@link NavigationTarget}.
 */
@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount"})
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class NavigationExecutor {

    protected final EventTracker eventTracker;
    protected final FeatureFlags featureFlags;

    public NavigationExecutor(EventTracker eventTracker, FeatureFlags featureFlags) {
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
        context.startActivity(createFullscreenVideoAdIntent(context, adUrn));
    }

    public void openPrestititalAd(Context context) {
        context.startActivity(createPrestititalAdIntent(context));
    }

    public void openAdClickthrough(Context context, Uri clickUrl) {
        context.startActivity(createAdClickthroughIntent(clickUrl));
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
        context.startActivity(createDirectCheckoutIntent(context, plan));
    }

    public void openPlaylistWithAutoPlay(Context context, Urn playlist, Screen screen) {
        context.startActivity(createPlaylistIntent(playlist, screen, true));
    }

    public void legacyOpenPlaylist(Context context, Urn playlist, Screen screen) {
        context.startActivity(createPlaylistIntent(playlist, screen, false));
    }

    public void legacyOpenPlaylist(Context context, Urn playlist, Screen screen,
                                   SearchQuerySourceInfo queryInfo, PromotedSourceInfo promotedInfo) {
        context.startActivity(createPlaylistIntent(playlist, screen, queryInfo, promotedInfo));
    }

    public void openPlaylist(Context context, Urn playlist, Screen screen, UIEvent event) {
        eventTracker.trackNavigation(event);

        context.startActivity(createPlaylistIntent(playlist, screen, false));
    }

    public void openPlaylist(Context context, Urn playlist, Screen screen,
                             SearchQuerySourceInfo queryInfo, PromotedSourceInfo promotedInfo, UIEvent event) {
        eventTracker.trackNavigation(event);

        context.startActivity(createPlaylistIntent(playlist, screen, queryInfo, promotedInfo));
    }

    public void legacyOpenProfile(Context context, Urn user) {
        context.startActivity(createProfileIntent(context, user));
    }

    public void openSearch(Activity activity) {
        activity.startActivity(createSearchIntent(activity));
    }

    public void openSearch(Activity activity, Intent searchIntent) {
        if (searchIntent.hasExtra(IntentFactory.EXTRA_SEARCH_INTENT)) {
            activity.startActivity(searchIntent.getParcelableExtra(IntentFactory.EXTRA_SEARCH_INTENT));
        } else {
            openSearch(activity);
        }
    }

    public void openSearch(Context context, Uri uri, Screen screen) {
        final Intent homeIntent = createSearchActionIntent(context, uri, screen);
        context.startActivity(homeIntent);
    }

    public void openSearchFromShortcut(Activity activity) {
        Intent intent = createSearchFromShortcutIntent(activity);
        activity.startActivity(intent);
    }

    public void openSearchPremiumContentResults(Context context,
                                                String searchQuery,
                                                SearchType searchType,
                                                List<Urn> premiumContentList,
                                                Optional<Link> nextHref,
                                                Urn queryUrn) {
        final Intent intent = createSearchPremiumContentResultsIntent(context, searchQuery, searchType, premiumContentList, nextHref, queryUrn);
        context.startActivity(intent);
    }

    public void performSearch(Context context, String query) {
        context.startActivity(createPerformSearchIntent(query));
    }

    public void openEmail(Activity activity, String emailAddress) {
        Intent intent = createEmailIntent(emailAddress);
        activity.startActivity(intent);
    }

    public void openExternal(Activity activity, Uri uri) {
        activity.startActivity(createViewIntent(uri));
    }

    public void legacyOpenProfile(Context context, Urn user, Screen screen) {
        context.startActivity(createProfileIntent(context, user, screen));
    }

    public void legacyOpenProfile(Context context,
                                  Urn user,
                                  Screen screen,
                                  SearchQuerySourceInfo searchQuerySourceInfo) {
        context.startActivity(addSearchQuerySource(createProfileIntent(context, user, screen), searchQuerySourceInfo));
    }

    public void openProfileReposts(Context context, Urn user, Screen screen, SearchQuerySourceInfo querySourceInfo) {
        context.startActivity(addSearchQuerySource(createProfileRepostsIntent(context, user, screen), querySourceInfo));
    }

    public void openProfileTracks(Context context, Urn user, Screen screen, SearchQuerySourceInfo querySourceInfo) {
        context.startActivity(addSearchQuerySource(createProfileTracksIntent(context, user, screen), querySourceInfo));
    }

    public void openProfileAlbums(Context context, Urn user, Screen screen, SearchQuerySourceInfo querySourceInfo) {
        context.startActivity(addSearchQuerySource(createProfileAlbumsIntent(context, user, screen), querySourceInfo));
    }

    public void openProfileLikes(Context context, Urn user, Screen screen, SearchQuerySourceInfo querySourceInfo) {
        context.startActivity(addSearchQuerySource(createProfileLikesIntent(context, user, screen), querySourceInfo));
    }

    public void openProfilePlaylists(Context context, Urn user, Screen screen, SearchQuerySourceInfo querySourceInfo) {
        context.startActivity(addSearchQuerySource(createProfilePlaylistsIntent(context, user, screen), querySourceInfo));
    }

    public void openSystemPlaylist(Context context, Urn urn, Screen screen) {
        context.startActivity(createSystemPlaylistIntent(context, urn, screen));
    }

    public void openBasicSettings(Context context) {
        context.startActivity(createSettingsIntent(context));
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

    void openSearchViewAll(NavigationTarget navigationTarget) {
        navigationTarget.activity().startActivity(createSearchViewAllIntent(navigationTarget.activity(), navigationTarget.topResultsMetaData().get(), navigationTarget.queryUrn()));
    }

    private boolean hasMicrophonePermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                                                 Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    public void openOfflineSettings(Context context) {
        context.startActivity(createOfflineSettingsIntent(context));
    }

    public void openNotificationPreferences(Context context) {
        context.startActivity(createNotificationPreferencesIntent(context));
    }

    public void openNotificationPreferencesFromDeeplink(Context context) {
        context.startActivity(createNotificationPreferencesFromDeeplinkIntent(context));
    }

    public void openLegal(Context context) {
        context.startActivity(createLegalIntent(context));
    }

    public void openHelpCenter(Context context) {
        context.startActivity(createViewIntent(Uri.parse(context.getString(R.string.url_support))));
    }

    public void openOnboarding(Context context, Uri deepLinkUri, Screen screen) {
        context.startActivity(createOnboardingIntent(context, screen, deepLinkUri));
    }

    public void openStream(Context context, Screen screen) {
        context.startActivity(createStreamIntent(screen));
    }

    public void openCollectionAsTopScreen(Context context) {
        context.startActivity(createCollectionIntent().setFlags(IntentFactory.FLAGS_TOP));
    }

    public void openCollectionAsRootScreen(Activity activity) {
        activity.finish();
        activity.startActivity(createCollectionAsRootIntent());
    }

    public void openCollection(Context context) {
        context.startActivity(createCollectionIntent());
    }

    public void openMore(Context context) {
        context.startActivity(createMoreIntent());
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
        context.startActivity(createRemoteSignInIntent(context, uri));
    }

    public void openResolveForUri(Context context, Uri uri) {
        context.startActivity(createResolveIntent(context, uri));
    }

    public void openViewAllRecommendations(Context context) {
        context.startActivity(createViewAllRecommendationsIntent(context));
    }

    public void openChart(Context context, Urn genre, ChartType type, ChartCategory category, String header) {
        context.startActivity(createChartsIntent(context, genre, type, category, header));
    }

    public void openAllGenres(Context context) {
        context.startActivity(createAllGenresIntent(context, null));
    }

    public void openAllGenres(Context context, ChartCategory category) {
        context.startActivity(createAllGenresIntent(context, category));
    }

    public void openLikedStations(Context context) {
        context.startActivity(createLikedStationsIntent(context));
    }

    public void openStationInfo(Context context,
                                Urn stationUrn,
                                Urn seedTrack,
                                DiscoverySource source,
                                UIEvent navigationEvent) {
        eventTracker.trackNavigation(navigationEvent);

        context.startActivity(createStationsInfoIntent(context, stationUrn, Optional.of(seedTrack), Optional.of(source)));
    }

    public void legacyOpenStationInfo(Context context, Urn stationUrn, Urn seedTrack, DiscoverySource source) {
        context.startActivity(createStationsInfoIntent(context, stationUrn, Optional.of(seedTrack), Optional.of(source)));
    }

    public void legacyOpenStationInfo(Context context, Urn stationUrn, DiscoverySource source) {
        context.startActivity(createStationsInfoIntent(context, stationUrn, Optional.absent(), Optional.of(source)));
    }

    public void openTrackLikes(Context context) {
        context.startActivity(createTrackLikesIntent(context));
    }

    public void openTrackLikesFromShortcut(Context context, Intent source) {
        context.startActivity(createTrackLikesFromShortcutIntent(context, source));
    }

    public void openNewForYou(Context context) {
        // TODO (REC-1174): Is screen tracking required?
        context.startActivity(createNewForYouIntent(context));
    }

    public void openPlayHistory(Context context) {
        context.startActivity(createPlayHistoryIntent(context));
    }

    public void openPlaylistDiscoveryTag(Context context, String playlistTag) {
        context.startActivity(createPlaylistDiscoveryIntent(context, playlistTag));
    }

    public void openTrackComments(Context context, Urn trackUrn) {
        context.startActivity(createTrackCommentsIntent(context, trackUrn));
    }

    public void openOfflineSettingsOnboarding(Context context) {
        context.startActivity(createOfflineSettingsOnboardingIntent(context));
    }

    public void openPlaylistsAndAlbumsCollection(Activity activity) {
        activity.startActivity(createPlaylistsAndAlbumsCollectionIntent(activity));
    }

    public void openPlaylistsCollection(Activity activity) {
        activity.startActivity(createPlaylistsCollectionIntent(activity));
    }

    public void openAlbumsCollection(Activity activity) {
        activity.startActivity(createAlbumsCollectionIntent(activity));
    }

    public void openDiscovery(Context context, Screen screen) {
        context.startActivity(createDiscoveryIntent(screen));
    }

    public void openRecentlyPlayed(Context context) {
        context.startActivity(createRecentlyPlayedIntent(context));
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
}
