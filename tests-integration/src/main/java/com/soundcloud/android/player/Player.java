package com.soundcloud.android.player;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.elements.PlayerElement;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

import android.test.suitebuilder.annotation.Suppress;

public class Player extends ActivityTestCase<MainActivity> {

    private PlayerElement playerElement;

    public Player() {
        super(MainActivity.class);
        setDependsOn(Feature.VISUAL_PLAYER);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());

        super.setUp();

        playerElement = new PlayerElement(solo);
    }

    public void testPlayerExpandsOnFooterTap() {
        playerElement.tapFooter();
        assertEquals("Player should be expanded", true, playerElement.isExpanded());
    }

    public void testPlayerCollapsesWhenBackButtonIsPressed() {
        playerElement.tapFooter();
        solo.goBack();
        assertEquals("Player should be collapsed", true, playerElement.isCollapsed());
    }

    public void testPlayStateCanBeToggledFromPlayerFooter() {
        assertEquals("Footer should show paused state", false, playerElement.isFooterInPlayingState());
        playerElement.toggleFooterPlay();
        assertEquals("Footer should show playing state", true, playerElement.isFooterInPlayingState());
    }

    public void testPlayerIsExpandedAfterClickingTrack() {
        playTrack();
        assertEquals("Player should be expanded", true, playerElement.isExpanded());
    }

    private PlayerElement playTrack() {
        StreamScreen streamScreen = new StreamScreen(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
        return streamScreen.clickFirstTrack();
    }

}
