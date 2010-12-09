package com.soundcloud.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.urbanstew.soundcloudapi.SoundCloudAPI;
import org.urbanstew.soundcloudapi.SoundCloudOptions;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.soundcloud.android.R;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.utils.HttpHelper;

public class CloudCommunicator {
	
	private static final String TAG = "CloudCommunicator";
	
	static private CloudCommunicator _instance;
	static public CloudCommunicator getInstance(Context context) {
		Log.i("[CloudComm]","Get instance " + _instance);
		if (_instance == null) {
			_instance = new CloudCommunicator(context);
		}
		return _instance;
	}

	public static String ERROR_UNAUTHORIZED = "401 - unauthorized";
	
	public static enum Events { track, favorite, playlist }
	
	public static String TYPE_TRACK = "track";
	public static String TYPE_FAVORITE = "favorite";
	public static String TYPE_PLAYLIST = "playlist";
	
	public static String ORDER_HOTNESS = "hotness";
	
	public static String PATH_MY_USERS = "me/followings";
	public static String PATH_MY_FEED = "events";
	public static String PATH_USERS= "users";
	public static String PATH_TRACKS= "tracks";
	public static String PATH_PLAYLISTS= "playlists";
	public static String PATH_MY_DETAILS = "me";
	public static String PATH_MY_ACTIVITIES = "me/activities/tracks";
	public static String PATH_MY_EXCLUSIVE_TRACKS = "me/activities/tracks/exclusive";
	public static String PATH_MY_TRACKS = "me/tracks";
	public static String PATH_MY_PLAYLISTS = "me/playlists";
	public static String PATH_MY_FAVORITES = "me/favorites";
	public static String PATH_MY_FOLLOWERS = "me/followers";
	public static String PATH_MY_FOLLOWINGS = "me/followings";
	public static String PATH_USER_DETAILS = "users/{user_permalink}";
	public static String PATH_USER_FOLLOWINGS = "users/{user_permalink}/followings";
	public static String PATH_USER_FOLLOWERS = "users/{user_permalink}/followers";
	public static String PATH_TRACK_DETAILS = "tracks/{track_id}";
	public static String PATH_USER_TRACKS = "users/{user_permalink}/tracks";
	public static String PATH_USER_FAVORITES = "users/{user_permalink}/favorites";
	public static String PATH_USER_PLAYLISTS = "users/{user_permalink}/playlists";
	public static String PATH_TRACK_COMMENTS = "tracks/{track_id}/comments";
	

	 public static SoundCloudOptions sSoundCloudOptions =
         SoundCloudAPI.USE_SANDBOX;
       //SoundCloudAPI.USE_PRODUCTION;


	private Context _context; 

	private static SoundCloudAPI api;
	private static HttpHelper helper;
	private static HttpClient httpClient;

	public static String sToken = null, sTokenSecret = null;


	/**
	 * Constructor
	 */
	private CloudCommunicator (Context context) {
		_context = context;

		Log.i("[cloudcomm]", "API connecting " + api);
		
		if (api == null)
			api = newSoundCloudRequest();

		Log.i("[cloudcomm]", "API connected ");
	}
	
	private HttpHelper getHelper()
	{
		if (helper == null)
			helper = new HttpHelper();
		
			return helper;
		
	}
	
	private HttpClient getHttpClient(){
		if (httpClient == null){
			HttpClient client = new DefaultHttpClient();
	        httpClient = new DefaultHttpClient
	                (
	                        new ThreadSafeClientConnManager(client.getParams(), client.getConnectionManager().getSchemeRegistry()),
	                        client.getParams()
	                );
		}
		return httpClient;
		 
	}
	
	private String getHost(){
		if (sSoundCloudOptions == SoundCloudAPI.USE_PRODUCTION){
			return "http://api.soundcloud.com/";
		}
		
		return "http://api.sandbox-soundcloud.com/";
	}
	
	public String getConsumerKey(){
		if (sSoundCloudOptions == SoundCloudAPI.USE_PRODUCTION){
			return _context.getResources().getString(R.string.consumer_key);
		} else {
			return _context.getResources().getString(R.string.sandbox_consumer_key);
		}
	}
	
