package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;

abstract class DiscoveryCard {
    enum Kind {
        SEARCH_ITEM,
        MULTIPLE_CONTENT_SELECTION_CARD,
        SINGLE_CONTENT_SELECTION_CARD,
        EMPTY_CARD
    }

    abstract Kind kind();

    static DiscoveryCard forSearchItem() {
        return DiscoveryCard.Default.create(Kind.SEARCH_ITEM);
    }

    boolean hasSelectionUrn(Urn selectionUrn) {
        return false;
    }

    @AutoValue
    abstract static class Default extends DiscoveryCard {
        public static DiscoveryCard create(Kind kind) {
            return new AutoValue_DiscoveryCard_Default(kind);
        }
    }

    @AutoValue
    abstract static class MultipleContentSelectionCard extends DiscoveryCard {

        abstract Urn selectionUrn();

        abstract Optional<Urn> queryUrn();

        abstract Optional<String> style();

        abstract Optional<String> title();

        abstract Optional<String> description();

        abstract List<SelectionItem> selectionItems();

        static MultipleContentSelectionCard create(Urn selectionUrn,
                                                   Optional<Urn> queryUrn,
                                                   Optional<String> style,
                                                   Optional<String> title,
                                                   Optional<String> description,
                                                   List<SelectionItem> selectionItems) {
            return new AutoValue_DiscoveryCard_MultipleContentSelectionCard(Kind.MULTIPLE_CONTENT_SELECTION_CARD, selectionUrn, queryUrn, style, title, description, selectionItems);
        }

        @Override
        boolean hasSelectionUrn(Urn selectionUrn) {
            return selectionUrn().equals(selectionUrn);
        }
    }

    @AutoValue
    abstract static class SingleContentSelectionCard extends DiscoveryCard {

        abstract Urn selectionUrn();

        abstract Optional<String> style();

        abstract Optional<String> title();

        abstract Optional<String> description();

        abstract Optional<Urn> queryUrn();

        abstract SelectionItem selectionItem();

        abstract Optional<String> socialProof();

        abstract List<String> socialProofAvatarUrlTemplates();

        static SingleContentSelectionCard create(Urn selectionUrn,
                                                 Optional<Urn> queryUrn,
                                                 Optional<String> style,
                                                 Optional<String> title,
                                                 Optional<String> description,
                                                 SelectionItem selectionItem,
                                                 Optional<String> socialProof,
                                                 List<String> socialProofAvatarUrlTemplates) {
            return new AutoValue_DiscoveryCard_SingleContentSelectionCard(Kind.SINGLE_CONTENT_SELECTION_CARD,
                    selectionUrn,
                    style,
                    title,
                    description,
                    queryUrn,
                    selectionItem,
                    socialProof,
                    socialProofAvatarUrlTemplates);
        }

        @Override
        boolean hasSelectionUrn(Urn selectionUrn) {
            return selectionUrn().equals(selectionUrn);
        }
    }

    @AutoValue
    abstract static class EmptyCard extends DiscoveryCard {

        abstract Optional<Throwable> throwable();

        static EmptyCard create(Optional<Throwable> throwable) {
            return new AutoValue_DiscoveryCard_EmptyCard(Kind.EMPTY_CARD, throwable);
        }
    }
}
