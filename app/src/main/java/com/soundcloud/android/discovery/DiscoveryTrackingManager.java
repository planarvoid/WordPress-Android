package com.soundcloud.android.discovery;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import java.util.List;

class DiscoveryTrackingManager {
    static final Screen SCREEN = Screen.DISCOVER;

    private final EventTracker eventTracker;

    @Inject
    public DiscoveryTrackingManager(EventTracker eventTracker) {
        this.eventTracker = eventTracker;
    }

    void trackSelectionItemClick(SelectionItem selectionItem, List<DiscoveryCard> discoveryCards) {
        final Optional<DiscoveryCard> discoveryCardOptional = Iterables.tryFind(discoveryCards, card -> card != null && card.hasSelectionUrn(selectionItem.selectionUrn()));
        discoveryCardOptional.ifPresent(discoveryCard -> {
            final int selectionPosition = discoveryCards.indexOf(discoveryCard);
            if (discoveryCard.kind() == DiscoveryCard.Kind.SINGLE_CONTENT_SELECTION_CARD) {
                trackSelectionItemInSingleContentSelectionCard(selectionPosition, (DiscoveryCard.SingleContentSelectionCard) discoveryCard);
            } else if (discoveryCard.kind() == DiscoveryCard.Kind.MULTIPLE_CONTENT_SELECTION_CARD) {
                trackSelectionItemInMultipleContentSelectionCard(selectionItem, selectionPosition, (DiscoveryCard.MultipleContentSelectionCard) discoveryCard);
            }
        });
    }

    private void trackSelectionItemInSingleContentSelectionCard(int selectionPosition, DiscoveryCard.SingleContentSelectionCard singleContentSelectionCard) {
        final EventContextMetadata.Builder builder = EventContextMetadata.builder();
        final Urn selectionUrn = singleContentSelectionCard.selectionUrn();
        builder.pageName(SCREEN.name());
        builder.clickSource(Optional.of(selectionUrn.toString()));
        builder.queryPosition(Optional.of(selectionPosition));
        builder.queryUrn(singleContentSelectionCard.queryUrn());
        eventTracker.trackNavigation(UIEvent.fromNavigation(selectionUrn, builder.build()));
    }

    private void trackSelectionItemInMultipleContentSelectionCard(SelectionItem selectionItem, int selectionPosition, DiscoveryCard.MultipleContentSelectionCard multipleContentSelectionCard) {
        final Optional<Urn> selectionItemUrn = selectionItem.urn();
        selectionItemUrn.ifPresent(itemUrn -> {
            final EventContextMetadata.Builder builder = EventContextMetadata.builder();
            builder.pageName(SCREEN.name());
            builder.clickSource(Optional.of(itemUrn.toString()));
            builder.queryPosition(Optional.of(selectionPosition));
            builder.queryUrn(multipleContentSelectionCard.queryUrn());

            final List<SelectionItem> selectionItems = multipleContentSelectionCard.selectionItems();
            final Optional<SelectionItem> selectionItemOptional = Iterables.tryFind(selectionItems, item -> item != null && item.urn().isPresent() && itemUrn.equals(item.urn().get()));
            selectionItemOptional.ifPresent(item -> {
                final int itemPosition = selectionItems.indexOf(item);
                builder.module(Module.create(multipleContentSelectionCard.selectionUrn().toString(), itemPosition));
            });
            eventTracker.trackNavigation(UIEvent.fromNavigation(itemUrn, builder.build()));
        });
    }
}
