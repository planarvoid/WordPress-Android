package com.soundcloud.android.navigation;

import static com.soundcloud.android.navigation.IntentFactory.createAlbumsCollectionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createCollectionAsRootIntent;
import static com.soundcloud.android.navigation.IntentFactory.createCollectionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createConversionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createDirectCheckoutIntent;
import static com.soundcloud.android.navigation.IntentFactory.createHomeIntent;
import static com.soundcloud.android.navigation.IntentFactory.createLaunchIntent;
import static com.soundcloud.android.navigation.IntentFactory.createMoreIntent;
import static com.soundcloud.android.navigation.IntentFactory.createNewForYouIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPerformSearchIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlayHistoryIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlaylistDiscoveryIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlaylistIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProductChoiceIntent;
import static com.soundcloud.android.navigation.IntentFactory.createRecentlyPlayedIntent;
import static com.soundcloud.android.navigation.IntentFactory.createResolveIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSearchFromShortcutIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSearchIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSearchPremiumContentResultsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createTrackCommentsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createTrackLikesFromShortcutIntent;
import static com.soundcloud.android.navigation.IntentFactory.createTrackLikesIntent;
import static com.soundcloud.android.navigation.IntentFactory.createViewAllRecommendationsIntent;
import static com.soundcloud.android.navigation.IntentFactory.rootScreen;

import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.search.SearchType;
import com.soundcloud.android.upgrade.GoOnboardingActivity;
import com.soundcloud.java.optional.Optional;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.TaskStackBuilder;

import java.util.List;

/**
 * This class should not be used directly to navigate within the app, since some requests might be a blocking operation.
 * All navigation requests should be executed through {@link Navigator} with a proper {@link NavigationTarget}.
 */
@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount"})
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class NavigationExecutor {

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

    Intent openDirectCheckout(Context context, Plan plan) {
        return createDirectCheckoutIntent(context, plan);
    }

    public void openPlaylistWithAutoPlay(Context context, Urn playlist, Screen screen) {
        context.startActivity(createPlaylistIntent(context, playlist, screen, true));
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

    public void openResolveForUri(Context context, Uri uri) {
        context.startActivity(createResolveIntent(context, uri));
    }

    public void openViewAllRecommendations(Context context) {
        context.startActivity(createViewAllRecommendationsIntent(context));
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

    public void openAlbumsCollection(Activity activity) {
        activity.startActivity(createAlbumsCollectionIntent(activity));
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
