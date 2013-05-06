package com.soundcloud.android.view.play;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.utils.ScTextUtils;

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

public class CommentPanel extends RelativeLayout {

    private final ImageView mIcon;
    private Comment mComment;

    private final TextView mTxtUsername;
    private final TextView mTxtTimestamp;
    private final TextView mTxtElapsed;

    private final TextView mTxtReadOn;
    private final ImageButton mBtnClose;
    protected final TextView mTxtComment;

    private WaveformController mController;
    private ScPlayer mPlayer;

    /* package */ boolean interacted, closing;

    private final String at_timestamp;

    private final Paint mBgPaint;
    private final Paint mLinePaint;
    private int mPlayheadOffset;
    private final int mPlayheadArrowWidth;
    private final int mPlayheadArrowHeight;


    public CommentPanel(Context context, boolean isLandscape) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.comment_panel, this);

        mBgPaint = new Paint();
        mBgPaint.setColor(getResources().getColor(R.color.playerControlBackground));
        mBgPaint.setAntiAlias(true);
        mBgPaint.setStyle(Paint.Style.FILL);

        mLinePaint = new Paint();
        mLinePaint.setColor(getResources().getColor(R.color.portraitPlayerCommentLine));
        mLinePaint.setStyle(Paint.Style.STROKE);

        final float density = getResources().getDisplayMetrics().density;
        setPadding(0, (int) (5 * density), 0, isLandscape ? (int) (5 * density) : (int) (30 * density));

        mIcon = (ImageView) findViewById(R.id.icon);
        mIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mPlayer, UserBrowser.class);
                intent.putExtra("user", mComment.user);
                mPlayer.startActivity(intent);
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
                if (mComment != null && mComment.track != null){
                    mPlayer.addNewComment(
                            Comment.build(
                                    mComment.track,
                                    mPlayer.getApp().getLoggedInUser(),
                                    mComment.timestamp,
                                    "",
                                    mComment.id,
                                    mComment.user.username));
                }
                return true;
            }
        });

        mTxtReadOn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.nextCommentInThread();
            }

        });

        mTxtUsername.setFocusable(true);
        mTxtUsername.setClickable(true);
        mTxtUsername.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mPlayer, UserBrowser.class);
                intent.putExtra("user", mComment.user);
                mPlayer.startActivity(intent);
            }

        });

        if (mBtnClose != null)
            mBtnClose.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.closeComment(true);
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

    protected void setControllers(ScPlayer player, WaveformController controller) {
        mPlayer = player;
        mController = controller;
    }

    protected void showComment(final Comment current) {
        mComment = current;
        if (mComment == null) return;

        mTxtUsername.setText(mComment.user.username);
        mTxtTimestamp.setText(String.format(at_timestamp, ScTextUtils.formatTimestamp(mComment.timestamp)));
        mTxtComment.setText(mComment.body);
        mTxtElapsed.setText(ScTextUtils.getElapsedTimeString(getResources(), mComment.created_at.getTime(), true));
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
            ImageLoader.get(getContext()).unbind(mIcon);
            return;
        }

        ImageLoader.get(getContext()).bind(mIcon,
                Consts.GraphicSize.formatUriForList(getContext(), mComment.user.avatar_url),
                new ImageLoader.Callback() {
                    @Override
                    public void onImageLoaded(ImageView view, String url) {
                    }

                    @Override
                    public void onImageError(ImageView view, String url, Throwable error) {
                    }
                });

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