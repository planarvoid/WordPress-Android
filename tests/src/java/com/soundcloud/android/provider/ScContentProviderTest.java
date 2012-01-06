package com.soundcloud.android.provider;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class ScContentProviderTest {
    @Test
    public void shouldMatchUrisToTables() throws Exception {
        ScContentProvider.TableInfo info = ScContentProvider.getTableInfo("content://com.soundcloud.android.provider.ScContentProvider/Users");
        assertThat(info.table.tableName, equalTo("Users"));
        assertThat(info.id, equalTo(-1L));
    }

    @Test
    public void shouldMatchUrisToTablesAndIds() throws Exception {
        ScContentProvider.TableInfo info = ScContentProvider.getTableInfo("content://com.soundcloud.android.provider.ScContentProvider/Users/20");
        assertThat(info.table.tableName, equalTo("Users"));
        assertThat(info.id, equalTo(20L));
    }

    @Test
    public void shouldMatchUrisToTablesAndIdsNegative() throws Exception {
        ScContentProvider.TableInfo info = ScContentProvider.getTableInfo("content://com.soundcloud.android.provider.ScContentProvider/Users/-1");
        assertThat(info.table.tableName, equalTo("Users"));
        assertThat(info.id, equalTo(-1L));
    }


    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIfUriCannotBeMatched() throws Exception {
        ScContentProvider.getTableInfo("foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIfTableDoesNotExist() throws Exception {
        ScContentProvider.getTableInfo("content://com.soundcloud.android.provider.ScContentProvider/Foos");
    }

    @Test
    public void shouldAppendIdToSelection() throws Exception {
        ScContentProvider.TableInfo info = ScContentProvider.getTableInfo("content://com.soundcloud.android.provider.ScContentProvider/Users/20");
        assertThat(info.where(""), equalTo("_id=20"));
        assertThat(info.where("foo=bar"), equalTo("foo=bar AND _id=20"));
    }
}
