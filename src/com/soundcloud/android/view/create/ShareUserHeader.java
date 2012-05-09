package com.soundcloud.android.view.create;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ShareUserHeader extends RelativeLayout {
    public ShareUserHeader(final ScListActivity activity, User user) {
        super(activity);

        LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.share_user_header, this);

        ScTextUtils.clickify((TextView) findViewById(R.id.share_header_logout_txt), null,
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        activity.safeShowDialog(Consts.Dialogs.DIALOG_LOGOUT);
                    }
                }, true);

        ((TextView) findViewById(R.id.share_header_username)).setText(user.username);

        final ImageView icon = (ImageView) findViewById(R.id.icon);
        if (user.shouldLoadIcon()) {
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
