package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WeakLruCacheTest {

    private WeakLruCache<String, Object> cache = new WeakLruCache<>(10);

    @Mock private BetterLruCache.ValueProvider<String, Object> valueProvider;

    @Before
    public void setUp() throws Exception {
        when(valueProvider.get("key")).thenReturn("lazy value");
    }

    @Test
    public void shouldGetPresentValue() {
        Object value = new Object();
        cache.put("key", value);
        assertThat(cache.get("key")).isSameAs(value);
    }

    @Test
    public void shouldReturnNullForAbsentValue() {
        assertThat(cache.get("key")).isNull();
    }

    @Test
    public void shouldUseValueProviderWhenNoEntryFound() throws Exception {
        assertThat(cache.get("key", valueProvider)).isEqualTo("lazy value");
    }

    @Test
    public void shouldCacheValueProviderValueWhenUsed() throws Exception {
        cache.get("key", valueProvider); // should trigger a cache write of the lazy value
        assertThat(cache.get("key")).isEqualTo("lazy value");
    }

    @Test
    public void shouldNotUseValueProviderWhenEntryFound() {
        cache.put("key", "value");
        assertThat(cache.get("key", valueProvider)).isEqualTo("value");
    }

    @Test
    public void shouldReturnNullWhenValueProviderThrows() throws Exception {
        when(valueProvider.get("key")).thenThrow(new Exception());
        assertThat(cache.get("key", valueProvider)).isNull();
    }
}