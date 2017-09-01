package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryFixtures.MULTI_CONTENT_SELECTION_CARD;
import static com.soundcloud.android.discovery.DiscoveryFixtures.MULTI_SELECTION_ITEM;
import static com.soundcloud.android.discovery.DiscoveryFixtures.QUERY_URN;
import static com.soundcloud.android.discovery.DiscoveryFixtures.SINGLE_CONTENT_SELECTION_CARD;
import static com.soundcloud.android.discovery.DiscoveryFixtures.SINGLE_SELECTION_ITEM;
import static com.soundcloud.android.discovery.DiscoveryTrackingManager.SCREEN;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DiscoveryTrackingManagerTest {

    @Test
    public void navigatesAndTracksSingleSelectionItemClick() {
        final Optional<SelectionItemViewModel.TrackingInfo> trackingInfo = DiscoveryTrackingManager.calculateUIEvent(SINGLE_SELECTION_ITEM, SINGLE_CONTENT_SELECTION_CARD, 1);

        final EventContextMetadata.Builder builder = EventContextMetadata.builder();
        builder.source(SINGLE_CONTENT_SELECTION_CARD.trackingFeatureName());
        builder.sourceUrn(SINGLE_CONTENT_SELECTION_CARD.selectionUrn());
        builder.sourceQueryUrn(SINGLE_CONTENT_SELECTION_CARD.queryUrn());
        builder.sourceQueryPosition(0);
        builder.pageName(SCREEN.get());
        builder.queryPosition(1);
        builder.queryUrn(SINGLE_CONTENT_SELECTION_CARD.parentQueryUrn());

        assertThat(trackingInfo.get().toUIEvent()).isEqualTo(UIEvent.fromDiscoveryCard(SINGLE_CONTENT_SELECTION_CARD.selectionItem().getUrn(), builder.build()));
    }

    @Test
    public void navigatesAndTracksMultiSelectionItemClick() {
        final Optional<SelectionItemViewModel.TrackingInfo> trackingInfo = DiscoveryTrackingManager.calculateUIEvent(MULTI_SELECTION_ITEM, MULTI_CONTENT_SELECTION_CARD, 2);

        final EventContextMetadata.Builder builder = EventContextMetadata.builder();
        builder.source(MULTI_CONTENT_SELECTION_CARD.trackingFeatureName());
        builder.sourceUrn(MULTI_CONTENT_SELECTION_CARD.selectionUrn());
        builder.sourceQueryUrn(MULTI_CONTENT_SELECTION_CARD.parentQueryUrn());
        builder.sourceQueryPosition(0);
        builder.pageName(SCREEN.get());
        builder.queryPosition(2);
        builder.queryUrn(QUERY_URN);
        builder.module(Module.create(MULTI_CONTENT_SELECTION_CARD.selectionUrn().toString(), 0));

        assertThat(trackingInfo.get().toUIEvent()).isEqualTo(UIEvent.fromDiscoveryCard(MULTI_SELECTION_ITEM.getUrn(), builder.build()));
    }
}
