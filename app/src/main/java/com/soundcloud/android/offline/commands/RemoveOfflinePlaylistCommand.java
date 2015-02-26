package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.OfflineContent;
import static com.soundcloud.android.storage.TableColumns.OfflineContent._TYPE;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.query.WhereBuilder;

import javax.inject.Inject;

public class RemoveOfflinePlaylistCommand  extends StoreCommand<Urn> {

    @Inject
    RemoveOfflinePlaylistCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        return database.delete(OfflineContent, buildWhereClause(input));
    }

    private Where buildWhereClause(Urn urn) {
        return new WhereBuilder()
                .whereEq(_ID, urn.getNumericId())
                .whereEq(_TYPE, TableColumns.OfflineContent.TYPE_PLAYLIST);
    }
}