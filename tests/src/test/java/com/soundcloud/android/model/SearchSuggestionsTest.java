package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class SearchSuggestionsTest {
    @Test
    public void shouldDeserializeCorrectly() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SearchSuggestions suggestions = mapper.readValue(getClass().getResourceAsStream("suggest.json"), SearchSuggestions.class);

        expect(suggestions.tx_id).toEqual("92dbb484c0d144afa6c193ece99514f3");
        expect(suggestions.query_time_in_millis).toEqual(1l);
        expect(suggestions.query).toEqual("f");
        expect(suggestions.limit).toEqual(5);
        expect(suggestions.suggestions).toNumber(5);

        expect(suggestions.suggestions.get(0).query).toEqual("Foo Fighters");
        expect(suggestions.suggestions.get(0).kind).toEqual("user");
        expect(suggestions.suggestions.get(0).id).toEqual(2097360l);
        expect(suggestions.suggestions.get(0).score).toEqual(889523l);
        expect(suggestions.suggestions.get(0).getClientUri()).toEqual("soundcloud:users:2097360");
    }
}
