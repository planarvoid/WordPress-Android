package com.soundcloud.android.playback.streaming;

import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;

public class UpdateMetadataTask extends AsyncTask<StreamItem, Void, Uri> {
    final ContentResolver resolver;

    public UpdateMetadataTask(ContentResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected Uri doInBackground(StreamItem... params) {
        StreamItem item = params[0];
        ContentValues values = new ContentValues();
        values.put(TableColumns.TrackMetadata._ID, item.trackId);
        values.put(TableColumns.TrackMetadata.ETAG, item.etag());
        values.put(TableColumns.TrackMetadata.SIZE, item.getContentLength());
        values.put(TableColumns.TrackMetadata.BITRATE, item.getBitrate());
        values.put(TableColumns.TrackMetadata.URL_HASH, item.urlHash);
        values.put(TableColumns.TrackMetadata.CACHED, 1);

       return resolver.insert(Content.TRACK_METADATA.uri, values);
    }
}
