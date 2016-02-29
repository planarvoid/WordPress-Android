package com.soundcloud.android.storage.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.PeriodicSync;
import android.database.Cursor;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class ScContentProviderTest {
    static final long USER_ID = 100L;
    ContentResolver resolver;

    @Before
    public void before() {
        TestHelper.setUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
    }

    @Test
    public void shouldGetCurrentUserId() throws Exception {
        Cursor c = resolver.query(Content.ME_USERID.uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToFirst()).toBeTrue();
        expect(c.getLong(0)).toEqual(USER_ID);
    }

    @Test
    public void shouldEnableSyncing() throws Exception {
        Account account = new Account("name", "type");
        ScContentProvider.enableSyncing(account, 3600);

        expect(ContentResolver.getSyncAutomatically(account, ScContentProvider.AUTHORITY)).toBeTrue();

        List<PeriodicSync> syncs = ContentResolver.getPeriodicSyncs(account, ScContentProvider.AUTHORITY);
        expect(syncs.size()).toEqual(1);

        final PeriodicSync sync = syncs.get(0);
        expect(sync.account).toEqual(account);
        expect(sync.period).toEqual(3600l);
    }

    @Test
    public void shouldDisableSyncing() throws Exception {
        Account account = new Account("name", "type");
        ScContentProvider.enableSyncing(account, 3600);
        List<PeriodicSync> syncs = ContentResolver.getPeriodicSyncs(account, ScContentProvider.AUTHORITY);
        expect(syncs.size()).toEqual(1);

        ScContentProvider.disableSyncing(account);

        syncs = ContentResolver.getPeriodicSyncs(account, ScContentProvider.AUTHORITY);
        expect(syncs.isEmpty()).toBeTrue();
        expect(ContentResolver.getSyncAutomatically(account, ScContentProvider.AUTHORITY)).toBeFalse();
    }

}
