package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentProvider;

@RunWith(SoundCloudTestRunner.class)
public class TableTest {

    @Before
    public void setup() {
        ContentProvider provider = new ScContentProvider(new DatabaseManager(Robolectric.application));
        provider.onCreate();
        ShadowContentResolver.registerProvider(ScContentProvider.AUTHORITY, provider);
    }

    @Test
    public void shouldProvideACreateStringForViews() throws Exception {
        Table view = Table.SoundView;
        expect(view.view).toBe(true);
        expect(view.createString).toMatch("CREATE VIEW IF NOT EXISTS " + view);
    }

    @Test
    public void shouldProvideACreateStringForTables() throws Exception {
        Table table = Table.Sounds;
        expect(table.view).toBe(false);
        expect(table.createString).toMatch("CREATE TABLE IF NOT EXISTS " + table);
    }

    @Test
    public void shouldHaveFieldMethod() throws Exception {
        Table table = Table.Sounds;
        expect(table.field("foo")).toEqual("Sounds.foo");
    }

}
