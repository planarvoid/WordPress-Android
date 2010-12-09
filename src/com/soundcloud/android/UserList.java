package com.soundcloud.android;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.soundcloud.android.R;
import com.soundcloud.android.objects.User;

//import com.evancharlton.magnatune.objects.Album;
//import com.evancharlton.magnatune.objects.Artist;

public class UserList extends LazyActivity {
	protected String mGroup = "";
	protected String mFilter = "";
	protected String mOrder = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		Intent i = getIntent();
		String group = i.getStringExtra(CloudUtils.EXTRA_GROUP);
		if (group != "") {
			mGroup = group;
		}

		String filter = i.getStringExtra(CloudUtils.EXTRA_FILTER);
		if (filter != "") {
			mFilter = filter;
		}

		String title = i.getStringExtra(CloudUtils.EXTRA_TITLE);
		if (title != "") {
			setTitle(title);
		}

		super.onCreate(savedInstanceState, R.layout.main);
	}


	
	
	
	protected String getUrl() {
		return "";
	}

	
	
	public void onItemClick(LazyList list, View row, int position, long id) {
		Intent i = new Intent(this, UserBrowser.class);
		User user = (User) list.getAdapter().getItem(position);
		i.putExtra("userInfo", user);
		startActivity(i);
	}
	
	protected void startActivityForPosition(Class<?> targetCls, HashMap<String, String> info) {
		
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}




	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
		
	}

}
