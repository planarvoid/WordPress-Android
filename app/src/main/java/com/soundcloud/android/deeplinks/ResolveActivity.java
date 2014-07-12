package com.soundcloud.android.deeplinks;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.main.TrackedActivity;
import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.onboarding.auth.FacebookSSOActivity;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.tasks.FetchModelTask;
import com.soundcloud.android.utils.AndroidUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import javax.inject.Inject;

public class ResolveActivity extends TrackedActivity implements FetchModelTask.Listener<PublicApiResource> {

    @Inject PublicCloudAPI oldCloudAPI;
    @Nullable private ResolveFetchTask resolveTask;
    @Inject AccountOperations accountOperations;
    @Inject FeatureFlags featureFlags;
    @Inject PlaybackOperations playbackOperations;

    public ResolveActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    ResolveActivity(PlaybackOperations playbackOperations, FeatureFlags featureFlags) {
        SoundCloudApplication.getObjectGraph().inject(this);
        this.playbackOperations = playbackOperations;
        this.featureFlags = featureFlags;
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

        if (accountOperations.isUserLoggedIn()) {
            Intent intent = getIntent();
            Uri data = intent.getData();

            final boolean shouldResolve = data != null &&
                    (Intent.ACTION_VIEW.equals(intent.getAction()) || FacebookSSOActivity.handleFacebookView(this, intent));

            if (shouldResolve) {
                fetchData(data);
            } else {
                finish();
            }
        } else {
            launchApplicationWithMessage(R.string.error_toast_user_not_logged_in);
        }
    }

    private void fetchData(Uri data) {
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
        resolveTask = new ResolveFetchTask(oldCloudAPI);
        resolveTask.setListener(this);
        resolveTask.execute(data);
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

    @Override
    public void onSuccess(PublicApiResource resource) {
        resolveTask = null;
        startActivityForResource(resource);
        finish();
    }

    private void startActivityForResource(PublicApiResource resource) {
        if (resource instanceof Playable && featureFlags.isEnabled(Feature.VISUAL_PLAYER)) {
            startPlayback(resource);
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
        Intent intent = new Intent(Actions.STREAM);
        intent.putExtra(MainActivity.EXPAND_PLAYER, true);
        startActivity(intent);
    }

    private void startPlayback(PublicApiResource resource) {
        if (resource instanceof PublicApiPlaylist) {
            playbackOperations.playPlaylist(((PublicApiPlaylist) resource), Screen.DEEPLINK);
        } else if (resource instanceof PublicApiTrack) {
            playbackOperations.playTrack(this, ((PublicApiTrack) resource), Screen.DEEPLINK);
        } else {
            throw new IllegalArgumentException("Unknown resource type : " + resource.getClass().getCanonicalName());
        }
    }
}


