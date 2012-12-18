package com.soundcloud.android.view.play;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.track.TrackComments;
import com.soundcloud.android.activity.track.TrackLikers;
import com.soundcloud.android.activity.track.TrackReposters;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.FlowLayout;
import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.nfc.Tag;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.List;

public class PlayerTrackDetails extends RelativeLayout {
    private final ScPlayer   mPlayer;
    private final FlowLayout mTrackTags;

    private final TableRow mLikersRow;
    private final TableRow mRepostersRow;
    private final TableRow mCommentersRow;

    private final TableRow mLikersDivider;
    private final TableRow mRepostersDivider;
    private final TableRow mCommentersDivider;

    private final TextView mLikersText;
    private final TextView mRepostersText;
    private final TextView mCommentersText;
    private final TextView mTxtInfo;

    private @Nullable long mTrackId;
    private @Nullable TagsHolder mLastTags;

    public PlayerTrackDetails(ScPlayer player) {
        super(player);
        View.inflate(player, R.layout.track_info, this);
        setBackgroundColor(0xFFFFFFFF);

        mPlayer = player;
        mTrackTags = (FlowLayout) findViewById(R.id.tags_holder);

        mLikersText    = (TextView) findViewById(R.id.likers_txt);
        mLikersRow     = (TableRow) findViewById(R.id.likers_row);
        mLikersDivider = (TableRow) findViewById(R.id.likers_divider);

        mLikersRow.setVisibility(View.GONE);
        mLikersDivider.setVisibility(View.GONE);

        mLikersRow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTrackId > 0) {
                    Intent i = new Intent(mPlayer, TrackLikers.class);
                    i.putExtra("track_id", mTrackId);
                    mPlayer.startActivity(i);
                }
            }
        });

        mRepostersText    = (TextView) findViewById(R.id.reposters_txt);
        mRepostersRow     = (TableRow) findViewById(R.id.reposters_row);
        mRepostersDivider = (TableRow) findViewById(R.id.reposters_divider);

        mRepostersRow.setVisibility(View.GONE);
        mRepostersDivider.setVisibility(View.GONE);

        mRepostersRow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTrackId > 0) {
                    Intent i = new Intent(mPlayer, TrackReposters.class);
                    i.putExtra("track_id", mTrackId);
                    mPlayer.startActivity(i);
                }
            }
        });

        mCommentersText    = (TextView) findViewById(R.id.commenters_txt);
        mCommentersRow     = (TableRow) findViewById(R.id.comments_row);
        mCommentersDivider = (TableRow) findViewById(R.id.comments_divider);

        mCommentersRow.setVisibility(View.GONE);
        mCommentersDivider.setVisibility(View.GONE);

        mCommentersRow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTrackId > 0) {
                    Intent i = new Intent(mPlayer, TrackComments.class);
                    i.putExtra("track_id", mTrackId);
                    mPlayer.startActivity(i);
                }
            }
        });

        mTxtInfo = (TextView) findViewById(R.id.txtInfo);
    }

    public void fillTrackDetails(Track track, boolean showLoading) {
        if (track == null){
            mTrackId = -1;
            return;
        }

        mTrackId = track.id;

        setViewVisibility(track.likes_count > 0, mLikersRow, mLikersDivider);
        mLikersText.setText(getResources().getQuantityString(R.plurals.track_info_likers,
                track.likes_count,
                track.likes_count));

        setViewVisibility(track.reposts_count > 0, mRepostersRow, mRepostersDivider);
        mRepostersText.setText(getResources().getQuantityString(R.plurals.track_info_reposters,
                track.reposts_count,
                track.reposts_count));

        setViewVisibility(track.comment_count > 0, mCommentersRow, mCommentersDivider);
        mCommentersText.setText(getResources().getQuantityString(R.plurals.track_info_commenters,
                track.comment_count,
                track.comment_count));


        // check for equality to avoid extra view inflation
        if (mLastTags == null || !mLastTags.hasSameTags(track)) {
            mTrackTags.removeAllViews();
            track.fillTags(mTrackTags, mPlayer);
            mLastTags = new TagsHolder(track);
        }

        final String trackInfo = track.trackInfo();
        if (!TextUtils.isEmpty(trackInfo)) {
            mTxtInfo.setGravity(Gravity.LEFT);
            mTxtInfo.setText(ScTextUtils.fromHtml(trackInfo));
            Linkify.addLinks(mTxtInfo, Linkify.WEB_URLS);

            // for some reason this needs to be set to support links
            // http://www.mail-archive.com/android-beginners@googlegroups.com/msg04465.html
            MovementMethod mm = mTxtInfo.getMovementMethod();
            if (!(mm instanceof LinkMovementMethod)) {
                mTxtInfo.setMovementMethod(LinkMovementMethod.getInstance());
            }
        } else {
            if (!showLoading){
                mTxtInfo.setText(R.string.no_info_available);
                mTxtInfo.setGravity(Gravity.CENTER_HORIZONTAL);
            } else {
                mTxtInfo.setText("");
            }
        }

        if (showLoading) {
            if (findViewById(R.id.loading_layout) != null) {
                findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.stub_loading).setVisibility(View.VISIBLE);
            }
        } else if (findViewById(R.id.loading_layout) != null) {
            findViewById(R.id.loading_layout).setVisibility(View.GONE);
        }
    }

    private static void setViewVisibility(boolean visible, View... views) {
        int visibility = visible ? VISIBLE : GONE;

        for (View view : views) {
            view.setVisibility(visibility);
        }
    }

    public static class TagsHolder {
        String genre;
        List<String> humanTags;

        public TagsHolder(Track track) {
            this.genre = track.genre;
            this.humanTags = track.humanTags();
        }

        public boolean hasSameTags(Track track){
            return track != null && TextUtils.equals(track.genre, genre) && humanTags.equals(track.humanTags());
        }
    }
}
