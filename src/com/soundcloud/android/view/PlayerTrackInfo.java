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
    private Track mPlayingTrack;

    private boolean mTrackInfoFilled;
    private boolean mTrackInfoCommentsFilled;


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

    }

    public void setPlayingTrack(Track t){
        if (mPlayingTrack == null || mPlayingTrack.id != t.id){
            mTrackInfoFilled = false;
            mTrackInfoCommentsFilled = false;
        }
        mPlayingTrack = t;
    }

    public boolean getIsTrackInfoFilled(){
        return mTrackInfoFilled;
    }

    public boolean getIsTrackInfoCommentsFilled(){
        return mTrackInfoCommentsFilled;
    }

    public void clearIsTrackInfoFilled(){
        mTrackInfoFilled = false;
    }

    public void clearIsTrackInfoCommentsFilled(){
        mTrackInfoCommentsFilled = false;
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
            fillTrackInfoComments();
            mTrackInfoFilled = true;
        }
    }

    public void fillTrackInfoComments() {
        LinearLayout commentsList;
        if (findViewById(R.id.comments_list) == null) {
            commentsList = (LinearLayout) ((ViewStub)findViewById(R.id.stub_comments_list)).inflate();
            commentsList.findViewById(R.id.btn_info_comment).setOnClickListener(
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mPlayer.addNewComment(CloudUtils.buildComment(mPlayer, mPlayer.getCurrentUserId(), mPlayingTrack.id,
                                    -1, "", 0));
                        }
                    });
        } else {
            commentsList = (LinearLayout) findViewById(R.id.comments_list);
            while (commentsList.getChildCount() > 1) {
                commentsList.removeViewAt(1);
            }
        }

        if (mPlayingTrack.comments == null) return;

        //sort by created date descending for this list
        Collections.sort(mPlayingTrack.comments, Comment.CompareCreatedAt.INSTANCE);

        final SpannableStringBuilder commentText = new SpannableStringBuilder();
        final ForegroundColorSpan fcs = new ForegroundColorSpan(getResources().getColor(R.color.commentGray));
        final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);

        int spanStartIndex;
        int spanEndIndex;

        for (final Comment comment : mPlayingTrack.comments){
            commentText.clear();

            View v = new View(mPlayer);
            v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,1));
            v.setBackgroundColor(R.color.background_dark);
            commentsList.addView(v);

            TextView tv = new TextView(mPlayer);
            tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setPadding(10, 5, 10, 5);
            tv.setTextSize(14);
            tv.setLineSpacing(5, 1);

            if (comment.user != null && comment.user.username != null) {
                commentText.append(comment.user.username).append(' ');
            }

            spanEndIndex = commentText.length();
            commentText.setSpan(bss, 0, spanEndIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (comment.timestamp > 0) {
                commentText.append(" ").append(CloudUtils.formatTimestamp(comment.timestamp)).append(" ");
            }

            spanStartIndex = commentText.length();
            commentText.append(" said ").append(CloudUtils.getTimeElapsed(getResources(), comment.created_at.getTime()));

            spanEndIndex = commentText.length();
            commentText.setSpan(fcs, spanStartIndex, spanEndIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            commentText.append("\n").append(comment.body);

            tv.setText(commentText);
            commentsList.addView(tv);

            if (comment.user != null && comment.user.username != null) {
                tv.setLinkTextColor(getResources().getColorStateList(R.drawable.txt_dark_states));
                CloudUtils.clickify(tv, comment.user.username, new ClickSpan.OnClickListener(){
                    @Override
                    public void onClick() {
                        Intent intent = new Intent(mPlayer, UserBrowser.class);
                        intent.putExtra("user", comment.user);
                        mPlayer.startActivity(intent);
                    }
                }, false);
            }
        }
        //restore default sort
        Collections.sort(mPlayingTrack.comments, Comment.CompareTimestamp.INSTANCE);
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
