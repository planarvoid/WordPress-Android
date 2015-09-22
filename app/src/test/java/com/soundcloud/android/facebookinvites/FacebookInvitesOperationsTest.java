package com.soundcloud.android.facebookinvites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.facebookapi.FacebookApi;
import com.soundcloud.android.facebookapi.FacebookApiHelper;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import java.util.Arrays;
import java.util.List;

public class FacebookInvitesOperationsTest extends AndroidUnitTest {

    private FacebookInvitesOperations operations;

    private long clickInterval = FacebookInvitesOperations.CLICK_INTERVAL_MS;
    private long dismissInterval = FacebookInvitesOperations.DISMISS_INTERVAL_MS;
    private int openCount = FacebookInvitesOperations.SHOW_AFTER_OPENS_COUNT;
    private int dismissCount = FacebookInvitesOperations.REST_AFTER_DISMISS_COUNT;

    @Mock private FacebookInvitesStorage storage;
    @Mock private Observer<Optional<FacebookInvitesItem>> observer;
    @Mock private FacebookApi facebookApi;
    @Mock private FacebookApiHelper facebookApiHelper;
    @Mock private FeatureFlags featureFlags;
    @Mock private NetworkConnectionHelper networkConnectionHelper;

    @Before
    public void setUp() throws Exception {
        operations = new FacebookInvitesOperations(storage, featureFlags, facebookApi, facebookApiHelper, networkConnectionHelper);

        when(storage.getMillisSinceLastClick()).thenReturn(clickInterval);
        when(storage.getTimesAppOpened()).thenReturn(openCount);
        when(storage.getMillisSinceLastDismiss()).thenReturn(dismissInterval);
        when(storage.getTimesDismissed()).thenReturn(0);
        when(facebookApiHelper.canShowAppInviteDialog()).thenReturn(true);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
    }

    @Test
    public void canShowOnSetup() throws Exception {
        assertThat(operations.canShow()).isTrue();
    }

    @Test
    public void shouldShowWhenAppModulusOfOpenTimes() throws Exception {
        when(storage.getTimesAppOpened()).thenReturn(openCount*7);

        assertThat(operations.canShow()).isTrue();
    }

    @Test
    public void shouldNotShowWhenAppInviteDialogIsFalse() throws Exception {
        when(facebookApiHelper.canShowAppInviteDialog()).thenReturn(false);

        assertThat(operations.canShow()).isFalse();
    }

    @Test
    public void shouldNotShowWhenAppNotOpenedEnoughTimes() throws Exception {
        when(storage.getTimesAppOpened()).thenReturn(openCount - 1);

        assertThat(operations.canShow()).isFalse();
    }

    @Test
    public void shouldNotShowFirstTime() throws Exception {
        when(storage.getTimesAppOpened()).thenReturn(0);

        assertThat(operations.canShow()).isFalse();
    }

    @Test
    public void shouldNotShowWhenAppNotModulusOfOpenTimes() throws Exception {
        when(storage.getTimesAppOpened()).thenReturn(openCount + 1);

        assertThat(operations.canShow()).isFalse();
    }

    @Test
    public void shouldNotShowWhenLastClickTooEarly() throws Exception {
        when(storage.getMillisSinceLastClick()).thenReturn(clickInterval - 1);

        assertThat(operations.canShow()).isFalse();
    }

    @Test
    public void shouldNotShowWhenConnectionNotAvailable() throws Exception {
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(false);

        assertThat(operations.canShow()).isFalse();
    }

    @Test
    public void shouldNotShowWhenDismissedBeforeClickInterval() throws Exception {
        when(storage.getTimesDismissed()).thenReturn(dismissCount - 1);
        when(storage.getMillisSinceLastDismiss()).thenReturn(clickInterval - 1);

        assertThat(operations.canShow()).isFalse();
    }

    @Test
    public void shouldShowWhenDismissedAfterClickInterval() throws Exception {
        when(storage.getTimesDismissed()).thenReturn(dismissCount - 1);
        when(storage.getMillisSinceLastDismiss()).thenReturn(clickInterval);

        assertThat(operations.canShow()).isTrue();
    }

    @Test
    public void shouldNotShowWhenDismissedBeforeDismissInterval() throws Exception {
        when(storage.getTimesDismissed()).thenReturn(dismissCount);
        when(storage.getMillisSinceLastDismiss()).thenReturn(dismissInterval - 1);

        assertThat(operations.canShow()).isFalse();
    }

    @Test
    public void shouldShowWhenDismissedAfterDismissInterval() throws Exception {
        when(storage.getTimesDismissed()).thenReturn(dismissCount);
        when(storage.getMillisSinceLastDismiss()).thenReturn(dismissInterval);

        assertThat(operations.canShow()).isTrue();
    }

    @Test
    public void shouldShowWhenLastClickAfter() throws Exception {
        when(storage.getMillisSinceLastClick()).thenReturn(clickInterval + 1);

        assertThat(operations.canShow()).isTrue();
    }

    @Test
    public void shouldReturnUnfulfilledWhenCantShow() {
        when(facebookApiHelper.canShowAppInviteDialog()).thenReturn(false);
        operations.loadWithPictures().subscribe(observer);

        verify(observer).onNext(Optional.<FacebookInvitesItem>absent());
    }

    @Test
    public void shouldReturnFulfilledWithImages() {
        List<String> pictureUrls = Arrays.asList("url1", "url2");
        when(facebookApi.friendPictureUrls()).thenReturn(Observable.just(pictureUrls));
        operations.loadWithPictures().subscribe(observer);

        verify(observer).onNext(Optional.of(new FacebookInvitesItem(pictureUrls)));
    }

}
