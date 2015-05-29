package com.soundcloud.android.creators.upload;

import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.Connection;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ConnectionItemLayout extends RelativeLayout {
    private final CheckBox postPublish;
    public final Connection connection;

    public ConnectionItemLayout(Context context, final Connection connection) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        inflater.inflate(R.layout.connection_list_item, this);

        this.connection = connection;

        final TextView display_name = (TextView) findViewById(R.id.display_name);
        display_name.setText(connection.display_name);

        postPublish = (CheckBox) findViewById(R.id.post_publish);
        TextView configure = (TextView) findViewById(R.id.txt_configure);

        if (connection.isActive()) {
            postPublish.setChecked(connection.post_publish);
            postPublish.setVisibility(VISIBLE);
            configure.setVisibility(GONE);
        } else {
            postPublish.setVisibility(INVISIBLE);
            configure.setVisibility(VISIBLE);
            display_name.setTextColor(getResources().getColor(R.color.darker_gray));
        }


        ImageView service_icon = (ImageView) findViewById(R.id.service_icon);
        service_icon.setImageDrawable(getResources().getDrawable(connection.service().resId));

        final OnClickListener toggle = new OnClickListener() {
            public void onClick(View v) {
                if (connection.isActive()) {
                    postPublish.toggle();
                }
            }
        };
        display_name.setOnClickListener(toggle);
        configure.setOnClickListener(toggle);
        setOnClickListener(toggle);
    }

    public void progress(boolean finished) {
        TextView configure = (TextView) findViewById(R.id.txt_configure);
        configure.setTextColor(finished ?
                Color.WHITE :
                getResources().getColor(R.color.darker_gray)
        );
    }

    public boolean isEnabled() {
        return postPublish.isChecked();
    }
}
