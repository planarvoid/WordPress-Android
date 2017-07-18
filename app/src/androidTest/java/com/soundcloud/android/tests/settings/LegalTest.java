package com.soundcloud.android.tests.settings;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.settings.CopyrightScreen;
import com.soundcloud.android.screens.settings.LegalScreen;
import com.soundcloud.android.tests.ActivityTest;

public class LegalTest extends ActivityTest<MainActivity> {
    private LegalScreen legalScreen;

    public LegalTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.defaultUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        legalScreen = mainNavHelper.goToLegal();
    }

    public void testGoingToCopyright() {
        CopyrightScreen copyrightScreen = legalScreen.clickCopyrightLink();
        assertTrue(copyrightScreen.isVisible());
    }
}
