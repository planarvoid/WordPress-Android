package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.playlists.PlaylistApiCreateObject;
import com.soundcloud.android.playlists.PlaylistApiUpdateObject;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;
import android.os.Parcel;

import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlaylistTest {

    @Test
    public void shouldConstructPlaylistFromId() {
        Playlist playlist = new Playlist(1L);
        expect(playlist.getUrn().toString()).toEqual("soundcloud:playlists:1");
        expect(playlist.getId()).toEqual(1L);
    }

    @Test
    public void setIdShouldUpdateUrn() throws Exception {
        Playlist playlist = new Playlist();
        playlist.setId(1000L);
        expect(playlist.getUrn().toString()).toEqual("soundcloud:playlists:1000");
    }

    @Test
    public void buildContentValuesShouldNotIncludeLastUpdatedWithNoUser() throws Exception {
        Playlist playlist = new Playlist();
        playlist.setId(1000L);
        final ContentValues actual = playlist.buildContentValues();
        expect(actual.containsKey(TableColumns.ResourceTable.LAST_UPDATED)).toBeFalse();
    }

    @Test
    public void buildContentValuesShouldIncludeLastUpdatedWithUser() throws Exception {
        Playlist playlist = new Playlist();
        playlist.setId(1000L);
        playlist.setUser(TestHelper.getModelFactory().createModel(User.class));
        final ContentValues actual = playlist.buildContentValues();
        expect(actual.containsKey(TableColumns.ResourceTable.LAST_UPDATED)).toBeTrue();
    }

    @Test
    public void setUrnShouldUpdateId() throws Exception {
        Playlist playlist = new Playlist();
        playlist.setUrn("soundcloud:playlists:1000");
        expect(playlist.getId()).toEqual(1000L);
    }

    @Test
    public void shouldDeserializePlaylist() throws Exception {
        Playlist p = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("e1_playlist.json"),
                Playlist.class);

        expect(p.title).toEqual("PA600QT Demos");
        expect(p.description).toEqual("blah blah blah");
        expect(p.release_year).toEqual(1992);
        expect(p.user_id).toEqual(1196047l);
        expect(p.tracks.size()).toEqual(14);
    }

    @Test
    public void shouldParcelAndUnparcelCorrectly() throws Exception {
        Playlist playlist = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("e1_playlist.json"),
                Playlist.class);

        Parcel p = Parcel.obtain();
        playlist.writeToParcel(p, 0);

        Playlist playlist2 = new Playlist(p);
        comparePlaylists(playlist, playlist2);
    }

    @Test
    public void shouldParcelAndUnparcelWithNoTracksCorrectly() throws Exception {
        Playlist playlist = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("e1_playlist_no_tracks.json"),
                Playlist.class);

        Parcel p = Parcel.obtain();
        playlist.writeToParcel(p, 0);

        Playlist playlist2 = new Playlist(p);
        comparePlaylists(playlist, playlist2);
    }

    @Test
    public void shouldProvideCreateJson() throws Exception {
        Playlist playlist = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("e1_playlist.json"),
                Playlist.class);

        //create mode
        PlaylistApiCreateObject createObject = new PlaylistApiCreateObject(playlist);

        expect(createObject.toJson())
                .toEqual("{\"playlist\":{\"title\":\"PA600QT Demos\",\"sharing\":\"public\",\"tracks\":[{\"id\":61363002},{\"id\":61363003},{\"id\":61363004},{\"id\":61363005},{\"id\":61363006},{\"id\":61363007},{\"id\":61363008},{\"id\":61363009},{\"id\":61363011},{\"id\":61363012},{\"id\":61363013},{\"id\":61363014},{\"id\":61363016},{\"id\":61363017}]}}");

    }

    @Test
    public void shouldProvideUpdateJson() throws Exception {
        Playlist playlist = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("e1_playlist.json"),
                Playlist.class);

        List<Long> toAdd = new ArrayList<Long>();
        for (Track t : playlist.tracks){
            toAdd.add(t.getId());
        }

        // update tracks mode
        PlaylistApiUpdateObject updateObject = new PlaylistApiUpdateObject(toAdd);
        expect(updateObject.toJson())
                .toEqual("{\"playlist\":{\"tracks\":[{\"id\":61363002},{\"id\":61363003},{\"id\":61363004},{\"id\":61363005},{\"id\":61363006},{\"id\":61363007},{\"id\":61363008},{\"id\":61363009},{\"id\":61363011},{\"id\":61363012},{\"id\":61363013},{\"id\":61363014},{\"id\":61363016},{\"id\":61363017}]}}");
    }

    @Test
    public void shouldConvertPlaylistSummaryToPlaylist() throws CreateModelException {
        PlaylistSummary source = TestHelper.getModelFactory().createModel(PlaylistSummary.class);
        Playlist playlist = new Playlist(source);
        expect(playlist.getId()).toEqual(source.getId());
        expect(playlist.getUrn()).toEqual(source.getUrn());
        expect(playlist.getTitle()).toEqual(source.getTitle());
        expect(playlist.tag_list).toEqual("tag1 tag2 tag3");
        expect(playlist.getUsername()).toEqual(source.getUsername());
        expect(playlist.artwork_url).toEqual(source.getArtworkUrl());
        expect(playlist.created_at).toEqual(source.getCreatedAt());
        expect(playlist.likes_count).toEqual(source.getStats().getLikesCount());
        expect(playlist.reposts_count).toEqual(source.getStats().getRepostsCount());
        expect(playlist.duration).toEqual(source.getDuration());
        expect(playlist.getSharing()).toEqual(source.getSharing());
        expect(playlist.getTrackCount()).toEqual(source.getTrackCount());
    }

    @Test
    public void shouldConcertToPropertySet() throws CreateModelException {
        Playlist playlist = TestHelper.getModelFactory().createModel(Playlist.class);
        PropertySet propertySet = playlist.toPropertySet();

        expect(propertySet.get(PlayableProperty.DURATION)).toEqual(playlist.duration);
        expect(propertySet.get(PlayableProperty.TITLE)).toEqual(playlist.title);
        expect(propertySet.get(PlayableProperty.URN)).toEqual(playlist.getUrn());
        expect(propertySet.get(PlayableProperty.CREATOR_URN)).toEqual(playlist.getUser().getUrn());
        expect(propertySet.get(PlayableProperty.CREATOR_NAME)).toEqual(playlist.getUsername());
        expect(propertySet.get(PlaylistProperty.TRACK_COUNT)).toEqual(playlist.getTrackCount());
        expect(propertySet.get(PlayableProperty.LIKES_COUNT)).toEqual(playlist.likes_count);
        expect(propertySet.get(PlayableProperty.IS_LIKED)).toEqual(playlist.user_like);
        expect(propertySet.get(PlayableProperty.IS_PRIVATE)).toEqual(playlist.isPrivate());
    }

    private void comparePlaylists(Playlist playlist, Playlist playlist1) {
        expect(playlist1.getId()).toEqual(playlist.getId());
        expect(playlist1.title).toEqual(playlist.title);
        expect(playlist1.permalink).toEqual(playlist.permalink);
        expect(playlist1.duration).toBeGreaterThan(0);
        expect(playlist1.duration).toEqual(playlist.duration);
        expect(playlist1.created_at).toEqual(playlist.created_at);
        expect(playlist1.tag_list).toEqual(playlist.tag_list);
        expect(playlist1.permalink_url).toEqual(playlist.permalink_url);
        expect(playlist1.artwork_url).toEqual(playlist.artwork_url);
        expect(playlist1.downloadable).toEqual(playlist.downloadable);
        expect(playlist1.streamable).toEqual(playlist.streamable);
        expect(playlist1.user_id).toEqual(playlist.user_id);
    }
}
