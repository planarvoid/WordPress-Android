package com.soundcloud.android.users

import com.soundcloud.android.discovery.DateAdapter
import com.soundcloud.android.discovery.UrnAdapter
import com.soundcloud.android.model.Urn
import java.util.Date

data class Following(val userUrn: Urn, val position: Long, val addedAt: Date? = null, val removedAt: Date? = null) : FollowingModel {

    override fun user_urn() = userUrn

    override fun position() = position

    override fun added_at() = addedAt

    override fun removed_at() = removedAt
}

private val URN_ADAPTER = UrnAdapter()
private val DATE_ADAPTER = DateAdapter()

private val CREATOR = FollowingModel.Creator<Following> {target_id, position, added_at, removed_at -> Following(target_id, position, added_at, removed_at) }
val FOLLOWING_FACTORY = FollowingModel.Factory(CREATOR, URN_ADAPTER, DATE_ADAPTER, DATE_ADAPTER)
val FOLLOWING_MAPPER: FollowingModel.Mapper<Following> = FOLLOWING_FACTORY.loadFollowedMapper()
