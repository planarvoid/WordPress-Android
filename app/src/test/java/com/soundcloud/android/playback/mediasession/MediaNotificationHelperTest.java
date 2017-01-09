package com.soundcloud.android.playback.mediasession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat.Builder;

import java.util.ArrayList;
import java.util.Collection;

public class MediaNotificationHelperTest extends AndroidUnitTest {

    private static final String SUBTITLE = "SUBTITLE";
    public static final String TITLE = "TITLE";
    public static final String DESCRIPTION = "DESCRIPTION";

    private static final Function<NotificationCompat.Action, String> ACTION_TO_TITLE = input -> input.getTitle().toString();

    @Mock private MediaSessionCompat mediaSession;
    @Mock private MediaControllerCompat mediaController;
    @Mock private MediaMetadataCompat metadata;
    @Mock private MediaDescriptionCompat description;
    @Mock private Bitmap bitmap;
    @Mock private Navigator navigator;

    @Before
    public void setUp() throws Exception {
        when(mediaSession.getController()).thenReturn(mediaController);
        when(mediaController.getMetadata()).thenReturn(metadata);
        when(metadata.getDescription()).thenReturn(description);

        when(description.getTitle()).thenReturn(TITLE);
        when(description.getSubtitle()).thenReturn(SUBTITLE);
        when(description.getDescription()).thenReturn(DESCRIPTION);
        when(description.getIconBitmap()).thenReturn(bitmap);
    }

    @Test
    public void builderHasCurrentMetadata() {
        Builder builder = MediaNotificationHelper.from(context(), navigator, mediaSession, false).get();

        // note: builder doesn't expose getters :(
        assertThat(builder.mContentTitle).isEqualTo(TITLE);
        assertThat(builder.mContentText).isEqualTo(SUBTITLE);
        assertThat(builder.mSubText).isEqualTo(DESCRIPTION);
        assertThat(builder.mLargeIcon).isEqualTo(bitmap);
    }

    @Test
    public void builderHasPreviousAction() {
        Builder builder = MediaNotificationHelper.from(context(), navigator, mediaSession, false).get();

        assertHasActionWithTitle(builder, R.string.previous);
    }

    @Test
    public void builderHasPlayActionWhenNotPlaying() {
        Builder builder = MediaNotificationHelper.from(context(), navigator, mediaSession, false).get();

        assertHasActionWithTitle(builder, R.string.play);
    }

    @Test
    public void builderHasPauseActionWhenPlaying() {
        Builder builder = MediaNotificationHelper.from(context(), navigator, mediaSession, true).get();

        assertHasActionWithTitle(builder, R.string.pause);
    }

    @Test
    public void builderHasNextAction() {
        Builder builder = MediaNotificationHelper.from(context(), navigator, mediaSession, false).get();

        assertHasActionWithTitle(builder, R.string.next);
    }

    @Test
    public void builderIsAbsentWhenNoControllerIsAvailable() {
        when(mediaSession.getController()).thenReturn(null);

        Optional<Builder> builderOptional = MediaNotificationHelper.from(context(), navigator, mediaSession, false);

        assertThat(builderOptional).isEqualTo(Optional.absent());
    }

    @Test
    public void builderIsAbsentWhenNoMetadataIsAvailable() {
        when(mediaController.getMetadata()).thenReturn(null);

        Optional<Builder> builderOptional = MediaNotificationHelper.from(context(), navigator, mediaSession, false);

        assertThat(builderOptional).isEqualTo(Optional.absent());
    }

    private Collection<String> actionsToTitles(Builder builder) {
        return MoreCollections.transform(new ArrayList<>(builder.mActions), ACTION_TO_TITLE);
    }

    private void assertHasActionWithTitle(Builder builder, int resId) {
        assertThat(actionsToTitles(builder)).contains(context().getString(resId));
    }

}
