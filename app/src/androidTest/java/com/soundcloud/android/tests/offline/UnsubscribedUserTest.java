package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetPolicyCheckTime;
import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.framework.matcher.element.IsVisible;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.BasicSettingsScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.hamcrest.core.IsNot;

import android.content.Context;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class UnsubscribedUserTest extends ActivityTest<MainActivity> {

    private final OfflineContentHelper offlineContentHelper;

    public UnsubscribedUserTest() {
        super(MainActivity.class);
        offlineContentHelper = new OfflineContentHelper();
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.likesUser;
    }

    @Ignore
    public void testDownloadIsUnavailableWhenTheyAccessLikes() throws Exception {
        final ViewElement offlineToggle = mainNavHelper.goToTrackLikes()
                                                       .offlineToggle();

        assertThat(offlineToggle, is(not(visible())));
    }

    @Ignore
    public void testDownloadIsUnavailableWhenTheyAccessPlaylists() throws Exception {
        final ViewElement offlineItem = mainNavHelper.goToCollections()
                                                     .clickPlaylistsPreview()
                                                     .clickOnFirstPlaylist()
                                                     .getDownloadToggle();

        assertThat(offlineItem, is(not(visible())));
    }

    @Ignore
    public void testDownloadIsUnavailableWhenTheyAccessPlaylistDetailScreen() throws Exception {
        final ViewElement offlineItem = mainNavHelper.goToCollections()
                                                     .clickPlaylistsPreview()
                                                     .clickOnFirstPlaylist()
                                                     .getDownloadToggle();

        assertThat(offlineItem, is(not(visible())));
    }

    public void testDoesNotDisplayGoBackOnlineWhenOfflineContentDisabled() {
        final Context context = getInstrumentation().getTargetContext();
        final BasicSettingsScreen settingsScreen = mainNavHelper.goToBasicSettings();

        networkManagerClient.switchWifiOff();
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(27, TimeUnit.DAYS).getTime());
        resetPolicyCheckTime(context);

        assertThat(settingsScreen.goBackAndDisplayGoBackOnlineDialog(), IsNot.not(IsVisible.visible()));
    }

    private Date getPreviousDate(int time, TimeUnit timeUnit) {
        return new Date(System.currentTimeMillis() - timeUnit.toMillis(time));
    }
}
