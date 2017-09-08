package com.soundcloud.android.tests.offline;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.framework.TestUser.offlineUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetPolicyCheckTime;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.setPolicyCheckTime;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.screens.elements.GoBackOnlineDialogElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

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
        return offlineUser;
    }

    @Test
    public void testRemovesOfflinePlaylistAfter30DaysOffline() throws Exception {
        enableOfflineContent(context);

        mainNavHelper.goToCollections()
                     .clickPlaylistsPreview()
                     .scrollToFirstPlaylist()
                     .click()
                     .clickDownloadButton()
                     .goBackToPlaylists()
                     .goBackToCollections();
        mainNavHelper.goToBasicSettings();

        connectionHelper.setNetworkConnected(false);
        resetPolicyCheckTime(context);
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(context, getPreviousDate(30, DAYS).getTime());

        final GoBackOnlineDialogElement goBackOnlineDialog = new GoBackOnlineDialogElement(solo);
        assertThat("Go back online dialog should be visible", goBackOnlineDialog, is(visible()));
        goBackOnlineDialog.clickContinue();
        solo.goBack();

        DownloadImageViewElement downloadElement = mainNavHelper.goToCollections()
                                                                .clickPlaylistsPreview()
                                                                .scrollToFirstPlaylist()
                                                                .downloadElement();

        assertThat("Save offline menu element should be visible", !downloadElement.isVisible());
    }

    @Test
    public void testDisplaysGoBackOnline() throws Exception {
        enableOfflineContent(context);
        mainNavHelper.goToBasicSettings();

        connectionHelper.setNetworkConnected(false);
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(27, DAYS).getTime());
        resetPolicyCheckTime(context);

        final GoBackOnlineDialogElement goBackOnlineDialog = new GoBackOnlineDialogElement(solo);
        assertThat("Go back online dialog should be visible", goBackOnlineDialog, is(visible()));

        goBackOnlineDialog.clickContinue();
        assertThat("Go back online dialog should be dismissed", goBackOnlineDialog, is(not(visible())));
    }

    @Test
    public void testDisplaysGoBackOnlineOnlyOnceADay() throws Exception {
        enableOfflineContent(context);
        mainNavHelper.goToBasicSettings();

        connectionHelper.setNetworkConnected(false);
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(27, DAYS).getTime());
        setPolicyCheckTime(context, currentTimeMillis());

        final GoBackOnlineDialogElement goBackOnlineDialog = new GoBackOnlineDialogElement(solo);
        assertThat("Go back online dialog should not be visible", goBackOnlineDialog, not(visible()));
    }

    @Test
    public void testDoesNotDisplayGoBackOnlineWhenPolicyCanBeUpdated() throws Exception {
        enableOfflineContent(context);
        mainNavHelper.goToBasicSettings();

        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(27, DAYS).getTime());
        resetPolicyCheckTime(context);

        final GoBackOnlineDialogElement goBackOnlineDialog = new GoBackOnlineDialogElement(solo);
        assertThat("Go back online dialog should not be visible", goBackOnlineDialog, not(visible()));
    }

    @Test
    public void testDoesNotDisplayDialogWhenOfflineForLessThan27Days() throws Exception {
        enableOfflineContent(context);
        mainNavHelper.goToBasicSettings();

        connectionHelper.setNetworkConnected(false);
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(26, DAYS).getTime());
        resetPolicyCheckTime(context);

        final GoBackOnlineDialogElement goBackOnlineDialog = new GoBackOnlineDialogElement(solo);
        assertThat("Go back online dialog should not be visible", goBackOnlineDialog, not(visible()));
    }

    private Date getPreviousDate(int time, TimeUnit timeUnit) {
        return new Date(currentTimeMillis() - timeUnit.toMillis(time));
    }
}
