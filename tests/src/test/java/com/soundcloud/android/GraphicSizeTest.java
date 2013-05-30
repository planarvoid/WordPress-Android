package com.soundcloud.android;

import com.soundcloud.android.utils.images.ImageSize;
import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    public void shouldFormatUri() throws Exception {
        expect(ImageSize.LARGE.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809");

        expect(ImageSize.LARGE.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809");

        expect(ImageSize.TINY_ARTWORK.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809");

    }

    @Test
    public void shouldFormatResolverUri() throws Exception {
        expect(ImageSize.TINY_ARTWORK.formatUri("https://api.soundcloud.com/resolve/image?url=soundcloud:users:1234&size=large"))
                .toEqual("https://api.soundcloud.com/resolve/image?url=soundcloud:users:1234&size=tiny");

        expect(ImageSize.TINY_ARTWORK.formatUri("https://api.soundcloud.com/resolve/image?url=soundcloud:users:1234"))
                .toEqual("https://api.soundcloud.com/resolve/image?url=soundcloud:users:1234&size=tiny");
    }

    @Test
    public void shouldFormatUriForSuggestionList() throws Exception {
        expect(ImageSize.getSearchSuggestionsListItemGraphicSize(Robolectric.application)
                .formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-small.jpg?2479809");
    }
}
