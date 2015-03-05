package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playlists.CreateNewPlaylistCommand.Params;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.TxnResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class CreateNewPlaylistCommandTest extends StorageIntegrationTest {

    private CreateNewPlaylistCommand command;

    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        command = new CreateNewPlaylistCommand(propeller(), accountOperations);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(321L));
    }

    @Test
    public void shouldInsertNewPlaylist() throws Exception {
        TxnResult txnResult = (TxnResult) command.with(new Params("title", true, Urn.forTrack(123))).call();

        expect(txnResult.success()).toBeTrue();
        InsertResult insertResult = (InsertResult) txnResult.getResults().get(0);

        assertPlaylistInserted(insertResult.getRowId(), "title", true);
    }

    @Test
    public void shouldInsertPlaylistPost() throws Exception {
        TxnResult txnResult = (TxnResult) command.with(new Params("title", true, Urn.forTrack(123))).call();

        expect(txnResult.success()).toBeTrue();
        InsertResult insertResult = (InsertResult) txnResult.getResults().get(0);

        databaseAssertions().assertPlaylistPostInsertedFor(Urn.forPlaylist(insertResult.getRowId()));
    }

    @Test
    public void shouldInsertFirstPlaylistTrack() throws Exception {
        TxnResult txnResult = (TxnResult) command.with(new Params("title", true, Urn.forTrack(123))).call();

        expect(txnResult.success()).toBeTrue();
        InsertResult insertResult = (InsertResult) txnResult.getResults().get(0);

        databaseAssertions().assertPlaylistTracklist(insertResult.getRowId(), Arrays.asList(Urn.forTrack(123)));
    }

    private void assertPlaylistInserted(long playlistId, String title, boolean isPrivate) {
        assertThat(select(from(Table.Sounds.name())
                .whereEq(TableColumns.Sounds._ID, playlistId)
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.Sounds.USER_ID, 321L)
                .whereNotNull(TableColumns.Sounds.CREATED_AT)
                .whereEq(TableColumns.Sounds.SHARING, Sharing.from(!isPrivate).value())
                .whereEq(TableColumns.Sounds.TITLE, title)), counts(1));
    }

}