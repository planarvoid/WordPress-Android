package com.soundcloud.android.playback.views;

import static com.soundcloud.android.associations.PlayableInteractionActivity.EXTRA_INTERACTION_TYPE;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.TrackInteractionActivity;
import com.soundcloud.android.search.SearchByTagActivity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.activities.Activity;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.FlowLayout;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.List;

public class PlayerTrackDetailsLayout extends LinearLayout {
    private View mInfoView;
    private ViewGroup mTrackTags;
    private TableRow mLikersRow;
    private TableRow mRepostersRow;
    private TableRow mCommentersRow;
    private TableRow mLikersDivider;
    private TableRow mRepostersDivider;
    private TableRow mCommentersDivider;
    private TextView mLikersText;
    private TextView mRepostersText;
    private TextView mCommentersText;
    private TextView mTxtInfo;

    private long mTrackId;
    private @Nullable TagsHolder mLastTags;

    public PlayerTrackDetailsLayout(Context context) {
        super(context);
        init(context);
    }
    public PlayerTrackDetailsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        View.inflate(context, R.layout.track_info, this);
        setBackgroundResource(R.color.playerControlBackground);
        setOrientation(VERTICAL);

        mInfoView = findViewById(R.id.info_view);

        mTrackTags = (ViewGroup) findViewById(R.id.tags_holder);

        mLikersText    = (TextView) findViewById(R.id.likers_txt);
        mLikersRow     = (TableRow) findViewById(R.id.likers_row);
        mLikersDivider = (TableRow) findViewById(R.id.likers_divider);

        mLikersRow.setVisibility(View.GONE);
        mLikersDivider.setVisibility(View.GONE);

        mLikersRow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openInteractionActivity(Activity.Type.TRACK_LIKE);
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
                openInteractionActivity(Activity.Type.TRACK_REPOST);
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
                openInteractionActivity(Activity.Type.COMMENT);
            }
        });

        mTxtInfo = (TextView) findViewById(R.id.txtInfo);
    }

    public void setTrack(Track track) {
        setTrack(track, false);
    }

    public void setTrack(Track track, boolean forceLoadingState) {
        if (forceLoadingState || track.isLoadingInfo()){
            mInfoView.setVisibility(View.GONE);
            if (findViewById(R.id.loading_layout) != null) {
                findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.stub_loading).setVisibility(View.VISIBLE);
            }
        } else {
            mInfoView.setVisibility(View.VISIBLE);
            if (findViewById(R.id.loading_layout) != null) {
                findViewById(R.id.loading_layout).setVisibility(View.GONE);
            }
        }
        fillTrackDetails(track);
    }

    private void fillTrackDetails(Track track) {
        mTrackId = track.getId();

        setViewVisibility(track.likes_count > 0, mLikersRow, mLikersDivider);
        mLikersText.setText(getResources().getQuantityString(R.plurals.track_info_likers,
                ((int) track.likes_count),
                ((int) track.likes_count)));

        setViewVisibility(track.reposts_count > 0, mRepostersRow, mRepostersDivider);
        mRepostersText.setText(getResources().getQuantityString(R.plurals.track_info_reposters,
                ((int) track.reposts_count),
                ((int) track.reposts_count)));

        setViewVisibility(track.comment_count > 0, mCommentersRow, mCommentersDivider);
        mCommentersText.setText(getResources().getQuantityString(R.plurals.track_info_commenters,
                ((int) track.comment_count),
                ((int) track.comment_count)));


        // check for equality to avoid extra view inflation
        if (mLastTags == null || !mLastTags.hasSameTags(track)) {
            mTrackTags.removeAllViews();
            fillTags(track);
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
        } else if (track.likes_count <= 0 && track.reposts_count <= 0 && track.comment_count <= 0
                && mLastTags.isEmpty()) {
            mTxtInfo.setText(R.string.no_info_available);
            mTxtInfo.setGravity(Gravity.CENTER_HORIZONTAL);
        } else {
            mTxtInfo.setText("");
        }
    }

    private void fillTags(final Track track) {
        TextView txt;
        FlowLayout.LayoutParams flowLP = new FlowLayout.LayoutParams(10, 10);

        final LayoutInflater inflater = LayoutInflater.from(getContext());
        if (!TextUtils.isEmpty(track.genre)) {
            txt = ((TextView) inflater.inflate(R.layout.tag_text, null));
            txt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), SearchByTagActivity.class);
                    intent.putExtra(SearchByTagActivity.EXTRA_GENRE, track.genre);
                    getContext().startActivity(intent);
                }
            });
            txt.setText(track.genre);
            mTrackTags.addView(txt, flowLP);
        }
        for (final String t : track.humanTags()) {
            if (!TextUtils.isEmpty(t)) {
                txt = ((TextView) inflater.inflate(R.layout.tag_text, null));
                txt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getContext(), SearchByTagActivity.class);
                        intent.putExtra(SearchByTagActivity.EXTRA_TAG, t);
                        getContext().startActivity(intent);
                    }
                });
                txt.setText(t);
                mTrackTags.addView(txt, flowLP);
            }
        }
    }

    private void openInteractionActivity(Activity.Type interactionType) {
        getContext().startActivity(new Intent(getContext(), TrackInteractionActivity.class)
                .putExtra(Track.EXTRA_ID, mTrackId)
                .putExtra(EXTRA_INTERACTION_TYPE, interactionType));
    }


    private static void setViewVisibility(boolean visible, View... views) {
        int visibility = visible ? VISIBLE : GONE;

        for (View view : views) {
            view.setVisibility(visibility);
        }
    }

    private static class TagsHolder {
        final String genre;
        final List<String> humanTags;

        public TagsHolder(Track track) {
            this.genre = track.genre;
            this.humanTags = track.humanTags();
        }

        public boolean hasSameTags(Track track){
            return track != null && TextUtils.equals(track.genre, genre) && humanTags.equals(track.humanTags());
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(genre) && (humanTags == null || humanTags.isEmpty());
        }
    }
}
