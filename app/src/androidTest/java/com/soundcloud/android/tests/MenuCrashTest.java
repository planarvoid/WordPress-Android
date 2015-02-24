package com.soundcloud.android.tests;

import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class MenuCrashTest extends ActivityTest<OnboardActivity> {

    public MenuCrashTest() {
        super(OnboardActivity.class);
    }

    public void testMenuCrash() throws Exception {
        new HomeScreen(solo)
                .clickLogInButton()
                .loginAs("andtestpl", "addtest");

        StreamScreen stream = new StreamScreen(solo);
        VisualPlayerElement player = stream.clickFirstTrack();
        player.clickMenu();
    }
}
