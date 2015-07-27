package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowPreferenceManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistTagStorageTest {

    private PlaylistTagStorage tagStorage;

    @Before
    public void setUp() throws Exception {
        Context context = Robolectric.application.getApplicationContext();
        tagStorage = new PlaylistTagStorage(
                ShadowPreferenceManager.getDefaultSharedPreferences(context),
                Schedulers.immediate());
    }

    @Test
    public void shouldSaveTagToSharedPreferences() {
        SharedPreferences prefs = mock(SharedPreferences.class);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        when(prefs.edit()).thenReturn(editor);
        when(prefs.getString(anyString(), anyString())).thenReturn("");
        when(editor.putString(anyString(), anyString())).thenReturn(editor);

        PlaylistTagStorage storage = new PlaylistTagStorage(prefs);
        storage.addRecentTag("tag1");

        verify(editor).putString(eq("recent_tags"), eq("tag1"));
    }

    @Test
    public void shouldClearTagSharedPreferences() throws Exception {
        tagStorage.addRecentTag("tag1");
        tagStorage.addRecentTag("tag2");

        tagStorage.clear();

        expect(tagStorage.getRecentTags().size()).toEqual(0);
    }

    @Test
    public void shouldReturnEmptyListWhenNoRecentTagsExist() {
        List<String> recentTags = tagStorage.getRecentTags();
        expect(recentTags.isEmpty()).toBeTrue();
    }

    @Test
    public void shouldReturnSingleTagAsListWithOneItem() {
        tagStorage.addRecentTag("tag1");

        List<String> recentTags = tagStorage.getRecentTags();
        expect(recentTags).toContainExactly("tag1");
    }

    @Test
    public void shouldReturnMostRecentTagsFirst() {
        tagStorage.addRecentTag("tag1");
        tagStorage.addRecentTag("tag2");

        List<String> recentTags = tagStorage.getRecentTags();
        expect(recentTags).toContainExactly("tag2", "tag1");
    }

    @Test
    public void shouldNotHaveDuplicates() {
        tagStorage.addRecentTag("tag1");
        tagStorage.addRecentTag("tag1");

        List<String> recentTags = tagStorage.getRecentTags();
        expect(recentTags).toContainExactly("tag1");
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
        expect(recentTags).toContainExactly("tag6", "tag5", "tag4", "tag3", "tag2");
    }

    @Test
    public void shouldIgnoreTextAfterComma() throws Exception {
        tagStorage.addRecentTag("tag1,tag2,tag3");

        List<String> recentTags = tagStorage.getRecentTags();
        expect(recentTags).toContainExactly("tag1");
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

        expect(tagsCaptor.getValue()).toContainExactly("tag2", "tag1");
    }

    @Test
    public void cachesPopularTagsList() {
        List<String> popularTags = newArrayList("tag1", "tag2", "tag3");
        tagStorage.cachePopularTags(popularTags);

        expect(tagStorage.getPopularTags()).toContainExactly("tag1", "tag2", "tag3");
    }

    @Test
    public void resetsPopularTagList() {
        List<String> popularTags = newArrayList("tag1", "tag2", "tag3");

        tagStorage.cachePopularTags(popularTags);
        tagStorage.resetPopularTags();

        expect(tagStorage.getPopularTags().size()).toEqual(0);
    }
}
