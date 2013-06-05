package com.soundcloud.android;

import com.soundcloud.android.utils.images.ImageSize;
import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

@RunWith(DefaultTestRunner.class)
public class GraphicSizeTest {

    @Test
    public void shouldGetMinimumGraphicSize() throws Exception {
        expect(ImageSize.getMinimumSizeFor(99, 101, true)).toEqual(ImageSize.T300);
        expect(ImageSize.getMinimumSizeFor(99, 101, false)).toEqual(ImageSize.LARGE);
        expect(ImageSize.getMinimumSizeFor(67, 67, true)).toEqual(ImageSize.T67);
        expect(ImageSize.getMinimumSizeFor(68, 67, true)).toEqual(ImageSize.LARGE);
        expect(ImageSize.getMinimumSizeFor(68, 67, false)).toEqual(ImageSize.T67);
    }

    @Test
<<<<<<< HEAD
    public void shouldFormatUri() throws Exception {
        expect(ImageSize.LARGE.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809");

        expect(ImageSize.LARGE.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809"))
=======
    public void shouldFormatUriFromAnyKnownSizeToAnyOtherSize() throws Exception {
        expect(GraphicSize.LARGE.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809");

        expect(GraphicSize.TINY_ARTWORK.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t300x300.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809");
    }

    @Test
    public void shouldFormatUriForT500Size() throws Exception {
        expect(GraphicSize.T500.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t500x500.jpg?2479809");
    }

    @Test
    public void shouldFormatUriForCropSize() throws Exception {
        expect(GraphicSize.CROP.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t500x500.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-crop.jpg?2479809");
    }

    @Test
    public void shouldFormatUriForT300Size() {
        expect(GraphicSize.T300.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t300x300.jpg?2479809");
    }


    @Test
    public void shouldFormatUriForLargeSize() throws Exception {
        expect(GraphicSize.LARGE.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809"))
>>>>>>> Added unit tests for GraphicSize
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809");
    }

    @Test
    public void shouldFormatUriForT67Size() throws Exception {
        expect(GraphicSize.T67.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-mini.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t67x67.jpg?2479809");
    }

    @Test
    public void shouldFormatUriForBadgeSize() throws Exception {
        expect(GraphicSize.BADGE.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-crop.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-badge.jpg?2479809");
    }

<<<<<<< HEAD
        expect(ImageSize.TINY_ARTWORK.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809"))
=======
    @Test
    public void shouldFormatUriForSmallSize() throws Exception {
        expect(GraphicSize.SMALL.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t300x300.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-small.jpg?2479809");
    }

    @Test
    public void shouldFormatUriForTinyArtSize() throws Exception {
        expect(GraphicSize.TINY_ARTWORK.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-small.jpg?2479809"))
>>>>>>> Added unit tests for GraphicSize
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809");
    }

    @Test
    public void shouldFormatUriForTinyAvatarSize() throws Exception {
        expect(GraphicSize.TINY_AVATAR.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t300x300.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809");
    }

    @Test
    public void shouldFormatUriForMiniSize() throws Exception {
        expect(GraphicSize.MINI.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t300x300.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-mini.jpg?2479809");
    }

    @Test
    public void shouldReturnLargeSizeForUnknown() throws Exception {
        expect(GraphicSize.Unknown.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-t300x300.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809");
    }



    @Test
    public void shouldFormatUriForTheSameSize() throws Exception {
        expect(GraphicSize.LARGE.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809");
    }


    @Test
    public void shouldReturnUnchangedUriIfItDoesNotContainSupportedSize() {
        expect(GraphicSize.LARGE.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-whateverSize.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-whateverSize.jpg?2479809");
    }

    @Test
    public void shouldFormatResolverUri() throws Exception {
        expect(ImageSize.TINY_ARTWORK.formatUri("https://api.soundcloud.com/resolve/image?url=soundcloud:users:1234&size=large"))
                .toEqual("https://api.soundcloud.com/resolve/image?url=soundcloud:users:1234&size=tiny");

        expect(ImageSize.TINY_ARTWORK.formatUri("https://api.soundcloud.com/resolve/image?url=soundcloud:users:1234"))
                .toEqual("https://api.soundcloud.com/resolve/image?url=soundcloud:users:1234&size=tiny");
    }

    @Test
    public void shouldReturnLargeIconSizeForHiResScreens() {
        assertFormatUri(2.0f, "https://i1.sndcdn.com/artworks-000032795722-aaqx24-mini.jpg", "https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg");
    }

    @Test
    public void shouldReturnT300IconSizeForHiResScreens() {
        assertFormatUri(10.0f, "https://i1.sndcdn.com/artworks-000032795722-aaqx24-mini.jpg", "https://i1.sndcdn.com/artworks-000032795722-aaqx24-t300x300.jpg");
    }

    private void assertFormatUri(float density, String input, String expected) {
        Resources resources = mock(Resources.class);
        Context context = mock(Context.class);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        displayMetrics.density = density;

        when(context.getResources()).thenReturn(resources);
        when(resources.getDisplayMetrics()).thenReturn(displayMetrics);

        expect(GraphicSize.formatUriForNotificationLargeIcon(context, input)).toEqual(expected);
    }

    @Test
    public void shouldFormatUriForSuggestionList() throws Exception {
        expect(ImageSize.getSearchSuggestionsListItemGraphicSize(Robolectric.application)
                .formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-small.jpg?2479809");
    }
}
