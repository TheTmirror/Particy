package com.mocoteam1.musicmatch.entities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import com.mocoteam1.musicmatch.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by Tristan on 04.01.2018.
 */

public class StreamingServicePlaylist {

    private static final String DEBUG_TAG = StreamingServicePlaylist.class.getSimpleName();

    private String name;
    private String id;
    private List<Song> songs;
    private String tracksUrl;
    private int totalTracks;

    public StreamingServicePlaylist() {

    };

    public StreamingServicePlaylist(String name) {
        this.name = name;

        songs = new LinkedList<Song>();

        for(int i = 0; i < 10; i++) {
            Song song = new Song();
            song.setSongID(i);
            song.setName("Song " + i);
            song.setArtist("Artist " + i);
            song.setAlbum("Album " + i);
            song.setUpvotes(0);
            song.setDownvotes(0);

            songs.add(song);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private void updateSongs(Context context) {
        AsyncTask<String, Integer, Object> updateTask = new SongFetcher();

        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_shared_preferences), context.MODE_PRIVATE);
        String accessToken = sharedPreferences.getString(context.getString(R.string.spotify_access_token), null);

        if(accessToken == null) {
            //TODO: Exception werfen
            Log.e(DEBUG_TAG, "Fehler Spotify.java Line 80");
            return;
        }

        try {
            Object result = updateTask.execute(accessToken, tracksUrl, "" + totalTracks).get();
            if(result instanceof LinkedList) {
                songs = (List<Song>) result;
            } else {
                //TODO: Boolean tritt nur auf wenn Fehler. Passende Exception basteln
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public List<Song> updateAndGetSongs(Context context) {
        updateSongs(context);
        return getSongs();
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTracksUrl() {
        return tracksUrl;
    }

    public void setTracksUrl(String tracksUrl) {
        this.tracksUrl = tracksUrl;
    }

    public int getTotalTracks() {
        return totalTracks;
    }

    public void setTotalTracks(int totalTracks) {
        this.totalTracks = totalTracks;
    }

    private class SongFetcher extends AsyncTask<String, Integer, Object> {

        private static final int SONG_LIMIT = 100;

        @Override
        protected Object doInBackground(String... strings) {

            String accessToken = strings[0];
            String urlFormatString = strings[1]+"?offset=%d";
            int total = Integer.parseInt(strings[2]);
            int requestCounts = total / SONG_LIMIT;
            if(total % SONG_LIMIT != 0)
                requestCounts++;

            HttpURLConnection connection = null;

            JsonReader jsonReader = null;
            final List<Song> songs = new LinkedList<Song>();
            int songID = 1;

            for(int i = 0; i < requestCounts; i++) {

                try {
                    String urlString = String.format(urlFormatString, i * SONG_LIMIT);
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

                    //TODO: In Zukunft abfangen was passiert wenn accessToken abläuft während der Schleifen
                    connection.setRequestProperty("Authorization", "Bearer " + accessToken);

                    Log.d(DEBUG_TAG, "" + connection.getResponseCode());

                    if (connection.getResponseCode() != 200)
                        return false;

                    InputStream responseBody = connection.getInputStream();
                    InputStreamReader streamReader = new InputStreamReader(responseBody, "UTF-8");
                    jsonReader = new JsonReader(streamReader);
                    jsonReader.beginObject(); // Start processing the JSON object

                    while(jsonReader.hasNext()){
                        String key = jsonReader.nextName();
                        if(key.equals("items")) {
                            jsonReader.beginArray();

                            while(jsonReader.hasNext()) {
                                jsonReader.beginObject();
                                Song song = new Song();
                                song.setSongID(songID++);

                                while(jsonReader.hasNext()) {
                                    String key1 = jsonReader.nextName();
                                    if(key1.equals("track")) {
                                        jsonReader.beginObject();

                                        while(jsonReader.hasNext()) {
                                            String key2 = jsonReader.nextName();
                                            if(key2.equals("album")) {
                                                jsonReader.beginObject();

                                                while(jsonReader.hasNext()) {
                                                    String key3 = jsonReader.nextName();
                                                    if(key3.equals("name")) {
                                                        song.setAlbum(jsonReader.nextString());
                                                    } else {
                                                        jsonReader.skipValue();
                                                    }
                                                }

                                                jsonReader.endObject();
                                            } else if(key2.equals("artists")) {
                                                jsonReader.beginArray();

                                                while(jsonReader.hasNext()) {
                                                    jsonReader.beginObject();

                                                    while(jsonReader.hasNext()) {
                                                        String key4 = jsonReader.nextName();
                                                        if(key4.equals("name")) {
                                                            song.addArtist(jsonReader.nextString());
                                                        } else {
                                                            jsonReader.skipValue();
                                                        }
                                                    }

                                                    jsonReader.endObject();
                                                }

                                                jsonReader.endArray();
                                            } else if(key2.equals("explicit")) {
                                                song.setExplicit(jsonReader.nextBoolean());
                                            } else if(key2.equals("id")) {
                                                song.setSpotifyID(jsonReader.nextString());
                                            } else if(key2.equals("name")) {
                                                song.setName(jsonReader.nextString());
                                            } else {
                                                jsonReader.skipValue();
                                            }
                                        }

                                        jsonReader.endObject();
                                    } else
                                        jsonReader.skipValue();
                                }

                                jsonReader.endObject();
                                songs.add(song);
                            }

                            jsonReader.endArray();
                        } else
                            jsonReader.skipValue();
                    }

                    jsonReader.endObject();

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return false;
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
                    if (jsonReader != null) {
                        try {
                            jsonReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (connection != null)
                        connection.disconnect();
                }

            }

            return songs;
        }

    }
}
