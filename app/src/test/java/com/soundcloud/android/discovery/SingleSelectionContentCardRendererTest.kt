package com.soundcloud.android.discovery

import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.R
import com.soundcloud.android.image.ImageOperations
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.utils.DisplayMetricsStub
import org.assertj.android.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

class SingleSelectionContentCardRendererTest : AndroidUnitTest() {

    @Mock private lateinit var resources: Resources
    @Mock private lateinit var imageOperations: ImageOperations
    private val card = DiscoveryFixtures.singleContentSelectionCardViewModel()

    private lateinit var renderer: SingleSelectionContentCardRenderer
    private lateinit var itemView: View

    @Before
    @Throws(Exception::class)
    fun setUp() {
        renderer = SingleSelectionContentCardRenderer(imageOperations, resources)
        itemView = renderer.createItemView(LinearLayout(AndroidUnitTest.context()))!!

        whenever(resources.displayMetrics).thenReturn(DisplayMetricsStub(50, 50))
    }

    @Test
    fun bindsTitleWhenPresent() {
        val cardWithTitle = card.copy(title = "title")
        val title = itemView.findViewById<TextView>(R.id.single_card_title)

        renderer.bindItemView(0, itemView, listOf(cardWithTitle))

        assertThat(title).isVisible
    }

    @Test
    fun doesNotBindTitleWhenNotPresent() {
        val cardWithoutTitle = card.copy(title = null)
        val title = itemView.findViewById<TextView>(R.id.single_card_title)

        renderer.bindItemView(0, itemView, listOf(cardWithoutTitle))

        assertThat(title).isNotVisible
    }

    @Test
    fun bindsDescriptionWhenPresent() {
        val cardWithDescription = card.copy(description = "description")
        val description = itemView.findViewById<TextView>(R.id.single_card_description)

        renderer.bindItemView(0, itemView, listOf(cardWithDescription))

        assertThat(description).isVisible
    }

    @Test
    fun doesNotBindDescriptionWhenNotPresent() {
        val description = itemView.findViewById<TextView>(R.id.single_card_description)
        val cardWithoutDescription = card.copy(description = null)

        renderer.bindItemView(0, itemView, listOf(cardWithoutDescription))

        assertThat(description).isNotVisible
    }

    @Test
    fun bindsSocialProofWhenPresent() {
        val cardWithSocialProofAvatars = card.copy(socialProof = "social_proof", socialProofAvatarUrlTemplates = listOf("link1", "link2"))
        `when`(resources.configuration).thenReturn(Configuration())
        val socialProofText = itemView.findViewById<TextView>(R.id.single_card_social_proof)

        renderer.bindItemView(0, itemView, listOf(cardWithSocialProofAvatars))

        assertThat(socialProofText).isVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_1) as View).isVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_2) as View).isVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_3) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_4) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_5) as View).isNotVisible
    }

    @Test
    fun doesNotBindSocialProofAvatarsWhenListIsEmpty() {
        val cardWithoutSocialProofAvatars = card.copy(socialProofAvatarUrlTemplates = emptyList())

        renderer.bindItemView(0, itemView, listOf(cardWithoutSocialProofAvatars))

        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_1) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_2) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_3) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_4) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_5) as View).isNotVisible
    }

    @Test
    fun doesNotBindSocialProofWhenNotPresent() {
        val socialProofText = itemView.findViewById<TextView>(R.id.single_card_social_proof)
        val cardWithoutSocialProof = card.copy(socialProof = null)

        renderer.bindItemView(0, itemView, listOf(cardWithoutSocialProof))

        assertThat(socialProofText).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_1) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_2) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_3) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_4) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_5) as View).isNotVisible
    }

    @Test
    fun doesNotBindSocialProofAvatarsWhenNotPresent() {
        val cardWithSocialProofWithoutAvatars = card.copy(socialProof = "social_proof", socialProofAvatarUrlTemplates = emptyList())
        val socialProofText = itemView.findViewById<TextView>(R.id.single_card_social_proof)

        renderer.bindItemView(0, itemView, listOf(cardWithSocialProofWithoutAvatars))

        assertThat(socialProofText).isVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_1) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_2) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_3) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_4) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_5) as View).isNotVisible
    }

    @Test
    fun doesNotBindSocialProofTextWhenNotPresent() {
        val cardWithSocialProofAvatarsWithoutSocialProof = card.copy(socialProofAvatarUrlTemplates = listOf("link1", "link2"), socialProof = null)
        `when`(resources.configuration).thenReturn(Configuration())
        val socialProofText = itemView.findViewById<TextView>(R.id.single_card_social_proof)

        renderer.bindItemView(0, itemView, listOf(cardWithSocialProofAvatarsWithoutSocialProof))

        assertThat(socialProofText).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_1) as View).isVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_2) as View).isVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_3) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_4) as View).isNotVisible
        assertThat(itemView.findViewById<View>(R.id.single_card_user_artwork_5) as View).isNotVisible
    }

    @Test
    fun bindsClickHandlingFromSelectionItem() {
        renderer.bindItemView(0, itemView, listOf(card))
        val testObserver = renderer.selectionItemClick().test()

        itemView.performClick()

        testObserver.assertValue(card.selectionItem)
    }
}
