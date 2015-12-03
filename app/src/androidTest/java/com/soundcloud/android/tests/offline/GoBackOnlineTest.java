package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetPolicyCheckTime;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.setPolicyCheckTime;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.framework.helpers.MainNavigationHelper;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.GoBackOnlineDialogElement;
import com.soundcloud.androidnetworkmanagerclient.NetworkManagerClient;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class GoBackOnlineTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private final OfflineContentHelper offlineContentHelper;
    private Context context;
    private NetworkManagerClient networkManagerClient;
    private Han testDriver;

    public GoBackOnlineTest() {
        super(MainActivity.class);
        offlineContentHelper = new OfflineContentHelper();
    }

    @Override
    public void setUp() throws Exception {
        initTestDriver();
        context = getInstrumentation().getTargetContext();

        initNetworkManager();
        networkManagerClient.switchWifiOn();
        TestUser.offlineUser.logIn(context);

        offlineContentHelper.clearOfflineContent(context);
        resetPolicyCheckTime(context);

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        networkManagerClient.switchWifiOn();
        super.tearDown();
    }

    private void initTestDriver() {
        testDriver = new Han(getInstrumentation());
        testDriver.setup();
    }

    private void initNetworkManager() {
        networkManagerClient = new NetworkManagerClient(context);
        if (!networkManagerClient.bind()) {
            throw new IllegalStateException("Could not bind network manager");
        }
    }

    private StreamScreen startMainActivity() {
        getActivity();
        return new StreamScreen(testDriver);
    }

    public void testRemovesOfflinePlaylistAfter30DaysOffline() {
        enableOfflineContent(context);

        // make playlist available offline
        StreamScreen streamScreen = startMainActivity();
        final MainNavigationHelper mainNavigationHelper = new MainNavigationHelper(testDriver);
        mainNavigationHelper.goToCollections()
                .getPlaylists()
                .get(0)
                .clickOverflow()
                .clickMakeAvailableOffline();

        // open other activity
        mainNavigationHelper.goToBasicSettings();

        networkManagerClient.switchWifiOff();
        resetPolicyCheckTime(context);
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(context, getPreviousDate(30, TimeUnit.DAYS).getTime());

        // going back should prompt the go back online dialog
        testDriver.goBack();

        GoBackOnlineDialogElement goBackOnlineDialog = streamScreen.getGoBackOnlineDialog();
        assertThat("Go back online dialog should be visible", goBackOnlineDialog, is(visible()));
        goBackOnlineDialog.clickContinue();

        // offline content deleted so playlist should not be offline anymore
        ViewElement makeAvailableOfflineItem = new MainNavigationHelper(testDriver).goToCollections()
                .getPlaylists()
                .get(0)
                .clickOverflow()
                .getMakeAvailableOfflineItem();

        assertTrue(makeAvailableOfflineItem.isVisible());
    }

    @Ignore // for Karolina to fix
    public void testDisplaysGoBackOnline() {
        enableOfflineContent(context);

        networkManagerClient.switchWifiOff();
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(27, TimeUnit.DAYS).getTime());

        final GoBackOnlineDialogElement goBackOnlineDialog = startMainActivity().getGoBackOnlineDialog();
        assertThat(goBackOnlineDialog, is(visible()));

        goBackOnlineDialog.clickContinue();
        assertThat(goBackOnlineDialog, is(not(visible())));
    }

    public void testDoesNotDisplayGoBackOnlineWhenOfflineContentDisabled() {
        disableOfflineContent(context);
        networkManagerClient.switchWifiOff();

        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(27, TimeUnit.DAYS).getTime());

        assertThat(startMainActivity().getGoBackOnlineDialog(), not(visible()));
    }

    public void testDisplaysGoBackOnlineOnlyOnceADay() {
        enableOfflineContent(context);
        networkManagerClient.switchWifiOff();

        setPolicyCheckTime(context, System.currentTimeMillis());
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(27, TimeUnit.DAYS).getTime());

        assertThat(startMainActivity().getGoBackOnlineDialog(), not(visible()));
    }

    public void testDoesNotDisplayGoBackOnlineWhenPolicyCanBeUpdated() {
        enableOfflineContent(context);
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(27, TimeUnit.DAYS).getTime());

        assertThat(startMainActivity().getGoBackOnlineDialog(), not(visible()));
    }

    public void testDoesNotDisplayDialogWhenOfflineForLessThan27Days() {
        enableOfflineContent(context);
        networkManagerClient.switchWifiOff();
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(26, TimeUnit.DAYS).getTime());

        assertThat(startMainActivity().getGoBackOnlineDialog(), not(visible()));
    }

    private Date getPreviousDate(int time, TimeUnit timeUnit) {
        return new Date(System.currentTimeMillis() - timeUnit.toMillis(time));
    }
}
