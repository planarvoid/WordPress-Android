package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.CreatePlaylistScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class TrackItemOverflowMenuTest extends ActivityTest<LauncherActivity> {
    private StreamScreen streamScreen;

    public TrackItemOverflowMenuTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.streamUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();
        streamScreen = new StreamScreen(solo);
    }

    //FIXME: https://github.com/soundcloud/SoundCloud-Android/issues/2914
    public void ignore_testClickingAddToPlaylistOverflowMenuItemOpensDialog() {
        final CreatePlaylistScreen createPlaylistScreen = streamScreen.clickFirstTrackOverflowButton().
                clickAddToPlaylist().
                clickCreateNewPlaylist();

        assertThat(createPlaylistScreen, is(com.soundcloud.android.framework.matcher.screen.IsVisible.visible()));
        assertThat(createPlaylistScreen.offlineCheck().isVisible(), is(false));
    }

    public void testPlayRelatedTracks() {
        final VisualPlayerElement player = streamScreen.clickFirstTrackOverflowButton().clickPlayRelatedTracks();

        assertThat(player, is(visible()));
    }

    public void testPlayRelatedTracksVisibleButDisabledWhenUserHasNoNetworkConnectivity() {
        toastObserver.observe();
        networkManagerClient.switchWifiOff();

        final VisualPlayerElement playerElement = streamScreen.clickFirstTrackOverflowButton().clickPlayRelatedTracks();

        assertThat(playerElement, is(not(visible())));
        assertFalse(toastObserver.wasToastObserved(solo.getString(R.string.unable_to_play_related_tracks)));

        networkManagerClient.switchWifiOn();
    }
}
