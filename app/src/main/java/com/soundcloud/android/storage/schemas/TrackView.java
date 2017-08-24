package com.soundcloud.android.storage.schemas;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.TableColumns.ResourceTable._TYPE;
import static com.soundcloud.android.storage.Tables.TrackView.ARTWORK_URL;
import static com.soundcloud.android.storage.Tables.TrackView.BLOCKED;
import static com.soundcloud.android.storage.Tables.TrackView.COMMENTS_COUNT;
import static com.soundcloud.android.storage.Tables.TrackView.CREATED_AT;
import static com.soundcloud.android.storage.Tables.TrackView.CREATOR_ID;
import static com.soundcloud.android.storage.Tables.TrackView.CREATOR_IS_PRO;
import static com.soundcloud.android.storage.Tables.TrackView.CREATOR_NAME;
import static com.soundcloud.android.storage.Tables.TrackView.FULL_DURATION;
import static com.soundcloud.android.storage.Tables.TrackView.GENRE;
import static com.soundcloud.android.storage.Tables.TrackView.ID;
import static com.soundcloud.android.storage.Tables.TrackView.IS_COMMENTABLE;
import static com.soundcloud.android.storage.Tables.TrackView.IS_USER_LIKE;
import static com.soundcloud.android.storage.Tables.TrackView.IS_USER_REPOST;
import static com.soundcloud.android.storage.Tables.TrackView.LIKES_COUNT;
import static com.soundcloud.android.storage.Tables.TrackView.MONETIZABLE;
import static com.soundcloud.android.storage.Tables.TrackView.MONETIZATION_MODEL;
import static com.soundcloud.android.storage.Tables.TrackView.OFFLINE_DOWNLOADED_AT;
import static com.soundcloud.android.storage.Tables.TrackView.OFFLINE_REMOVED_AT;
import static com.soundcloud.android.storage.Tables.TrackView.OFFLINE_REQUESTED_AT;
import static com.soundcloud.android.storage.Tables.TrackView.OFFLINE_UNAVAILABLE_AT;
import static com.soundcloud.android.storage.Tables.TrackView.PERMALINK_URL;
import static com.soundcloud.android.storage.Tables.TrackView.PLAY_COUNT;
import static com.soundcloud.android.storage.Tables.TrackView.POLICY;
import static com.soundcloud.android.storage.Tables.TrackView.REPOSTS_COUNT;
import static com.soundcloud.android.storage.Tables.TrackView.SHARING;
import static com.soundcloud.android.storage.Tables.TrackView.SNIPPED;
import static com.soundcloud.android.storage.Tables.TrackView.SNIPPET_DURATION;
import static com.soundcloud.android.storage.Tables.TrackView.SUB_HIGH_TIER;
import static com.soundcloud.android.storage.Tables.TrackView.SUB_MID_TIER;
import static com.soundcloud.android.storage.Tables.TrackView.TAG_LIST;
import static com.soundcloud.android.storage.Tables.TrackView.TITLE;
import static com.soundcloud.android.storage.Tables.TrackView.WAVEFORM_URL;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

public class TrackView {

