package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetPolicyCheckTime;
import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.configuration.experiments.PlaylistAndAlbumsPreviewsExperiment;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.framework.matcher.element.IsVisible;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.GoBackOnlineDialogElement;
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

    @Override
    public void setUp() throws Exception {
        super.setUp();

        getExperiments().set(PlaylistAndAlbumsPreviewsExperiment.CONFIGURATION, PlaylistAndAlbumsPreviewsExperiment.VARIANT_CONTROL);
    }

    @Ignore
    public void testDownloadIsUnavailableWhenTheyAccessLikes() throws Exception {
        final ViewElement offlineToggle = mainNavHelper.goToTrackLikes()
                                                       .offlineButton();

        assertThat(offlineToggle, is(not(visible())));
    }

    @Ignore
    public void testDownloadIsUnavailableWhenTheyAccessPlaylists() throws Exception {
        final ViewElement offlineItem = mainNavHelper.goToCollections()
                                                     .clickPlaylistsPreview()
                                                     .clickOnFirstPlaylist()
                                                     .getDownloadButton();

        assertThat(offlineItem, is(not(visible())));
    }

    @Ignore
    public void testDownloadIsUnavailableWhenTheyAccessPlaylistDetailScreen() throws Exception {
        final ViewElement offlineItem = mainNavHelper.goToCollections()
                                                     .clickPlaylistsPreview()
                                                     .clickOnFirstPlaylist()
                                                     .getDownloadButton();

        assertThat(offlineItem, is(not(visible())));
    }

    public void testDoesNotDisplayGoBackOnlineWhenOfflineContentDisabled() {
        final Context context = getInstrumentation().getTargetContext();
        mainNavHelper.goToBasicSettings();

        connectionHelper.setNetworkConnected(false);
        offlineContentHelper.updateOfflineTracksPolicyUpdateTime(
                context, getPreviousDate(27, TimeUnit.DAYS).getTime());
        resetPolicyCheckTime(context);

        final GoBackOnlineDialogElement goBackOnlineDialog = new GoBackOnlineDialogElement(solo);
        assertThat("Go back online dialog should not be visible", goBackOnlineDialog, IsNot.not(IsVisible.visible()));
    }

    private Date getPreviousDate(int time, TimeUnit timeUnit) {
        return new Date(System.currentTimeMillis() - timeUnit.toMillis(time));
    }
}
