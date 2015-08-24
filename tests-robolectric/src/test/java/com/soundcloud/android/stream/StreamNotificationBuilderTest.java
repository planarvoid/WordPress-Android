package com.soundcloud.android.stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.java.collections.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.observers.TestSubscriber;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;
import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class StreamNotificationBuilderTest {

    private static final int NOTIFICATION_MAX = 3;

    private StreamNotificationBuilder streamNotificationBuilder;

    @Mock private Context context;
    @Mock private NotificationCompat.Builder notificationBuilder;
    @Mock private Notification notification;

    final TestSubscriber<Notification> subscriber = new TestSubscriber<>();


    @Before
    public void setUp() throws Exception {
        when(notificationBuilder.build()).thenReturn(notification);

        streamNotificationBuilder = new StreamNotificationBuilder(Robolectric.application, new Provider<NotificationCompat.Builder>() {
            @Override
            public NotificationCompat.Builder get() {
                return notificationBuilder;
            }
        }, NOTIFICATION_MAX);
    }

    @Test
    public void notificationReturnsNotificationForSingleTrack() throws Exception {
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"))).subscribe(subscriber);

        subscriber.assertReceivedOnNext(Arrays.asList(notification));
        subscriber.assertTerminalEvent();
        subscriber.assertNoErrors();
    }

    @Test
    public void notificationReturnsNotificationForMultipleTracks() throws Exception {
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"), getTrack("creator2"))).subscribe(subscriber);

        subscriber.assertReceivedOnNext(Arrays.asList(notification));
        subscriber.assertTerminalEvent();
        subscriber.assertNoErrors();
    }

    @Test
    public void notificationSetsTickerForSingleTrack() throws Exception {
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"))).subscribe(subscriber);
        verify(notificationBuilder).setTicker("1 new sound");
    }

    @Test
    public void notificationSetsTickerForTwoTracks() throws Exception {
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"), getTrack("creator2"))).subscribe(subscriber);
        verify(notificationBuilder).setTicker("2 new sounds");
    }

    @Test
    public void notificationSetsTickerForMoreThanMaxTracks() throws Exception {
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"), getTrack("creator2"), getTrack("creator3"), getTrack("creator4"))).subscribe(subscriber);
        verify(notificationBuilder).setTicker("3+ new sounds");
    }

    @Test
    public void notificationSetsTitleAndTextForSingleTrack() throws Exception {
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"))).subscribe(subscriber);
        verify(notificationBuilder).setContentTitle("SoundCloud");
        verify(notificationBuilder).setContentText("1 new sound from creator1");
    }

    @Test
    public void notificationSetsTitleAndTextForTwoTracks() throws Exception {
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"), getTrack("creator2"))).subscribe(subscriber);
        verify(notificationBuilder).setContentTitle("SoundCloud");
        verify(notificationBuilder).setContentText("2 new sounds from creator1 and creator2");
    }

    @Test
    public void notificationSetsTitleAndTextForMoreThanMaxTracks() throws Exception {
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"), getTrack("creator2"), getTrack("creator3"), getTrack("creator4"))).subscribe(subscriber);
        verify(notificationBuilder).setContentTitle("SoundCloud");
        verify(notificationBuilder).setContentText("3+ new sounds from creator1, creator2 and others");
    }

    @Test
    public void notificationSetsTitleAndMessageForThreeTracksWithOneCreator() throws Exception {
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"), getTrack("creator1"), getTrack("creator1"))).subscribe(subscriber);
        verify(notificationBuilder).setContentTitle("SoundCloud");
        verify(notificationBuilder).setContentText("3 new sounds from creator1");
    }

    @Test
    public void notificationSetsVisibilityToPublic() throws Exception {
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"))).subscribe(subscriber);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    private PropertySet getTrack(String creatorName) {
        return PropertySet.from(PlayableProperty.CREATOR_NAME.bind(creatorName));
    }
}
