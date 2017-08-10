package com.soundcloud.java.collections;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.TransformedCollection;
import com.soundcloud.java.functions.Functions;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TransformedCollectionTest {

    @Test
    public void shouldForwardClearToWrappedCollection() {
        List<Integer> source = Lists.newArrayList(1, 2, 3);
        TransformedCollection<Integer, String> transformedCollection =
                createTransformedCollection(source);

        transformedCollection.clear();

        assertThat(source).isEmpty();
    }

    @Test
    public void shouldForwardIsEmptyToWrappedCollectionForNonEmptyCollection() {
        List<Integer> source = Lists.newArrayList(1, 2, 3);
        TransformedCollection<Integer, String> transformedCollection =
                createTransformedCollection(source);

        assertThat(transformedCollection).isNotEmpty();
    }

    @Test
    public void shouldForwardIsEmptyToWrappedCollectionForEmptyCollection() {
        List<Integer> source = Collections.emptyList();
        TransformedCollection<Integer, String> transformedCollection =
                createTransformedCollection(source);

        assertThat(transformedCollection).isEmpty();
    }

    @Test
    public void shouldForwardSizeToWrappedCollection() {
        List<Integer> source = Arrays.asList(1, 2, 3);
        TransformedCollection<Integer, String> transformedCollection =
                createTransformedCollection(source);

        assertThat(transformedCollection).hasSize(3);
    }

    @Test
    public void shouldTransformSourceIterator() {
        List<Integer> source = Arrays.asList(1, 2, 3);
        TransformedCollection<Integer, String> transformedCollection =
                createTransformedCollection(source);

        assertThat(Lists.newArrayList(transformedCollection.iterator()))
                .containsExactly("1", "2", "3");
    }

    private TransformedCollection<Integer, String>  createTransformedCollection(Collection<Integer> source) {
        return new TransformedCollection<>(
                source,
                Functions.toStringFunction()
        );
    }
}
