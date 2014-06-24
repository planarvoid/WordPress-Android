package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.images.ImageUtils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class CommentPanelLayout extends RelativeLayout {

    private final ImageView mIcon;
    private Comment mComment;

    private final TextView mTxtUsername;
    private final TextView mTxtTimestamp;
    private final TextView mTxtElapsed;

    private final TextView mTxtReadOn;
    private final ImageButton mBtnClose;
    protected final TextView mTxtComment;

    /* package */ boolean interacted, closing;

    private final String at_timestamp;

    private final Paint mBgPaint;
    private final Paint mLinePaint;
    private int mPlayheadOffset;
    private final int mPlayheadArrowWidth;
    private final int mPlayheadArrowHeight;

    private CommentPanelListener mListener;

    private ImageOperations mImageOperations;

    public interface CommentPanelListener {
        void onNextCommentInThread();
        void onCloseComment();
    }

    public CommentPanelLayout(Context context, ImageOperations imageOperations, boolean isLandscape) {
        super(context);

        mImageOperations = imageOperations;

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.comment_panel, this);

        mBgPaint = new Paint();
        mBgPaint.setColor(getResources().getColor(R.color.player_control_background));
        mBgPaint.setAntiAlias(true);
        mBgPaint.setStyle(Paint.Style.FILL);

        mLinePaint = new Paint();
        mLinePaint.setColor(getResources().getColor(R.color.portrait_player_comment_line));
        mLinePaint.setStyle(Paint.Style.STROKE);

        final float density = getResources().getDisplayMetrics().density;
        setPadding(0, (int) (5 * density), 0, isLandscape ? (int) (5 * density) : (int) (30 * density));

        mIcon = (ImageView) findViewById(R.id.icon);
        mIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ProfileActivity.class);
                intent.putExtra("user", mComment.user);
                getContext().startActivity(intent);
            }

        });

        mPlayheadArrowWidth = (int) (getResources().getDisplayMetrics().density * 10);
        mPlayheadArrowHeight = (int) (getResources().getDisplayMetrics().density * 10);

        at_timestamp = getResources().getString(R.string.at_timestamp);

        mTxtUsername = (TextView) findViewById(R.id.txt_username);
        mTxtTimestamp = (TextView) findViewById(R.id.txt_timestamp);
        mTxtElapsed = (TextView) findViewById(R.id.txt_elapsed);
        mBtnClose = (ImageButton) findViewById(R.id.btn_close);
        mTxtReadOn = (TextView) findViewById(R.id.txt_read_on);
        mTxtComment = (TextView) findViewById(R.id.txt_comment);

        mTxtReadOn.setText(Html.fromHtml(getResources().getString(R.string.comment_panel_read_on)));
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mComment != null && mComment.track != null) {
                    Intent intent = new Intent(Comment.ACTION_CREATE_COMMENT)
                            .putExtra(Comment.EXTRA, Comment.build(
                                    mComment.track,
                                    SoundCloudApplication.fromContext(getContext()).getAccountOperations().getLoggedInUser(),
                                    mComment.timestamp,
                                    "",
                                    mComment.getId(),
                                    mComment.user.username));
                    getContext().sendBroadcast(intent);

                }
                return true;
            }
        });

        mTxtReadOn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onNextCommentInThread();
            }

        });

        mTxtUsername.setFocusable(true);
        mTxtUsername.setClickable(true);
        mTxtUsername.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ProfileActivity.class);
                intent.putExtra("user", mComment.user);
                getContext().startActivity(intent);
            }

        });

        if (mBtnClose != null)
            mBtnClose.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onCloseComment();
                }
            });

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                interacted = true;
            }
        });

        interacted = false;
    }


    public void setListener(CommentPanelListener commentPanelListener) {
        mListener = commentPanelListener;
    }

    protected void showComment(final Comment current) {
        mComment = current;
        if (mComment == null) return;

        mTxtUsername.setText(mComment.user.username);
        mTxtTimestamp.setText(String.format(at_timestamp, ScTextUtils.formatTimestamp(mComment.timestamp, TimeUnit.MILLISECONDS)));
        mTxtComment.setText(mComment.body);
        mTxtElapsed.setText(ScTextUtils.getElapsedTimeString(getResources(), mComment.getCreatedAt().getTime(), true));
        mTxtUsername.setVisibility(View.VISIBLE);
        mTxtTimestamp.setVisibility(View.VISIBLE);
        mTxtElapsed.setVisibility(View.VISIBLE);
        mTxtComment.setVisibility(View.VISIBLE);

        if (mBtnClose != null) mBtnClose.setVisibility(View.VISIBLE);
        if (mTxtReadOn != null) {
            if (mComment.nextComment != null) {
                mTxtReadOn.setVisibility(View.VISIBLE);
            } else {
                mTxtReadOn.setVisibility(View.GONE);
            }
        }

        if (!mComment.shouldLoadIcon()) {
            mImageOperations.cancel(mIcon);
            return;
        }

        mImageOperations.display(mComment.user.avatar_url, mIcon);
        mPlayheadOffset = mComment.xPos;
    }

    public Comment getComment() {
        return mComment;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        //if (mIsLandscape){
        //noinspection ConstantIfStatement
        if (true) { // might change this later
            ImageUtils.drawSquareBubbleOnCanvas(canvas,
                    mBgPaint,
                    mLinePaint,
                    getMeasuredWidth(),
                    getMeasuredHeight() - mPlayheadArrowHeight,
                    mPlayheadArrowWidth, mPlayheadArrowHeight, mPlayheadOffset);
        } else {
            canvas.drawRect(0,0,getMeasuredWidth(),getMeasuredHeight(),mBgPaint);
        }
        super.dispatchDraw(canvas);
    }

}