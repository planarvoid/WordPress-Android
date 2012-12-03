package com.soundcloud.android.view.play;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;

import android.widget.TableRow;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.track.TrackComments;
import com.soundcloud.android.activity.track.TrackLikers;
import com.soundcloud.android.activity.track.TrackReposters;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.FlowLayout;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

    private @Nullable Track mPlayingTrack;

    private boolean mTrackInfoFilled;

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
                if (mPlayingTrack != null) {
                    Intent i = new Intent(mPlayer, TrackLikers.class);
                    i.putExtra("track_id", mPlayingTrack.id);
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
                if (mPlayingTrack != null) {
                    Intent i = new Intent(mPlayer, TrackReposters.class);
                    i.putExtra("track_id", mPlayingTrack.id);
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
                if (mPlayingTrack != null) {
                    Intent i = new Intent(mPlayer, TrackComments.class);
                    i.putExtra("track_id", mPlayingTrack.id);
                    mPlayer.startActivity(i);
                }
            }
        });
    }

    public void setPlayingTrack(Track t){
        if (mPlayingTrack == null || mPlayingTrack.id != t.id){
            mTrackInfoFilled = false;
        }
        mPlayingTrack = t;
    }

    public boolean getIsTrackInfoFilled(){
        return mTrackInfoFilled;
    }

    public void fillTrackDetails() {
        if (mPlayingTrack == null) return;

        setViewVisibility(mPlayingTrack.likes_count > 0, mLikersRow, mLikersDivider);
        mLikersText.setText(getResources().getQuantityString(R.plurals.track_info_likers,
                mPlayingTrack.likes_count,
                mPlayingTrack.likes_count));

        setViewVisibility(mPlayingTrack.reposts_count > 0, mRepostersRow, mRepostersDivider);
        mRepostersText.setText(getResources().getQuantityString(R.plurals.track_info_reposters,
                                                                mPlayingTrack.reposts_count,
                                                                mPlayingTrack.reposts_count));

        setViewVisibility(mPlayingTrack.comment_count > 0, mCommentersRow, mCommentersDivider);
        mCommentersText.setText(getResources().getQuantityString(R.plurals.track_info_commenters,
                                                                 mPlayingTrack.comment_count,
                                                                 mPlayingTrack.comment_count));

        mTrackTags.removeAllViews();
        mPlayingTrack.fillTags(mTrackTags, mPlayer);

        TextView txtInfo = (TextView) findViewById(R.id.txtInfo);
        if (txtInfo != null && mPlayingTrack != null) { // should never be null, but sure enough it is in rare cases. Maybe not inflated yet??
            txtInfo.setText(ScTextUtils.fromHtml(mPlayingTrack.trackInfo()));
            Linkify.addLinks(txtInfo, Linkify.WEB_URLS);

            // for some reason this needs to be set to support links
            // http://www.mail-archive.com/android-beginners@googlegroups.com/msg04465.html
            MovementMethod mm = txtInfo.getMovementMethod();
            if (!(mm instanceof LinkMovementMethod)) {
                txtInfo.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
        mTrackInfoFilled = true;
    }

    private static void setViewVisibility(boolean visible, View... views) {
        int visibility = visible ? VISIBLE : GONE;

        for (View view : views) {
            view.setVisibility(visibility);
        }
    }

    public void onInfoLoadError() {
        if (findViewById(R.id.loading_layout) != null) {
            findViewById(R.id.loading_layout).setVisibility(View.GONE);
        }

        if (findViewById(android.R.id.empty) != null) {
            findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else {
            addView(buildEmptyView(mPlayer, getResources().getString(R.string.info_error)), getChildCount() - 2);
        }
        if (findViewById(R.id.info_view) != null){
            findViewById(R.id.info_view).setVisibility(View.GONE);
        }
    }

    public void onInfoLoadSuccess() {
        fillTrackDetails();
        final View loading = findViewById(R.id.loading_layout);
        if (loading != null) {
            findViewById(R.id.loading_layout).setVisibility(View.GONE);
        }
        final int empty = android.R.id.empty;
        if (findViewById(empty) != null) {
            findViewById(android.R.id.empty).setVisibility(View.GONE);
        }

        final View infoView = findViewById(R.id.info_view);
        if (infoView != null) {
            findViewById(R.id.info_view).setVisibility(View.VISIBLE);
        }
    }

    private static TextView buildEmptyView(Context context, CharSequence emptyText) {
        TextView emptyView = new TextView(context);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(FILL_PARENT, FILL_PARENT));
        emptyView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        emptyView.setTextAppearance(context, R.style.txt_empty_view);
        emptyView.setText(emptyText);
        emptyView.setId(android.R.id.empty);
        return emptyView;
    }
}
