package com.soundcloud.android.tests.settings;

import static com.soundcloud.android.framework.TestUser.defaultUser;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.settings.CopyrightScreen;
import com.soundcloud.android.screens.settings.LegalScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class LegalTest extends ActivityTest<MainActivity> {
    private LegalScreen legalScreen;

    public LegalTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        legalScreen = mainNavHelper.goToLegal();
    }

    @Test
    public void testGoingToCopyright() throws Exception {
        CopyrightScreen copyrightScreen = legalScreen.clickCopyrightLink();
        assertTrue(copyrightScreen.isVisible());
    }
}
