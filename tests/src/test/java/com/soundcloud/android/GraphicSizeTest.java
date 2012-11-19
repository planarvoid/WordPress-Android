package com.soundcloud.android;

import static com.soundcloud.android.Consts.GraphicSize;
import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class GraphicSizeTest {
    @Test
    public void shouldGetMinimumGraphicSize() throws Exception {
        expect(GraphicSize.getMinimumSizeFor(99, 101, true)).toEqual(GraphicSize.T300);
        expect(GraphicSize.getMinimumSizeFor(99, 101, false)).toEqual(GraphicSize.LARGE);
        expect(GraphicSize.getMinimumSizeFor(67, 67, true)).toEqual(GraphicSize.T67);
        expect(GraphicSize.getMinimumSizeFor(68, 67, true)).toEqual(GraphicSize.LARGE);
        expect(GraphicSize.getMinimumSizeFor(68, 67, false)).toEqual(GraphicSize.T67);
    }

    @Test
    public void shouldFormatUri() throws Exception {
        expect(GraphicSize.LARGE.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809");

        expect(GraphicSize.LARGE.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809");

        expect(GraphicSize.TINY_ARTWORK.formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-large.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809");
    }

    @Test
    public void shouldFormatUriForSuggestionList() throws Exception {
        expect(GraphicSize.getSearchSuggestionsListItemGraphicSize(Robolectric.application)
                .formatUri("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809"))
                .toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-small.jpg?2479809");
    }
}
