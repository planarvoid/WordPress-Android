package com.soundcloud.android.deeplinks;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.TrackedActivity;
import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tasks.FetchModelTask;
import com.soundcloud.android.utils.AndroidUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import javax.inject.Inject;

public class ResolveActivity extends TrackedActivity implements FetchModelTask.Listener<PublicApiResource> {
    private static final String SOUNDCLOUD_HOME_DEEP_LINK = "soundcloud://home";

    private boolean loadRelated = PlaybackOperations.WITH_RELATED;
    @Nullable private ResolveFetchTask resolveTask;

    @Inject PublicCloudAPI oldCloudAPI;
    @Inject AccountOperations accountOperations;
    @Inject PlaybackOperations playbackOperations;
    @Inject PlayQueueManager playQueueManager;
    @Inject ReferrerResolver referrerResolver;

    public ResolveActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    public static boolean accept(Uri data, Resources resources) {
        return Urn.SOUNDCLOUD_SCHEME.equalsIgnoreCase(data.getScheme())
                || (data.getHost() != null && data.getHost().contains(resources.getString(R.string.host_name)));
    }

    @VisibleForTesting
    ResolveActivity(PlaybackOperations playbackOperations, ReferrerResolver referrerResolver,
                    EventBus eventBus, AccountOperations accountOperations, PlayQueueManager playQueueManager) {
        SoundCloudApplication.getObjectGraph().inject(this);
        this.playbackOperations = playbackOperations;
        this.referrerResolver = referrerResolver;
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
        this.playQueueManager = playQueueManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resolve);
        oldCloudAPI = new PublicApi(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        Uri data = intent.getData();
        Referrer referrer = getReferrer(intent);

        if (data == null || isHomeDeepLink(data)) {
            trackForegroundEvent(referrer);
            startHomeScreen();
        } else if (Referrer.GOOGLE_CRAWLER.equals(referrer)) {
            loginGoogleCrawlerAndFetch(data);
        } else if (accountOperations.isUserLoggedIn()) {
            final boolean shouldResolve = Intent.ACTION_VIEW.equals(intent.getAction()) || referrerResolver.isFacebookAction(intent, getResources());

            if (shouldResolve) {
                fetchData(data, PlaybackOperations.WITH_RELATED);
            } else {
                trackForegroundEvent(referrer);
                finish();
            }
        } else {
            trackForegroundEvent(referrer);
            launchApplicationWithMessage(R.string.error_toast_user_not_logged_in);
        }
    }

    private void loginGoogleCrawlerAndFetch(final Uri data) {
        accountOperations.loginCrawlerUser();
        playbackOperations.resetService();
        playQueueManager.clearAll(); // do not leave previous played tracks visible for crawlers
        fetchData(data, PlaybackOperations.WITHOUT_RELATED);
    }

    private boolean isHomeDeepLink(Uri uri) {
        return SOUNDCLOUD_HOME_DEEP_LINK.equals(uri.toString());
    }

    private void fetchData(Uri data, boolean loadRelated) {
        this.loadRelated = loadRelated;
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
        resolveTask = new ResolveFetchTask(oldCloudAPI, getContentResolver());
        resolveTask.setListener(this);
        resolveTask.execute(data);
    }

    @Override
    public void onSuccess(PublicApiResource resource) {
        resolveTask = null;
        startActivityForResource(resource);
        finish();
    }

    @Override
    public void onError(Object context) {
        resolveTask = null;

        if (shouldFallbackOnWebapp(context)) {
            startActivity(new Intent(this, WebViewActivity.class).setData((Uri) context));
        } else {
            launchApplicationWithMessage(R.string.error_loading_url);
        }
    }

    private void launchApplicationWithMessage(int messageId) {
        AndroidUtils.showToast(this, messageId);
        startActivity(new Intent(this, LauncherActivity.class));
        finish();
    }

    private boolean shouldFallbackOnWebapp(Object context) {
        if (context instanceof Uri) {
            Uri unresolved = (Uri) context;
            // resolved to a soundcloud.com url ?
            return "http".equals(unresolved.getScheme()) || "https".equals(unresolved.getScheme());
        }
        return false;
    }

    private void startActivityForResource(PublicApiResource resource) {
        trackForegroundEventForResource(getIntent(), resource);

        if (resource instanceof PublicApiTrack) {
            fireAndForget(playbackOperations.startPlayback(((PublicApiTrack) resource), Screen.DEEPLINK, loadRelated));
            startStreamScreenWithAnExpandedPlayer();
        } else {
            Intent intent = resource.getViewIntent();
            if (intent != null) {
                Screen.DEEPLINK.addToIntent(intent);
                startActivity(intent);
            } else {
                Log.e(SoundCloudApplication.TAG, "Cannot find view intent for resource " + resource);
            }
        }
    }

    private void startStreamScreenWithAnExpandedPlayer() {
        Intent intent = new Intent(Actions.STREAM)
                .putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void startHomeScreen() {
        if (accountOperations.isCrawler()) {
            accountOperations.clearLoggedInUser();
        }
        Intent intent = new Intent(Actions.STREAM)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void trackForegroundEventForResource(Intent intent, PublicApiResource resource) {
        trackForegroundEvent(ForegroundEvent.open(Screen.DEEPLINK, getReferrer(intent), resource.getUrn()));
    }

    private void trackForegroundEvent(Referrer referrer) {
        trackForegroundEvent(ForegroundEvent.open(Screen.DEEPLINK, referrer));
    }

    private void trackForegroundEvent(ForegroundEvent event) {
        getEventBus().publish(EventQueue.TRACKING, event);
    }

    private Referrer getReferrer(Intent intent) {
        return referrerResolver.getReferrerFromIntent(intent, getResources());
    }
}
