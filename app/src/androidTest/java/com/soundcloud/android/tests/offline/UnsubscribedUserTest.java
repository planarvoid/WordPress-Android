package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.TestUser.unubscribedlikesUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetPolicyCheckTime;
import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.framework.matcher.element.IsVisible;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.GoBackOnlineDialogElement;
import com.soundcloud.android.tests.ActivityTest;
import org.hamcrest.core.IsNot;
import org.junit.Test;

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
        return unubscribedlikesUser;
    }

    @Test
    @org.junit.Ignore
    @Ignore
    public void testDownloadIsUnavailableWhenTheyAccessLikes() throws Exception {
        final ViewElement offlineToggle = mainNavHelper.goToTrackLikes()
                                                       .offlineButton();

        assertThat(offlineToggle, is(not(visible())));
    }

    @Test
    @org.junit.Ignore
    @Ignore
    public void testDownloadIsUnavailableWhenTheyAccessPlaylists() throws Exception {
        final ViewElement offlineItem = mainNavHelper.goToCollections()
                                                     .clickPlaylistsPreview()
                                                     .clickOnFirstPlaylist()
                                                     .getDownloadButton();

        assertThat(offlineItem, is(not(visible())));
    }

    @Test
    @org.junit.Ignore
    @Ignore
    public void testDownloadIsUnavailableWhenTheyAccessPlaylistDetailScreen() throws Exception {
        final ViewElement offlineItem = mainNavHelper.goToCollections()
                                                     .clickPlaylistsPreview()
                                                     .clickOnFirstPlaylist()
                                                     .getDownloadButton();

        assertThat(offlineItem, is(not(visible())));
    }

    @Test
    public void testDoesNotDisplayGoBackOnlineWhenOfflineContentDisabled() throws Exception {
        final Context context = getInstrumentation().getTargetContext();
        mainNavHelper.goToBasicSettings();

        connectionHelper.setNetworkConnected(false);
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(27, DAYS).getTime());
        resetPolicyCheckTime(context);

        final GoBackOnlineDialogElement goBackOnlineDialog = new GoBackOnlineDialogElement(solo);
        assertThat("Go back online dialog should not be visible", goBackOnlineDialog, IsNot.not(IsVisible.visible()));
    }

    private Date getPreviousDate(int time, TimeUnit timeUnit) {
        return new Date(currentTimeMillis() - timeUnit.toMillis(time));
    }
}
