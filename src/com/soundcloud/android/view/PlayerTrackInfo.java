package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.TrackFavoriters;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.ClickSpan;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Collections;

public class PlayerTrackInfo extends RelativeLayout{

    private final ScPlayer mPlayer;
    private final FlowLayout mTrackTags;
    private final TextView mFavoritersTxt;
    private final TextView mCommentersTxt;
    private Track mPlayingTrack;

    private boolean mTrackInfoFilled;


    public PlayerTrackInfo(ScPlayer player) {
        super(player);

        LayoutInflater inflater = (LayoutInflater) player
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.track_info, this);

        mPlayer = player;

        mTrackTags = (FlowLayout) findViewById(R.id.tags_holder);
        mFavoritersTxt = (TextView) findViewById(R.id.favoriters_txt);
        mFavoritersTxt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayingTrack != null) {
                    Intent i = new Intent(mPlayer, TrackFavoriters.class);
                    i.putExtra("track_id", mPlayingTrack.id);
                    mPlayer.startActivity(i);
                }
            }
        });

        mCommentersTxt = (TextView) findViewById(R.id.commenters_txt);
        mCommentersTxt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayingTrack != null) {
                    Intent i = new Intent(mPlayer, TrackFavoriters.class);
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
        if (!mPlayingTrack.info_loaded) {
            if (findViewById(R.id.loading_layout) != null) {
                findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.stub_loading).setVisibility(View.VISIBLE);
            }

            findViewById(R.id.info_view).setVisibility(View.GONE);

            if (findViewById(android.R.id.empty) != null) {
                findViewById(android.R.id.empty).setVisibility(View.GONE);
            }
        } else {

            if (mPlayingTrack.favoritings_count == 0) {
                mFavoritersTxt.setVisibility(View.GONE);
            } else {
                mFavoritersTxt.setVisibility(View.VISIBLE);
                mFavoritersTxt.setText(getResources().getQuantityString(R.plurals.track_info_favoriters,
                        mPlayingTrack.favoritings_count,mPlayingTrack.favoritings_count));
            }

            if (mPlayingTrack.comment_count == 0) {
                mCommentersTxt.setVisibility(View.GONE);
            } else {
                mCommentersTxt.setVisibility(View.VISIBLE);
                mCommentersTxt.setText(getResources().getQuantityString(R.plurals.track_info_commenters,
                        mPlayingTrack.comment_count,mPlayingTrack.comment_count));
            }

            mTrackTags.removeAllViews();
            mPlayingTrack.fillTags(mTrackTags, mPlayer);

            TextView txtInfo = (TextView) findViewById(R.id.txtInfo);
            txtInfo.setText(Html.fromHtml(mPlayingTrack.trackInfo()));
            Linkify.addLinks(txtInfo, Linkify.WEB_URLS);

            // for some reason this needs to be set to support links
            // http://www.mail-archive.com/android-beginners@googlegroups.com/msg04465.html
            MovementMethod mm = txtInfo.getMovementMethod();
            if (!(mm instanceof LinkMovementMethod)) {
                txtInfo.setMovementMethod(LinkMovementMethod.getInstance());
            }
            mTrackInfoFilled = true;
        }
    }



    public void onInfoLoadError() {
        if (findViewById(R.id.loading_layout) != null) {
            findViewById(R.id.loading_layout).setVisibility(View.GONE);
        }

        if (findViewById(android.R.id.empty) != null) {
            findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else {
            addView(CloudUtils.buildEmptyView(mPlayer,getResources().getString(R.string.info_error)), getChildCount() - 2);
        }
        findViewById(R.id.info_view).setVisibility(View.GONE);
    }

    public void onInfoLoadSuccess() {
        fillTrackDetails();
        if (findViewById(R.id.loading_layout) != null) {
            findViewById(R.id.loading_layout).setVisibility(View.GONE);
        }
        if (findViewById(android.R.id.empty) != null) {
            findViewById(android.R.id.empty).setVisibility(View.GONE);
        }
        findViewById(R.id.info_view).setVisibility(View.VISIBLE);
    }
}
