package com.soundcloud.android.main;

import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesController;
import com.soundcloud.android.gcm.GcmManager;
import com.soundcloud.android.main.LegacyNavigationFragment.NavItem;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.lightcycle.LightCycle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;

import javax.inject.Inject;

public class MainActivity extends ScActivity implements LegacyNavigationFragment.NavigationCallbacks {

    public static final String EXTRA_REFRESH_STREAM = "refresh_stream";
    public static final String EXTRA_FROM_SIGNIN = "from_sign_in";

    @Inject PlaySessionController playSessionController;
    @Inject CastConnectionHelper castConnectionHelper;

    @Inject @LightCycle NavigationPresenter mainPresenter;
    @Inject @LightCycle PlayerController playerController;
    @Inject @LightCycle ActionBarHelper actionBarHelper;
    @Inject @LightCycle GcmManager gcmManager;
    @Inject @LightCycle FacebookInvitesController facebookInvitesController;

    protected void onCreate(Bundle savedInstanceState) {
        redirectToResolverIfNecessary(getIntent());
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            playSessionController.reloadQueueAndShowPlayerIfEmpty();
            setupEmailOptIn();
        }
        castConnectionHelper.reconnectSessionIfPossible();
    }

    @Override
    protected void setActivityContentView() {
        mainPresenter.setBaseLayout(this);
    }

    private void setupEmailOptIn() {
        if (getIntent().getBooleanExtra(EXTRA_FROM_SIGNIN, false)) {
            EmailOptInDialogFragment.show(this);
        }
    }

    @Override
    public void onBackPressed() {
        if (accountOperations.isCrawler()
                || !(playerController.handleBackPressed() || mainPresenter.handleBackPressed())) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        redirectToResolverIfNecessary(intent);
        super.onNewIntent(intent);
        trackForegroundEvent(intent);
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

    private void trackForegroundEvent(Intent intent) {
        if (Referrer.hasReferrer(intent) && Screen.hasScreen(intent)) {
            eventBus.publish(EventQueue.TRACKING, ForegroundEvent.open(Screen.fromIntent(intent), Referrer.fromIntent(intent)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!accountOperations.isUserLoggedIn()) {
            accountOperations.triggerLoginFlow(this);
            finish();
        }

        if (shouldTrackScreen()) {
            mainPresenter.trackScreen();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        configureMainOptionMenuItems(menu);
        mainPresenter.onInvalidateOptionsMenu();
        return true;
    }

    @Override
    public void onSmoothSelectItem(NavItem item) {
        mainPresenter.onSmoothSelectItem(item);
    }

    @Override
    public void onSelectItem(NavItem item) {
        mainPresenter.onSelectItem(item);
    }

}
