package com.soundcloud.android.stream;

import static com.soundcloud.android.storage.TableColumns.PromotedTracks;
import static com.soundcloud.android.storage.TableColumns.SoundStreamView;
import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

import java.util.List;

public class StreamItemMapper {

    public static RxResultMapper<StreamPlayable> getMapper() {
        return new RxResultMapper<StreamPlayable>() {
            @Override
            public StreamPlayable map(CursorReader cursorReader) {
                final PropertySet propertySet = toPlayablePropertySet(cursorReader);
                return StreamPlayable.createFromPropertySet(cursorReader.getDateFromTimestamp(SoundStreamView.CREATED_AT), propertySet);
            }
        };
    }

    public static RxResultMapper<StreamPlayable> getPromotedMapper() {
        return new RxResultMapper<StreamPlayable>() {
            @Override
            public StreamPlayable map(CursorReader cursorReader) {
                final PropertySet propertySet = toPlayablePropertySet(cursorReader);
                addOptionalPromotedProperties(cursorReader, propertySet);
                return StreamPlayable.createFromPropertySet(cursorReader.getDateFromTimestamp(SoundStreamView.CREATED_AT), propertySet);
            }
        };
    }

    private static PropertySet toPlayablePropertySet(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

        final Urn urn = readSoundUrn(cursorReader);
        propertySet.put(PlayableProperty.URN, urn);
        addTitle(cursorReader, propertySet);
        propertySet.put(PlayableProperty.CREATOR_NAME, cursorReader.getString(SoundView.USERNAME));
        propertySet.put(PlayableProperty.CREATOR_URN, Urn.forUser(cursorReader.getInt(SoundView.USER_ID)));
        propertySet.put(SoundStreamProperty.AVATAR_URL_TEMPLATE,
                        Optional.fromNullable(cursorReader.getString(SoundView.USER_AVATAR_URL)));
        propertySet.put(PlayableProperty.CREATED_AT, cursorReader.getDateFromTimestamp(SoundView.CREATED_AT));
        propertySet.put(PlayableProperty.IS_PRIVATE,
                        Sharing.PRIVATE.name()
                                       .equalsIgnoreCase(cursorReader.getString(SoundView.SHARING)));
        propertySet.put(EntityProperty.IMAGE_URL_TEMPLATE,
                        Optional.fromNullable(cursorReader.getString(SoundView.ARTWORK_URL)));

        addDurations(cursorReader, propertySet, urn.isPlaylist());
        addSetType(cursorReader, propertySet, urn.isTrack());
        addUserLike(cursorReader, propertySet);
        addUserRepost(cursorReader, propertySet);

        addOptionalPlayCount(cursorReader, propertySet);
        addOptionalTrackCount(cursorReader, propertySet);
        addOptionalReposter(cursorReader, propertySet);

        if (cursorReader.isNotNull(SoundView.POLICIES_SUB_HIGH_TIER)) {
            propertySet.put(TrackProperty.SUB_HIGH_TIER, cursorReader.getBoolean(SoundView.POLICIES_SUB_HIGH_TIER));
        }

        if (cursorReader.isNotNull(SoundView.POLICIES_SNIPPED)) {
            propertySet.put(TrackProperty.SNIPPED, cursorReader.getBoolean(SoundView.POLICIES_SNIPPED));
        }
        return propertySet;
    }

    private static Urn readSoundUrn(CursorReader cursorReader) {
        final int soundId = cursorReader.getInt(SoundStreamView.SOUND_ID);
        return getSoundType(cursorReader) == Tables.Sounds.TYPE_TRACK ? Urn.forTrack(soundId) : Urn.forPlaylist(soundId);
    }

    private static int getSoundType(CursorReader cursorReader) {
        return cursorReader.getInt(SoundStreamView.SOUND_TYPE);
    }

    private static void addSetType(CursorReader cursorReader, PropertySet propertySet, boolean isTrack) {
        if (isTrack) { return; }

        if (cursorReader.isNotNull(SoundView.IS_ALBUM)) {
            propertySet.put(PlaylistProperty.IS_ALBUM, cursorReader.getBoolean(SoundView.IS_ALBUM));
        }

        if (cursorReader.isNotNull(SoundView.SET_TYPE)) {
            propertySet.put(PlaylistProperty.SET_TYPE, cursorReader.getString(SoundView.SET_TYPE));
        }
    }

    private static void addDurations(CursorReader cursorReader, PropertySet propertySet, boolean isPlaylist) {
        if (isPlaylist) {
            propertySet.put(PlaylistProperty.PLAYLIST_DURATION, cursorReader.getLong(SoundView.DURATION));
        } else {
            propertySet.put(TrackProperty.SNIPPET_DURATION, cursorReader.getLong(SoundView.SNIPPET_DURATION));
            propertySet.put(TrackProperty.FULL_DURATION, cursorReader.getLong(SoundView.FULL_DURATION));
        }
    }

