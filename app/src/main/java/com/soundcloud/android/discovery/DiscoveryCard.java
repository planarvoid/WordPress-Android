package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;

abstract class DiscoveryCard {
    enum Kind {
        SEARCH_ITEM,
        MULTIPLE_CONTENT_SELECTION_CARD,
        SINGLE_CONTENT_SELECTION_CARD
    }

    abstract Kind kind();

    static DiscoveryCard forSearchItem() {
        return DiscoveryCard.Default.create(Kind.SEARCH_ITEM);
    }

    @AutoValue
    static abstract class Default extends DiscoveryCard {
        public static DiscoveryCard create(Kind kind) {
            return new AutoValue_DiscoveryCard_Default(kind);
        }
    }

    @AutoValue
    static abstract class MultipleContentSelectionCard extends DiscoveryCard {

        abstract Urn selectionUrn();

        abstract Optional<String> style();

        abstract Optional<String> title();

        abstract Optional<String> description();

        abstract Optional<List<SelectionItem>> selectionItems();

        static MultipleContentSelectionCard create(Urn selectionUrn,
                                                   Optional<String> style,
                                                   Optional<String> title,
                                                   Optional<String> description,
                                                   Optional<List<SelectionItem>> selectionItems) {
            return new AutoValue_DiscoveryCard_MultipleContentSelectionCard(Kind.MULTIPLE_CONTENT_SELECTION_CARD, selectionUrn, style, title, description, selectionItems);
        }
    }

    @AutoValue
    static abstract class SingleContentSelectionCard extends DiscoveryCard {

        abstract Urn selectionUrn();

        abstract Optional<String> style();

        abstract Optional<String> title();

        abstract Optional<String> description();

        abstract Optional<Urn> queryUrn();

        abstract SelectionItem selectionItem();

        abstract Optional<String> socialProof();

        abstract Optional<List<String>> socialProofAvatarUrlTemplates();

        static SingleContentSelectionCard create(Urn selectionUrn,
                                                 Optional<Urn> queryUrn,
                                                 Optional<String> style,
                                                 Optional<String> title,
                                                 Optional<String> description,
                                                 SelectionItem selectionItem,
                                                 Optional<String> socialProof,
                                                 Optional<List<String>> socialProofAvatarUrlTemplates) {
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
    }
}
