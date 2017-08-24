package com.soundcloud.android.storage.schemas;

import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.Tables.PlaylistView.ARTWORK_URL;
import static com.soundcloud.android.storage.Tables.PlaylistView.CREATED_AT;
import static com.soundcloud.android.storage.Tables.PlaylistView.CREATOR_IS_PRO;
import static com.soundcloud.android.storage.Tables.PlaylistView.DURATION;
import static com.soundcloud.android.storage.Tables.PlaylistView.GENRE;
import static com.soundcloud.android.storage.Tables.PlaylistView.HAS_DOWNLOADED_TRACKS;
import static com.soundcloud.android.storage.Tables.PlaylistView.HAS_PENDING_DOWNLOAD_REQUEST;
import static com.soundcloud.android.storage.Tables.PlaylistView.HAS_UNAVAILABLE_TRACKS;
import static com.soundcloud.android.storage.Tables.PlaylistView.ID;
import static com.soundcloud.android.storage.Tables.PlaylistView.IS_ALBUM;
import static com.soundcloud.android.storage.Tables.PlaylistView.IS_MARKED_FOR_OFFLINE;
import static com.soundcloud.android.storage.Tables.PlaylistView.IS_USER_LIKE;
import static com.soundcloud.android.storage.Tables.PlaylistView.IS_USER_REPOST;
import static com.soundcloud.android.storage.Tables.PlaylistView.LIKES_COUNT;
import static com.soundcloud.android.storage.Tables.PlaylistView.LOCAL_TRACK_COUNT;
import static com.soundcloud.android.storage.Tables.PlaylistView.PERMALINK_URL;
import static com.soundcloud.android.storage.Tables.PlaylistView.RELEASE_DATE;
import static com.soundcloud.android.storage.Tables.PlaylistView.REPOSTS_COUNT;
import static com.soundcloud.android.storage.Tables.PlaylistView.SET_TYPE;
import static com.soundcloud.android.storage.Tables.PlaylistView.SHARING;
import static com.soundcloud.android.storage.Tables.PlaylistView.TAG_LIST;
import static com.soundcloud.android.storage.Tables.PlaylistView.TITLE;
import static com.soundcloud.android.storage.Tables.PlaylistView.TRACK_COUNT;
import static com.soundcloud.android.storage.Tables.PlaylistView.USERNAME;
import static com.soundcloud.android.storage.Tables.PlaylistView.USER_ID;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.playlists.PlaylistQueries;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

public class PlaylistView {

