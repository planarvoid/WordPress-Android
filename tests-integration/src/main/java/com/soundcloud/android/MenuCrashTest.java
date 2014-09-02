package com.soundcloud.android;

import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTestCase;

public class MenuCrashTest extends ActivityTestCase<OnboardActivity> {

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
