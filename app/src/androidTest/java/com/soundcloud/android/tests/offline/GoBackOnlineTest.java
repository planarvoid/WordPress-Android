package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetPolicyUpdateAndCheckTime;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.setPolicyCheckTime;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.setPolicyUpdateTime;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

import com.robotium.solo.Condition;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.BrokenScrollingTest;
import com.soundcloud.android.framework.helpers.MainNavigationHelper;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.androidnetworkmanagerclient.NetworkManagerClient;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class GoBackOnlineTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private static final Urn PLAYLIST = Urn.forPlaylist(100132741L);
    private static final Urn TRACK = Urn.forTrack(49173281L);
    private final OfflineContentHelper offlineContentHelper;
    private Context context;
    private NetworkManagerClient networkManagerClient;
    private Han testDriver;
    private Condition isWifiEnabled = new Condition() {
        @Override
        public boolean isSatisfied() {
            return networkManagerClient.isWifiEnabled();
        }
    };

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

    @BrokenScrollingTest
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
        resetPolicyUpdateAndCheckTime(context);
        setPolicyUpdateTime(context, getPreviousDate(30, TimeUnit.DAYS).getTime());

        // going back should prompt the go back online dialog
        testDriver.goBack();
        streamScreen
                .getGoBackOnlineDialog()
                .clickContinue();

        // offline content deleted so playlist should not be offline anymore
        ViewElement makeAvailableOfflineItem = new MainNavigationHelper(testDriver).goToCollections()
                .getPlaylists()
                .get(0)
                .clickOverflow()
                .getMakeAvailableOfflineItem();

        assertTrue(makeAvailableOfflineItem.isVisible());
    }

    public void testDisplaysGoBackOnline() {
        networkManagerClient.switchWifiOff();
        resetPolicyUpdateAndCheckTime(context);
        setPolicyUpdateTime(context, getPreviousDate(27, TimeUnit.DAYS).getTime());
        enableOfflineContent(context);

        final StreamScreen streamScreen = startMainActivity();
        assertThat(streamScreen.getGoBackOnlineDialog(), visible());
        streamScreen.getGoBackOnlineDialog().clickContinue();
        assertThat(streamScreen.getGoBackOnlineDialog(), not(visible()));
    }

    public void testDoesNotDisplayGoBackOnlineWhenOfflineContentDisabled() {
        networkManagerClient.switchWifiOff();
        resetPolicyUpdateAndCheckTime(context);
        setPolicyUpdateTime(context, getPreviousDate(27, TimeUnit.DAYS).getTime());
        disableOfflineContent(context);

        assertThat(startMainActivity().getGoBackOnlineDialog(), not(visible()));
    }

    public void testDisplaysGoBackOnlineOnlyOnceADay() {
        networkManagerClient.switchWifiOff();
        setPolicyCheckTime(context, System.currentTimeMillis());
        setPolicyUpdateTime(context, getPreviousDate(27, TimeUnit.DAYS).getTime());
        enableOfflineContent(context);

        assertThat(startMainActivity().getGoBackOnlineDialog(), not(visible()));
    }

    public void testDoesNotDisplayGoBackOnlineWhenPolicyCanBeUpdated() {
        networkManagerClient.switchWifiOn();
        // FIXME : This is temporary. Remove this as soon as we fix the network manger
        // notifying the wifi is up even though it is not connected yet.
        testDriver.waitForCondition(isWifiEnabled, 10000);
        resetPolicyUpdateAndCheckTime(context);
        setPolicyUpdateTime(context, getPreviousDate(27, TimeUnit.DAYS).getTime());
        enableOfflineContent(context);

        assertThat(startMainActivity().getGoBackOnlineDialog(), not(visible()));
    }

    public void testDoesNotDisplayDialogWhenOfflineForLessThan27Days() {
        networkManagerClient.switchWifiOff();
        resetPolicyUpdateAndCheckTime(context);
        setPolicyUpdateTime(context, getPreviousDate(26, TimeUnit.DAYS).getTime());
        enableOfflineContent(context);

        assertThat(startMainActivity().getGoBackOnlineDialog(), not(visible()));
    }

    private Date getPreviousDate(int time, TimeUnit timeUnit) {
        return new Date(System.currentTimeMillis() - timeUnit.toMillis(time));
    }
}
