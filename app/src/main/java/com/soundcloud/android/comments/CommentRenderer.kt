package com.soundcloud.android.comments

import android.content.res.Resources
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.image.ApiImageSize
import com.soundcloud.android.image.ImageOperations
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.utils.ScTextUtils
import com.soundcloud.java.optional.Optional
import kotlinx.android.synthetic.main.engagement_list_item.view.body
import kotlinx.android.synthetic.main.engagement_list_item.view.date
import kotlinx.android.synthetic.main.engagement_list_item.view.image
import kotlinx.android.synthetic.main.engagement_list_item.view.username
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CommentRenderer
@Inject
constructor(private val resources: Resources, private val imageOperations: ImageOperations) : CellRenderer<Comment> {

    override fun createItemView(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.engagement_list_item, parent, false)

    override fun bindItemView(position: Int, view: View, items: List<Comment>) {
        with(items[position]) {
            view.username.ellipsize = TextUtils.TruncateAt.MIDDLE
            view.username.text = timeStamp?.let { resources.getString(R.string.user_commented_at_timestamp, username, ScTextUtils.formatTimestamp(timeStamp, TimeUnit.MILLISECONDS)) } ?: username
            view.body.text = text
            imageOperations.displayCircularInAdapterView(Optional.of(userUrn), imageUrlTemplate, ApiImageSize.getListItemImageSize(resources), view.image)
            view.date.text = ScTextUtils.formatTimeElapsedSince(resources, date.time, true)
        }
    }

}
