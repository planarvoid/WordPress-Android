package com.soundcloud.android.api.legacy.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.sync.ApiSyncerTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;
import android.database.Cursor;

@RunWith(DefaultTestRunner.class)
public class ConnectionTest {


    @Test
    public void shouldPersistConnections() throws Exception {
        TestHelper.setUserId(100L);

        Connection[] connections = TestHelper.getObjectMapper()
                .readValue(ApiSyncerTest.class.getResourceAsStream("connections.json"), Connection[].class);
        expect(connections).not.toBeNull();
        expect(connections.length).toEqual(4);
        Connection c = connections[0];

        ContentValues[] values = new ContentValues[connections.length];
        for ( int i = 0; i < connections.length; i++){
            values[i] = connections[i].buildContentValues();
        }
        expect(DefaultTestRunner.application.getContentResolver().bulkInsert(Content.ME_CONNECTIONS.uri, values)).toEqual(connections.length);

        final Cursor cursor = DefaultTestRunner.application.getContentResolver().query(Content.ME_CONNECTIONS.forId(c.getId()), null, null, null, null);
        expect(cursor.moveToFirst()).toBeTrue();
        compareConnection(new Connection(cursor), c);
    }

    private void compareConnection(Connection connection1, Connection connection2) {
        expect(connection2.getId()).toEqual(connection1.getId());
        expect(connection2.service).toEqual(connection1.service);
        expect(connection2.type).toEqual(connection1.type);
        expect(connection2.created_at).toEqual(connection1.created_at);
        expect(connection2.display_name).toEqual(connection1.display_name);
        expect(connection2.post_like).toEqual(connection1.post_like);
        expect(connection2.post_publish).toEqual(connection1.post_publish);
        expect(connection2.uri).toEqual(connection1.uri);
    }
}
