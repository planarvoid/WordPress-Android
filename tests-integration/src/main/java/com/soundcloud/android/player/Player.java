package com.soundcloud.android.player;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

import android.test.suitebuilder.annotation.Suppress;

public class Player extends ActivityTestCase<MainActivity> {

    private PlayerScreen playerScreen;

    public Player() {
        super(MainActivity.class);
        setDependsOn(Feature.VISUAL_PLAYER);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());

        super.setUp();

        playerScreen = new PlayerScreen(solo);
    }

    public void testPlayerExpandsOnFooterTap() {
        playerScreen.tapFooter();
        assertEquals("Player should be expanded", true, playerScreen.isExpanded());
    }

    public void testPlayerCollapsesWhenBackButtonIsPressed() {
        playerScreen.tapFooter();
        solo.goBack();
        assertEquals("Player should be collapsed", true, playerScreen.isCollapsed());
    }

    public void testPlayStateCanBeToggledFromPlayerFooter() {
        assertEquals("Footer should show paused state", false, playerScreen.isFooterInPlayingState());
        playerScreen.toggleFooterPlay();
        assertEquals("Footer should show playing state", true, playerScreen.isFooterInPlayingState());
    }

    @Suppress
    public void testPlayerIsExpandedAfterClickingTrack() {
        playTrack();
        assertEquals("Player should be expanded", true, playerScreen.isExpanded());
    }

    private PlayerScreen playTrack() {
        StreamScreen streamScreen = new StreamScreen(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
        return streamScreen.clickFirstTrack();
    }

}
