package com.soundcloud.android.olddiscovery.recommendations

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.BaseRxResultMapperV2
import com.soundcloud.android.storage.DefaultCleanupHelper
import com.soundcloud.android.storage.Tables.Recommendations
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

class RecommendationsCleanupHelper @Inject constructor(private val propeller: PropellerDatabase) : DefaultCleanupHelper() {

    override fun getTracksToKeep(): MutableSet<Urn> {
        return propeller.query(Query.from(Recommendations.TABLE).select(Recommendations.SEED_ID, Recommendations.RECOMMENDED_SOUND_ID, Recommendations.RECOMMENDED_SOUND_TYPE))
                .flatMap { setOf(Urn.forTrack(it.getLong(Recommendations.SEED_ID)), BaseRxResultMapperV2.readSoundUrn(it, Recommendations.RECOMMENDED_SOUND_ID, Recommendations.RECOMMENDED_SOUND_TYPE)) }
                //There shouldn't be anything else than tracks
                .filter { it.isTrack }
                .toMutableSet()
    }
}
