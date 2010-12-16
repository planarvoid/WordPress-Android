package com.soundcloud.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
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

import com.soundcloud.android.objects.Comment;

public class CloudCommunicator {

	public static enum Events {
		track, favorite, playlist
	}

	private static final String TAG = "CloudCommunicator";
	static private CloudCommunicator _instance;

	public static String ERROR_UNAUTHORIZED = "401 - unauthorized";

	public static String TYPE_TRACK = "track";
	public static String TYPE_FAVORITE = "favorite";
	public static String TYPE_PLAYLIST = "playlist";
	public static String TYPE_ACTIVITY = "activity";

	public static String ORDER_HOTNESS = "hotness";

	public static String PATH_MY_USERS = "me/followings";
	public static String PATH_MY_FEED = "events";
	public static String PATH_USERS = "users";
	public static String PATH_TRACKS = "tracks";
	public static String PATH_PLAYLISTS = "playlists";
	public static String PATH_MY_DETAILS = "me";
	public static String PATH_MY_ACTIVITIES = "me/activities/tracks";
	public static String PATH_MY_EXCLUSIVE_TRACKS = "me/activities/tracks/exclusive";
	public static String PATH_MY_TRACKS = "me/tracks";
	public static String PATH_MY_PLAYLISTS = "me/playlists";
	public static String PATH_MY_FAVORITES = "me/favorites";
	public static String PATH_MY_FOLLOWERS = "me/followers";
	public static String PATH_MY_FOLLOWINGS = "me/followings";
	public static String PATH_USER_DETAILS = "users/{user_id}";
	public static String PATH_USER_FOLLOWINGS = "users/{user_id}/followings";
	public static String PATH_USER_FOLLOWERS = "users/{user_id}/followers";
	public static String PATH_TRACK_DETAILS = "tracks/{track_id}";
	public static String PATH_USER_TRACKS = "users/{user_id}/tracks";
	public static String PATH_USER_FAVORITES = "users/{user_id}/favorites";
	public static String PATH_USER_PLAYLISTS = "users/{user_id}/playlists";
	public static String PATH_TRACK_COMMENTS = "tracks/{track_id}/comments";
	public static SoundCloudOptions sSoundCloudOptions = 
		SoundCloudAPI.USE_SANDBOX;
		//SoundCloudAPI.USE_PRODUCTION;
	//SoundCloudAPI.USE_SANDBOX.with(OAuthVersion.V2_0);
	//SoundCloudAPI.USE_PRODUCTION.with(OAuthVersion.V2_0);

	public static String formatContent(InputStream is) throws IOException {
		if (is == null) {
			return "";
		}

		StringBuilder builder = new StringBuilder();
		BufferedReader buffer = new BufferedReader(new InputStreamReader(is));
		String line = null;
		while ((line = buffer.readLine()) != null) {
			builder.append(line).append("\n");
		}
		buffer.close();

		//Log.i(TAG, "Content formatted: " + builder.toString().trim());

		return builder.toString().trim();
	}

	public static String getErrorFromJSONResponse(String rawString)
			throws JSONException {
		if (rawString.startsWith("[")) {
			return ""; // arrays do not result from errors
		} else {
			JSONObject errorChecker = new JSONObject(rawString);
			try {
				if (errorChecker.get("error") != null) {
					return errorChecker.getString("error");
				} else {
					return "";
				}
			} catch (Exception e) {
				return "";
			}

		}
	}

	static public CloudCommunicator getInstance(Context context) {
		Log.i("[CloudComm]", "Get instance " + _instance);
		if (_instance == null) {
			_instance = new CloudCommunicator(context);
		}
		return _instance;
	}

	private Context _context;
	private static SoundCloudAPI api;
	private static HttpClient httpClient;
	public static String sToken = null, sTokenSecret = null;

	/**
	 * Constructor
	 */
	private CloudCommunicator(Context context) {
		_context = context;

		if (api == null) {
			api = newSoundCloudRequest();
		}

	}
	
	private SoundCloudAPI newSoundCloudRequest() {
		if (api != null) {
			return new SoundCloudAPI(api);
		}

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(_context);

		SoundCloudAPI soundCloud = new SoundCloudAPI(getConsumerKey(),
				getConsumerSecret(), preferences.getString(
						"oauth_access_token", ""), preferences.getString(
						"oauth_access_token_secret", ""), sSoundCloudOptions);

		return soundCloud;
	}

	public final void clearSoundCloudAccount() {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(_context);
		Log.i("[cloudcomm]", "current access token "
				+ preferences.getString("oauth_access_token", "null"));
		preferences.edit().putString("oauth_access_token", "").putString(
				"oauth_access_token_secret", "").putString("currentUserId", "")
				.putString("currentUsername", "").commit();
		Log.i("[cloudcomm]", "current access token111 "
				+ preferences.getString("oauth_access_token", "null"));
		api.unauthorize();

		// api = newSoundCloudRequest();
	}

	public InputStream deleteContent(String path) throws IllegalStateException,
			OAuthMessageSignerException, OAuthExpectationFailedException,
			ClientProtocolException, IOException, OAuthCommunicationException {
		return api.delete(path).getEntity().getContent();
	}

	public SoundCloudAPI getApi() {
		return api;
	}

	public final Intent getAuthorizationIntent() throws Exception {
		
		Log.i(TAG,"Getting auth intent");
		if (api.getState() == SoundCloudAPI.State.AUTHORIZED) {
			return null;
		}

		String authorizationUrl = api.obtainRequestToken("soundcloud://auth")
				+ "&display=popup";

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setData(Uri.parse(authorizationUrl));
		return i;
	}