	public String getConsumerSecret(){
		if (sSoundCloudOptions == SoundCloudAPI.USE_PRODUCTION){
			return _context.getResources().getString(R.string.consumer_secret);
		} else {
			return _context.getResources().getString(R.string.sandbox_consumer_secret);
		}
	}
	


	private SoundCloudAPI newSoundCloudRequest()
	{
		if (api != null){
			return new SoundCloudAPI(api);
		}
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(_context);


		Log.i("[cloudcomm]", "current access token " + preferences.getString("oauth_access_token", "null"));

		SoundCloudAPI soundCloud = new SoundCloudAPI
		(
				getConsumerKey(),
				getConsumerSecret(),
				preferences.getString("oauth_access_token", ""),
				preferences.getString("oauth_access_token_secret", ""), sSoundCloudOptions
		);

		return soundCloud;
	}
	
	
	public final void clearSoundCloudAccount(){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(_context);
		Log.i("[cloudcomm]", "current access token " + preferences.getString("oauth_access_token", "null"));
		preferences.edit()
		.putString("oauth_access_token", "")
		.putString("oauth_access_token_secret", "")
		.putString("currentUserId","")
		.putString("currentUsername","")
		.commit();
		Log.i("[cloudcomm]", "current access token111 " + preferences.getString("oauth_access_token", "null"));
		api.unauthorize();
		
		//api = newSoundCloudRequest();
	}
	
	public final Intent getAuthorizationIntent() throws Exception
    {
			if (api.getState() == SoundCloudAPI.State.AUTHORIZED)
				return null;
			
			//if(api.getToken() != null && api.getTokenSecret() != null)
				//	return;

			Log.i(getClass().toString(), "Obtaining request token " + api.getState());
            String authorizationUrl = api.obtainRequestToken("soundcloud://auth") + "&display=popup";
            //OVERCAST_ACTIVITY.loadAccessPage(authorizationUrl);
            
            Log.i(getClass().toString(), "Obtained auth url " + authorizationUrl);
            
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	i.setData( Uri.parse(authorizationUrl));
	    	return i;
    }
	
	public final void launchAuthorization() throws Exception
	{
		((Activity) _context).startActivity(getAuthorizationIntent());
	}

