package com.soundcloud.android.dao;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncServiceTest;
import com.soundcloud.android.service.sync.SyncAdapterServiceTest;
import com.soundcloud.android.service.sync.SyncStateManager;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.soundcloud.android.Wrapper.CloudDateFormat.toTime;
import static com.soundcloud.android.Expect.expect;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class ActivitiesStorageTest {
    ActivitiesStorage storage;

    @Before public void before() {
        storage = new ActivitiesStorage(Robolectric.application);
    }

    @Test
    public void shouldGetActivitiesFromDBWithTimeFiltering() throws Exception {
        Activities a = TestHelper.readJson(Activities.class, SyncAdapterServiceTest.class,  "e1_stream_1.json");
        storage.insert(Content.ME_SOUND_STREAM, a);
        expect(Content.ME_SOUND_STREAM).toHaveCount(22);

        expect(
                storage.getSince(Content.ME_SOUND_STREAM,
                        toTime("2012/09/27 14:08:01 +0000")).size()
        ).toEqual(2);

        expect(
                storage.getBefore(Content.ME_SOUND_STREAM.uri,
                        toTime("2012/09/26 15:00:00 +0000")).size()
        ).toEqual(1);
    }

    @Test
    public void shouldCountActivitiesSinceGivenTime() throws IOException {
        Activities a = TestHelper.readJson(Activities.class, SyncAdapterServiceTest.class,  "e1_stream_1.json");
        storage.insert(Content.ME_SOUND_STREAM, a);
        expect(Content.ME_SOUND_STREAM).toHaveCount(22);

        expect(storage.getCountSince(toTime("2012/09/27 14:15:00 +0000"), Content.ME_SOUND_STREAM)).toBe(2);
    }

    @Test
    public void shouldGetFirstAndLastActivity() throws Exception {
        Activity first = storage.getFirstActivity(Content.ME_SOUND_STREAM);
        Activity last = storage.getLastActivity(Content.ME_SOUND_STREAM);
        expect(first).toBeNull();
        expect(last).toBeNull();

        Activities one_of_each = TestHelper.readJson(Activities.class, ApiSyncServiceTest.class,
                "e1_one_of_each_activity.json");

        expect(storage.insert(Content.ME_SOUND_STREAM, one_of_each)).toBe(7);

        first = storage.getFirstActivity(Content.ME_SOUND_STREAM);
        expect(first).not.toBeNull();
        expect(first.uuid).toEqual("8e3bf200-0744-11e2-9817-590114067ab0");

        last = storage.getLastActivity(Content.ME_SOUND_STREAM);
        expect(last).not.toBeNull();
        expect(last.uuid).toEqual("75e9d700-0819-11e2-81bb-70dbfa89bdb9");
        expect(first.created_at.after(last.created_at)).toBeTrue();
    }

    @Test
    public void shouldGetLastActivity() throws Exception {
        Activities a = TestHelper.readJson(Activities.class, SyncAdapterServiceTest.class,  "e1_stream_1.json");
        storage.insert(Content.ME_SOUND_STREAM, a);
        expect(Content.ME_SOUND_STREAM).toHaveCount(22);

        expect(
                storage.getLastActivity(Content.ME_SOUND_STREAM).created_at.getTime()
        ).toEqual(toTime("2012/09/26 14:52:27 +0000"));
    }

    @Test
    public void shouldClearAllActivities() throws Exception {
        Activities a = TestHelper.readJson(Activities.class, SyncAdapterServiceTest.class,  "e1_stream_1.json");

        storage.insert(Content.ME_SOUND_STREAM, a);
        expect(Content.ME_SOUND_STREAM).toHaveCount(22);

        LocalCollection lc = new LocalCollection(
                Content.ME_SOUND_STREAM.uri,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                LocalCollection.SyncState.IDLE,
                a.size(),
                a.future_href);
        new LocalCollectionDAO(DefaultTestRunner.application.getContentResolver()).create(lc);

        storage.clear(null);

        expect(Content.ME_SOUND_STREAM).toHaveCount(0);
        expect(Content.COLLECTIONS).toHaveCount(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfContentPassedToClearIsUnrelated() throws Exception {
        storage.clear(Content.ME);
    }
}
