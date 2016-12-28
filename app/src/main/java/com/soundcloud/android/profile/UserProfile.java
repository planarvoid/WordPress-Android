package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;

public class UserProfile {
    private final UserItem user;
    private final ModelCollection<PlayableItem> spotlight;
    private final ModelCollection<TrackItem> tracks;
    private final ModelCollection<PlaylistItem> albums;
    private final ModelCollection<PlaylistItem> playlists;
    private final ModelCollection<PlayableItem> reposts;
    private final ModelCollection<PlayableItem> likes;

    public UserProfile(UserItem user,
                       ModelCollection<PlayableItem> spotlight,
                       ModelCollection<TrackItem> tracks,
                       ModelCollection<PlaylistItem> albums,
                       ModelCollection<PlaylistItem> playlists,
                       ModelCollection<PlayableItem> reposts,
                       ModelCollection<PlayableItem> likes) {

        this.user = user;
        this.spotlight = spotlight;
        this.tracks = tracks;
        this.albums = albums;
        this.playlists = playlists;
        this.reposts = reposts;
        this.likes = likes;
    }

    public UserItem getUser() {
        return user;
    }

    public ModelCollection<PlayableItem> getSpotlight() {
        return spotlight;
    }

    public ModelCollection<TrackItem> getTracks() {
        return tracks;
    }

    public ModelCollection<PlaylistItem> getAlbums() {
        return albums;
    }

    public ModelCollection<PlaylistItem> getPlaylists() {
        return playlists;
    }

    public ModelCollection<PlayableItem> getReposts() {
        return reposts;
    }

    public ModelCollection<PlayableItem> getLikes() {
        return likes;
    }

    public static UserProfile fromUserProfileRecord(ApiUserProfile userProfileRecord) {
        UserItem user = UserItem.from(userProfileRecord.getUser());
        ModelCollection<PlayableItem> spotlight = userProfileRecord.getSpotlight().transform(PlayableItem::from);
        ModelCollection<TrackItem> tracks = userProfileRecord.getTracks().transform(post -> TrackItem.from(post.getApiTrack()));
        ModelCollection<PlaylistItem> albums = userProfileRecord.getAlbums().transform(post -> PlaylistItem.from(post.getApiPlaylist()));
        ModelCollection<PlaylistItem> playlists = userProfileRecord.getPlaylists().transform(post -> PlaylistItem.from(post.getApiPlaylist()));
        ModelCollection<PlayableItem> reposts = userProfileRecord.getReposts().transform(PlayableItem::from);
        ModelCollection<PlayableItem> likes = userProfileRecord.getLikes().transform(PlayableItem::from);

        return new UserProfile(user, spotlight, tracks, albums, playlists, reposts, likes);
    }

}
