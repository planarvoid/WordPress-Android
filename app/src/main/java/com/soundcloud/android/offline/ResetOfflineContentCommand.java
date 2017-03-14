package com.soundcloud.android.offline;

import static com.soundcloud.android.storage.Tables.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.Tables.TrackDownloads.REQUESTED_AT;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

class ResetOfflineContentCommand extends Command<OfflineContentLocation, List<Urn>> {

    private final PropellerDatabase propeller;
    private final SecureFileStorage secureFileStorage;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final TrackOfflineStateProvider trackOfflineStateProvider;
    private final DateProvider dateProvider;

    @Inject
    ResetOfflineContentCommand(PropellerDatabase propeller,
                               SecureFileStorage secureFileStorage,
                               OfflineSettingsStorage offlineSettingsStorage,
                               TrackOfflineStateProvider trackOfflineStateProvider,
                               CurrentDateProvider dateProvider) {
        this.propeller = propeller;
        this.secureFileStorage = secureFileStorage;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.trackOfflineStateProvider = trackOfflineStateProvider;
        this.dateProvider = dateProvider;
    }

    @Override
    public List<Urn> call(OfflineContentLocation offlineContentLocation) {
        final List<Urn> resetEntities = queryTrackDownloadsUrns(propeller);

        final TxnResult txnResult = propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                ContentValues contentValues = ContentValuesBuilder.values()
                                                                  .put(REQUESTED_AT, dateProvider.getCurrentTime())
                                                                  .put(DOWNLOADED_AT, null)
                                                                  .get();
                step(propeller.update(Tables.TrackDownloads.TABLE, contentValues, OfflineFilters.DOWNLOADED_OFFLINE_TRACK_FILTER));
            }
        });

        if (txnResult.success()) {
            trackOfflineStateProvider.clear();
            secureFileStorage.deleteAllTracks();
            offlineSettingsStorage.setOfflineContentLocation(offlineContentLocation);
            secureFileStorage.updateOfflineDir();
            return resetEntities;
        }

        return Collections.emptyList();
    }

    private List<Urn> queryTrackDownloadsUrns(PropellerDatabase propeller) {
        return propeller.query(Query.from(Tables.TrackDownloads.TABLE)
                                    .where(OfflineFilters.DOWNLOADED_OFFLINE_TRACK_FILTER)).toList(new TrackUrnMapper());
    }
}
