package com.soundcloud.android.streaming;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

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
        values.put(DBHelper.TrackMetadata._ID, item.trackId);
        values.put(DBHelper.TrackMetadata.ETAG, item.etag());
        values.put(DBHelper.TrackMetadata.SIZE, item.getContentLength());
        values.put(DBHelper.TrackMetadata.BITRATE, item.getBitrate());
        values.put(DBHelper.TrackMetadata.URL_HASH, item.urlHash);
        values.put(DBHelper.TrackMetadata.CACHED, 1);

       return resolver.insert(Content.TRACK_METADATA.uri, values);
    }
}
