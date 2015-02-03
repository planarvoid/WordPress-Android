package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.TrackDownloads;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.query.WhereBuilder;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class DeleteOfflineContentCommand extends Command<List<Urn>, Void, DeleteOfflineContentCommand> {
    private final SecureFileStorage fileStorage;
    private final PropellerDatabase database;

    @Inject
    public DeleteOfflineContentCommand(SecureFileStorage fileStorage, PropellerDatabase database) {
        this.fileStorage = fileStorage;
        this.database = database;
    }

    @Override
    public Void call() throws Exception {
        for (Urn track : input) {
            deleteFromFileSystem(track);
            deleteFromDatabase(track);
        }
        return null; // returning void
    }

    private void deleteFromDatabase(Urn track) {
        final Where whereClause = new WhereBuilder().whereEq(_ID, track.getNumericId());
        database.delete(TrackDownloads, whereClause);
    }

    private void deleteFromFileSystem(Urn track) throws EncryptionException, IOException {
        fileStorage.deleteTrack(track);
    }
}
