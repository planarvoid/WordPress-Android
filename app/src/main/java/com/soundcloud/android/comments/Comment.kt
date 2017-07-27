package com.soundcloud.android.comments;

import com.soundcloud.android.model.Urn
import com.soundcloud.android.presentation.ListItem
import com.soundcloud.java.optional.Optional
import java.util.*

data class Comment(
        override val urn: Urn,
        val userUrn: Urn,
        val username: String,
        val timeStamp: Long,
        val text: String,
        val date: Date
) : ListItem {
    constructor(comment: ApiComment) : this(comment.urn, comment.user.urn, comment.user.username, comment.trackTime, comment.body, comment.createdAt)

    override val imageUrlTemplate = Optional.absent<String>()
}
