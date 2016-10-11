package com.soundcloud.android.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DisplayMetricsStub;
import org.junit.Test;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public class ApiImageSizeTest extends AndroidUnitTest {

    @Test
    public void shouldFormatUriFromAnyKnownSizeToAnyOtherSize() throws Exception {
        assertThat(ApiImageSize.T120.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t47x47.jpg?2479809"))
                .isEqualTo("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t120x120.jpg?2479809");

        assertThat(ApiImageSize.T120.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t300x300.jpg?2479809"))
                .isEqualTo("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t120x120.jpg?2479809");
    }

    @Test
    public void shouldFormatUriForT500Size() throws Exception {
        assertThat(ApiImageSize.T500.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t120x120.jpg?2479809"))
                .isEqualTo("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t500x500.jpg?2479809");
    }

    @Test
    public void shouldFormatUriForT300Size() {
        assertThat(ApiImageSize.T300.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t120x120.jpg?2479809"))
                .isEqualTo("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t300x300.jpg?2479809");
    }


    @Test
    public void shouldFormatUriForLargeSize() throws Exception {
        assertThat(ApiImageSize.T120.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t47x47.jpg?2479809"))
                .isEqualTo("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t120x120.jpg?2479809");
    }

    @Test
    public void shouldFormatUriForT47Size() throws Exception {
        assertThat(ApiImageSize.T47.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t120x120.jpg?2479809"))
                .isEqualTo("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t47x47.jpg?2479809");
    }

    @Test
    public void shouldReturnLargeSizeForUnknown() throws Exception {
        assertThat(ApiImageSize.Unknown.formatUri(
                "https://i1.sndcdn.com/artworks-000032795722-aaqx24-t300x300.jpg?2479809"))
                .isEqualTo("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t120x120.jpg?2479809");
    }

    @Test
    public void shouldFormatUriForTheSameSize() throws Exception {
        assertThat(ApiImageSize.T120.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t120x120.jpg?2479809"))
                .isEqualTo("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t120x120.jpg?2479809");
    }

    @Test
    public void shouldReturnUnchangedUriIfItDoesNotContainSupportedSize() {
        assertThat(ApiImageSize.T120.formatUri(
                "https://i1.sndcdn.com/artworks-000032795722-aaqx24-whateverSize.jpg?2479809"))
                .isEqualTo("https://i1.sndcdn.com/artworks-000032795722-aaqx24-whateverSize.jpg?2479809");
    }

    @Test
    public void shouldFormatResolverUri() throws Exception {
        assertThat(ApiImageSize.T120.formatUri(
                "https://api.soundcloud.com/resolve/image?url=soundcloud:users:1234&size=badge"))
                .isEqualTo("https://api.soundcloud.com/resolve/image?url=soundcloud:users:1234&size=t120x120");

        assertThat(ApiImageSize.T120.formatUri("https://api.soundcloud.com/resolve/image?url=soundcloud:users:1234"))
                .isEqualTo("https://api.soundcloud.com/resolve/image?url=soundcloud:users:1234&size=t120x120");
    }

    @Test
    public void shouldReturnLargeIconSizeForHiResScreens() {
        assertFormatUri(2.0f,
                        "https://i1.sndcdn.com/artworks-000032795722-aaqx24-t120x120.jpg",
                        "https://i1.sndcdn.com/artworks-000032795722-aaqx24-t120x120.jpg");
    }

    @Test
    public void shouldReturnT300IconSizeForHiResScreens() {
        assertFormatUri(10.0f,
                        "https://i1.sndcdn.com/artworks-000032795722-aaqx24-t120x120.jpg",
                        "https://i1.sndcdn.com/artworks-000032795722-aaqx24-t300x300.jpg");
    }

    @Test
    public void shouldReturnT500FullImageForHiResScreens() {
        assertFullImageUri(1080, 1920, ApiImageSize.T500);
    }

    @Test
    public void shouldReturnT300FullImageForMidResScreens() {
        assertFullImageUri(640, 480, ApiImageSize.T300);
    }

    @Test
    public void shouldReturnLargeFullImageForLowResScreens() {
        assertFullImageUri(240, 320, ApiImageSize.T120);
    }

    @Test
    public void shouldReturnT2480x520ForHiResScreens() {
        assertFullBannerUri(1080, 1920, ApiImageSize.T2480x520);
    }

    @Test
    public void shouldReturnT1240x260ForMidResScreens() {
        assertFullBannerUri(640, 480, ApiImageSize.T1240x260);
    }

    private void assertFormatUri(float density, String input, String expected) {
        Resources resources = mock(Resources.class);
        Context context = mock(Context.class);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        displayMetrics.density = density;

        when(context.getResources()).thenReturn(resources);
        when(resources.getDisplayMetrics()).thenReturn(displayMetrics);

        assertThat(ApiImageSize.formatUriForNotificationLargeIcon(context, input)).isEqualTo(expected);
    }

    private void assertFullImageUri(int width, int height, ApiImageSize expected) {
        Resources resources = mock(Resources.class);
        DisplayMetrics displayMetrics = new DisplayMetricsStub(width, height);

        when(resources.getDisplayMetrics()).thenReturn(displayMetrics);

        assertThat(ApiImageSize.getFullImageSize(resources)).isEqualTo(expected);
    }


    private void assertFullBannerUri(int width, int height, ApiImageSize expected) {
        Resources resources = mock(Resources.class);
        DisplayMetrics displayMetrics = new DisplayMetricsStub(width, height);

        when(resources.getDisplayMetrics()).thenReturn(displayMetrics);

        assertThat(ApiImageSize.getFullBannerSize(resources)).isEqualTo(expected);
    }
}
