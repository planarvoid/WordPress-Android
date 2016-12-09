package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetPolicyCheckTime;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.setPolicyCheckTime;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.BasicSettingsScreen;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.screens.elements.GoBackOnlineDialogElement;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class GoBackOnlineTest extends ActivityTest<MainActivity> {

    private final OfflineContentHelper offlineContentHelper;
    private Context context;

    public GoBackOnlineTest() {
        super(MainActivity.class);
        offlineContentHelper = new OfflineContentHelper();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getTargetContext();
        offlineContentHelper.clearOfflineContent(context);
        resetPolicyCheckTime(context);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.offlineUser;
    }

    public void testRemovesOfflinePlaylistAfter30DaysOffline() {
        enableOfflineContent(context);

        mainNavHelper.goToCollections()
                     .clickPlaylistsPreview()
                     .scrollToFirstPlaylist()
                     .click()
                     .clickDownloadToggle()
                     .goBackToPlaylists()
                     .goBackToCollections();

        final BasicSettingsScreen basicSettingsScreen = mainNavHelper.goToBasicSettings();

        networkManagerClient.switchWifiOff();
        resetPolicyCheckTime(context);
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(context, getPreviousDate(30, TimeUnit.DAYS).getTime());

        final GoBackOnlineDialogElement goBackOnlineDialog = basicSettingsScreen.goBackAndDisplayGoBackOnlineDialog();
        assertThat("Go back online dialog should be visible", goBackOnlineDialog, is(visible()));
        goBackOnlineDialog.clickContinue();

        DownloadImageViewElement downloadElement = mainNavHelper.goToCollections()
                                                                .clickPlaylistsPreview()
                                                                .scrollToFirstPlaylist()
                                                                .downloadElement();

        assertThat("Save offline menu element should be visible", !downloadElement.isVisible());
    }

    public void testDisplaysGoBackOnline() {
        enableOfflineContent(context);
        final BasicSettingsScreen settingsScreen = mainNavHelper.goToBasicSettings();

        networkManagerClient.switchWifiOff();
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(27, TimeUnit.DAYS).getTime());
        resetPolicyCheckTime(context);

        final GoBackOnlineDialogElement goBackOnlineDialog = settingsScreen.goBackAndDisplayGoBackOnlineDialog();
        assertThat("Go back online dialog should be visible", goBackOnlineDialog, is(visible()));

        goBackOnlineDialog.clickContinue();
        assertThat("Go back online dialog should be dismissed", goBackOnlineDialog, is(not(visible())));
    }

    public void testDisplaysGoBackOnlineOnlyOnceADay() {
        enableOfflineContent(context);
        final BasicSettingsScreen settingsScreen = mainNavHelper.goToBasicSettings();

        networkManagerClient.switchWifiOff();
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(27, TimeUnit.DAYS).getTime());
        setPolicyCheckTime(context, System.currentTimeMillis());

        assertThat(settingsScreen.goBackAndDisplayGoBackOnlineDialog(), not(visible()));
    }

    public void testDoesNotDisplayGoBackOnlineWhenPolicyCanBeUpdated() {
        enableOfflineContent(context);
        final BasicSettingsScreen settingsScreen = mainNavHelper.goToBasicSettings();

        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(27, TimeUnit.DAYS).getTime());
        resetPolicyCheckTime(context);

        assertThat(settingsScreen.goBackAndDisplayGoBackOnlineDialog(), not(visible()));
    }

    public void testDoesNotDisplayDialogWhenOfflineForLessThan27Days() {
        enableOfflineContent(context);
        final BasicSettingsScreen settingsScreen = mainNavHelper.goToBasicSettings();

        networkManagerClient.switchWifiOff();
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(26, TimeUnit.DAYS).getTime());
        resetPolicyCheckTime(context);

        assertThat(settingsScreen.goBackAndDisplayGoBackOnlineDialog(), not(visible()));
    }

    private Date getPreviousDate(int time, TimeUnit timeUnit) {
        return new Date(System.currentTimeMillis() - timeUnit.toMillis(time));
    }
}
