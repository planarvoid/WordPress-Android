package com.soundcloud.android.streaming;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class UpdateMetadataTaskTest {

    @Test
    public void shouldUpdateMetadata() throws Exception {
        final ContentResolver resolver = DefaultTestRunner.application.getContentResolver();
        UpdateMetadataTask task = new UpdateMetadataTask(resolver);
        StreamItem item = new StreamItem("https://api.soundcloud.com/tracks/1/stream", 123l, "4748cdb4de48635e843db0670e1ad47a");
        Uri uri = task.doInBackground(item);
        expect(uri).not.toBeNull();
        expect(uri).toEqual("content://com.soundcloud.android.provider.ScContentProvider/track_metadata/1");

        Cursor c = resolver.query(Content.TRACK_METADATA.uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToFirst()).toBeTrue();

        expect(c.getLong(c.getColumnIndex(DBHelper.TrackMetadata.SIZE))).toEqual(123l);
        expect(c.getString(c.getColumnIndex(DBHelper.TrackMetadata.ETAG))).toEqual("4748cdb4de48635e843db0670e1ad47a");
        expect(c.getString(c.getColumnIndex(DBHelper.TrackMetadata.URL_HASH))).toEqual(item.urlHash);
        expect(c.getInt(c.getColumnIndex(DBHelper.TrackMetadata.CACHED))).toEqual(1);
        expect(c.getInt(c.getColumnIndex(DBHelper.TrackMetadata.BITRATE))).toEqual(0);
        expect(c.getInt(c.getColumnIndex(DBHelper.TrackMetadata.TYPE))).toEqual(0);
        expect(c.getInt(c.getColumnIndex(DBHelper.TrackMetadata.PLAY_COUNT))).toEqual(0);
    }
}
