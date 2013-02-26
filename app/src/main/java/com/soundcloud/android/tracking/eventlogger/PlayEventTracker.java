package com.soundcloud.android.tracking.eventlogger;

import com.soundcloud.android.model.ClientUri;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.UUID;

public class PlayEventTracker {

    private ContentResolver resolver;

    public PlayEventTracker(ContentResolver resolver) {
        this.resolver = resolver;
    }

    public void trackEvent(final @Nullable Track track, final Action action, final long userId, final String originUrl,
                           final String level) {
        if (track != null) {
            ContentValues values = buildContentValues(track, action.toApiName(), userId, originUrl, level);
            resolver.insert(Content.TRACKING_EVENTS.uri, values);
        }
    }

    private ContentValues buildContentValues(final Track track, final String action, final long userId,
                                             final String originUrl, final String level) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.TrackingEvents.TIMESTAMP, System.currentTimeMillis());
        values.put(DBHelper.TrackingEvents.ACTION, action);
        values.put(DBHelper.TrackingEvents.SOUND_URN, ClientUri.forTrack(track.id).toString());
        values.put(DBHelper.TrackingEvents.SOUND_DURATION, track.duration);
        values.put(DBHelper.TrackingEvents.USER_URN, buildUserUrn(userId));
        values.put(DBHelper.TrackingEvents.ORIGIN_URL, originUrl);
        values.put(DBHelper.TrackingEvents.LEVEL, level);
        return values;
    }

    private String buildUserUrn(final long userId) {
        if (userId < 0) {
            return "anonymous:" + UUID.randomUUID();
        } else {
            return ClientUri.forUser(userId).toString();
        }
    }
}
