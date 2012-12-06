package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import scala.remote;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.List;

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

        expect(Content.ME_SHORTCUTS_ICON.forId(1234).toString()).toEqual(
                "content://com.soundcloud.android.provider.ScContentProvider/me/shortcut_icon/1234");
    }

    @Test
    public void shouldBuildQuery() throws Exception {
        expect(Content.ME_ACTIVITIES.withQuery("a", "1", "b", "2"))
                .toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/activities/all/own?a=1&b=2");
    }
}
