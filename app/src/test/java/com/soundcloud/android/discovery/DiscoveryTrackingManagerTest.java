package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryFixtures.MULTI_CONTENT_SELECTION_CARD;
import static com.soundcloud.android.discovery.DiscoveryFixtures.MULTI_SELECTION_ITEM;
import static com.soundcloud.android.discovery.DiscoveryFixtures.QUERY_URN;
import static com.soundcloud.android.discovery.DiscoveryFixtures.SEARCH_ITEM;
import static com.soundcloud.android.discovery.DiscoveryFixtures.SINGLE_CONTENT_SELECTION_CARD;
import static com.soundcloud.android.discovery.DiscoveryFixtures.SINGLE_SELECTION_ITEM;
import static com.soundcloud.android.discovery.DiscoveryTrackingManager.SCREEN;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.java.collections.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class DiscoveryTrackingManagerTest {
    @Mock private EventTracker eventTracker;

    private DiscoveryTrackingManager discoveryTrackingManager;

    @Before
    public void setUp() throws Exception {
        discoveryTrackingManager = new DiscoveryTrackingManager(eventTracker);
    }

    @Test
    public void navigatesAndTracksSingleSelectionItemClick() {
        final ArrayList<DiscoveryCard> cards = Lists.newArrayList(SEARCH_ITEM, SINGLE_CONTENT_SELECTION_CARD, MULTI_CONTENT_SELECTION_CARD);

        discoveryTrackingManager.trackSelectionItemClick(SINGLE_SELECTION_ITEM, cards);

        final EventContextMetadata.Builder builder = EventContextMetadata.builder();
        builder.source(SINGLE_CONTENT_SELECTION_CARD.trackingFeatureName());
        builder.sourceUrn(SINGLE_CONTENT_SELECTION_CARD.selectionUrn());
        builder.sourceQueryUrn(SINGLE_CONTENT_SELECTION_CARD.queryUrn());
        builder.sourceQueryPosition(0);
        builder.pageName(SCREEN.get());
        builder.queryPosition(1);
        builder.queryUrn(SINGLE_CONTENT_SELECTION_CARD.parentQueryUrn());

        verify(eventTracker).trackClick(eq(UIEvent.fromDiscoveryCard(SINGLE_CONTENT_SELECTION_CARD.selectionUrn(), builder.build())));
    }

    @Test
    public void navigatesAndTracksMultiSelectionItemClick() {
        final ArrayList<DiscoveryCard> cards = Lists.newArrayList(SEARCH_ITEM, SINGLE_CONTENT_SELECTION_CARD, MULTI_CONTENT_SELECTION_CARD);

        discoveryTrackingManager.trackSelectionItemClick(MULTI_SELECTION_ITEM, cards);

        final EventContextMetadata.Builder builder = EventContextMetadata.builder();
        builder.source(MULTI_CONTENT_SELECTION_CARD.trackingFeatureName());
        builder.sourceUrn(MULTI_SELECTION_ITEM.urn());
        builder.sourceQueryUrn(MULTI_CONTENT_SELECTION_CARD.parentQueryUrn());
        builder.sourceQueryPosition(0);
        builder.pageName(SCREEN.get());
        builder.queryPosition(2);
        builder.queryUrn(QUERY_URN);
        builder.module(Module.create(MULTI_CONTENT_SELECTION_CARD.selectionUrn().toString(), 0));

        verify(eventTracker).trackClick(eq(UIEvent.fromDiscoveryCard(MULTI_SELECTION_ITEM.urn().get(), builder.build())));
    }
}
