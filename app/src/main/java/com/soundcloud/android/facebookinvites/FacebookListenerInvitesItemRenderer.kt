package com.soundcloud.android.facebookinvites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.soundcloud.android.R
import com.soundcloud.android.facebookapi.FacebookApi
import com.soundcloud.android.image.ImageOperations
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.rx.observers.DefaultObserver
import com.soundcloud.android.stream.StreamItem
import com.soundcloud.java.optional.Optional
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.facebook_invites_notification.view.*
import kotlinx.android.synthetic.main.facebook_invites_notification_card.view.*
import java.lang.ref.WeakReference
import javax.inject.Inject

class FacebookListenerInvitesItemRenderer
@Inject
constructor(private val imageOperations: ImageOperations,
            private val facebookInvitesStorage: FacebookInvitesStorage,
            private val facebookApi: FacebookApi) : CellRenderer<StreamItem> {

    val notificationCallback: PublishSubject<FacebookNotificationCallback<StreamItem.FacebookListenerInvites>> = PublishSubject.create()

    override fun createItemView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context)
                .inflate(R.layout.facebook_invites_notification_card, parent, false)
    }

    override fun bindItemView(position: Int, itemView: View, items: MutableList<StreamItem>) {
        val item = items[position] as? StreamItem.FacebookListenerInvites
        itemView.isEnabled = false
        item?.let {
            setClickListeners(itemView, position, it)
        }

        if (item != null && item.friendPictureUrls.isPresent) {
            setContent(itemView, item)
        } else {
            setLoading(itemView)
            facebookApi.friendPictureUrls()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(PictureLoadedSubscriber(itemView, position, items))
        }
    }

    private fun setLoading(itemView: View) {
        itemView.loading.visibility = View.VISIBLE
        itemView.content.visibility = View.INVISIBLE
    }

    private fun setContent(itemView: View, item: StreamItem.FacebookListenerInvites) {
        itemView.loading.visibility = View.GONE
        itemView.content.visibility = View.VISIBLE

        if (item.hasPictures()) {
            val friendImageUrls = item.friendPictureUrls.get()
            itemView.friends.visibility = View.VISIBLE
            itemView.facebook_invite_introduction_text.visibility = View.GONE
            setFriendImage(itemView.friend_1, friendImageUrls, 0)
            setFriendImage(itemView.friend_2, friendImageUrls, 1)
            setFriendImage(itemView.friend_3, friendImageUrls, 2)
        } else {
            itemView.friends.visibility = View.GONE
            itemView.facebook_invite_introduction_text.visibility = View.VISIBLE
        }
    }

    private fun setFriendImage(imageView: ImageView, friendImageUrls: List<String>, position: Int) {
        if (friendImageUrls.size > position) {
            imageView.visibility = View.VISIBLE
            imageOperations.displayCircular(friendImageUrls[position], imageView)
        } else {
            imageView.visibility = View.GONE
        }
    }

    private fun setClickListeners(itemView: View, position: Int, facebookListenerInvites: StreamItem.FacebookListenerInvites) {
        itemView.close_button.setOnClickListener {
            facebookInvitesStorage.setDismissed()
            notificationCallback.onNext(FacebookNotificationCallback.Dismiss(position, facebookListenerInvites))
        }

        itemView.action_button.setOnClickListener {
            facebookInvitesStorage.setClicked()
            notificationCallback.onNext(FacebookNotificationCallback.Click(position, facebookListenerInvites))
        }
    }

    internal inner class PictureLoadedSubscriber(itemView: View,
                                                 private val position: Int,
                                                 private val items: MutableList<StreamItem>) : DefaultObserver<List<String>>() {

        private val itemView: WeakReference<View> = WeakReference(itemView)

        override fun onNext(friendPictureUrls: List<String>) {
            if (listContainsInvitesItem()) {
                itemView.get()?.let {
                    val item = StreamItem.FacebookListenerInvites(Optional.of(friendPictureUrls))
                    items[position] = item
                    notificationCallback.onNext(FacebookNotificationCallback.Load(item.hasPictures()))
                    setContent(it, item)
                }
            }
        }

        override fun onError(e: Throwable) {
            super.onError(e)
            if (listContainsInvitesItem()) {
                val view = itemView.get()
                val streamItem = items[position] as? StreamItem.FacebookListenerInvites
                if (view != null && streamItem != null) {
                    setContent(view, streamItem)
                }
            }
        }

        private fun listContainsInvitesItem(): Boolean {
            return items.size > position && items[position].kind === StreamItem.Kind.FACEBOOK_LISTENER_INVITES
        }
    }
}
