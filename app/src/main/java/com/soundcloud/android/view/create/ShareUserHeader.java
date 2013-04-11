package com.soundcloud.android.view.create;

import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ShareUserHeader extends RelativeLayout {
    public ShareUserHeader(final ScActivity activity, User user) {
        super(activity);
        View.inflate(getContext(), R.layout.share_user_header, this);

        ScTextUtils.clickify((TextView) findViewById(R.id.share_header_logout_txt), null,
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        activity.safeShowDialog(Consts.Dialogs.DIALOG_LOGOUT);
                    }
                }, true, false);

        ((TextView) findViewById(R.id.share_header_username)).setText(user.username);

        if (user.shouldLoadIcon()) {
            final ImageView icon = (ImageView) findViewById(R.id.icon);
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
