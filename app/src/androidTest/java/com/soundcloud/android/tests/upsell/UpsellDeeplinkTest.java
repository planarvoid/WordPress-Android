package com.soundcloud.android.tests.upsell;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.GoogleAccountTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.matcher.view.IsVisible;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.TestConsts;

import android.content.Intent;

public class UpsellDeeplinkTest extends ActivityTest<ResolveActivity> {

    public UpsellDeeplinkTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.upsellUser;
    }

    @Override
    protected void beforeStartActivity() {
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(TestConsts.UPGRADE_URI));
        super.setUp();
    }

    @GoogleAccountTest
    public void testSettingsUpsellImpressionAndClick() {
        UpgradeScreen upgradeScreen = new UpgradeScreen(solo);

        assertThat(upgradeScreen, is(visible()));
        assertThat(upgradeScreen.upgradeButton(), is(IsVisible.visible()));
    }

}
