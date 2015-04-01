package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.TrackDownloads;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Where;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

public class DeleteOfflineTrackCommand extends Command<Collection<Urn>, Collection<Urn>> {
    private static final String TAG = DeleteOfflineTrackCommand.class.getSimpleName();

    private final SecureFileStorage fileStorage;
    private final PropellerDatabase database;

    @Inject
    public DeleteOfflineTrackCommand(SecureFileStorage fileStorage, PropellerDatabase database) {
        this.fileStorage = fileStorage;
        this.database = database;
    }

    @Override
    public Collection<Urn> call(Collection<Urn> input) {
        Collection<Urn> output = new ArrayList<>(input.size());
        for (Urn track : input) {
            if (deleteFromFileSystem(track)) {
                deleteFromDatabase(track);
                output.add(track);
            }
        }
        return output;
    }

    private ChangeResult deleteFromDatabase(Urn track) {
        final Where whereClause = Filter.filter().whereEq(_ID, track.getNumericId());
        return database.delete(TrackDownloads, whereClause);
    }

    private boolean deleteFromFileSystem(Urn track) {
        try {
            fileStorage.deleteTrack(track);
            return true;
        } catch (EncryptionException e) {
            Log.e(TAG, "Could not delete file from filesystem. " + e.getMessage());
            return false;
        }
    }
}
