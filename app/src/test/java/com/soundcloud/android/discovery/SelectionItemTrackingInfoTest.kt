package com.soundcloud.android.discovery

import com.soundcloud.android.discovery.DiscoveryFixtures.multiSelectionItem
import com.soundcloud.android.discovery.DiscoveryFixtures.multipleContentSelectionCard
import com.soundcloud.android.discovery.DiscoveryFixtures.queryUrn
import com.soundcloud.android.discovery.DiscoveryFixtures.singleContentSelectionCard
import com.soundcloud.android.discovery.DiscoveryFixtures.singleSelectionItem
import com.soundcloud.android.events.EventContextMetadata
import com.soundcloud.android.events.Module
import com.soundcloud.android.events.UIEvent
import com.soundcloud.android.main.Screen
import com.soundcloud.android.model.Urn
import com.soundcloud.java.optional.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SelectionItemTrackingInfoTest {

    @Test
    fun navigatesAndTracksSingleSelectionItemClick() {
        val trackingInfo = SelectionItemTrackingInfo.create(singleSelectionItem, singleContentSelectionCard, 1)

        val builder = EventContextMetadata.builder()
        builder.source(Optional.fromNullable(singleContentSelectionCard.trackingFeatureName))
        builder.sourceUrn(singleContentSelectionCard.selectionUrn)
        builder.sourceQueryUrn(Optional.fromNullable(singleContentSelectionCard.queryUrn))
        builder.sourceQueryPosition(0)
        builder.pageName(Screen.DISCOVER.get())
        builder.queryPosition(1)
        builder.queryUrn(Optional.fromNullable(singleContentSelectionCard.parentQueryUrn))

        assertThat(trackingInfo?.toUIEvent()).isEqualTo(UIEvent.fromDiscoveryCard(Optional.fromNullable<Urn>(singleContentSelectionCard.selectionItem.urn), builder.build()))
    }

    @Test
    fun navigatesAndTracksMultiSelectionItemClick() {
        val trackingInfo = SelectionItemTrackingInfo.create(multiSelectionItem, multipleContentSelectionCard, 2)

        val builder = EventContextMetadata.builder()
        builder.source(Optional.fromNullable(multipleContentSelectionCard.trackingFeatureName))
        builder.sourceUrn(multipleContentSelectionCard.selectionUrn)
        builder.sourceQueryUrn(Optional.fromNullable(multipleContentSelectionCard.parentQueryUrn))
        builder.sourceQueryPosition(0)
        builder.pageName(Screen.DISCOVER.get())
        builder.queryPosition(2)
        builder.queryUrn(Optional.fromNullable(queryUrn))
        builder.module(Module.create(multipleContentSelectionCard.selectionUrn.toString(), 0))

        assertThat(trackingInfo?.toUIEvent()).isEqualTo(UIEvent.fromDiscoveryCard(Optional.fromNullable<Urn>(multiSelectionItem.urn), builder.build()))
    }
}
