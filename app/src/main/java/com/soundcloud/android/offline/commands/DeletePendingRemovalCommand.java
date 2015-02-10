package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REMOVED_AT;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.UrnMapper;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.query.WhereBuilder;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DeletePendingRemovalCommand extends Command<Urn, List<Urn>, DeletePendingRemovalCommand> {
    private static final long DELAY = TimeUnit.MINUTES.toMillis(3);

    private final SecureFileStorage fileStorage;
    private final PropellerDatabase database;

    @Inject
    public DeletePendingRemovalCommand(SecureFileStorage fileStorage, PropellerDatabase database) {
        this.fileStorage = fileStorage;
        this.database = database;
    }

    @Override
    public List<Urn> call() throws Exception {
        final List<Urn> tracksToRemove = getTracksRemovedBefore();

        for (Urn track : tracksToRemove) {
            if (isNotPlayingTrack(track)) {
                deleteFromFileSystem(track);
                deleteFromDatabase(track);
            }
        }
        return tracksToRemove;
    }

    private boolean isNotPlayingTrack(Urn track) {
        return input == null || !input.equals(track);
    }

    private List<Urn> getTracksRemovedBefore() {
        long removalDelayedTimestamp = System.currentTimeMillis() - DELAY;
        return database.query(Query.from(Table.TrackDownloads.name())
                .select(_ID)
                .whereLe(REMOVED_AT, removalDelayedTimestamp))
                .toList(new UrnMapper());
    }

    private ChangeResult deleteFromDatabase(Urn track) {
        final Where whereClause = new WhereBuilder().whereEq(_ID, track.getNumericId());
        return database.delete(TrackDownloads, whereClause);
    }

    private void deleteFromFileSystem(Urn track) throws EncryptionException, IOException {
        fileStorage.deleteTrack(track);
    }
}
