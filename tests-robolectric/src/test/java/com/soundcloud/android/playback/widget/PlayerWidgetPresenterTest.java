package com.soundcloud.android.playback.widget;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.service.PlayerAppWidgetProvider;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.verification.VerificationMode;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

@RunWith(SoundCloudTestRunner.class)
public class PlayerWidgetPresenterTest {
    @Mock private AppWidgetManager appWidgetManager;
    @Mock private ImageOperations imageOperations;

    private PlayerWidgetPresenter presenter;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.application;
        presenter = new PlayerWidgetPresenter(appWidgetManager, imageOperations);
    }

    @Test
    public void shouldUpdateWidgetUsingPlayerAppWidgetProviderWhenPlayStateChange() throws Exception {
        final PropertySet trackProperties = TestPropertySets.expectedTrackForWidget();
        setupArtworkLoad(trackProperties, Observable.<Bitmap>empty());
        presenter.updateTrackInformation(context, trackProperties);

        presenter.updatePlayState(context, true);

        verifyUpdateViaPlayBackWidgetProvider(times(2)); // one for track info and one for update state
    }

    @Test
    public void shouldUpdateTrackUsingPlayerAppWidgetProviderWhenPlayableChange() throws Exception {
        final PropertySet trackProperties = TestPropertySets.expectedTrackForWidget();
        setupArtworkLoad(trackProperties, Observable.<Bitmap>empty());
        presenter.updateTrackInformation(context, trackProperties);

        verifyUpdateViaPlayBackWidgetProvider();
    }

    @Test
    public void shouldUpdateWidgetUsingPlayerAppWidgetProviderOnReset() throws Exception {
        presenter.reset(context);

        verifyUpdateViaPlayBackWidgetProvider();
    }

    @Test
    public void unsubscribesFromArtworkLoadingWhenResetting() {
        final PropertySet trackProperties = TestPropertySets.expectedTrackForWidget();
        final PublishSubject<Bitmap> subject = PublishSubject.create();
        setupArtworkLoad(trackProperties, subject);

        presenter.updateTrackInformation(context, trackProperties);
        presenter.reset(context);
        reset(appWidgetManager);

        subject.onNext(mock(Bitmap.class));
        verifyUpdateViaPlayBackWidgetProvider(never());
    }

    private void verifyUpdateViaPlayBackWidgetProvider(VerificationMode verificationMode) {
        ComponentName expectedComponentName = new ComponentName("com.soundcloud.android", PlayerAppWidgetProvider.class.getCanonicalName());
        verify(appWidgetManager, verificationMode).updateAppWidget(eq(expectedComponentName), any(RemoteViews.class));
    }

    private void verifyUpdateViaPlayBackWidgetProvider() {
        ComponentName expectedComponentName = new ComponentName("com.soundcloud.android", PlayerAppWidgetProvider.class.getCanonicalName());
        verify(appWidgetManager).updateAppWidget(eq(expectedComponentName), any(RemoteViews.class));
    }

    private void setupArtworkLoad(PropertySet trackProperties, Observable<Bitmap> bitmapObservable) {
        when(imageOperations.artwork(trackProperties.get(TrackProperty.URN),
                ApiImageSize.getNotificationLargeIconImageSize(Robolectric.application.getResources()),
                context.getResources().getDimensionPixelSize(R.dimen.widget_image_estimated_width),
                context.getResources().getDimensionPixelSize(R.dimen.widget_image_estimated_height)))
                .thenReturn(bitmapObservable);
    }

}
