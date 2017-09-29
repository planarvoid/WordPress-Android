package com.soundcloud.android.users

import com.soundcloud.android.model.Urn
import java.util.Date

fun FollowingDatabase.insertFollowingPendingRemoval(followedUrn: Urn, removedAt: Long) {
    val insertRow = FollowingModel.InsertOrReplaceFromToggle(this.writableDatabase(), FOLLOWING_FACTORY)
    insertRow.bind(followedUrn, null, Date(removedAt))
    this.insert(FollowingModel.TABLE_NAME, insertRow)
}

fun FollowingDatabase.insertFollowingPendingAddition(followedUrn: Urn, addedAt: Long) {
    val insertRow = FollowingModel.InsertOrReplaceFromToggle(this.writableDatabase(), FOLLOWING_FACTORY)
    insertRow.bind(followedUrn, Date(addedAt), null)
    this.insert(FollowingModel.TABLE_NAME, insertRow)
}
