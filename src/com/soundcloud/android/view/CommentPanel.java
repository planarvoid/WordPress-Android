package com.soundcloud.android.view;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

public class CommentPanel extends CommentDisplay {

    ImageView mIcon;

    public CommentPanel(Context context) {
        super(context);
    }

    public CommentPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void init(){
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.comment_panel, this);

        final float density = getResources().getDisplayMetrics().density;
        setBackgroundColor(getResources().getColor(R.color.commentPanelBg));
        setPadding(0, (int) (5 * density), 0, (int) (25 * density));

        mIcon = (ImageView) findViewById(R.id.icon);
        mIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mPlayer, UserBrowser.class);
                intent.putExtra("user", mComment.user);
                mPlayer.startActivity(intent);
            }

        });

        super.init();
    }

    protected void showComment(Comment currentShowingComment) {
        super.showComment(currentShowingComment);

        if (currentShowingComment.user.avatar_url == null){
            ImageLoader.get(getContext()).unbind(mIcon);
            return;
        }

        ImageLoader.get(getContext()).bind(mIcon,
                ImageUtils.formatGraphicsUrlForList(getContext(), currentShowingComment.user.avatar_url),
                new ImageLoader.ImageViewCallback() {
            @Override public void onImageLoaded(ImageView view, String url) {}
            @Override public void onImageError(ImageView view, String url, Throwable error) {}
        });
    }
}