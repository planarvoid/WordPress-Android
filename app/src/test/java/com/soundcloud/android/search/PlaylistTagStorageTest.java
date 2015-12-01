package com.soundcloud.android.search;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

public class PlaylistTagStorageTest extends AndroidUnitTest {

    private PlaylistTagStorage tagStorage;
    private TestDateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        dateProvider = new TestDateProvider();
        tagStorage = new PlaylistTagStorage(
                sharedPreferences("test", Context.MODE_PRIVATE),
                dateProvider,
                Schedulers.immediate());
    }

    @Test
    public void shouldSaveTagToSharedPreferences() {
        SharedPreferences prefs = mock(SharedPreferences.class);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        when(prefs.edit()).thenReturn(editor);
        when(prefs.getString(anyString(), anyString())).thenReturn("");
        when(editor.putString(anyString(), anyString())).thenReturn(editor);

        PlaylistTagStorage storage = new PlaylistTagStorage(prefs, dateProvider);
        storage.addRecentTag("tag1");

        verify(editor).putString(eq("recent_tags"), eq("tag1"));
    }

    @Test
    public void shouldClearTagSharedPreferences() throws Exception {
        tagStorage.addRecentTag("tag1");
        tagStorage.addRecentTag("tag2");

        tagStorage.clear();

        assertThat(tagStorage.getRecentTags().size()).isEqualTo(0);
    }

    @Test
    public void shouldReturnEmptyListWhenNoRecentTagsExist() {
        List<String> recentTags = tagStorage.getRecentTags();
        assertThat(recentTags.isEmpty()).isTrue();
    }

    @Test
    public void shouldReturnSingleTagAsListWithOneItem() {
        tagStorage.addRecentTag("tag1");

        List<String> recentTags = tagStorage.getRecentTags();
        assertThat(recentTags).containsExactly("tag1");
    }

    @Test
    public void shouldReturnMostRecentTagsFirst() {
        tagStorage.addRecentTag("tag1");
        tagStorage.addRecentTag("tag2");

        List<String> recentTags = tagStorage.getRecentTags();
        assertThat(recentTags).containsExactly("tag2", "tag1");
    }

    @Test
    public void shouldNotHaveDuplicates() {
        tagStorage.addRecentTag("tag1");
        tagStorage.addRecentTag("tag1");

        List<String> recentTags = tagStorage.getRecentTags();
        assertThat(recentTags).containsExactly("tag1");
    }

    @Test
    public void shouldReturnMostRecentFiveTags() {
        tagStorage.addRecentTag("tag1");
        tagStorage.addRecentTag("tag2");
        tagStorage.addRecentTag("tag3");
        tagStorage.addRecentTag("tag4");
        tagStorage.addRecentTag("tag5");
        tagStorage.addRecentTag("tag6");

        List<String> recentTags = tagStorage.getRecentTags();
        assertThat(recentTags).containsExactly("tag6", "tag5", "tag4", "tag3", "tag2");
    }

    @Test
    public void shouldIgnoreTextAfterComma() throws Exception {
        tagStorage.addRecentTag("tag1,tag2,tag3");

        List<String> recentTags = tagStorage.getRecentTags();
        assertThat(recentTags).containsExactly("tag1");
    }

    @Test
    public void shouldEmitRecentPlaylistTagsCollection() {
        tagStorage.addRecentTag("tag1");
        tagStorage.addRecentTag("tag2");

        Observer observer = mock(Observer.class);
        Observable<List<String>> observable = tagStorage.getRecentTagsAsync();
        observable.subscribe(observer);

        ArgumentCaptor<List> tagsCaptor = ArgumentCaptor.forClass(List.class);
        verify(observer).onNext(tagsCaptor.capture());

        assertThat(tagsCaptor.getValue()).containsExactly("tag2", "tag1");
    }

    @Test
    public void cachesPopularTagsList() {
        List<String> popularTags = newArrayList("tag1", "tag2", "tag3");
        tagStorage.cachePopularTags(popularTags);

        assertThat(tagStorage.getPopularTags()).containsExactly("tag1", "tag2", "tag3");
    }
}
