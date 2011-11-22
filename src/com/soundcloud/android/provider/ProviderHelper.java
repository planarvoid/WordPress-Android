package com.soundcloud.android.provider;

import android.content.Context;

public class ProviderHelper {

	private static final String LOG = ProviderHelper.class.getName();
	private Context context;
	private static ProviderHelper serviceHelper;

	public static ProviderHelper getInstance(Context context) {
		if (serviceHelper == null) serviceHelper = new ProviderHelper(context);
		serviceHelper.setContext(context);
		return serviceHelper;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public ProviderHelper(Context context) {
		this.context = context;
	}

}
