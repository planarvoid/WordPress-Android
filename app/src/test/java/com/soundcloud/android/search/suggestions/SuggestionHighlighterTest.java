package com.soundcloud.android.search.suggestions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SuggestionHighlighterTest {
    @Test
    public void highlightsText() throws Exception {
        final String query = "light";
        final String displayedText = "highlighter";
        final SuggestionHighlight highlight = SuggestionHighlighter.findHighlight(query, displayedText);

        assertThat(highlight.getStart()).isEqualTo(4);
        assertThat(highlight.getEnd()).isEqualTo(9);
    }
}
