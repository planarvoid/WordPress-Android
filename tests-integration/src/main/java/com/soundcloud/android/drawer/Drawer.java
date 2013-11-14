package com.soundcloud.android.drawer;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;


public class Drawer extends ActivityTestCase<LauncherActivity> {

    public Drawer() {
        super(LauncherActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        IntegrationTestHelper.loginAsDefault(getInstrumentation());
        super.setUp();
    }

    //if user opens overflow menu while drawer is open, drawer remains open
    public void testOpeningOverflowDoesNotCloseDrawer() throws Exception {
        menuScreen.clickHomeButton();
    }

    //if user selects something from overflow menu while drawer is open, drawer closes before opening overflow menu item
    public void testDrawerClosesWhenOverflowMenuItemPicked() {

    }

    public void testDrawerProfileButtonOpensProfile() {

    }

    public void testDrawerOpensSettings() {

    }

    //user image and follower count update
    public void testDrawerShowsFollowers() {

    }

    public void testDrawerOpensLikes() {

    }
}

