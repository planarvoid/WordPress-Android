package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ClickSpan;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ShareUserHeader extends RelativeLayout {
    public ShareUserHeader(final ScActivity activity, User user) {
        super(activity);

        LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.share_user_header, this);

        CloudUtils.clickify((TextView) findViewById(R.id.share_header_logout_txt), null,
                new ClickSpan.OnClickListener() {
            @Override public void onClick() {
                activity.safeShowDialog(Consts.Dialogs.DIALOG_LOGOUT);
            }
        }, true);

        ((TextView) findViewById(R.id.share_header_username)).setText(user.username);

        final ImageView icon = (ImageView) findViewById(R.id.icon);
        if (CloudUtils.checkIconShouldLoad(user.avatar_url)) {
            if (ImageLoader.get(activity).bind(icon, user.avatar_url, null) != ImageLoader.BindResult.OK) {
                icon.setImageDrawable(getResources().getDrawable(R.drawable.avatar_badge));
            }
        }
    }

    /** @noinspection UnusedDeclaration*/
    public ShareUserHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** @noinspection UnusedDeclaration*/
    public ShareUserHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
