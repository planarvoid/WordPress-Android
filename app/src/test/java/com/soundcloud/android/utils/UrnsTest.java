package com.soundcloud.android.utils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import org.junit.Test;

import java.util.List;

public class UrnsTest {

    @Test
    public void shouldConvertUrnsToStringsWithFunction() {
        final List<Urn> input = asList(new Urn("a"), new Urn("b"));
        final List<String> output = Lists.transform(input, Urns.toStringFunc());
        assertThat(output).containsExactly("a", "b");
    }

    @Test
    public void shouldConvertUrnsToStrings() {
        final List<Urn> input = asList(new Urn("a"), new Urn("b"));
        final List<String> output = Urns.toString(input);
        assertThat(output).containsExactly("a", "b");
    }

    @Test
    public void shouldConvertUrnsToIdString() {
        final List<Urn> input = asList(Urn.forTrack(1), Urn.forTrack(2));
        final String output = Urns.toJoinedIds(input, ",");
        assertThat(output).isEqualTo("1,2");
    }
}
