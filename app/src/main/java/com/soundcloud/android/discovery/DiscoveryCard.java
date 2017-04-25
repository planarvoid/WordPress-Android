package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;

abstract class DiscoveryCard {
    enum Kind {
        SEARCH_ITEM,
        SELECTION_CARD,
        SINGLETON_SELECTION_CARD
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
    static abstract class SelectionCard extends DiscoveryCard {

        abstract Urn selectionUrn();

        abstract Optional<String> style();

        abstract Optional<String> title();

        abstract Optional<String> description();

        abstract Optional<List<SelectionPlaylist>> selectionPlaylists();

        static SelectionCard create(Urn selectionUrn,
                                    Optional<String> style,
                                    Optional<String> title,
                                    Optional<String> description,
                                    Optional<List<SelectionPlaylist>> selectionPlaylists) {
            return new AutoValue_DiscoveryCard_SelectionCard(Kind.SELECTION_CARD, selectionUrn, style, title, description, selectionPlaylists);
        }
    }

    @AutoValue
    static abstract class SingletonSelectionCard extends DiscoveryCard {

        abstract Urn selectionUrn();

        abstract Optional<String> style();

        abstract Optional<String> title();

        abstract Optional<String> description();

        abstract Optional<Urn> queryUrn();

        abstract SelectionPlaylist selectionPlaylist();

        abstract Optional<String> socialProof();

        abstract Optional<List<String>> socialProofAvatarUrlTemplates();

        static SingletonSelectionCard create(Urn selectionUrn,
                                             Optional<Urn> queryUrn,
                                             Optional<String> style,
                                             Optional<String> title,
                                             Optional<String> description,
                                             SelectionPlaylist selectionPlaylist,
                                             Optional<String> socialProof,
                                             Optional<List<String>> socialProofAvatarUrlTemplates) {
            return new AutoValue_DiscoveryCard_SingletonSelectionCard(Kind.SINGLETON_SELECTION_CARD,
                    selectionUrn,
                    style,
                    title,
                    description,
                    queryUrn,
                    selectionPlaylist,
                    socialProof,
                    socialProofAvatarUrlTemplates);
        }
    }
}
