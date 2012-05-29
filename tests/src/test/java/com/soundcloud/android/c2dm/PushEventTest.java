package com.soundcloud.android.c2dm;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.sync.SyncAdapterService;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.os.Bundle;

@RunWith(DefaultTestRunner.class)
public class PushEventTest {

    @Test
    public void shouldGetEventFromIntent() throws Exception {
        expect(PushEvent.fromIntent(
                new Intent().putExtra(C2DMReceiver.SC_EXTRA_EVENT_TYPE, "like"))
        ).toEqual(PushEvent.LIKE);

        expect(PushEvent.fromIntent(
                new Intent().putExtra(C2DMReceiver.SC_EXTRA_EVENT_TYPE, "follower"))
        ).toEqual(PushEvent.FOLLOWER);

        expect(PushEvent.fromIntent(
                new Intent().putExtra(C2DMReceiver.SC_EXTRA_EVENT_TYPE, "comment"))
        ).toEqual(PushEvent.COMMENT);

        expect(PushEvent.fromIntent(
                new Intent().putExtra(C2DMReceiver.SC_EXTRA_EVENT_TYPE, "bla"))
        ).toEqual(PushEvent.UNKNOWN);

        expect(PushEvent.fromIntent(new Intent())).toEqual(PushEvent.NONE);
        expect(PushEvent.fromIntent(null)).toEqual(PushEvent.NONE);
    }

    @Test
    public void shouldGetEventFromExtras() throws Exception {
        Bundle extras = new Bundle();
        extras.putString(C2DMReceiver.SC_EXTRA_EVENT_TYPE, "like");
        expect(PushEvent.fromExtras(extras)).toEqual(PushEvent.LIKE);

        extras.putString(C2DMReceiver.SC_EXTRA_EVENT_TYPE, "follower");
        expect(PushEvent.fromExtras(extras)).toEqual(PushEvent.FOLLOWER);

        extras.putString(C2DMReceiver.SC_EXTRA_EVENT_TYPE, "comment");
        expect(PushEvent.fromExtras(extras)).toEqual(PushEvent.COMMENT);

        extras.putString(C2DMReceiver.SC_EXTRA_EVENT_TYPE, "bla");
        expect(PushEvent.fromExtras(extras)).toEqual(PushEvent.UNKNOWN);

        expect(PushEvent.fromExtras(new Bundle())).toEqual(PushEvent.NONE);
        expect(PushEvent.fromExtras(null)).toEqual(PushEvent.NONE);
    }

    @Test
    public void shouldGetEventFromExtrasSyncAdapter() throws Exception {
        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, "like");
        expect(PushEvent.fromExtras(extras)).toEqual(PushEvent.LIKE);

        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, "follower");
        expect(PushEvent.fromExtras(extras)).toEqual(PushEvent.FOLLOWER);

        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, "comment");
        expect(PushEvent.fromExtras(extras)).toEqual(PushEvent.COMMENT);

        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, "bla");
        expect(PushEvent.fromExtras(extras)).toEqual(PushEvent.UNKNOWN);

        expect(PushEvent.fromExtras(new Bundle())).toEqual(PushEvent.NONE);
        expect(PushEvent.fromExtras(null)).toEqual(PushEvent.NONE);
    }

    @Test
    public void shouldGetIdFromUri() throws Exception {
        expect(PushEvent.getIdFromUri("soundcloud:people:1234")).toEqual(1234l);
        expect(PushEvent.getIdFromUri("soundcloud:people:foo")).toEqual(-1l);
        expect(PushEvent.getIdFromUri("blargh")).toEqual(-1l);
    }
}
