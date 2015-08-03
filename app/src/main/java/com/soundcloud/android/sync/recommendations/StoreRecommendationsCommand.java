package com.soundcloud.android.sync.recommendations;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.RecommendationSeeds;
import com.soundcloud.android.storage.Tables.Recommendations;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;

public class StoreRecommendationsCommand extends DefaultWriteStorageCommand<Iterable<? extends ApiRecommendation>, WriteResult> {

    @Inject
    public StoreRecommendationsCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final Iterable<? extends ApiRecommendation> input) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {

                clearTables(propeller);

                for (ApiRecommendation apiRecommendation : input) {
                    if (isReasonValid(apiRecommendation)) {
                        //Store seed track
                        writeTrack(propeller, apiRecommendation.getSeedTrack());

                        //Store seed track in recommendations
                        final InsertResult seedInsert = propeller.insert(RecommendationSeeds.TABLE, buildSeedContentValues(apiRecommendation));
                        step(seedInsert);

                        //Store recommended tracks
                        for (ApiTrack trackRecommendation : apiRecommendation.getRecommendations()) {
                            writeTrack(propeller, trackRecommendation);
                            step(propeller.upsert(Recommendations.TABLE, buildRecommendationContentValues(trackRecommendation, seedInsert.getRowId())));
                        }
                    }
                }
            }

            private void clearTables(PropellerDatabase propeller) {
                step(propeller.delete(Recommendations.TABLE));
                step(propeller.delete(RecommendationSeeds.TABLE));
            }

            //TODO: Create a way of sharing the track writing logic, with a base class (e.g. WriteTrackTransaction)
            private void writeTrack(PropellerDatabase propeller, ApiTrack seedTrack) {
                step(propeller.upsert(Table.Users, StoreUsersCommand.buildUserContentValues(seedTrack.getUser())));
                step(propeller.upsert(Table.Sounds, StoreTracksCommand.buildTrackContentValues(seedTrack)));
                step(propeller.upsert(Table.TrackPolicies, StoreTracksCommand.buildPolicyContentValues(seedTrack)));
            }
        });
    }

    private boolean isReasonValid(ApiRecommendation apiRecommendation) {
        return apiRecommendation.getRecommendationReason() != ApiRecommendation.Reason.UNKNOWN;
    }

    private ContentValues buildRecommendationContentValues(ApiTrack trackRecommendation, long seedId) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(Recommendations.SEED_ID.name(), seedId);
        contentValues.put(Recommendations.RECOMMENDED_SOUND_ID.name(), trackRecommendation.getUrn().getNumericId());
        contentValues.put(Recommendations.RECOMMENDED_SOUND_TYPE.name(), TableColumns.Sounds.TYPE_TRACK);
        return contentValues;
    }

    private ContentValues buildSeedContentValues(ApiRecommendation apiRecommendation) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(RecommendationSeeds.SEED_SOUND_ID.name(), apiRecommendation.getSeedTrack().getUrn().getNumericId());
        contentValues.put(RecommendationSeeds.SEED_SOUND_TYPE.name(), TableColumns.Sounds.TYPE_TRACK);
        contentValues.put(RecommendationSeeds.RECOMMENDATION_REASON.name(), getReason(apiRecommendation));
        return contentValues;
    }

    @SuppressWarnings("UnusedParameters")
    private int getReason(ApiRecommendation apiRecommendation) {
        ApiRecommendation.Reason reason = apiRecommendation.getRecommendationReason();
        switch (reason) {
            case LIKED:
                return RecommendationSeeds.REASON_LIKED;
            case LISTENED_TO:
                return RecommendationSeeds.REASON_LISTENED_TO;
            default:
                throw new IllegalArgumentException("Unhandled reason " + reason);
        }

    }
}
