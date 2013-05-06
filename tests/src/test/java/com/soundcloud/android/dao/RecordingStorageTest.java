package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Recording;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;

@RunWith(DefaultTestRunner.class)
public class RecordingStorageTest {

    protected static final long USER_ID = 133201L;

    private RecordingStorage storage;

    @Before
    public void setup() {
        storage = new RecordingStorage();
    }

    @Test
    public void shouldDeleteRecording() throws Exception {
        Recording r = TestHelper.createRecording(USER_ID);
        TestHelper.insertWithDependencies(r);
        expect(r.exists()).toBeTrue();
        expect(storage.delete(r).last()).toBeTrue();
        expect(r.exists()).toBeFalse();
    }

    @Test
    public void shouldNotDeleteRecordingIfExternal() throws Exception {
        Recording r = TestHelper.createRecording(USER_ID);
        TestHelper.insertWithDependencies(r);
        r.external_upload = true;
        expect(storage.delete(r).last()).toBeFalse();
        expect(r.exists()).toBeTrue();
    }

    //TODO: blargh. Looks like we're testing something we don't actually wanna have. Moving it here for now.
    @Test
    public void shouldGetRecordingFromIntentViaDatabase() throws Exception {
        Recording r = TestHelper.createRecording(USER_ID);
        TestHelper.insertWithDependencies(r);

        Intent i = new Intent().setData(r.toUri());

        Recording r2 = Recording.fromIntent(i, Robolectric.application, -1);
        expect(r2).not.toBeNull();
        expect(r2.description).toEqual(r.description);
        expect(r2.is_private).toEqual(r.is_private);
        expect(r2.where_text).toEqual(r.where_text);
        expect(r2.what_text).toEqual(r.what_text);
    }



}
