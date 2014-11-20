package com.soundcloud.android.storage.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Request;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class ContentTest {

    @Test
    public void shouldDefineRequest() throws Exception {
        expect(Content.ME_SOUND_STREAM.request().toUrl())
                .toEqual(Content.ME_SOUND_STREAM.remoteUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfNoRemoteUriDefined() throws Exception {
        Content.COLLECTIONS.request();
    }

    @Test
    public void shouldFindContentByUri() throws Exception {
        expect(Content.byUri(Content.ME.uri)).toBe(Content.ME);
    }

    @Test
    public void shouldProvideToString() throws Exception {
        expect(Content.ME_ACTIVITIES.toString()).toEqual("Content.ME_ACTIVITIES");
    }

    @Test
    public void shouldGenerateUriForId() throws Exception {
        expect(Content.COLLECTION_ITEMS.forId(1234).toString()).toEqual(
                "content://com.soundcloud.android.provider.ScContentProvider/collection_items/1234");
    }

    @Test
    public void shouldBuildQuery() throws Exception {
        expect(Content.ME_ACTIVITIES.withQuery("a", "1", "b", "2"))
                .toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/activities/all/own?a=1&b=2");
    }

    @Test
    public void shouldCreateRequestWithQueryCopy() throws Exception {
        Request request = Content.TRACK_SEARCH.request(Content.TRACK_SEARCH.uri.buildUpon().appendQueryParameter("tag","tagValue").build());
        expect(request.getParams().size()).toBe(1);
        expect(request.getParams().get("tag")).toEqual("tagValue");
    }
}
