package com.soundcloud.android.main;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.facebookinvites.FacebookInvitesController;
import com.soundcloud.android.gcm.GcmManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.lightcycle.LightCycle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import javax.inject.Inject;

public class MainActivity extends PlayerActivity {

    @Inject PlaySessionController playSessionController;
    @Inject CastConnectionHelper castConnectionHelper;
    @Inject Navigator navigator;
    @Inject FeatureFlags featureFlags;

    @Inject @LightCycle MainTabsPresenter mainPresenter;
    @Inject @LightCycle GcmManager gcmManager;
    @Inject @LightCycle FacebookInvitesController facebookInvitesController;

    protected void onCreate(Bundle savedInstanceState) {
        redirectToResolverIfNecessary(getIntent());
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            playSessionController.reloadQueueAndShowPlayerIfEmpty();
        }
        castConnectionHelper.reconnectSessionIfPossible();
    }

    @Override
    protected void setActivityContentView() {
        mainPresenter.setBaseLayout(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        redirectToResolverIfNecessary(intent);
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void redirectToResolverIfNecessary(Intent intent) {
        final Uri data = intent.getData();
        if (data != null
                && ResolveActivity.accept(data, getResources())
                && !NavigationIntentHelper.resolvesToNavigationItem(data)) {
            redirectFacebookDeeplinkToResolver(data);
        }
    }

    private void redirectFacebookDeeplinkToResolver(Uri data) {
        startActivity(new Intent(this, ResolveActivity.class).setAction(Intent.ACTION_VIEW).setData(data));
        finish();
    }

    private void setupUpgradeUpsell() {
        if (getIntent().getBooleanExtra(Navigator.EXTRA_UPGRADE_INTENT, false)) {
            getIntent().removeExtra(Navigator.EXTRA_UPGRADE_INTENT);
            navigator.openUpgradeNoTransition(this);
        }
    }

    private void fetchFeatureFlags() {
        if (!this.isChangingConfigurations() && featureFlags.isEnabled(Flag.REMOTE_FEATURE_TOGGLES)) {
            featureFlags.fetchRemoteFlags(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupUpgradeUpsell();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        fetchFeatureFlags();
    }

    @Override
    public Screen getScreen() {
        return Screen.UNKNOWN;
    }

}
