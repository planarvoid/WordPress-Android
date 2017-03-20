package com.soundcloud.android.stream;

import static com.soundcloud.android.storage.TableColumns.PromotedTracks;
import static com.soundcloud.android.storage.TableColumns.SoundStreamView;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.strings.Strings.isNotBlank;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

import java.util.Date;
import java.util.List;

class StreamEntityMapper {

    static RxResultMapper<StreamEntity> getMapper() {
        return new RxResultMapper<StreamEntity>() {
            @Override
            public StreamEntity map(CursorReader cursorReader) {
                final Urn urn = readSoundUrn(cursorReader);
                return toStreamEntity(urn, cursorReader);
            }
        };
    }

    static RxResultMapper<StreamEntity> getPromotedMapper() {
        return new RxResultMapper<StreamEntity>() {
            @Override
            public StreamEntity map(CursorReader cursorReader) {
                final Urn urn = readSoundUrn(cursorReader);
                return toPromotedStreamEntity(urn, cursorReader);
            }
        };
    }

    private static StreamEntity toStreamEntity(Urn urn, CursorReader cursorReader) {
        return streamEntityBuilder(urn, cursorReader).build();
    }

    private static StreamEntity.Builder streamEntityBuilder(Urn urn, CursorReader cursorReader) {
        final Date createdAt = cursorReader.getDateFromTimestamp(SoundStreamView.CREATED_AT);
        final StreamEntity.Builder builder = StreamEntity.builder(urn, createdAt);
        Optional<String> avatarUrl = Optional.fromNullable(cursorReader.getString(TableColumns.SoundView.USER_AVATAR_URL));

        final String reposterUserName = cursorReader.getString(SoundStreamView.REPOSTER_USERNAME);
        if (isNotBlank(reposterUserName)) {
            final Urn reposterUrn = Urn.forUser(cursorReader.getInt(SoundStreamView.REPOSTER_ID));
            avatarUrl = Optional.fromNullable(cursorReader.getString(SoundStreamView.REPOSTER_AVATAR_URL));
            builder.repostedProperties(RepostedProperties.create(reposterUserName, reposterUrn));
        }
        return builder.avatarUrlTemplate(avatarUrl);
    }

    private static StreamEntity toPromotedStreamEntity(Urn urn, CursorReader cursorReader) {
        final StreamEntity.Builder builder = streamEntityBuilder(urn, cursorReader);
        if (cursorReader.isNotNull(PromotedTracks.AD_URN)) {
            final String adUrn = cursorReader.getString(PromotedTracks.AD_URN);
            final List<String> trackClickedUrls = splitUrls(cursorReader.getString(PromotedTracks.TRACKING_TRACK_CLICKED_URLS));
            final List<String> trackImpressionUrls = splitUrls(cursorReader.getString(PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS));
            final List<String> trackPlayedUrls = splitUrls(cursorReader.getString(PromotedTracks.TRACKING_TRACK_PLAYED_URLS));
            final List<String> promoterClickedUrls = splitUrls(cursorReader.getString(PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS));
            Optional<Urn> promoterUrn = Optional.absent();
            Optional<String> promoterName = Optional.absent();
            if (cursorReader.isNotNull(PromotedTracks.PROMOTER_ID)) {
                promoterUrn = Optional.of(Urn.forUser(cursorReader.getLong(PromotedTracks.PROMOTER_ID)));
                promoterName = Optional.of(cursorReader.getString(PromotedTracks.PROMOTER_NAME));
                builder.avatarUrlTemplate(Optional.fromNullable(cursorReader.getString(SoundStreamView.PROMOTER_AVATAR_URL)));
            }

            final PromotedProperties promotedProperties = PromotedProperties.create(adUrn,
                                                                                    trackClickedUrls,
                                                                                    trackImpressionUrls,
                                                                                    trackPlayedUrls,
                                                                                    promoterClickedUrls,
                                                                                    promoterUrn,
                                                                                    promoterName);
            builder.promotedProperties(promotedProperties);
        }
        return builder.build();
    }


    private static Urn readSoundUrn(CursorReader cursorReader) {
        final int soundId = cursorReader.getInt(SoundStreamView.SOUND_ID);
        return getSoundType(cursorReader) == Tables.Sounds.TYPE_TRACK ? Urn.forTrack(soundId) : Urn.forPlaylist(soundId);
    }

    private static int getSoundType(CursorReader cursorReader) {
        return cursorReader.getInt(SoundStreamView.SOUND_TYPE);
    }

    private static List<String> splitUrls(String urls) {
        return newArrayList(urls.split(" "));
    }
}
