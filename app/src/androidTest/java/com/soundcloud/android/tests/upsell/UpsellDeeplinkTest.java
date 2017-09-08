package com.soundcloud.android.tests.upsell;

import static android.content.Intent.ACTION_VIEW;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.framework.TestUser.upsellUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableUpsell;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.UPGRADE_URI;
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
import org.junit.Test;

import android.content.Intent;

public class UpsellDeeplinkTest extends ActivityTest<ResolveActivity> {

    public UpsellDeeplinkTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return upsellUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        enableUpsell(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setActivityIntent(new Intent(ACTION_VIEW).setData(UPGRADE_URI));
        super.setUp();
    }

    @Test
    public void testSettingsUpsellImpressionAndClick() throws Exception {
        UpgradeScreen upgradeScreen = new UpgradeScreen(solo);

        assertThat(upgradeScreen, is(visible()));
        assertThat(upgradeScreen.upgradeButton(), is(IsVisible.visible()));
    }

}
