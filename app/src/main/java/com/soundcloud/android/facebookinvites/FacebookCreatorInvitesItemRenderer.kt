package com.soundcloud.android.facebookinvites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.FacebookInvitesEvent
import com.soundcloud.android.image.ApiImageSize
import com.soundcloud.android.image.ImageOperations
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.stream.StreamItem
import com.soundcloud.java.optional.Optional
import com.soundcloud.rx.eventbus.EventBusV2
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.facebook_creator_invites_notification.view.*
import kotlinx.android.synthetic.main.facebook_creator_invites_notification_card.view.*
import javax.inject.Inject

class FacebookCreatorInvitesItemRenderer
@Inject
constructor(private val imageOperations: ImageOperations,
            private val facebookInvitesStorage: FacebookInvitesStorage,
            private val eventBus: EventBusV2) : CellRenderer<StreamItem.FacebookCreatorInvites> {
    val loadingResult: PublishSubject<FacebookLoadingResult> = PublishSubject.create()

    override fun createItemView(parent: ViewGroup): View {
        eventBus.publish(EventQueue.TRACKING, FacebookInvitesEvent.forCreatorShown())
        return LayoutInflater.from(parent.context)
                .inflate(R.layout.facebook_creator_invites_notification_card, parent, false)
    }

    override fun bindItemView(position: Int, itemView: View, items: List<StreamItem.FacebookCreatorInvites>) {
        itemView.isEnabled = false
        itemView.close_button.setOnClickListener {
            facebookInvitesStorage.setCreatorDismissed()
            loadingResult.onNext(FacebookLoadingResult.Dismiss(position))
        }
        itemView.action_button.setOnClickListener {
            facebookInvitesStorage.setClicked()
            loadingResult.onNext(FacebookLoadingResult.Click(position))
        }
        imageOperations.displayWithPlaceholder(
                items[position].trackUrn,
                Optional.absent(),
                ApiImageSize.T300,
                itemView.artwork)
    }

}
