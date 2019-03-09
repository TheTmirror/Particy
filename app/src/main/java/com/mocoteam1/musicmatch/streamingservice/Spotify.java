package com.mocoteam1.musicmatch.streamingservice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import com.mocoteam1.musicmatch.R;
import com.mocoteam1.musicmatch.entities.Song;
import com.mocoteam1.musicmatch.entities.StreamingServicePlaylist;
import com.mocoteam1.musicmatch.entities.StreamingServiceSong;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.mocoteam1.musicmatch.streamingservice.SpotifyLogin.CLIENT_ID;

/**
 * Created by Tristan on 04.01.2018.
 */

public class Spotify implements IStreamingService, Player.NotificationCallback, ConnectionStateCallback{

    private static final String DEBUG_TAG = Spotify.class.getSimpleName();

    private static final int PLAYLIST_ITEM_LIMIT = 50;

    private Activity context;
    private Player player;
    private String uri;

    public Spotify(Activity context) {
        this.context = context;
    }

    public static boolean isAccessTokenValid(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_shared_preferences), Context.MODE_PRIVATE);
        String accessToken = sharedPreferences.getString(context.getString(R.string.spotify_access_token), null);
        int expiresIn = sharedPreferences.getInt(context.getString(R.string.spotify_access_token_expire_time), 0);
        expiresIn -= (1200 * 1000);
        Long savedAt = sharedPreferences.getLong(context.getString(R.string.spotify_access_token_save_time), 0);
        Log.d(DEBUG_TAG, "Read Time: " + savedAt);
        Date currentTime = Calendar.getInstance().getTime();
        Log.d(DEBUG_TAG, "Current Time: " + currentTime.getTime());
        Log.d(DEBUG_TAG, "" + expiresIn);
        Log.d(DEBUG_TAG, "" + savedAt);
        Log.d(DEBUG_TAG, "" + currentTime.getTime());
        Log.d(DEBUG_TAG, "" + (currentTime.getTime() >= (expiresIn + savedAt)));
        if(accessToken == null || expiresIn <= 0 || savedAt <= 0 || currentTime.getTime() >= (expiresIn + savedAt))
            return false;
        else
            return true;
    }

    public static boolean isAccessTokenValidOld(String accessToken) {
        AsyncTask<String, Integer, Boolean> validateTask = new AsyncTask<String, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(String... strings) {

                HttpURLConnection connection = null;

                try {
                    String urlString = "https://api.spotify.com/v1/me/following/contains";
                    URL url = new URL(urlString);
                    connection = (HttpURLConnection) url.openConnection();
                    // Timeout for reading InputStream arbitrarily set to 3000ms.
                    connection.setReadTimeout(3000);
                    // Timeout for connection.connect() arbitrarily set to 3000ms.
                    connection.setConnectTimeout(3000);
                    // For this use case, set HTTP method to GET.
                    connection.setRequestMethod("GET");
                    // Already true by default but setting just in case; needs to be true since this request
                    // is carrying an input (response) body.
                    connection.setDoInput(true);

                    connection.setRequestProperty("Authorization", "Bearer " + strings[0]);

                    return connection.getResponseCode() != 401;
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return false;
            }
        };

        if(accessToken == null)
            return false;

        boolean result = false;

        try {
            result = validateTask.execute(accessToken).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public void playSong(String uri) {
        this.uri = uri;

        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_shared_preferences), context.MODE_PRIVATE);

        String accessToken = sharedPreferences.getString(context.getString(R.string.spotify_access_token), "");

        Log.d(DEBUG_TAG, "Accesstoken: " + accessToken);

        Config playerConfig = new Config(context, accessToken, CLIENT_ID);
        player = com.spotify.sdk.android.player.Spotify.getPlayer(playerConfig, context, new SpotifyPlayer.InitializationObserver() {
            @Override
            public void onInitialized(SpotifyPlayer spotifyPlayer) {
                spotifyPlayer.addConnectionStateCallback((ConnectionStateCallback) Spotify.this);
                spotifyPlayer.addNotificationCallback((Player.NotificationCallback) Spotify.this);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(DEBUG_TAG, "Could not initialize player: " + throwable.getMessage());
            }
        });
    }

    @Override
    public List<StreamingServicePlaylist> getPlaylists() {

        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_shared_preferences), context.MODE_PRIVATE);
        String accessToken = sharedPreferences.getString(context.getString(R.string.spotify_access_token), null);

        if(accessToken == null) {
            //TODO: Exception werfen
            Log.e(DEBUG_TAG, "Fehler Spotify.java Line 80");
            return null;
        }

        List<StreamingServicePlaylist> playlists = null;

        AsyncTask<String, Integer, Object> getPlaylists = new AsyncTask<String, Integer, Object>() {
            @Override
            protected Object doInBackground(String... strings) {

                HttpURLConnection connection = null;

                JsonReader jsonReader = null;
                List<StreamingServicePlaylist> playlists = null;

                try {
                    String urlString = "https://api.spotify.com/v1/me/playlists?limit=" + PLAYLIST_ITEM_LIMIT;
                    Log.d(DEBUG_TAG, "Zum Überprüfen: " + urlString);
                    URL url = new URL(urlString);
                    connection = (HttpURLConnection) url.openConnection();
                    // Timeout for reading InputStream arbitrarily set to 3000ms.
                    connection.setReadTimeout(3000);
                    // Timeout for connection.connect() arbitrarily set to 3000ms.
                    connection.setConnectTimeout(3000);
                    // For this use case, set HTTP method to GET.
                    connection.setRequestMethod("GET");
                    // Already true by default but setting just in case; needs to be true since this request
                    // is carrying an input (response) body.
                    connection.setDoInput(true);

                    connection.setRequestProperty("Authorization", "Bearer " + strings[0]);

                    Log.d(DEBUG_TAG, "" + connection.getResponseCode());

                    if(connection.getResponseCode() != 200)
                        return false;

                    InputStream responseBody = connection.getInputStream();
                    InputStreamReader streamReader = new InputStreamReader(responseBody, "UTF-8");

                    int total = -1;

                    playlists = new LinkedList<StreamingServicePlaylist>();

                    jsonReader = new JsonReader(streamReader);
                    //TODO: Es fehlen scheinbar zwei jsonReader.endObject();
                    jsonReader.beginObject(); // Start processing the JSON object

                    while (jsonReader.hasNext()) { // Loop through all keys
                        String key = jsonReader.nextName(); // Fetch the next key
                        if (key.equals("total")) { // Check if desired key
                            // Fetch the value as a String
                            int value = jsonReader.nextInt();

                            Log.d(DEBUG_TAG, "Total ist: " + value);

                            break; // Break out of the loop
                        } else if(key.equals("items")) {
                            jsonReader.beginArray();

                            while(jsonReader.hasNext()) {
                                jsonReader.beginObject();

                                StreamingServicePlaylist playlist = new StreamingServicePlaylist();

                                while (jsonReader.hasNext()) {
                                    //Sind im Object
                                    String key1 = jsonReader.nextName();

                                    if(key1.equals("id")) {
                                        playlist.setId(jsonReader.nextString());
                                    } else if(key1.equals("name")) {
                                        playlist.setName(jsonReader.nextString());
                                    } else if(key1.equals("tracks")) {
                                        jsonReader.beginObject();

                                        while(jsonReader.hasNext()) {
                                            String key2 = jsonReader.nextName();
                                            if(key2.equals("href")) {
                                                playlist.setTracksUrl(jsonReader.nextString());
                                            } else if(key2.equals("total")) {
                                                playlist.setTotalTracks(jsonReader.nextInt());
                                            } else {
                                                jsonReader.skipValue();
                                            }
                                        }

                                        jsonReader.endObject();
                                    } else
                                        jsonReader.skipValue();
                                    Log.d(DEBUG_TAG, key1);
                                }

                                jsonReader.endObject();

                                playlists.add(playlist);
                            }

                            jsonReader.endArray();
                        } else {
                            jsonReader.skipValue(); // Skip values of other keys
                        }
                    }

                    Log.d(DEBUG_TAG, connection.getResponseMessage());

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(jsonReader != null) {
                        try {
                            jsonReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if(connection != null)
                        connection.disconnect();
                }

                return playlists;
            }
        };

        try {
            Object result = getPlaylists.execute(accessToken).get();
            Log.d(DEBUG_TAG, "Waiting");
            if(result instanceof Boolean) {
                //TODO: Richtig damit umgehen
            } else if(result instanceof LinkedList) {
                playlists = (LinkedList<StreamingServicePlaylist>) result;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return playlists;
    }

    @Override
    public List<Song> getSong(String name) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_shared_preferences), context.MODE_PRIVATE);
        String accessToken = sharedPreferences.getString(context.getString(R.string.spotify_access_token), null);

        if(accessToken == null) {
            //TODO: Exception werfen
            Log.e(DEBUG_TAG, "Fehler Spotify.java Line 80");
            return null;
        }

        AsyncTask<String, Integer, Object> searchTask = new AsyncTask<String, Integer, Object>() {


            @Override
            protected Object doInBackground(String... strings) {

                HttpURLConnection connection = null;

                JsonReader jsonReader = null;

                List<Song> result = new LinkedList<Song>();

                try {
                    String urlString = "https://api.spotify.com/v1/search?q=";
                    urlString += URLEncoder.encode(strings[1], "UTF-8");
                    //Type track scheint anscheinend für alles zu funktionieren
                    urlString += "&type=track";
                    Log.d(DEBUG_TAG, "Zum Überprüfen: " + urlString);
                    URL url = new URL(urlString);
                    connection = (HttpURLConnection) url.openConnection();
                    // Timeout for reading InputStream arbitrarily set to 3000ms.
                    connection.setReadTimeout(3000);
                    // Timeout for connection.connect() arbitrarily set to 3000ms.
                    connection.setConnectTimeout(3000);
                    // For this use case, set HTTP method to GET.
                    connection.setRequestMethod("GET");
                    // Already true by default but setting just in case; needs to be true since this request
                    // is carrying an input (response) body.
                    connection.setDoInput(true);

                    connection.setRequestProperty("Authorization", "Bearer " + strings[0]);

                    Log.d(DEBUG_TAG, "" + connection.getResponseCode());

                    if(connection.getResponseCode() != 200)
                        return false;

                    InputStream responseBody = connection.getInputStream();
                    InputStreamReader streamReader = new InputStreamReader(responseBody, "UTF-8");

                    jsonReader = new JsonReader(streamReader);
                    jsonReader.beginObject();
                        while(jsonReader.hasNext()) {
                            String key = jsonReader.nextName();
                            if(key.equals("tracks")) {
                                jsonReader.beginObject();
                                while (jsonReader.hasNext()) {
                                    String key1 = jsonReader.nextName();
                                    if (key1.equals("items")) {
                                        //region items
                                        jsonReader.beginArray();
                                        while (jsonReader.hasNext()) {
                                            jsonReader.beginObject();

                                            Song song = new Song();

                                            while (jsonReader.hasNext()) {
                                                String key2 = jsonReader.nextName();
                                                if (key2.equals("album")) {
                                                    //region album
                                                    jsonReader.beginObject();
                                                    while (jsonReader.hasNext()) {
                                                        String key3 = jsonReader.nextName();
                                                        if (key3.equals("name")) {
                                                            song.setAlbum(jsonReader.nextString());
                                                        } else {
                                                            jsonReader.skipValue();
                                                        }
                                                    }
                                                    jsonReader.endObject();
                                                    //endregion
                                                } else if (key2.equals("artists")) {
                                                    //region artists
                                                    jsonReader.beginArray();
                                                    while (jsonReader.hasNext()) {
                                                        jsonReader.beginObject();
                                                        while (jsonReader.hasNext()) {
                                                            String key4 = jsonReader.nextName();
                                                            if (key4.equals("name")) {
                                                                song.setArtist(jsonReader.nextString());
                                                            } else {
                                                                jsonReader.skipValue();
                                                            }
                                                        }
                                                        jsonReader.endObject();
                                                    }
                                                    jsonReader.endArray();
                                                    //endregion
                                                } else if (key2.equals("explicit")) {
                                                    song.setExplicit(jsonReader.nextBoolean());
                                                } else if (key2.equals("id")) {
                                                    song.setSpotifyID(jsonReader.nextString());
                                                } else if (key2.equals("name")) {
                                                    Log.d("UUUUUUUUUUU", "hier");
                                                    song.setName(jsonReader.nextString());
                                                } else {
                                                    jsonReader.skipValue();
                                                }
                                            }
                                            jsonReader.endObject();

                                            result.add(song);
                                        }
                                        jsonReader.endArray();
                                        //endregion
                                    } else
                                        jsonReader.skipValue();
                                }
                                jsonReader.endObject();
                            } else {
                                jsonReader.skipValue();
                            }
                        }
                    jsonReader.endObject();

                    return result;
                } catch (ProtocolException e) {
                    e.printStackTrace();
                    return false;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return false;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                } finally {
                    try {
                        if(jsonReader != null)
                            jsonReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if(connection != null)
                        connection.disconnect();

                    return result;
                }
            }
        };

        LinkedList<Song> searchResult = null;

        try {
            Object result = searchTask.execute(accessToken, name).get();
            if(result instanceof LinkedList)
                searchResult = (LinkedList<Song>) result;
            else {
                //TODO: Richtig damit umgehen
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return searchResult;
    }

    @Override
    public boolean removePermissions() {
        return false;
    }

    @Override
    public boolean login(int requestCode) {

        Intent i = new Intent(context, SpotifyLogin.class);
        context.startActivityForResult(i, requestCode);

        return false;
    }

    public void onDestroy() {
        // VERY IMPORTANT! This must always be called or else you will leak resources
        com.spotify.sdk.android.player.Spotify.destroyPlayer(context);
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d(DEBUG_TAG, "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d(DEBUG_TAG, "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d(DEBUG_TAG, "User logged in");
        player.playUri(null, uri, 0, 0);
    }

    @Override
    public void onLoggedOut() {
        Log.d(DEBUG_TAG, "User logged out");
    }

    @Override
    public void onLoginFailed(Error error) {
        Log.d(DEBUG_TAG, "LoginError occured");
    }

    @Override
    public void onTemporaryError() {
        Log.d(DEBUG_TAG, "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d(DEBUG_TAG, "Received connection message: " + message);
    }

    public class UserIDTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {
            HttpURLConnection connection = null;

            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_shared_preferences), context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = sharedPreferences.edit();

            JsonReader jsonReader = null;

            try {
                String urlString = "https://api.spotify.com/v1/me";
                Log.d(DEBUG_TAG, "Zum Überprüfen: " + urlString);
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                // Timeout for reading InputStream arbitrarily set to 3000ms.
                connection.setReadTimeout(3000);
                // Timeout for connection.connect() arbitrarily set to 3000ms.
                connection.setConnectTimeout(3000);
                // For this use case, set HTTP method to GET.
                connection.setRequestMethod("GET");
                // Already true by default but setting just in case; needs to be true since this request
                // is carrying an input (response) body.
                connection.setDoInput(true);

                connection.setRequestProperty("Authorization", "Bearer " + strings[0]);

                if(connection.getResponseCode() != 200)
                    return false;

                InputStream responseBody = connection.getInputStream();
                InputStreamReader streamReader = new InputStreamReader(responseBody, "UTF-8");

                jsonReader = new JsonReader(streamReader);
                jsonReader.beginObject(); // Start processing the JSON object
                while (jsonReader.hasNext()) { // Loop through all keys
                    String key = jsonReader.nextName(); // Fetch the next key
                    if (key.equals("id")) { // Check if desired key
                        // Fetch the value as a String
                        String value = jsonReader.nextString();

                        Log.d(DEBUG_TAG, "ID ist: " + value);

                        editor.putString("spotifyID", value);

                        break; // Break out of the loop
                    } else {
                        jsonReader.skipValue(); // Skip values of other keys
                    }
                }

                Log.d(DEBUG_TAG, connection.getResponseMessage());

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(jsonReader != null) {
                    try {
                        jsonReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                editor.commit();
                connection.disconnect();
            }

            return true;
        }
    }

}
