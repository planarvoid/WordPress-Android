package com.soundcloud.android.playback;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.newLinkedList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.LinkedList;

@RunWith(MockitoJUnitRunner.class)
public class ShuffledListTest {

    private ArrayList<String> originalArrayList = newArrayList("original 0", "original 1", "original 2");
    private ShuffledPlayQueue.ShuffledList<String> shuffledArrayList = new ShuffledPlayQueue.ShuffledList<>(originalArrayList, newArrayList(1, 0, 2));

    private LinkedList<String> originalLinkedList = newLinkedList(asList("original 0", "original 1", "original 2"));
    private ShuffledPlayQueue.ShuffledList<String> shuffledLinkedList = new ShuffledPlayQueue.ShuffledList<>(originalLinkedList, newArrayList(1, 0, 2));

    @Test
    public void iterators() {
        assertThat(shuffledArrayList.iterator()).containsExactly("original 1", "original 0", "original 2");
        assertThat(shuffledLinkedList.iterator()).containsExactly("original 1", "original 0", "original 2");
    }

    @Test
    public void initWithGivenIndices() {
        assertThat(shuffledArrayList).containsExactly("original 1", "original 0", "original 2");
        assertThat(shuffledArrayList.get(0)).isEqualTo("original 1");
        assertThat(shuffledArrayList.get(1)).isEqualTo("original 0");
        assertThat(shuffledArrayList.get(2)).isEqualTo("original 2");

        assertThat(shuffledLinkedList).containsExactly("original 1", "original 0", "original 2");
        assertThat(shuffledLinkedList.get(0)).isEqualTo("original 1");
        assertThat(shuffledLinkedList.get(1)).isEqualTo("original 0");
        assertThat(shuffledLinkedList.get(2)).isEqualTo("original 2");
    }

    @Test
    public void addObject() {
        shuffledArrayList.add("test 3");
        assertThat(shuffledArrayList).containsExactly("original 1", "original 0", "original 2", "test 3");
        assertThat(originalArrayList).containsExactly("original 0", "original 1", "original 2", "test 3");
        assertThat(shuffledArrayList.get(3)).isEqualTo("test 3");

        shuffledLinkedList.add("test 3");
        assertThat(shuffledLinkedList).containsExactly("original 1", "original 0", "original 2", "test 3");
        assertThat(originalLinkedList).containsExactly("original 0", "original 1", "original 2", "test 3");
        assertThat(shuffledLinkedList.get(3)).isEqualTo("test 3");
    }

    @Test
    public void addObjectAtIndex() {
        shuffledArrayList.add(1, "test 3");
        assertThat(shuffledArrayList).containsExactly("original 1", "test 3", "original 0", "original 2");
        assertThat(originalArrayList).containsExactly("original 0", "original 1", "original 2", "test 3");
        assertThat(shuffledArrayList.get(1)).isEqualTo("test 3");

        shuffledLinkedList.add(1, "test 3");
        assertThat(shuffledLinkedList).containsExactly("original 1", "test 3", "original 0", "original 2");
        assertThat(originalLinkedList).containsExactly("original 0", "original 1", "original 2", "test 3");
        assertThat(shuffledLinkedList.get(1)).isEqualTo("test 3");
    }

    @Test
    public void addAll() {
        shuffledArrayList.addAll(asList("test 3", "test 4"));
        assertThat(shuffledArrayList).containsExactly("original 1", "original 0", "original 2", "test 3", "test 4");
        assertThat(originalArrayList).containsExactly("original 0", "original 1", "original 2", "test 3", "test 4");
        assertThat(shuffledArrayList.get(3)).isEqualTo("test 3");

        shuffledLinkedList.addAll(asList("test 3", "test 4"));
        assertThat(shuffledLinkedList).containsExactly("original 1", "original 0", "original 2", "test 3", "test 4");
        assertThat(originalLinkedList).containsExactly("original 0", "original 1", "original 2", "test 3", "test 4");
        assertThat(shuffledLinkedList.get(3)).isEqualTo("test 3");
    }

