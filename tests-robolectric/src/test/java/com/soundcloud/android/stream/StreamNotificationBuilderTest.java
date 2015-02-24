package com.soundcloud.android.stream;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
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
    private static final String TITLE = "title";
    private static final String TICKER = "ticker";
    private static final String MESSAGE = "message";

    private StreamNotificationBuilder streamNotificationBuilder;

    @Mock private Context context;
    @Mock private NotificationCompat.Builder notificationBuilder;
    @Mock private Notification notification;

    final TestSubscriber<Notification> subscriber = new TestSubscriber<>();


    @Before
    public void setUp() throws Exception {
        when(notificationBuilder.build()).thenReturn(notification);

        streamNotificationBuilder = new StreamNotificationBuilder(context, new Provider<NotificationCompat.Builder>() {
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
        when(context.getString(R.string.dashboard_notifications_ticker_single)).thenReturn(TICKER);
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"))).subscribe(subscriber);

        verify(notificationBuilder).setTicker(TICKER);
    }

    @Test
    public void notificationSetsTickerForTwoTracks() throws Exception {
        when(context.getString(R.string.dashboard_notifications_ticker, "2")).thenReturn(TICKER);
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"), getTrack("creator2"))).subscribe(subscriber);

        verify(notificationBuilder).setTicker(TICKER);
    }

    @Test
    public void notificationSetsTickerForMoreThanMaxTracks() throws Exception {
        when(context.getString(R.string.dashboard_notifications_ticker, "3+")).thenReturn(TICKER);
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"), getTrack("creator2"), getTrack("creator3"), getTrack("creator4"))).subscribe(subscriber);

        verify(notificationBuilder).setTicker(TICKER);
    }

    @Test
    public void notificationSetsTitleForSingleTrack() throws Exception {
        when(context.getString(R.string.dashboard_notifications_title_single)).thenReturn(TITLE);
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"))).subscribe(subscriber);

        verify(notificationBuilder).setContentTitle(TITLE);
    }

    @Test
    public void notificationSetsTitleForTwoTracks() throws Exception {
        when(context.getString(R.string.dashboard_notifications_title, "2")).thenReturn(TITLE);
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"), getTrack("creator2"))).subscribe(subscriber);

        verify(notificationBuilder).setContentTitle(TITLE);
    }

    @Test
    public void notificationSetsTitleForMoreThanMaxTracks() throws Exception {
        when(context.getString(R.string.dashboard_notifications_title, "3+")).thenReturn(TITLE);
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"), getTrack("creator2"), getTrack("creator3"), getTrack("creator4"))).subscribe(subscriber);

        verify(notificationBuilder).setContentTitle(TITLE);
    }

    @Test
    public void notificationSetsMessageForSingleTrack() throws Exception {
        when(context.getString(eq(R.string.dashboard_notifications_message_incoming), eq("creator1"))).thenReturn(MESSAGE);
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"))).subscribe(subscriber);

        verify(notificationBuilder).setContentText(MESSAGE);
    }

    @Test
    public void notificationSetsMessageForTwoTracksWithDifferentCreators() throws Exception {
        when(context.getString(R.string.dashboard_notifications_message_incoming_2, "creator1", "creator2")).thenReturn(MESSAGE);
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"), getTrack("creator2"))).subscribe(subscriber);

        verify(notificationBuilder).setContentText(MESSAGE);
    }

    @Test
    public void notificationSetsMessageForTracksWithMoreThanTwoCreators() throws Exception {
        when(context.getString(R.string.dashboard_notifications_message_incoming_others, "creator1", "creator2")).thenReturn(MESSAGE);
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"), getTrack("creator2"), getTrack("creator3"))).subscribe(subscriber);

        verify(notificationBuilder).setContentText(MESSAGE);
    }

    @Test
    public void notificationSetsMessageForThreeTracksWithOneCreator() throws Exception {
        when(context.getString(R.string.dashboard_notifications_message_incoming, "creator1")).thenReturn(MESSAGE);
        streamNotificationBuilder.notification(Arrays.asList(getTrack("creator1"), getTrack("creator1"), getTrack("creator1"))).subscribe(subscriber);

        verify(notificationBuilder).setContentText(MESSAGE);
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