	public Document getCloudData(String path){

		HttpResponse response;
		try {

			response = api.get(path);

			
			
			if(response.getStatusLine().getStatusCode() != 200)
				return null;

			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document dom = db.parse(response.getEntity().getContent());
			dom.getDocumentElement().normalize();

			return dom;

		} catch (OAuthMessageSignerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OAuthExpectationFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OAuthCommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		return null;

	}




	public SoundCloudAPI.State getState()
	{
		return api.getState();
	}


	public void updateAuthorizationStatus(String verificationCode)
	{
		try {

			Log.i("[cloudcomm]", "obtaining access token " + verificationCode);

			api.obtainAccessToken(verificationCode);
				
			Log.i("[cloudcomm]", "Access token obtained: " + api.getToken());
			Log.i("[cloudcomm]", "Access token secret obtained: " + api.getTokenSecret());
			Log.i("[cloudcomm]", "Access token secret obtained: " + getUserName());


			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(_context);
			preferences.edit()
			.putString("oauth_access_token", api.getToken())
			.putString("oauth_access_token_secret", api.getTokenSecret())
			.commit();

			//String token = api.getToken();
			//String tokenSecret = api.getTokenSecret();

		} catch (OAuthMessageSignerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OAuthNotAuthorizedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OAuthExpectationFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OAuthCommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getUserName()
	{
		HttpResponse response;
		try
		{
			Document dom = getCloudData("me");
			return dom.getElementsByTagName("username").item(0).getFirstChild().getNodeValue();

		}catch(Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public HttpResponse getHttpResponse(String path)
	{

		try
		{
			Log.i("sending", path);
			return api.get(path);

		}catch(Exception e) {
			Log.i("EXCEPTION", "got an exception");
			e.printStackTrace();
		}
		
		return null;

	}

	public InputStream getContent(String path) throws IllegalStateException, OAuthMessageSignerException, OAuthExpectationFailedException, ClientProtocolException, IOException, OAuthCommunicationException
	{
		
	    	    	  
			Log.i("CLOUDCOMM","Getting Content " + api.signStreamUrl(urlEncode(getHost()+path,null)));
			
			SoundCloudAPI scr = newSoundCloudRequest();
			HttpUriRequest req = api.getRequest(path, null);
			req.getParams().setParameter("consumer_key", getConsumerKey());
			req.addHeader("Accept", "application/json");
			
			
			HttpResponse response = getHttpClient().execute(req);
			InputStream ret = response.getEntity().getContent();
			Log.i("CLOUDCOMM","Got Content " + ret);
			scr = null;
			return ret;
			
		
		
		/*	Log.i("CLOUDCOMM","Get Content " + path);

			SoundCloudAPI scr = newSoundCloudRequest();

			InputStream ret = scr.get(path).getEntity().getContent();

			Log.i("CLOUDCOMM","Got Content " + ret);

			scr = null;
			
			return ret;*/
	}
	
	
	
	private String urlEncode(String resource, List<NameValuePair> params)
    {
            String resourceUrl;
                    if(resource.startsWith("/"))
                            resourceUrl = getHost() + resource.substring(1);
                    else
                            resourceUrl = resource.contains("://") ? resource : getHost() + resource;
            return params == null ?
                    resourceUrl :
                    resourceUrl + "?" + URLEncodedUtils.format(params, "UTF-8");
    }
	
	
	
	public String signStreamUrl(String path)
	{
		try
		{
			return api.signStreamUrl(path)+"&consumer_key="+getConsumerKey();
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;

	}
	
	public String signStreamUrlNaked(String path)
	{
		try
		{
			return api.signStreamUrl(path);
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;

	}
	
	
	public InputStream putContent(String path)
	{

		try
		{
			Log.i("sending put ", path);
			return api.put(path).getEntity().getContent();

			
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;

	}
	
	public InputStream deleteContent(String path) throws IllegalStateException, OAuthMessageSignerException, OAuthExpectationFailedException, ClientProtocolException, IOException, OAuthCommunicationException
	{

		Log.i("sending delete ", path);
		return api.delete(path).getEntity().getContent();
		


	}
	
	public static String formatContent(InputStream is) throws IOException {
		if (is == null)
			return "";
		
		StringBuilder builder = new StringBuilder();
		BufferedReader buffer = new BufferedReader(new InputStreamReader(is));
		String line = null;
		while ((line = buffer.readLine()) != null) {
			builder.append(line).append("\n");
		}
		buffer.close();
		
		Log.i(TAG,"Content formatted: " + builder.toString().trim());
		
		return builder.toString().trim();
	}
	
	public static String getErrorFromJSONResponse(String rawString) throws JSONException {
		if (rawString.startsWith("["))
			return ""; //arrays do not result from errors
		else {
			JSONObject errorChecker = (new JSONObject(rawString));
			try {
				if (errorChecker.get("error") != null){
					return errorChecker.getString("error");
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";	
			}
			
		}
	}
	
	public HttpEntity putComment(Comment comment)
	{
		try
		{
			List<NameValuePair> params = comment.mapDataToParams();
			String path = PATH_TRACK_COMMENTS.replace("{track_id}", comment.getData(Comment.key_track_id));
			HttpResponse response = api.post(path+"/", params);
			Header[] headers = response.getAllHeaders();
			
			return response.getEntity();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;

	}
	
	
	public SoundCloudAPI getApi()
	{
		return api;

	}
	
	
	

//	public void getContent(String path)
//	{
//
//		try
//		{
//			HttpResponse response;
//			response = api.get(path);
//
//			if(response.getStatusLine().getStatusCode() != 200)
//				return;
//
//			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//			Document dom = db.parse(response.getEntity().getContent());
//
//
//		}catch(Exception e) {
//			e.printStackTrace();
//		}
//
//	}

}
