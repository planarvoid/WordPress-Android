package com.soundcloud.android.creators.upload;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ShareUserHeaderLayout extends RelativeLayout {

    public ShareUserHeaderLayout(final ScActivity activity, User user, ImageOperations imageOperations) {
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
            imageOperations.displayWithPlaceholder(user.getUrn(), ApiImageSize.LARGE, icon);
        }
    }

    /** @noinspection UnusedDeclaration*/
    public ShareUserHeaderLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** @noinspection UnusedDeclaration*/
    public ShareUserHeaderLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
