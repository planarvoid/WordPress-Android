package com.soundcloud.android.stream;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.java.collections.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;
import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class RichStreamNotificationBuilderTest {

    private RichStreamNotificationBuilder richStreamNotificationBuilder;

    @Mock private Context context;
    @Mock private Resources resources;
    @Mock private ImageOperations imageOperations;
    @Mock private NotificationCompat.Builder notificationBuilder;
    @Mock private Notification notification;

    final TestSubscriber<Notification> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        when(notificationBuilder.setLargeIcon(any(Bitmap.class))).thenReturn(notificationBuilder);
        when(notificationBuilder.build()).thenReturn(notification);
        when(context.getResources()).thenReturn(Robolectric.application.getResources());
        richStreamNotificationBuilder = new RichStreamNotificationBuilder(context, imageOperations, new Provider<NotificationCompat.Builder>() {
            @Override
            public NotificationCompat.Builder get() {
                return notificationBuilder;
            }
        });
    }

    @Test
    public void notificationReturnsNotificationWithImageLoad() throws Exception {
        final Urn trackUrn = Urn.forTrack(123L);
        final Bitmap bitmap = Bitmap.createBitmap(0, 0, Bitmap.Config.RGB_565);
        when(imageOperations.artwork(trackUrn, ApiImageSize.LARGE, 128, 128)).thenReturn(Observable.just(bitmap));
        richStreamNotificationBuilder.notification(Arrays.asList(getTrack(trackUrn, "creator"))).subscribe(subscriber);

        subscriber.assertReceivedOnNext(Arrays.asList(notification));
        subscriber.assertTerminalEvent();
        subscriber.assertNoErrors();
    }

    @Test
    public void notificationReturnsNotificationWithImageError() throws Exception {
        final Urn trackUrn = Urn.forTrack(123L);
        when(imageOperations.artwork(trackUrn, ApiImageSize.LARGE, 128, 128)).thenReturn(Observable.<Bitmap>error(new Exception()));
        richStreamNotificationBuilder.notification(Arrays.asList(getTrack(trackUrn, "creator"))).subscribe(subscriber);

        subscriber.assertReceivedOnNext(Arrays.asList(notification));
        subscriber.assertTerminalEvent();
        subscriber.assertNoErrors();
    }

    @Test
    public void notificationSetsImageOnBuilder() throws Exception {
        final Urn trackUrn = Urn.forTrack(123L);
        final Bitmap bitmap = Bitmap.createBitmap(0, 0, Bitmap.Config.RGB_565);
        when(imageOperations.artwork(trackUrn, ApiImageSize.LARGE, 128, 128)).thenReturn(Observable.just(bitmap));
        richStreamNotificationBuilder.notification(Arrays.asList(getTrack(trackUrn, "creator"))).subscribe(subscriber);

        verify(notificationBuilder).setLargeIcon(bitmap);
    }

    @Test
    public void notificationSetsVisibilityToPublic() throws Exception {
        final Urn trackUrn = Urn.forTrack(123L);
        final Bitmap bitmap = Bitmap.createBitmap(0, 0, Bitmap.Config.RGB_565);
        when(imageOperations.artwork(trackUrn, ApiImageSize.LARGE, 128, 128)).thenReturn(Observable.just(bitmap));
        richStreamNotificationBuilder.notification(Arrays.asList(getTrack(trackUrn, "creator"))).subscribe(subscriber);

        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    private PropertySet getTrack(Urn trackUrn, String creatorName) {
        return PropertySet.from(PlayableProperty.URN.bind(trackUrn), PlayableProperty.CREATOR_NAME.bind(creatorName));
    }
}