    private static void addTitle(CursorReader cursorReader, PropertySet propertySet) {
        final String string = cursorReader.getString(SoundView.TITLE);
        if (string == null) {
            ErrorUtils.handleSilentException("urn : " + readSoundUrn(cursorReader),
                                             new IllegalStateException("Unexpected null title in stream"));
            propertySet.put(PlayableProperty.TITLE, Strings.EMPTY);
        } else {
            propertySet.put(PlayableProperty.TITLE, string);
        }
    }

    private static void addUserLike(CursorReader cursorReader, PropertySet propertySet) {
        propertySet.put(PlayableProperty.IS_USER_LIKE, cursorReader.getBoolean(SoundView.USER_LIKE));
        propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(SoundView.LIKES_COUNT));
    }

    private static void addUserRepost(CursorReader cursorReader, PropertySet propertySet) {
        propertySet.put(PlayableProperty.IS_USER_REPOST, cursorReader.getBoolean(SoundView.USER_REPOST));
        propertySet.put(PlayableProperty.REPOSTS_COUNT, cursorReader.getInt(SoundView.REPOSTS_COUNT));
    }

    private static void addOptionalPlayCount(CursorReader cursorReader, PropertySet propertySet) {
        if (getSoundType(cursorReader) == Tables.Sounds.TYPE_TRACK) {
            propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(SoundView.PLAYBACK_COUNT));
        }
    }

    private static void addOptionalTrackCount(CursorReader cursorReader, PropertySet propertySet) {
        if (getSoundType(cursorReader) == Tables.Sounds.TYPE_PLAYLIST) {
            propertySet.put(PlaylistProperty.TRACK_COUNT, cursorReader.getInt(SoundView.TRACK_COUNT));
        }
    }

    private static void addOptionalReposter(CursorReader cursorReader, PropertySet propertySet) {
        final String reposter = cursorReader.getString(SoundStreamView.REPOSTER_USERNAME);
        if (Strings.isNotBlank(reposter)) {
            propertySet.put(PostProperty.REPOSTER, cursorReader.getString(SoundStreamView.REPOSTER_USERNAME));
            propertySet.put(PostProperty.REPOSTER_URN,
                            Urn.forUser(cursorReader.getInt(SoundStreamView.REPOSTER_ID)));
            propertySet.put(SoundStreamProperty.AVATAR_URL_TEMPLATE,
                            Optional.fromNullable(cursorReader.getString(SoundStreamView.REPOSTER_AVATAR_URL)));
        }
    }

    private static void addOptionalPromotedProperties(CursorReader cursorReader, PropertySet propertySet) {
        if (cursorReader.isNotNull(PromotedTracks.AD_URN)) {
            propertySet.put(PromotedItemProperty.AD_URN, cursorReader.getString(PromotedTracks.AD_URN));
            propertySet.put(PromotedItemProperty.TRACK_CLICKED_URLS,
                            splitUrls(cursorReader.getString(PromotedTracks.TRACKING_TRACK_CLICKED_URLS)));
            propertySet.put(PromotedItemProperty.TRACK_IMPRESSION_URLS,
                            splitUrls(cursorReader.getString(PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS)));
            propertySet.put(PromotedItemProperty.TRACK_PLAYED_URLS,
                            splitUrls(cursorReader.getString(PromotedTracks.TRACKING_TRACK_PLAYED_URLS)));
            propertySet.put(PromotedItemProperty.PROMOTER_CLICKED_URLS,
                            splitUrls(cursorReader.getString(PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS)));
            addOptionalPromoter(cursorReader, propertySet);
        }
    }

    private static void addOptionalPromoter(CursorReader cursorReader, PropertySet propertySet) {
        if (cursorReader.isNotNull(PromotedTracks.PROMOTER_ID)) {
            propertySet.put(PromotedItemProperty.PROMOTER_URN,
                            Optional.of(Urn.forUser(cursorReader.getLong(PromotedTracks.PROMOTER_ID))));
            propertySet.put(PromotedItemProperty.PROMOTER_NAME,
                            Optional.of(cursorReader.getString(PromotedTracks.PROMOTER_NAME)));
            propertySet.put(SoundStreamProperty.AVATAR_URL_TEMPLATE,
                            Optional.fromNullable(cursorReader.getString(SoundStreamView.PROMOTER_AVATAR_URL)));
        } else {
            propertySet.put(PromotedItemProperty.PROMOTER_URN, Optional.<Urn>absent());
            propertySet.put(PromotedItemProperty.PROMOTER_NAME, Optional.<String>absent());
        }
    }

    private static List<String> splitUrls(String urls) {
        return newArrayList(urls.split(" "));
    }
}
