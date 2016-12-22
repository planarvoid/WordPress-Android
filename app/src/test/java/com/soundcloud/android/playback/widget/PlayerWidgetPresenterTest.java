package com.soundcloud.android.playback.widget;

import static com.soundcloud.android.testsupport.matchers.ImageResourceMatcher.isImageResourceFor;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.service.PlayerAppWidgetProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.verification.VerificationMode;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

public class PlayerWidgetPresenterTest extends AndroidUnitTest {

    @Mock private AppWidgetManager appWidgetManager;
    @Mock private ImageOperations imageOperations;

    private PlayerWidgetPresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new PlayerWidgetPresenter(appWidgetManager, imageOperations);
    }

    @Test
    public void updatesWidgetStateWhenPlayStateChanges() {
        final PropertySet trackProperties = TestPropertySets.expectedTrackForWidget();
        setupArtworkLoad(trackProperties, Observable.empty());
        final TrackItem trackItem = ModelFixtures.trackItem();
        trackItem.updated(trackProperties);
        presenter.updateTrackInformation(context(), trackItem);

        presenter.updatePlayState(context(), true);

        verifyUpdateViaPlayBackWidgetProvider(times(2)); // one for track info and one for update state
    }

    @Test
    public void updatesTrackWhenPlayableChanges() {
        final PropertySet trackProperties = TestPropertySets.expectedTrackForWidget();
        setupArtworkLoad(trackProperties, Observable.empty());
        final TrackItem trackItem = ModelFixtures.trackItem();
        trackItem.updated(trackProperties);
        presenter.updateTrackInformation(context(), trackItem);

        verifyUpdateViaPlayBackWidgetProvider();
    }

    @Test
    public void resetsWidgetState() {
        presenter.reset(context());

        verifyUpdateViaPlayBackWidgetProvider();
    }

    @Test
    public void unsubscribesFromArtworkLoadingWhenResetting() {
        final PropertySet trackProperties = TestPropertySets.expectedTrackForWidget();
        final PublishSubject<Bitmap> subject = PublishSubject.create();
        setupArtworkLoad(trackProperties, subject);
        final TrackItem trackItem = ModelFixtures.trackItem();
        trackItem.updated(trackProperties);

        presenter.updateTrackInformation(context(), trackItem);
        presenter.reset(context());
        reset(appWidgetManager);

        subject.onNext(mock(Bitmap.class));
        verifyUpdateViaPlayBackWidgetProvider(never());
    }

    private void verifyUpdateViaPlayBackWidgetProvider(VerificationMode verificationMode) {
        ComponentName expectedComponentName =
                new ComponentName(BuildConfig.APPLICATION_ID, PlayerAppWidgetProvider.class.getCanonicalName());
        verify(appWidgetManager, verificationMode).updateAppWidget(eq(expectedComponentName), any(RemoteViews.class));
    }

    private void verifyUpdateViaPlayBackWidgetProvider() {
        ComponentName expectedComponentName =
                new ComponentName(BuildConfig.APPLICATION_ID, PlayerAppWidgetProvider.class.getCanonicalName());
        verify(appWidgetManager).updateAppWidget(eq(expectedComponentName), any(RemoteViews.class));
    }

    private void setupArtworkLoad(PropertySet trackProperties, Observable<Bitmap> bitmapObservable) {
        when(imageOperations.artwork(argThat(isImageResourceFor(trackProperties)),
                                     same(ApiImageSize.getNotificationLargeIconImageSize(resources())),
                                     eq(resources().getDimensionPixelSize(R.dimen.widget_image_estimated_width)),
                                     eq(resources().getDimensionPixelSize(R.dimen.widget_image_estimated_height))))
                .thenReturn(bitmapObservable);
    }

}
