package com.soundcloud.android.search.suggestions;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SearchSuggestionTest {

    @Test
    public void shortcutsAndApiSuggestionsShouldAlwaysBeEqualWithSameUrns() {
        final ApiTrack track = ModelFixtures.create(ApiTrack.class);

        SearchSuggestion searchSuggestion1 = new AutoValue_Shortcut(track.getUrn(), "blah",
                Arrays.asList(Collections.singletonMap("asdf", 1)), true);

        SearchSuggestion searchSuggestion2 = new AutoValue_ApiSearchSuggestion("halb",
                Arrays.asList(Collections.singletonMap("fdsa", 2)),
                Optional.of(track), Optional.<ApiUser>absent(),
                false);

        assertThat(searchSuggestion1).isEqualTo(searchSuggestion2);
    }
}
