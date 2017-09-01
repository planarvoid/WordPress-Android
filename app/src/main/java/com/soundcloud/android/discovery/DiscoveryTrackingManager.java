package com.soundcloud.android.discovery;

import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;

import java.util.List;

final class DiscoveryTrackingManager {
    static final Screen SCREEN = Screen.DISCOVER;

    private DiscoveryTrackingManager() {
    }

    static Optional<SelectionItemViewModel.TrackingInfo> calculateUIEvent(SelectionItem selectionItem, DiscoveryCard parentCard, int cardPosition) {
        Optional<SelectionItemViewModel.TrackingInfo> eventOptional = Optional.absent();
        if (parentCard.kind() == DiscoveryCard.Kind.SINGLE_CONTENT_SELECTION_CARD) {
            eventOptional = trackSelectionItemInSingleContentSelectionCard(cardPosition, (DiscoveryCard.SingleContentSelectionCard) parentCard);
        } else if (parentCard.kind() == DiscoveryCard.Kind.MULTIPLE_CONTENT_SELECTION_CARD) {
            eventOptional = trackSelectionItemInMultipleContentSelectionCard(selectionItem, cardPosition, (DiscoveryCard.MultipleContentSelectionCard) parentCard);
        }
        return eventOptional;
    }

    private static Optional<SelectionItemViewModel.TrackingInfo> trackSelectionItemInSingleContentSelectionCard(int selectionPosition, DiscoveryCard.SingleContentSelectionCard singleContentSelectionCard) {
        final EventContextMetadata.Builder builder = EventContextMetadata.builder();
        final Urn selectionUrn = singleContentSelectionCard.selectionUrn();
        builder.pageName(SCREEN.get());
        builder.source(singleContentSelectionCard.trackingFeatureName());
        builder.sourceUrn(selectionUrn);
        builder.sourceQueryUrn(singleContentSelectionCard.queryUrn());
        builder.sourceQueryPosition(0);
        builder.queryPosition(selectionPosition);
        builder.queryUrn(singleContentSelectionCard.parentQueryUrn());
        return Optional.of(new SelectionItemViewModel.TrackingInfo(singleContentSelectionCard.selectionItem().getUrn(), builder.build()));
    }

    private static Optional<SelectionItemViewModel.TrackingInfo> trackSelectionItemInMultipleContentSelectionCard(SelectionItem selectionItem,
                                                                               int selectionPosition,
                                                                               DiscoveryCard.MultipleContentSelectionCard multipleContentSelectionCard) {
        final Optional<Urn> selectionItemUrn = selectionItem.getUrn();
        if (selectionItemUrn.isPresent()) {
            final Urn itemUrn = selectionItemUrn.get();
            final EventContextMetadata.Builder builder = EventContextMetadata.builder();
            builder.pageName(SCREEN.get());
            builder.source(multipleContentSelectionCard.trackingFeatureName());
            builder.sourceUrn(multipleContentSelectionCard.selectionUrn());
            builder.sourceQueryUrn(multipleContentSelectionCard.queryUrn());
            builder.queryPosition(selectionPosition);

            builder.queryUrn(multipleContentSelectionCard.parentQueryUrn());

            final List<SelectionItem> selectionItems = multipleContentSelectionCard.selectionItems();
            final Optional<SelectionItem> selectionItemOptional = Iterables.tryFind(selectionItems, item -> item != null && item.getUrn().isPresent() && itemUrn.equals(item.getUrn().get()));
            selectionItemOptional.ifPresent(item -> {
                final int itemPosition = selectionItems.indexOf(item);
                builder.sourceQueryPosition(itemPosition);
                builder.module(Module.create(multipleContentSelectionCard.selectionUrn().toString(), itemPosition));
            });
            return Optional.of(new SelectionItemViewModel.TrackingInfo(selectionItemUrn, builder.build()));
        }
        return Optional.absent();
    }
}
