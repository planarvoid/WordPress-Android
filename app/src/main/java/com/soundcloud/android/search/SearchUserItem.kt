package com.soundcloud.android.search

import com.soundcloud.android.model.Urn
import com.soundcloud.android.presentation.ListItem
import com.soundcloud.android.users.UserItem
import com.soundcloud.java.optional.Optional

data class SearchUserItem(val userItem: UserItem,
                          val queryUrn: Optional<Urn>,
                          override val urn: Urn = userItem.urn,
                          override val imageUrlTemplate: Optional<String> = userItem.imageUrlTemplate) : ListItem
