package com.soundcloud.android.profile

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.Tables
import com.soundcloud.android.storage.Tables.Posts
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.propeller.query.Query
import com.soundcloud.propeller.query.Query.Order.DESC
import com.soundcloud.propeller.rx.PropellerRxV2
import io.reactivex.Single
import java.util.Date
import javax.inject.Inject

@OpenForTesting
class PostsStorage
@Inject
constructor(private val propellerRx: PropellerRxV2) {

    fun loadPostedTracksSortedByDateDesc(): Single<List<Pair<Urn, Date>>> {
        return propellerRx.queryResult(buildQuery()).map { it.toList { Urn.forTrack(it.getLong(Posts.TARGET_ID)) to it.getDateFromTimestamp(Posts.CREATED_AT) } }.singleOrError()
    }

    private fun buildQuery(): Query {
        return Query.from(Posts.TABLE)
                .select(Posts.TARGET_ID, Posts.CREATED_AT)
                .whereEq(Posts.TYPE, Posts.TYPE_POST)
                .whereEq(Posts.TARGET_TYPE, Tables.Sounds.TYPE_TRACK)
                .order(Posts.CREATED_AT.qualifiedName(), DESC)
    }
}
