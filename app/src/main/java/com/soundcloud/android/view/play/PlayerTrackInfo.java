package com.soundcloud.android.view.play;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.track.TrackComments;
import com.soundcloud.android.activity.track.TrackLikers;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.FlowLayout;

import android.content.Context;
import android.content.Intent;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PlayerTrackInfo extends RelativeLayout{

    private final ScPlayer mPlayer;
    private final FlowLayout mTrackTags;
    private final TextView mLikersText;
    private final View mLikersHr;
    private final TextView mCommentersTxt;
    private final View mCommentersHr;
    private Track mPlayingTrack;

    private boolean mTrackInfoFilled;

    public PlayerTrackInfo(ScPlayer player) {
        super(player);

        LayoutInflater inflater = (LayoutInflater) player
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.track_info, this);

        setBackgroundColor(0xFFFFFFFF);

        mPlayer = player;

        mTrackTags = (FlowLayout) findViewById(R.id.tags_holder);
        mLikersText = (TextView) findViewById(R.id.likers_txt);
        mLikersHr = findViewById(R.id.likers_hr);
        mLikersText.setVisibility(View.GONE);
        mLikersHr.setVisibility(View.GONE);
        mLikersText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayingTrack != null) {
                    Intent i = new Intent(mPlayer, TrackLikers.class);
                    i.putExtra("track_id", mPlayingTrack.id);
                    mPlayer.startActivity(i);
                }
            }
        });

        mCommentersTxt = (TextView) findViewById(R.id.commenters_txt);
        mCommentersHr = findViewById(R.id.commenters_hr);
        mCommentersTxt.setVisibility(View.GONE);
        mCommentersHr.setVisibility(View.GONE);
        mCommentersTxt.setOnClickListener(new OnClickListener() {
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

    public void clearIsTrackInfoFilled(){
        mTrackInfoFilled = false;
    }


    public void fillTrackDetails() {

        if (mPlayingTrack == null) return;


        if (mPlayingTrack.favoritings_count == 0) {
            mLikersText.setVisibility(View.GONE);
            mLikersHr.setVisibility(View.GONE);
        } else {
            mLikersText.setVisibility(View.VISIBLE);
            mLikersHr.setVisibility(View.VISIBLE);
            mLikersText.setText(getResources().getQuantityString(R.plurals.track_info_likers,
                    mPlayingTrack.favoritings_count, mPlayingTrack.favoritings_count));
        }

        if (mPlayingTrack.comment_count == 0) {
            mCommentersTxt.setVisibility(View.GONE);
            mCommentersHr.setVisibility(View.GONE);
        } else {
            mCommentersTxt.setVisibility(View.VISIBLE);
            mCommentersHr.setVisibility(View.VISIBLE);
            mCommentersTxt.setText(getResources().getQuantityString(R.plurals.track_info_commenters,
                    mPlayingTrack.comment_count, mPlayingTrack.comment_count));
        }

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
