package com.soundcloud.android.home;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;

abstract class DiscoveryCard {
    enum Kind {
        SELECTION_CARD,
        SINGLETON_SELECTION_CARD
    }

    abstract Kind kind();

    abstract Urn selectionUrn();

    abstract Optional<Urn> queryUrn();

    abstract Optional<String> style();

    abstract Optional<String> title();

    abstract Optional<String> description();

    @AutoValue
    static abstract class SelectionCard extends DiscoveryCard {
        abstract Optional<List<SelectionPlaylist>> selectionPlaylists();

        static SelectionCard create(Urn selectionUrn,
                                    Optional<Urn> queryUrn,
                                    Optional<String> style,
                                    Optional<String> title,
                                    Optional<String> description,
                                    Optional<List<SelectionPlaylist>> selectionPlaylists) {
            return new AutoValue_DiscoveryCard_SelectionCard(Kind.SELECTION_CARD, selectionUrn, queryUrn, style, title, description, selectionPlaylists);
        }
    }

    @AutoValue
    static abstract class SingletonSelectionCard extends DiscoveryCard {

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
                                                                 queryUrn,
                                                                 style,
                                                                 title,
                                                                 description,
                                                                 selectionPlaylist,
                                                                 socialProof,
                                                                 socialProofAvatarUrlTemplates);
        }
    }
}
