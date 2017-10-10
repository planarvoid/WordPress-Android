package com.soundcloud.android.playlists;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import javax.inject.Inject;

public class RemovePlaylistFromDatabaseCommand extends DefaultWriteStorageCommand<Urn, TxnResult> {

    @Inject
    RemovePlaylistFromDatabaseCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, final Urn playlist) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.delete(Tables.Sounds.TABLE, filter()
                        .whereEq(Tables.Sounds._TYPE, Tables.Sounds.TYPE_PLAYLIST)
                        .whereEq(Tables.Sounds._ID, playlist.getNumericId())));

                removePlaylistFromAssociatedViews(propeller);
            }

            private void removePlaylistFromAssociatedViews(PropellerDatabase propeller) {
                //TODO : When these are removed, we should delete this and go to the repository to remove instead
                step(propeller.delete(Table.Activities, filter()
                        .whereEq(TableColumns.Activities.SOUND_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                        .whereEq(TableColumns.Activities.SOUND_ID, playlist.getNumericId())));

                step(propeller.delete(Table.SoundStream, filter()
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                        .whereEq(TableColumns.SoundStream.SOUND_ID, playlist.getNumericId())));
            }
        });
    }
}