    public static final String SQL_VERSION_117 = "CREATE VIEW IF NOT EXISTS TrackView AS " +
            Query.from(SoundView.name())
                 .select(field(SoundView.field(TableColumns.SoundView._ID)).as(ID.name()),
                         field(SoundView.field(TableColumns.SoundView.CREATED_AT)).as(CREATED_AT.name()),
                         field(SoundView.field(TableColumns.SoundView.TITLE)).as(TITLE.name()),
                         field(SoundView.field(TableColumns.SoundView.USERNAME)).as(CREATOR_NAME.name()),
                         field(SoundView.field(TableColumns.SoundView.USER_ID)).as(CREATOR_ID.name()),
                         field("(" + creatorIsProQuery().build() + ")").as(CREATOR_IS_PRO.name()),
                         field(SoundView.field(TableColumns.SoundView.PERMALINK_URL)).as(PERMALINK_URL.name()),
                         field(SoundView.field(TableColumns.SoundView.WAVEFORM_URL)).as(WAVEFORM_URL.name()),
                         field(SoundView.field(TableColumns.SoundView.SNIPPET_DURATION)).as(SNIPPET_DURATION.name()),
                         field(SoundView.field(TableColumns.SoundView.FULL_DURATION)).as(FULL_DURATION.name()),

                         field(SoundView.field(TableColumns.SoundView.GENRE)).as(GENRE.name()),
                         field(SoundView.field(TableColumns.SoundView.TAG_LIST)).as(TAG_LIST.name()),

                         field(SoundView.field(TableColumns.SoundView.PLAYBACK_COUNT)).as(PLAY_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(LIKES_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.REPOSTS_COUNT)).as(REPOSTS_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.COMMENT_COUNT)).as(COMMENTS_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.COMMENTABLE)).as(IS_COMMENTABLE.name()),
                         field(SoundView.field(TableColumns.SoundView.SHARING)).as(SHARING.name()),

                         field(SoundView.field(TableColumns.SoundView.POLICIES_POLICY)).as(POLICY.name()),
                         field(SoundView.field(TableColumns.SoundView.POLICIES_MONETIZABLE)).as(MONETIZABLE.name()),
                         field(SoundView.field(TableColumns.SoundView.POLICIES_MONETIZATION_MODEL)).as(MONETIZATION_MODEL.name()),
                         field(SoundView.field(TableColumns.SoundView.POLICIES_BLOCKED)).as(BLOCKED.name()),
                         field(SoundView.field(TableColumns.SoundView.POLICIES_SNIPPED)).as(SNIPPED.name()),
                         field(SoundView.field(TableColumns.SoundView.POLICIES_SUB_HIGH_TIER)).as(SUB_HIGH_TIER.name()),
                         field(SoundView.field(TableColumns.SoundView.POLICIES_SUB_MID_TIER)).as(SUB_MID_TIER.name()),

                         field(SoundView.field(TableColumns.SoundView.OFFLINE_DOWNLOADED_AT)).as(OFFLINE_DOWNLOADED_AT.name()),
                         field(SoundView.field(TableColumns.SoundView.OFFLINE_REMOVED_AT)).as(OFFLINE_REMOVED_AT.name()),
                         field(SoundView.field(TableColumns.SoundView.OFFLINE_REQUESTED_AT)).as(OFFLINE_REQUESTED_AT.name()),
                         field(SoundView.field(TableColumns.SoundView.OFFLINE_UNAVAILABLE_AT)).as(OFFLINE_UNAVAILABLE_AT.name()),

                         field(SoundView.field(TableColumns.SoundView.ARTWORK_URL)).as(ARTWORK_URL.name()),

                         field(Tables.Likes._ID.qualifiedName() + " IS NOT NULL").as(IS_USER_LIKE.name()),
                         field(Tables.Posts.TYPE.qualifiedName() + " IS NOT NULL").as(IS_USER_REPOST.name()))

                 .leftJoin(Tables.Likes.TABLE, getLikeJoinConditions())
                 .leftJoin(Tables.Posts.TABLE, getRepostJoinConditions())
                 .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Tables.Sounds.TYPE_TRACK);

    public static final String SQL_VERSION_0_TO_116 = "CREATE VIEW IF NOT EXISTS TrackView AS " +
            Query.from(SoundView.name())
                 .select(field(SoundView.field(TableColumns.SoundView._ID)).as(ID.name()),
                         field(SoundView.field(TableColumns.SoundView.CREATED_AT)).as(CREATED_AT.name()),
                         field(SoundView.field(TableColumns.SoundView.TITLE)).as(TITLE.name()),
                         field(SoundView.field(TableColumns.SoundView.USERNAME)).as(CREATOR_NAME.name()),
                         field(SoundView.field(TableColumns.SoundView.USER_ID)).as(CREATOR_ID.name()),
                         field(SoundView.field(TableColumns.SoundView.PERMALINK_URL)).as(PERMALINK_URL.name()),
                         field(SoundView.field(TableColumns.SoundView.WAVEFORM_URL)).as(WAVEFORM_URL.name()),
                         field(SoundView.field(TableColumns.SoundView.SNIPPET_DURATION)).as(SNIPPET_DURATION.name()),
                         field(SoundView.field(TableColumns.SoundView.FULL_DURATION)).as(FULL_DURATION.name()),

                         field(SoundView.field(TableColumns.SoundView.GENRE)).as(GENRE.name()),
                         field(SoundView.field(TableColumns.SoundView.TAG_LIST)).as(TAG_LIST.name()),

                         field(SoundView.field(TableColumns.SoundView.PLAYBACK_COUNT)).as(PLAY_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(LIKES_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.REPOSTS_COUNT)).as(REPOSTS_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.COMMENT_COUNT)).as(COMMENTS_COUNT.name()),
                         field(SoundView.field(TableColumns.SoundView.COMMENTABLE)).as(IS_COMMENTABLE.name()),
                         field(SoundView.field(TableColumns.SoundView.SHARING)).as(SHARING.name()),

                         field(SoundView.field(TableColumns.SoundView.POLICIES_POLICY)).as(POLICY.name()),
                         field(SoundView.field(TableColumns.SoundView.POLICIES_MONETIZABLE)).as(MONETIZABLE.name()),
                         field(SoundView.field(TableColumns.SoundView.POLICIES_MONETIZATION_MODEL)).as(
                                 MONETIZATION_MODEL.name()),
                         field(SoundView.field(TableColumns.SoundView.POLICIES_BLOCKED)).as(BLOCKED.name()),
                         field(SoundView.field(TableColumns.SoundView.POLICIES_SNIPPED)).as(SNIPPED.name()),
                         field(SoundView.field(TableColumns.SoundView.POLICIES_SUB_HIGH_TIER)).as(SUB_HIGH_TIER.name()),
                         field(SoundView.field(TableColumns.SoundView.POLICIES_SUB_MID_TIER)).as(SUB_MID_TIER.name()),

                         field(SoundView.field(TableColumns.SoundView.OFFLINE_DOWNLOADED_AT)).as(OFFLINE_DOWNLOADED_AT.name()),
                         field(SoundView.field(TableColumns.SoundView.OFFLINE_REMOVED_AT)).as(OFFLINE_REMOVED_AT.name()),
                         field(SoundView.field(TableColumns.SoundView.OFFLINE_REQUESTED_AT)).as(OFFLINE_REQUESTED_AT.name()),
                         field(SoundView.field(TableColumns.SoundView.OFFLINE_UNAVAILABLE_AT)).as(OFFLINE_UNAVAILABLE_AT.name()),

                         field(SoundView.field(TableColumns.SoundView.ARTWORK_URL)).as(ARTWORK_URL.name()),

                         field(Tables.Likes._ID.qualifiedName() + " IS NOT NULL").as(IS_USER_LIKE.name()),
                         field(Tables.Posts.TYPE.qualifiedName() + " IS NOT NULL").as(IS_USER_REPOST.name()))

                 .leftJoin(Tables.Likes.TABLE, getLikeJoinConditions())
                 .leftJoin(Tables.Posts.TABLE, getRepostJoinConditions())
                 .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Tables.Sounds.TYPE_TRACK);

    private static Query creatorIsProQuery() {
        final Where joinConditions = filter()
                .whereEq(Table.SoundView.field(TableColumns.SoundView.USER_ID), Tables.Users._ID.qualifiedName());

        return Query.from(Tables.Users.TABLE)
                    .innerJoin(Tables.Sounds.TABLE, joinConditions)
                    .select(Tables.Users.IS_PRO.qualifiedName());
    }

    private static Where getLikeJoinConditions() {
        return Filter.filter()
                     .whereEq(Table.SoundView.field(_ID), Tables.Likes._ID)
                     .whereEq(Table.SoundView.field(_TYPE), Tables.Likes._TYPE)
                     .whereNull(Tables.Likes.REMOVED_AT);
    }

    private static Where getRepostJoinConditions() {
        return Filter.filter()
                     .whereEq(Table.SoundView.field(_ID), Tables.Posts.TARGET_ID)
                     .whereEq(Table.SoundView.field(_TYPE), Tables.Posts.TARGET_TYPE)
                     .whereEq(Tables.Posts.TYPE.qualifiedName(), typeRepostDelimited());
    }

    private static String typeRepostDelimited() {
        return "'" + Tables.Posts.TYPE_REPOST + "'";
    }
}
