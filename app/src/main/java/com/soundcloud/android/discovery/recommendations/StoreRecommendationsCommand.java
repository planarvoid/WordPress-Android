package com.soundcloud.android.discovery.recommendations;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.RecommendationSeeds;
import com.soundcloud.android.storage.Tables.Recommendations;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;

public class StoreRecommendationsCommand
        extends DefaultWriteStorageCommand<ModelCollection<ApiRecommendation>, WriteResult> {

    private final PropellerDatabase propeller;

    @Inject
    public StoreRecommendationsCommand(PropellerDatabase database) {
        super(database);
        this.propeller = database;
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final ModelCollection<ApiRecommendation> recommendations) {
        clearTables();
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                //For tracking we need to keep both query_urn and query_position.
                //https://github.com/soundcloud/eventgateway-schemas/blob/v0/doc/personalized-recommender-tracking.md
                long queryPosition = 0;
                final Urn queryUrn = recommendations.getQueryUrn().or(Urn.NOT_SET);

                for (ApiRecommendation apiRecommendation : recommendations) {
                    if (isReasonValid(apiRecommendation)) {
                        //Store seed track
                        writeTrack(propeller, apiRecommendation.getSeedTrack());

                        //Store seed track in recommendations
                        final InsertResult seedInsert = propeller.insert(RecommendationSeeds.TABLE,
                                                                         buildSeedContentValues(apiRecommendation,
                                                                                                queryUrn,
                                                                                                queryPosition));
                        step(seedInsert);

                        //Store recommended tracks
                        for (ApiTrack trackRecommendation : apiRecommendation.getRecommendations()) {
                            writeTrack(propeller, trackRecommendation);
                            step(propeller.upsert(Recommendations.TABLE,
                                                  buildRecommendationContentValues(trackRecommendation,
                                                                                   seedInsert.getRowId())));
                        }
                        queryPosition++;
                    }
                }
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

    private ContentValues buildSeedContentValues(ApiRecommendation apiRecommendation,
                                                 Urn queryUrn,
                                                 long queryPosition) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(RecommendationSeeds.SEED_SOUND_ID.name(),
                          apiRecommendation.getSeedTrack().getUrn().getNumericId());
        contentValues.put(RecommendationSeeds.SEED_SOUND_TYPE.name(), TableColumns.Sounds.TYPE_TRACK);
        contentValues.put(RecommendationSeeds.RECOMMENDATION_REASON.name(), getReason(apiRecommendation));
        contentValues.put(RecommendationSeeds.QUERY_URN.name(), queryUrn.toString());
        contentValues.put(RecommendationSeeds.QUERY_POSITION.name(), queryPosition);
        return contentValues;
    }

    @SuppressWarnings("UnusedParameters")
    private int getReason(ApiRecommendation apiRecommendation) {
        ApiRecommendation.Reason reason = apiRecommendation.getRecommendationReason();
        switch (reason) {
            case LIKED:
                return RecommendationSeeds.REASON_LIKED;
            case LISTENED_TO:
                return RecommendationSeeds.REASON_PLAYED;
            default:
                throw new IllegalArgumentException("Unhandled reason " + reason);
        }

    }

    public void clearTables() {
        propeller.delete(Recommendations.TABLE);
        propeller.delete(RecommendationSeeds.TABLE);
    }
}
