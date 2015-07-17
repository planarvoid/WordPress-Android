package com.soundcloud.android.utils.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SoftValuesCacheTest {

    private SoftValuesCache<String, Object> cache = new SoftValuesCache<>(10);

    @Mock private DefaultCache.ValueProvider<String, Object> valueProvider;

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

    @Test
    public void shouldReportItsSize() {
        assertThat(cache.size()).isEqualTo(0);

        cache.put("key1", "value");
        cache.put("key2", "value");

        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    public void shouldClearTheCache() {
        cache.put("key", "value");

        cache.clear();

        assertThat(cache.get("key")).isNull();
        assertThat(cache.size()).isEqualTo(0);
    }

    @Test
    public void shouldReportHitAndMissCount() {
        cache.put("key", "value");

        cache.get("key");
        cache.get("key");
        cache.get("doesn't exist");

        assertThat(cache.hitCount()).isEqualTo(2);
        assertThat(cache.missCount()).isEqualTo(1);
    }
}