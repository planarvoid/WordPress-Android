package com.soundcloud.android.tracks

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.Tables
import com.soundcloud.propeller.CursorReader
import com.soundcloud.propeller.QueryResult
import com.soundcloud.propeller.query.Query
import com.soundcloud.propeller.rx.PropellerRxV2
import com.soundcloud.propeller.rx.RxResultMapperV2
import io.reactivex.Single
import java.util.Date
import javax.inject.Inject

class TrackPolicyStorage
@Inject constructor(private val propeller: PropellerRxV2) {

    fun filterForStalePolicies(trackUrns: Set<Urn>, staleTime: Date): Single<Set<Urn>> {
        return propeller.queryResult(buildQuery(trackUrns, staleTime))
                .map { queryResult: QueryResult ->  queryResult.toList (PolicyTrackUrnMapper())}
                .map { trackUrns.subtract(it) }
                .singleOrError()
    }

    private fun buildQuery(trackUrns: Set<Urn>, staleTime: Date): Query {
        return Query.from(Tables.TrackPolicies.TABLE)
                .select(Tables.TrackPolicies.TRACK_ID)
                .whereIn(Tables.TrackPolicies.TRACK_ID, trackUrns.map { it.numericId })
                .whereGt(Tables.TrackPolicies.LAST_UPDATED, staleTime.time)
    }

    class PolicyTrackUrnMapper : RxResultMapperV2<Urn>() {
        override fun map(cursorReader: CursorReader): Urn = Urn.forTrack(cursorReader.getLong(Tables.TrackPolicies.TRACK_ID))
    }
}
