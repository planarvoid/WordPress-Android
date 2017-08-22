package com.soundcloud.android.comments;

import com.soundcloud.android.api.legacy.model.PublicApiComment
import com.soundcloud.android.model.Urn
import com.soundcloud.android.presentation.ListItem
import com.soundcloud.java.optional.Optional
import java.util.Date

data class Comment(
        override val urn: Urn,
        val userUrn: Urn,
        val username: String,
        val timeStamp: Long?,
        val text: String,
        val date: Date,
        override val imageUrlTemplate: Optional<String>
) : ListItem {
    constructor(comment: ApiComment) : this(comment.urn, comment.user.urn, comment.user.username, comment.trackTime.orNull(), comment.body, comment.createdAt, comment.user.imageUrlTemplate)
    constructor(comment: PublicApiComment) : this(comment.urn, comment.user.urn, comment.user.username ?: "", comment.timestamp, comment.body, comment.createdAt, comment.user.imageUrlTemplate)
}
