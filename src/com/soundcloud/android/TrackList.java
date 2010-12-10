package com.soundcloud.android;

import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;

//import com.evancharlton.magnatune.objects.Album;
//import com.evancharlton.magnatune.objects.Artist;

public class TrackList extends LazyActivity {

	private static final String TAG = "TrackList";
	
	protected String mFavoriteResult;
	protected Track mFavoriteTrack;
	protected TracklistRow mRefreshRow;
	
	protected HashMap<String,Track> tracks = new HashMap<String, Track>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		Intent i = getIntent();


		String filter = i.getStringExtra(CloudUtils.EXTRA_FILTER);
		if (filter != null) {
			mFilter = filter;
		}

		String title = i.getStringExtra(CloudUtils.EXTRA_TITLE);
		if (title != null) {
			setTitle(title);
		}
		
		super.onCreate(savedInstanceState, getLayoutID());
	}


	
	
	
	protected int getLayoutID(){
		return R.layout.main;
	}
	
	
	
	protected String getUrl() {
		return "";
	}
	


	
	@Override
	public void resolveParcelable(Parcelable p){
		if (p instanceof Track){
			CloudUtils.resolveTrack(this, (Track) p, false, CloudUtils.getCurrentUserId(this));
		} else if (p instanceof Event){
			
			if (((Event) p).getDataParcelable(Event.key_track) != null)
				CloudUtils.resolveTrack(this, (Track) ((Event) p).getDataParcelable(Event.key_track), false, CloudUtils.getCurrentUserId(this));
			
		}
	}
	
	public void playTrack(List<Parcelable> list, int playPos){
		
		Track[] tl = new Track[list.size()];
		for (int i = 0; i < list.size(); i++){
			if (list.get(i) instanceof Track)
				tl[i] = (Track) list.get(i);
			else if (list.get(i) instanceof Event){
				tl[i] = (Track) ((Event) list.get(i)).getDataParcelable(Event.key_track);
			}
		}
		
		
		
		
		
		//CloudUtils.resolveTrack(this,track,true,CloudUtils.getCurrentUserId(this));
		try {
			mService.enqueue(tl, playPos);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		 Intent i = new Intent(this, ScPlayer.class);
		 startActivity(i);
		
	}
	
	
	@Override
	public void onItemClick(AdapterView<?> list, View row, int position, long id) {
		/*if (((Track) ((TracklistAdapter) list.getAdapter()).getData().get(position)).isPlaylist()){
			Log.i(TAG,"Loading Playlist");
			Intent i = new Intent(this, PlaylistBrowser.class);
			Track playlist = (Track) mAdapterData.get(position);
			i.putExtra("playlistLoadId", playlist.getData(Track.key_id));
			startActivity(i);
		} else {
			((TracklistAdapter) list.getAdapter()).setSelected(position);
			((TracklistAdapter) list.getAdapter()).notifyDataSetChanged();	
		}
		*/
		
		this.playTrack(((LazyBaseAdapter) list.getAdapter()).getData(),position);

		
	}
	
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	}
	
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
	/*	if (v.getClass() == android.widget.ListView.class || v.getClass().getGenericSuperclass() == android.widget.ListView.class){
		
			if (((android.widget.ListView) v).getAdapter().getClass() == TracklistAdapter.class || ((android.widget.ListView) v).getAdapter().getClass().getGenericSuperclass() == TracklistAdapter.class){
					AdapterView.AdapterContextMenuInfo info;
					try {
					    info = (AdapterView.AdapterContextMenuInfo) menuInfo;
					} catch (ClassCastException e) {
					    Log.e(getClass().getSimpleName(), "bad menuInfo", e);
					    return;
					}
					
					menuParcelable = (Track) ((LazyBaseAdapter)((android.widget.ListView) v).getAdapter()).getData().get(info.position);
					
					if ((((Track) menuParcelable).getData(Track.key_play_url) != null && ((Track) menuParcelable).getData(Track.key_play_url) != "") || (((Track) menuParcelable).getData(Track.key_download_status).contentEquals(Track.DOWNLOAD_STATUS_DOWNLOADED))){
						menu.add(0, CloudUtils.ContextMenu.PLAY_TRACK, 0, getString(R.string.context_menu_play_track));
						menu.add(0, CloudUtils.ContextMenu.ADD_TO_PLAYLIST, 0, getString(R.string.context_menu_add_to_playlist));	
					}
					if ((((Track) menuParcelable).getData(Track.key_download_status).contentEquals(Track.DOWNLOAD_STATUS_DOWNLOADED))){
						menu.add(0, CloudUtils.ContextMenu.DELETE_TRACK, 0, getString(R.string.context_menu_delete_track));
						menu.add(0, CloudUtils.ContextMenu.RE_DOWNLOAD, 0, getString(R.string.context_menu_re_download));	
					}
					
					menu.add(0, CloudUtils.ContextMenu.VIEW_UPLOADER, 0, getString(R.string.context_menu_view_uploader));
				
			}
			
		}*/
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		
			//basic track functions
			case CloudUtils.ContextMenu.VIEW_TRACK:
				CloudUtils.gotoTrackDetails(this, (Track) menuParcelable);
				break;
			case CloudUtils.ContextMenu.PLAY_TRACK:
				//playTrack((Track) menuParcelable);
				break;
			case CloudUtils.ContextMenu.DELETE_TRACK:
				dialogParcelable = menuParcelable;
				showDialog(CloudUtils.Dialogs.DIALOG_CONFIRM_DELETE_TRACK);
				break;
			case CloudUtils.ContextMenu.RE_DOWNLOAD:
				dialogParcelable = menuParcelable;
				showDialog(CloudUtils.Dialogs.DIALOG_CONFIRM_RE_DOWNLOAD_TRACK);
				break;
			case CloudUtils.ContextMenu.ADD_TO_PLAYLIST:
				CloudUtils.addTrackToPlaylist(mService, (Track) menuParcelable);
				break;
			default:
		
		}
		
		return super.onContextItemSelected(item);
	}
	

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
		
	}
	  
		
	
	    
	    

}
