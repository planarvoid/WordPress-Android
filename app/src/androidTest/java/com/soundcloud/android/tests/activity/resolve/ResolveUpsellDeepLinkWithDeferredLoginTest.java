package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.TestUser.upsellUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveUpsellDeepLinkWithDeferredLoginTest extends ResolveBaseTest {

    private HomeScreen homeScreen;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        homeScreen = new HomeScreen(solo);
    }

    @Override
    protected Uri getUri() {
        return TestConsts.UPGRADE_URI;
    }

    @Override
    protected TestUser getUserForLogin() {
        // Start off with logged out user to test deferred deep link
        return null;
    }

    @Override
    protected void beforeStartActivity() {
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }

    public void testResolveUpsellTracksRefParam() {
        assertTrue(waiter.expectToastWithText(toastObserver, resourceString(R.string.error_toast_user_not_logged_in)));

        UpgradeScreen upgradeScreen = homeScreen
                .clickLogInButton()
                .loginFromUpgradeDeepLink(upsellUser.getEmail(), upsellUser.getPassword());

        assertThat(upgradeScreen, is(visible()));
    }
}