	public Document getCloudData(String path) {

		HttpResponse response;
		try {

			response = api.get(path);

			if (response.getStatusLine().getStatusCode() != 200) {
				return null;
			}

			DocumentBuilder db = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
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

	public String getConsumerKey() {
		if (sSoundCloudOptions == SoundCloudAPI.USE_PRODUCTION) {
			return _context.getResources().getString(R.string.consumer_key);
		} else {
			return _context.getResources().getString(
					R.string.sandbox_consumer_key);
		}
	}

	public String getConsumerSecret() {
		if (sSoundCloudOptions == SoundCloudAPI.USE_PRODUCTION) {
			return _context.getResources().getString(R.string.consumer_secret);
		} else {
			return _context.getResources().getString(
					R.string.sandbox_consumer_secret);
		}
	}

	public InputStream getContent(String path) throws IllegalStateException,
			OAuthMessageSignerException, OAuthExpectationFailedException,
			ClientProtocolException, IOException, OAuthCommunicationException {

		Log.i("CLOUDCOMM", "Getting Content "
				+ api.signStreamUrl(urlEncode(getDomain() + path, null)));

		newSoundCloudRequest();
		HttpUriRequest req = api.getRequest(path, null);
		req.getParams().setParameter("consumer_key", getConsumerKey());
		req.addHeader("Accept", "application/json");

		HttpResponse response = getHttpClient().execute(req);
		return response.getEntity().getContent();
	}
	
	public HttpUriRequest getRequest(String path) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
		return getRequest(path, null);
	}
	
	public HttpUriRequest getRequest(String path,List<NameValuePair> params) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
		return api.getRequest(path, params);
	}

	private String getDomain() {
		if (sSoundCloudOptions == SoundCloudAPI.USE_PRODUCTION) {
			return "http://api.soundcloud.com/";
		} else {
			return "http://api.sandbox-soundcloud.com/";
		}
	}

	private HttpClient getHttpClient() {
		if (httpClient == null) {
			HttpClient client = new DefaultHttpClient();
			httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(
					client.getParams(), client.getConnectionManager()
							.getSchemeRegistry()), client.getParams());
		}
		return httpClient;

	}

	public HttpResponse getHttpResponse(String path) {

		try {
			return api.get(path);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

	public SoundCloudAPI.State getState() {
		return api.getState();
	}

	public String getUserName() {
		try {
			Document dom = getCloudData("me");
			return dom.getElementsByTagName("username").item(0).getFirstChild()
					.getNodeValue();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public final void launchAuthorization() throws Exception {
		((Activity) _context).startActivity(getAuthorizationIntent());
	}
	
	public void updateAuthorizationStatus(String verificationCode) {
		try {

			api.obtainAccessToken(verificationCode);

			SharedPreferences preferences = PreferenceManager
					.getDefaultSharedPreferences(_context);
			preferences.edit().putString("oauth_access_token", api.getToken())
					.putString("oauth_access_token_secret",
							api.getTokenSecret()).commit();

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

	

	public HttpEntity putComment(Comment comment) {
		try {
			List<NameValuePair> params = comment.mapDataToParams();
			String path = PATH_TRACK_COMMENTS.replace("{track_id}", comment
					.getData(Comment.key_track_id));
			HttpResponse response = api.post(path + "/", params);
			response.getAllHeaders();

			return response.getEntity();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

	public InputStream putContent(String path) {

		try {
			Log.i("sending put ", path);
			return api.put(path).getEntity().getContent();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

	public String signStreamUrl(String path) {
		try {
			return api.signStreamUrl(path) + "&consumer_key="
					+ getConsumerKey();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

	public String signStreamUrlNaked(String path) {
		try {
			return api.signStreamUrl(path);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}
	
	public HttpResponse upload(ContentBody trackBody,  List<NameValuePair> params) throws OAuthMessageSignerException, OAuthExpectationFailedException, ClientProtocolException, IOException, OAuthCommunicationException
    {
            
            return upload(trackBody,null,params);
    }
	
	public HttpResponse upload(ContentBody trackBody, ContentBody artworkBody, List<NameValuePair> params) throws OAuthMessageSignerException, OAuthExpectationFailedException, ClientProtocolException, IOException, OAuthCommunicationException
     {
             HttpPost post = new HttpPost(urlEncode("tracks", null));  
             // fix contributed by Bjorn Roche
             post.getParams().setBooleanParameter( "http.protocol.expect-continue", false );

             MultipartEntity entity = new MultipartEntity();
             for(NameValuePair pair : params)
             {
                     try
                     {
                             entity.addPart(pair.getName(), new StringBodyNoHeaders(pair.getValue()));
                     } catch (UnsupportedEncodingException e)
                     {
                     }  
             }
             entity.addPart("track[asset_data]", trackBody);  
             
             if (artworkBody != null) entity.addPart("track[artwork_data]", artworkBody);  

             post.setEntity(entity);
             
             getHttpClient().execute(post);
             return api.performRequest(post);  
     }
	

	private String urlEncode(String resource, List<NameValuePair> params) {
		String resourceUrl;
		if (resource.startsWith("/")) {
			resourceUrl = getDomain() + resource.substring(1);
		} else {
			resourceUrl = resource.contains("://") ? resource : getDomain()
					+ resource;
		}
		return params == null ? resourceUrl : resourceUrl + "?"
				+ URLEncodedUtils.format(params, "UTF-8");
	}
	
	
	
	class StringBodyNoHeaders extends StringBody
	{
	        public StringBodyNoHeaders(String value) throws UnsupportedEncodingException
	        {
	                super(value);
	        }       
	        
	        public String getMimeType()
	        {
	                return null;
	        }

	        public String getTransferEncoding()
	        {
	                return null;
	        }       
	}


}
