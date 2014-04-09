package com.soundcloud.android.playback.views;

import static com.soundcloud.android.associations.PlayableInteractionActivity.EXTRA_INTERACTION_TYPE;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.TrackInteractionActivity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.activities.Activity;
import com.soundcloud.android.search.SearchByTagActivity;
import com.soundcloud.android.utils.ScTextUtils;
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
    private TableRow mTagsAndDescriptionRow;
    private TableRow mLikersRow;
    private TableRow mRepostersRow;
    private TableRow mCommentersRow;
    private TextView mLikersText;
    private TextView mRepostersText;
    private TextView mCommentersText;
    private TextView mTxtInfo;

    private long mTrackId;
    private @Nullable TagsHolder mLastTags;

    private final OnClickListener mTagClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getContext(), SearchByTagActivity.class);
            intent.putExtra(SearchByTagActivity.EXTRA_TAG, (String) v.getTag());
            getContext().startActivity(intent);
        }
    };

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
        setBackgroundResource(R.color.playerTrackDetailsBg);
        setOrientation(VERTICAL);

        mInfoView = findViewById(R.id.info_view);
        mTrackTags = (ViewGroup) findViewById(R.id.tags_holder);
        mTagsAndDescriptionRow = (TableRow) findViewById(R.id.tags_and_description_row);

        mLikersText    = (TextView) findViewById(R.id.likers_txt);
        mLikersRow     = (TableRow) findViewById(R.id.likers_row);
        mLikersRow.setVisibility(View.GONE);
        mLikersRow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openInteractionActivity(Activity.Type.TRACK_LIKE);
            }
        });

        mRepostersText    = (TextView) findViewById(R.id.reposters_txt);
        mRepostersRow     = (TableRow) findViewById(R.id.reposters_row);
        mRepostersRow.setVisibility(View.GONE);
        mRepostersRow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openInteractionActivity(Activity.Type.TRACK_REPOST);
            }
        });

        mCommentersText    = (TextView) findViewById(R.id.commenters_txt);
        mCommentersRow     = (TableRow) findViewById(R.id.comments_row);
        mCommentersRow.setVisibility(View.GONE);
        mCommentersRow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openInteractionActivity(Activity.Type.COMMENT);
            }
        });

        mTxtInfo = (TextView) findViewById(R.id.txtInfo);
    }

    public void setTrack(Track mTrack, PlayerTrackView.TrackLoadingState mTrackLoadingState) {
        fillTrackDetails(mTrack);

        switch(mTrackLoadingState){
            case WAITING :
                showLoadingState();
                break;
            default:
                hideLoadingState();
                break;
        }
    }

    private void showLoadingState(){
        mInfoView.setVisibility(View.GONE);
        if (findViewById(R.id.loading_layout) != null) {
            findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.stub_loading).setVisibility(View.VISIBLE);
        }
    }

    private void hideLoadingState(){
        mInfoView.setVisibility(View.VISIBLE);
        if (findViewById(R.id.loading_layout) != null) {
            findViewById(R.id.loading_layout).setVisibility(View.GONE);
        }
    }

    private void fillTrackDetails(Track track) {
        mTrackId = track.getId();

        boolean filledStats = false;
        if (track.likes_count > 0){
            mLikersText.setText(getResources().getQuantityString(R.plurals.track_info_likers,
                    ((int) track.likes_count),
                    ((int) track.likes_count)));
            mLikersRow.setVisibility(View.VISIBLE);
            filledStats = true;
        } else {
            mLikersRow.setVisibility(View.GONE);
        }

        if (track.reposts_count > 0){
            mRepostersText.setText(getResources().getQuantityString(R.plurals.track_info_reposters,
                    ((int) track.reposts_count),
                    ((int) track.reposts_count)));
            mRepostersRow.setVisibility(View.VISIBLE);
            filledStats = true;
        } else {
            mRepostersRow.setVisibility(View.GONE);
        }

        if (track.comment_count > 0){
            mCommentersText.setText(getResources().getQuantityString(R.plurals.track_info_commenters,
                    ((int) track.comment_count),
                    ((int) track.comment_count)));
            mCommentersRow.setVisibility(View.VISIBLE);
            filledStats = true;
        } else {
            mCommentersRow.setVisibility(View.GONE);
        }

        List<String> humanTags = track.humanTags();
        final String trackInfo = track.trackInfo();
        final boolean isMissingTagsAndDescription = humanTags.isEmpty() && ScTextUtils.isBlank(track.genre) && ScTextUtils.isBlank(trackInfo);

        if (isMissingTagsAndDescription && filledStats){
            // no need to show anything
            mTagsAndDescriptionRow.setVisibility(View.GONE);
        } else {
            mTagsAndDescriptionRow.setVisibility(View.VISIBLE);
            mTagsAndDescriptionRow.setBackgroundResource(filledStats ? R.drawable.bottom_separator : android.R.color.transparent);

            if (isMissingTagsAndDescription){
                // show an empty message to avoid a blank screen
                mTxtInfo.setVisibility(View.VISIBLE);
                mTxtInfo.setText(R.string.no_info_available);
                mTxtInfo.setGravity(Gravity.CENTER_HORIZONTAL);
            } else {
                populateTags(track);
                populateDescription(trackInfo);
            }
        }
    }

    private void populateDescription(String trackInfo) {
        if (ScTextUtils.isNotBlank(trackInfo)){
            mTxtInfo.setVisibility(View.VISIBLE);
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
            mTxtInfo.setVisibility(View.GONE);
        }
    }

    private void populateTags(final Track track) {

        // check for equality to avoid extra view inflation
        if (mLastTags != null && mLastTags.hasSameTags(track)) {
            return;
        }

        mLastTags = new TagsHolder(track);
        TextView txt;

        mTrackTags.removeAllViews();

        final List<String> tags = track.humanTags();
        if (tags.isEmpty() && ScTextUtils.isBlank(track.genre)){
            mTrackTags.setVisibility(View.GONE);
        } else {
            mTrackTags.setVisibility(View.VISIBLE);

            final LayoutInflater inflater = LayoutInflater.from(getContext());
            if (!TextUtils.isEmpty(track.genre)) {
                txt = ((TextView) inflater.inflate(R.layout.btn_tag_small, null));
                txt.setText(withHashTag(track.genre));
                txt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getContext(), SearchByTagActivity.class);
                        intent.putExtra(SearchByTagActivity.EXTRA_GENRE, track.genre);
                        getContext().startActivity(intent);
                    }
                });
                mTrackTags.addView(txt);
            }
            for (final String t : tags) {
                if (!TextUtils.isEmpty(t)) {
                    txt = ((TextView) inflater.inflate(R.layout.btn_tag_small, null));
                    txt.setText(withHashTag(t));
                    txt.setTag(t);
                    txt.setOnClickListener(mTagClickListener);
                    mTrackTags.addView(txt);
                }
            }
        }
    }

    private String withHashTag(String tag) {
        return "#" + tag;
    }

    private void openInteractionActivity(Activity.Type interactionType) {
        getContext().startActivity(new Intent(getContext(), TrackInteractionActivity.class)
                .putExtra(Track.EXTRA_ID, mTrackId)
                .putExtra(EXTRA_INTERACTION_TYPE, interactionType));
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
