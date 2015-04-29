package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetPolicyUpdateCheckTime;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.setPolicyUpdateCheckTime;
import static com.soundcloud.android.framework.helpers.OfflineContentHelper.clearOfflineContent;
import static com.soundcloud.android.framework.helpers.OfflineContentHelper.insertOfflinePlaylistAndTrackWithPolicy;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

import com.robotium.solo.Condition;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.networkmanager.NetworkManager;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.screens.StreamScreen;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class GoBackOnlineTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private static final Urn PLAYLIST = Urn.forPlaylist(213L);
    private static final Urn TRACK = Urn.forTrack(123L);
    private Context context;
    private NetworkManager networkManager;
    private Han testDriver;
    private Condition isWifiEnabled = new Condition() {
        @Override
        public boolean isSatisfied() {
            return networkManager.isWifiEnabled();
        }
    };

    public GoBackOnlineTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        initTestDriver();
        context = getInstrumentation().getTargetContext();

        initNetworkManager();
        networkManager.switchWifiOn();
        TestUser.offlineUser.logIn(context);
        clearOfflineContent(context);

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        networkManager.switchWifiOn();
        super.tearDown();
    }

    private void initTestDriver() {
        testDriver = new Han(getInstrumentation());
        testDriver.setup();
    }

    private void initNetworkManager() {
        networkManager = new NetworkManager(context);
        if (!networkManager.bind()) {
            throw new IllegalStateException("Could not bind network manager");
        }
    }

    private StreamScreen startMainActivity() {
        getActivity();
        return new StreamScreen(testDriver);
    }

    public void testDisplaysGoBackOnline() throws Exception {
        networkManager.switchWifiOff();
        resetPolicyUpdateCheckTime(context);
        insertOfflinePlaylistAndTrackWithPolicy(context, PLAYLIST, TRACK, getPreviousDate(27, TimeUnit.DAYS));
        enableOfflineContent(context);

        final StreamScreen streamScreen = startMainActivity();
        assertThat(streamScreen.getGoBackOnlineDialog(), visible());
        streamScreen.getGoBackOnlineDialog().clickContinue();
        assertThat(streamScreen.getGoBackOnlineDialog(), not(visible()));
    }

    public void testDoesNotDisplayGoBackOnlineWhenOfflineContentDisabled() throws Exception {
        networkManager.switchWifiOff();
        resetPolicyUpdateCheckTime(context);
        insertOfflinePlaylistAndTrackWithPolicy(context, PLAYLIST, TRACK, getPreviousDate(27, TimeUnit.DAYS));
        disableOfflineContent(context);

        assertThat(startMainActivity().getGoBackOnlineDialog(), not(visible()));
    }

    public void testDisplaysGoBackOnlineOnlyOnceADay() throws Exception {
        networkManager.switchWifiOff();
        setPolicyUpdateCheckTime(context, System.currentTimeMillis());
        insertOfflinePlaylistAndTrackWithPolicy(context, PLAYLIST, TRACK, getPreviousDate(27, TimeUnit.DAYS));
        enableOfflineContent(context);

        assertThat(startMainActivity().getGoBackOnlineDialog(), not(visible()));
    }

    public void testDoesNotDisplayGoBackOnlineWhenPolicyCanBeUpdated() throws Exception {
        networkManager.switchWifiOn();
        // FIXME : This is temporary. Remove this as soon as we fix the network manger
        // notifying the wifi is up even though it is not connected yet.
        testDriver.waitForCondition(isWifiEnabled, 10000);
        resetPolicyUpdateCheckTime(context);
        insertOfflinePlaylistAndTrackWithPolicy(context, PLAYLIST, TRACK, getPreviousDate(27, TimeUnit.DAYS));
        enableOfflineContent(context);

        assertThat(startMainActivity().getGoBackOnlineDialog(), not(visible()));
    }

    public void testDoesNotDisplayDialogWhenOfflineForLessThan27Days() throws Exception {
        networkManager.switchWifiOff();
        resetPolicyUpdateCheckTime(context);
        insertOfflinePlaylistAndTrackWithPolicy(context, PLAYLIST, TRACK, getPreviousDate(26, TimeUnit.DAYS));
        enableOfflineContent(context);

        assertThat(startMainActivity().getGoBackOnlineDialog(), not(visible()));
    }

    private Date getPreviousDate(int time, TimeUnit timeUnit) {
        return new Date(System.currentTimeMillis() - timeUnit.toMillis(time));
    }
}
