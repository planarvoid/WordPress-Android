package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class StartStationTest extends ActivityTest<LauncherActivity> {
    private StreamScreen streamScreen;

    public StartStationTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.streamUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setRequiredEnabledFeatures(Flag.STATIONS);

        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();
        streamScreen = new StreamScreen(solo);
    }

    public void testStartRadio() {
        final VisualPlayerElement player = streamScreen.clickFirstTrackOverflowButton().clickStartRadio();

        assertThat(player, is(visible()));
    }

    public void testStartRadioVisibleButDisabledWhenUserHasNoNetworkConnectivity() {
        toastObserver.observe();
        networkManagerClient.switchWifiOff();

        final VisualPlayerElement playerElement = streamScreen.clickFirstTrackOverflowButton().clickStartRadio();

        assertThat(playerElement, is(not(visible())));
        assertFalse(toastObserver.wasToastObserved(solo.getString(R.string.unable_to_start_radio)));

        networkManagerClient.switchWifiOn();
    }

}
