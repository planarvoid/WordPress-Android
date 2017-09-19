package com.soundcloud.android.discovery

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.soundcloud.android.R
import com.soundcloud.android.image.ApiImageSize
import com.soundcloud.android.image.ImageOperations
import com.soundcloud.android.model.Urn
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.optional.Optional
import com.soundcloud.java.optional.Optional.absent
import com.soundcloud.java.optional.Optional.fromNullable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.discovery_single_selection_card.view.*
import javax.inject.Inject

@OpenForTesting
class SingleSelectionContentCardRenderer
@Inject
constructor(private val imageOperations: ImageOperations, private val resources: Resources) : CellRenderer<DiscoveryCardViewModel.SingleContentSelectionCard> {
    private val selectionItemPublishSubject: PublishSubject<SelectionItemViewModel> = PublishSubject.create()

    override fun createItemView(viewGroup: ViewGroup) = LayoutInflater.from(viewGroup.context)?.inflate(R.layout.discovery_single_selection_card, viewGroup, false)

    override fun bindItemView(position: Int, view: View, list: List<DiscoveryCardViewModel.SingleContentSelectionCard>) {
        val singleContentSelectionCard = list[position]
        bindText(view.single_card_title, singleContentSelectionCard.title)
        bindText(view.single_card_description, singleContentSelectionCard.description)
        bindSelectionItem(view.single_card_artwork, view.single_card_track_count, singleContentSelectionCard.selectionItem)
        bindSocialProof(view, singleContentSelectionCard)
        bindClickHandling(view, singleContentSelectionCard)
    }

    internal fun selectionItemClick() = selectionItemPublishSubject

    private fun bindText(view: TextView, value: String?) {
        if (value != null) {
            view.visibility = View.VISIBLE
            view.text = value
        } else {
            view.visibility = View.GONE
        }
    }

    private fun bindSelectionItem(view: ImageView, countTextView: TextView, selectionItem: SelectionItemViewModel) {
        imageOperations.displayInAdapterView(
                selectionItem.urn,
                fromNullable(selectionItem.artworkUrlTemplate),
                ApiImageSize.getFullImageSize(resources),
                view,
                ImageOperations.DisplayType.DEFAULT)

        bindText(countTextView, selectionItem.count?.toString())
    }

    private fun bindSocialProof(socialProofView: View, singleContentSelectionCard: DiscoveryCardViewModel.SingleContentSelectionCard) {
        bindSocialProofContainer(socialProofView.single_card_social_proof_container, singleContentSelectionCard.socialProof, singleContentSelectionCard.socialProofAvatarUrlTemplates)
        bindText(socialProofView.single_card_social_proof, singleContentSelectionCard.socialProof)
        bindSocialProofAvatars(socialProofView, singleContentSelectionCard.socialProofAvatarUrlTemplates)
    }

    private fun bindSocialProofContainer(container: LinearLayout, socialProofText: String?, socialProofUrls: List<String>) {
        if (socialProofText != null || socialProofUrls.isNotEmpty()) {
            container.visibility = View.VISIBLE
        } else {
            container.visibility = View.GONE
        }
    }

    private fun bindSocialProofAvatars(container: View, imageUrls: List<String>) {
        val artworkViews = listOf<ImageView>(container.single_card_user_artwork_1,
                                             container.single_card_user_artwork_2,
                                             container.single_card_user_artwork_3,
                                             container.single_card_user_artwork_4,
                                             container.single_card_user_artwork_5)

        if (imageUrls.size == 1) {
            artworkViews.forEach { it.visibility = View.GONE }
            bindSocialProofUserArtwork(container.single_card_user_artwork_single, imageUrls.getOptional(0))
        } else {
            container.single_card_user_artwork_single.visibility = View.GONE
            artworkViews.forEachIndexed { index, view -> bindSocialProofUserArtwork(view, imageUrls.getOptional(index)) }
        }
    }

    private fun List<String>.getOptional(position: Int): Optional<String> =
            if (this.size > position) Optional.of(get(position)) else absent()

    private fun bindSocialProofUserArtwork(imageView: ImageView, userArtwork: Optional<String>) {
        if (userArtwork.isPresent) {
            imageView.visibility = View.VISIBLE
            imageOperations.displayCircularWithPlaceholder(Urn.NOT_SET,
                                                           userArtwork,
                                                           ApiImageSize.getListItemImageSize(resources),
                                                           imageView)
        } else {
            imageView.visibility = View.GONE
        }
    }

    private fun bindClickHandling(view: View, selectionCard: DiscoveryCardViewModel.SingleContentSelectionCard) {
        view.setOnClickListener { selectionItemPublishSubject.onNext(selectionCard.selectionItem) }
    }
}