    public static final String SQL_VERSION_117 = "CREATE VIEW IF NOT EXISTS PlaylistView AS " +
            Query.from(SoundView.name())
                 .select(field(SoundView.field(TableColumns.SoundView._ID)).as(ID.name()),
                         field(SoundView.field(TableColumns.SoundView.TITLE)).as(TITLE.name()),
                         field(SoundView.field(TableColumns.SoundView.USERNAME)).as(USERNAME.name()),
                         field(SoundView.field(TableColumns.SoundView.USER_ID)).as(USER_ID.name()),
                         field("(" + creatorIsProQuery().build() + ")").as(CREATOR_IS_PRO.name()),
                         field(SoundView.field(TableColumns.SoundView.TRACK_COUNT)).as(TRACK_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.DURATION)).as(DURATION.name()),
                         field(SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(LIKES_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.REPOSTS_COUNT)).as(REPOSTS_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.SHARING)).as(SHARING.name()),
                         field(SoundView.field(TableColumns.SoundView.ARTWORK_URL)).as(ARTWORK_URL.name()),
                         field(SoundView.field(TableColumns.SoundView.PERMALINK_URL)).as(PERMALINK_URL.name()),
                         field(SoundView.field(TableColumns.SoundView.GENRE)).as(GENRE.name()),
                         field(SoundView.field(TableColumns.SoundView.TAG_LIST)).as(TAG_LIST.name()),
                         field(SoundView.field(TableColumns.SoundView.CREATED_AT)).as(CREATED_AT.name()),
                         field(SoundView.field(TableColumns.SoundView.RELEASE_DATE)).as(RELEASE_DATE.name()),
                         field(SoundView.field(TableColumns.SoundView.SET_TYPE)).as(SET_TYPE.name()),
                         field(SoundView.field(TableColumns.SoundView.IS_ALBUM)).as(IS_ALBUM.name()),
                         field("(" + PlaylistQueries.LOCAL_TRACK_COUNT.build() + ")").as(LOCAL_TRACK_COUNT.name()),
                         exists(likeQuery()).as(IS_USER_LIKE.name()),
                         exists(repostQuery()).as(IS_USER_REPOST.name()),
                         exists(PlaylistQueries.HAS_PENDING_DOWNLOAD_REQUEST_QUERY).as(HAS_PENDING_DOWNLOAD_REQUEST.name()),
                         exists(PlaylistQueries.HAS_DOWNLOADED_OFFLINE_TRACKS_FILTER).as(HAS_DOWNLOADED_TRACKS.name()),
                         exists(PlaylistQueries.HAS_UNAVAILABLE_OFFLINE_TRACKS_FILTER).as(HAS_UNAVAILABLE_TRACKS.name()),
                         exists(PlaylistQueries.IS_MARKED_FOR_OFFLINE_QUERY).as(IS_MARKED_FOR_OFFLINE.name()))
                 .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Tables.Sounds.TYPE_PLAYLIST);

    public static final String SQL_VERSION_0_TO_116 = "CREATE VIEW IF NOT EXISTS PlaylistView AS " +
            Query.from(SoundView.name())
                 .select(field(SoundView.field(TableColumns.SoundView._ID)).as(ID.name()),
                         field(SoundView.field(TableColumns.SoundView.TITLE)).as(TITLE.name()),
                         field(SoundView.field(TableColumns.SoundView.USERNAME)).as(USERNAME.name()),
                         field(SoundView.field(TableColumns.SoundView.USER_ID)).as(USER_ID.name()),
                         field(SoundView.field(TableColumns.SoundView.TRACK_COUNT)).as(TRACK_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.DURATION)).as(DURATION.name()),
                         field(SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(LIKES_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.REPOSTS_COUNT)).as(REPOSTS_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.SHARING)).as(SHARING.name()),
                         field(SoundView.field(TableColumns.SoundView.ARTWORK_URL)).as(ARTWORK_URL.name()),
                         field(SoundView.field(TableColumns.SoundView.PERMALINK_URL)).as(PERMALINK_URL.name()),
                         field(SoundView.field(TableColumns.SoundView.GENRE)).as(GENRE.name()),
                         field(SoundView.field(TableColumns.SoundView.TAG_LIST)).as(TAG_LIST.name()),
                         field(SoundView.field(TableColumns.SoundView.CREATED_AT)).as(CREATED_AT.name()),
                         field(SoundView.field(TableColumns.SoundView.RELEASE_DATE)).as(RELEASE_DATE.name()),
                         field(SoundView.field(TableColumns.SoundView.SET_TYPE)).as(SET_TYPE.name()),
                         field(SoundView.field(TableColumns.SoundView.IS_ALBUM)).as(IS_ALBUM.name()),
                         field("(" + PlaylistQueries.LOCAL_TRACK_COUNT.build() + ")").as(LOCAL_TRACK_COUNT.name()),
                         exists(likeQuery()).as(IS_USER_LIKE.name()),
                         exists(repostQuery()).as(IS_USER_REPOST.name()),
                         exists(PlaylistQueries.HAS_PENDING_DOWNLOAD_REQUEST_QUERY).as(HAS_PENDING_DOWNLOAD_REQUEST.name()),
                         exists(PlaylistQueries.HAS_DOWNLOADED_OFFLINE_TRACKS_FILTER).as(HAS_DOWNLOADED_TRACKS.name()),
                         exists(PlaylistQueries.HAS_UNAVAILABLE_OFFLINE_TRACKS_FILTER).as(HAS_UNAVAILABLE_TRACKS.name()),
                         exists(PlaylistQueries.IS_MARKED_FOR_OFFLINE_QUERY).as(IS_MARKED_FOR_OFFLINE.name()))
                 .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Tables.Sounds.TYPE_PLAYLIST);

    private static Query creatorIsProQuery() {
        final Where joinConditions = filter()
                .whereEq(Table.SoundView.field(TableColumns.SoundView.USER_ID), Tables.Users._ID.qualifiedName());

        return Query.from(Tables.Users.TABLE)
                    .innerJoin(Tables.Sounds.TABLE, joinConditions)
                    .select(Tables.Users.IS_PRO.qualifiedName());
    }

    private static Query likeQuery() {
        final Where joinConditions = filter()
                .whereEq(Table.SoundView.field(TableColumns.SoundView._ID), Tables.Likes._ID)
                .whereEq(Table.SoundView.field(TableColumns.SoundView._TYPE), Tables.Likes._TYPE);

        return Query.from(Tables.Likes.TABLE)
                    // do not use SoundView here. The exists query will fail, in spite of passing tests
                    .innerJoin(Tables.Sounds.TABLE, joinConditions)
                    .whereNull(Tables.Likes.REMOVED_AT);
    }

    private static Query repostQuery() {
        final Where joinConditions = filter()
                .whereEq(Table.SoundView.field(TableColumns.SoundView._ID), Tables.Posts.TARGET_ID)
                .whereEq(Table.SoundView.field(TableColumns.SoundView._TYPE), Tables.Posts.TARGET_TYPE);

        return Query.from(Tables.Posts.TABLE)
                    .innerJoin(Tables.Sounds.TABLE, joinConditions)
                    .whereEq(Tables.Sounds._TYPE.qualifiedName(), Tables.Sounds.TYPE_PLAYLIST)
                    .whereEq(Tables.Posts.TYPE.qualifiedName(), typeRepostDelimited());
    }

    private static String typeRepostDelimited() {
        return "'" + Tables.Posts.TYPE_REPOST + "'";
    }

}
