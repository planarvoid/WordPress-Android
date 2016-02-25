package com.soundcloud.android.main;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.facebookinvites.FacebookInvitesController;
import com.soundcloud.android.gcm.GcmManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.lightcycle.LightCycle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;

import javax.inject.Inject;

public class MainActivity extends PlayerActivity {

    public static final String EXTRA_REFRESH_STREAM = "refresh_stream";

    @Inject PlaySessionController playSessionController;
    @Inject CastConnectionHelper castConnectionHelper;
    @Inject Navigator navigator;

    @Inject @LightCycle MainTabsPresenter mainPresenter;
    @Inject @LightCycle ActionBarHelper actionBarHelper;
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

    @Override
    protected void onStart() {
        super.onStart();
        setupUpgradeUpsell();
    }

    private void setupUpgradeUpsell() {
        if (getIntent().getBooleanExtra(Navigator.EXTRA_UPGRADE_INTENT, false)) {
            getIntent().removeExtra(Navigator.EXTRA_UPGRADE_INTENT);
            navigator.openUpgrade(this);
        }
    }

    @Override
    public Screen getScreen() {
        return mainPresenter.getScreen();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        configureMainOptionMenuItems(menu);
        return true;
    }
}
