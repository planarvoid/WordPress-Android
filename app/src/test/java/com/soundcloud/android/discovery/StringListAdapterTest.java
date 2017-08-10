package com.soundcloud.android.discovery;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class StringListAdapterTest {
    @Mock private JacksonJsonTransformer alwaysFails;
    private StringListAdapter stringListAdapter;

    @Before
    public void setUp() throws Exception {
        when(alwaysFails.fromJson(anyString(), eq(new TypeToken<List<String>>(){}))).thenThrow(new IllegalStateException("expected"));
        when(alwaysFails.toJson(any())).thenThrow(new IllegalStateException("expected"));
        stringListAdapter = new StringListAdapter(new JacksonJsonTransformer());
    }

    @Test
    public void shouldDecodeFromCommaSeparatedStrings() {
        assertThat(stringListAdapter.decode("[\"foo\",\"bar\",\"baz\"]")).isEqualTo(asList("foo", "bar", "baz"));
        assertThat(stringListAdapter.decode("[\"foo\"]")).isEqualTo(singletonList("foo"));
        assertThat(stringListAdapter.decode("[\"\"]")).isEqualTo(singletonList(""));
        assertThat(stringListAdapter.decode("[\"\",\"\"]")).isEqualTo(asList("", ""));
        assertThat(stringListAdapter.decode("[]")).isEqualTo(emptyList());
    }

    @Test
    public void shouldEncodeToCommaSeparatedStrings() {
        assertThat(stringListAdapter.encode(asList("foo", "bar", "baz"))).isEqualTo("[\"foo\",\"bar\",\"baz\"]");
        assertThat(stringListAdapter.encode(singletonList("foo"))).isEqualTo("[\"foo\"]");
        assertThat(stringListAdapter.encode(singletonList(""))).isEqualTo("[\"\"]");
        assertThat(stringListAdapter.encode(asList("", ""))).isEqualTo("[\"\",\"\"]");
        assertThat(stringListAdapter.encode(emptyList())).isEqualTo("[]");
    }

    @Test(expected = IllegalStateException.class)
    public void failsHardWhenDecodingFails() {
        new StringListAdapter(alwaysFails).decode("[\"foo\",\"bar\",\"baz\"]");
    }

    @Test(expected = IllegalStateException.class)
    public void failsHardWhenEncodingFails() {
        new StringListAdapter(alwaysFails).encode(asList("foo", "bar", "baz"));
    }
}
