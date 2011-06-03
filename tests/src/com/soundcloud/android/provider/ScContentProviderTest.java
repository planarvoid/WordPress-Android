package com.soundcloud.android.provider;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;


@RunWith(DefaultTestRunner.class)
public class ScContentProviderTest {
    @Test
    public void shouldMatchUrisToTables() throws Exception {
        ScContentProvider.TableInfo info = ScContentProvider.getTableInfo("content://com.soundcloud.android.providers.ScContentProvider/Users");
        assertThat(info.table.tableName, equalTo("Users"));
        assertThat(info.id, equalTo(-1L));
    }

    @Test
    public void shouldMatchUrisToTablesAndIds() throws Exception {
        ScContentProvider.TableInfo info = ScContentProvider.getTableInfo("content://com.soundcloud.android.providers.ScContentProvider/Users/20");
        assertThat(info.table.tableName, equalTo("Users"));
        assertThat(info.id, equalTo(20L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIfUriCannotBeMatched() throws Exception {
        ScContentProvider.getTableInfo("foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIfTableDoesNotExist() throws Exception {
        ScContentProvider.getTableInfo("content://com.soundcloud.android.providers.ScContentProvider/Foos");
    }
}
