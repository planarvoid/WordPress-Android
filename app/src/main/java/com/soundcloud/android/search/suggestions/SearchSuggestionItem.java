package com.soundcloud.android.search.suggestions;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class SearchSuggestionItem extends SuggestionItem implements ImageResource, ListItem {

    abstract Optional<SuggestionHighlight> suggestionHighlight();
    abstract String displayedText();

    public static SearchSuggestionItem forUser(Urn urn, Optional<String> imageUrlTemplate, String query, Optional<SuggestionHighlight> suggestionHighlight, String displayedText) {
        return new AutoValue_SearchSuggestionItem(urn, imageUrlTemplate, Kind.UserItem, query, suggestionHighlight, displayedText);
    }

    public static SearchSuggestionItem forTrack(Urn urn, Optional<String> imageUrlTemplate, String query, Optional<SuggestionHighlight> suggestionHighlight, String displayedText) {
        return new AutoValue_SearchSuggestionItem(urn, imageUrlTemplate, Kind.TrackItem, query, suggestionHighlight, displayedText);
    }

    public static SearchSuggestionItem forPlaylist(Urn urn, Optional<String> imageUrlTemplate, String query, Optional<SuggestionHighlight> suggestionHighlight, String displayedText) {
        return new AutoValue_SearchSuggestionItem(urn, imageUrlTemplate, Kind.PlaylistItem, query, suggestionHighlight, displayedText);
    }
}
