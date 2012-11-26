package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.sync.ApiSyncerTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class ConnectionTest {


    @Test
    public void shouldPersistConnections() throws Exception {
        DefaultTestRunner.application.setCurrentUserId(100L);

        Connection[] connections = AndroidCloudAPI.Mapper
                .readValue(ApiSyncerTest.class.getResourceAsStream("connections.json"), Connection[].class);
        expect(connections).not.toBeNull();
        expect(connections.length).toEqual(6);
        Connection c = connections[0];

        ContentValues[] values = new ContentValues[connections.length];
        for ( int i = 0; i < connections.length; i++){
            values[i] = connections[i].buildContentValues();
        }
        expect(DefaultTestRunner.application.getContentResolver().bulkInsert(Content.ME_CONNECTIONS.uri, values)).toEqual(6);

        final Cursor cursor = DefaultTestRunner.application.getContentResolver().query(Content.ME_CONNECTIONS.forId(c.id), null, null, null, null);
        expect(cursor.moveToFirst()).toBeTrue();
        compareConnection(new Connection(cursor), c);
    }

    private void compareConnection(Connection connection1, Connection connection2) {
        expect(connection2.id).toEqual(connection1.id);
        expect(connection2.service).toEqual(connection1.service);
        expect(connection2.type).toEqual(connection1.type);
        expect(connection2.created_at).toEqual(connection1.created_at);
        expect(connection2.display_name).toEqual(connection1.display_name);
        expect(connection2.post_like).toEqual(connection1.post_like);
        expect(connection2.post_publish).toEqual(connection1.post_publish);
        expect(connection2.uri).toEqual(connection1.uri);
    }
}