    @Test
    public void addAllAtIndex() {
        shuffledArrayList.addAll(1, asList("test 3", "test 4"));
        assertThat(shuffledArrayList).containsExactly("original 1", "test 3", "test 4", "original 0", "original 2");
        assertThat(originalArrayList).containsExactly("original 0", "original 1", "original 2", "test 3", "test 4");
        assertThat(shuffledArrayList.get(1)).isEqualTo("test 3");

        shuffledLinkedList.addAll(1, asList("test 3", "test 4"));
        assertThat(shuffledLinkedList).containsExactly("original 1", "test 3", "test 4", "original 0", "original 2");
        assertThat(originalLinkedList).containsExactly("original 0", "original 1", "original 2", "test 3", "test 4");
        assertThat(shuffledLinkedList.get(1)).isEqualTo("test 3");
    }

    @Test
    public void removeAtIndex() {
        assertThat(shuffledArrayList.remove(1)).isEqualTo("original 0");
        assertThat(shuffledArrayList).containsExactly("original 1", "original 2");
        assertThat(originalArrayList).containsExactly("original 1", "original 2");
        assertThat(shuffledArrayList.get(1)).isEqualTo("original 2");

        assertThat(shuffledLinkedList.remove(1)).isEqualTo("original 0");
        assertThat(shuffledLinkedList).containsExactly("original 1", "original 2");
        assertThat(originalLinkedList).containsExactly("original 1", "original 2");
        assertThat(shuffledLinkedList.get(1)).isEqualTo("original 2");
    }

    @Test
    public void removeObject() {
        shuffledArrayList.remove("original 0");
        assertThat(shuffledArrayList).containsExactly("original 1", "original 2");
        assertThat(originalArrayList).containsExactly("original 1", "original 2");
        assertThat(shuffledArrayList.get(1)).isEqualTo("original 2");

        shuffledLinkedList.remove("original 0");
        assertThat(shuffledLinkedList).containsExactly("original 1", "original 2");
        assertThat(originalLinkedList).containsExactly("original 1", "original 2");
        assertThat(shuffledLinkedList.get(1)).isEqualTo("original 2");
    }

    @Test
    public void removeAll() {
        shuffledArrayList.removeAll(singletonList("original 0"));
        assertThat(shuffledArrayList).containsExactly("original 1", "original 2");
        assertThat(originalArrayList).containsExactly("original 1", "original 2");

        shuffledLinkedList.removeAll(singletonList("original 0"));
        assertThat(shuffledLinkedList).containsExactly("original 1", "original 2");
        assertThat(originalLinkedList).containsExactly("original 1", "original 2");
    }

    @Test
    public void indexOf() {
        assertThat(shuffledArrayList.indexOf("original 1")).isEqualTo(0);
        assertThat(shuffledArrayList.indexOf("original 0")).isEqualTo(1);
        assertThat(shuffledArrayList.indexOf("original 2")).isEqualTo(2);

        assertThat(shuffledLinkedList.indexOf("original 1")).isEqualTo(0);
        assertThat(shuffledLinkedList.indexOf("original 0")).isEqualTo(1);
        assertThat(shuffledLinkedList.indexOf("original 2")).isEqualTo(2);
    }

    @Test
    public void toArray() {
        assertThat(shuffledArrayList.toArray()).containsExactly("original 1", "original 0", "original 2");
        assertThat(shuffledLinkedList.toArray()).containsExactly("original 1", "original 0", "original 2");
    }

    @Test
    public void subList() {
        assertThat(shuffledArrayList.subList(0, 1)).containsExactly("original 1");
        assertThat(shuffledLinkedList.subList(0, 1)).containsExactly("original 1");

        assertThat(shuffledArrayList.subList(0, shuffledArrayList.size())).containsExactly("original 1", "original 0", "original 2");
        assertThat(shuffledLinkedList.subList(0, shuffledLinkedList.size())).containsExactly("original 1", "original 0", "original 2");

        assertThat(shuffledArrayList.subList(2, shuffledArrayList.size())).containsExactly("original 2");
        assertThat(shuffledLinkedList.subList(2, shuffledLinkedList.size())).containsExactly("original 2");
    }

    @Test
    public void isEmpty() {
        assertThat(new ShuffledPlayQueue.ShuffledList<>(emptyList(), 0)).isEmpty();
    }

    @Test
    public void equals() {
        assertThat(shuffledArrayList).isEqualTo(shuffledLinkedList);

        shuffledLinkedList.add("new item");
        assertThat(shuffledArrayList).isNotEqualTo(shuffledLinkedList);
    }
}
