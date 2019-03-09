package com.mocoteam1.musicmatch.proto;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mocoteam1.musicmatch.R;
import com.mocoteam1.musicmatch.entities.Party;
import com.mocoteam1.musicmatch.entities.Song;
import com.mocoteam1.musicmatch.helper.SortedLinkedList;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

/**
 * Created by Tristan on 16.01.2018.
 */

public class PlayerService extends Service implements Player.NotificationCallback {

    private static final String DEBUG_TAG = PlayerService.class.getSimpleName();

    public static final String ACTION_INIT = "actionInit";
    public static final String RESULT_ACTION_INIT = "resultActionInit";
    public static final String RESULT_VALUE_ACTION_INIT = "resultValueActionInit";
    public static final String ACTION_PLAY = "actionPlay";
    public static final String SERVICE_RESPONSE = "serviceResponse";
    public static final String TYPE_PLAY_RESPONSE = "playResponse";
    public static final String RESULT_VALUE_PLAY = "resultValuePlay";
    public static final String ACTION_PAUSE = "actionPause";
    public static final String TYPE = "type";

    public static final String IS_PLAYING = "isPlaying";
    public static final String PARTY_ID = "partyID";
    public static final String TYPE_PAUSE_RESPONSE = "pauseResponse";
    public static final String RESULT_VALUE_PAUSE = "resultValuePause";

    public static final String CURRENT_SONG_NAME = "currentSongName";
    public static final String CURRENT_SONG_ARTIST = "currentSongArtist";
    public static final String CURRENT_SONG_ALBUM = "currentSongAlbum";

    public static final String TYPE_NEW_TRACK_INFORMATION = "typeNewTrackInformation";
    public static final String IS_FIRST_TRACK = "isFirstTrack";
    public static final String TYPE_IS_EMPTY_NOTIFICATION = "typeIsEmptyNotification";

    private SpotifyPlayer player;
    private static final String CLIENT_ID = "9d348edc95004d878a4e4cdbc0db66a2";
    private boolean isPlaying = false;
    private Song currentSong;

    private SortedLinkedList bufferedPlaylist;
    private SortedLinkedList activePlaylist;
    private DatabaseReference activePartyRef;
    private FirebaseListener activePartyListener;
    private FirebaseListener bufferedPartyListener;
    private int activeParty = -1;
    private int currentParty = -1;

    //region Firebase
    FirebaseDatabase database;
    DatabaseReference rootRef;
    DatabaseReference partyRef;

    ChildEventListener partyListener;
    //endregion


    @Override
    public void onCreate() {
        super.onCreate();
        //region Spotify
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_shared_preferences), MODE_PRIVATE);
        String accessToken = sharedPreferences.getString(getString(R.string.spotify_access_token), null);

        if(!com.mocoteam1.musicmatch.streamingservice.Spotify.isAccessTokenValid(this)) {
            Intent initResult = new Intent(RESULT_ACTION_INIT);
            initResult.putExtra(RESULT_VALUE_ACTION_INIT, false);
            LocalBroadcastManager.getInstance(this).sendBroadcast(initResult);
            stopSelf();
            return;
        }

        //GGF Performance einsparen durch player != null abfrage?
        Config config = new Config(this, accessToken, CLIENT_ID);
        Spotify.getPlayer(config, this, new SpotifyPlayer.InitializationObserver() {
            @Override
            public void onInitialized(SpotifyPlayer spotifyPlayer) {
                player = spotifyPlayer;
                player.addNotificationCallback(PlayerService.this);
            }

            @Override
            public void onError(Throwable throwable) {
                Intent resultIntent = new Intent(RESULT_ACTION_INIT);
                resultIntent.putExtra(RESULT_VALUE_ACTION_INIT, false);
                LocalBroadcastManager.getInstance(PlayerService.this).sendBroadcast(resultIntent);
                stopSelf();
            }
        });
        //endregion
    }

    public class FirebaseListener implements ChildEventListener {
        SortedLinkedList manipulatedList = bufferedPlaylist;

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            if (dataSnapshot.getKey().equals("nextID"))
                return;
            int songID = Integer.parseInt(dataSnapshot.getKey().replace("songID:", ""));
            Song song = new Song();
            song.setSongID(songID);
            if (manipulatedList.containsID(song) == -1) {
                song.setName(dataSnapshot.child("name").getValue(String.class));
                song.setArtist(dataSnapshot.child("artist").getValue(String.class));
                song.setAlbum(dataSnapshot.child("album").getValue(String.class));
                song.setUpvotes(dataSnapshot.child("upvotes").getValue(Long.class).intValue());
                song.setDownvotes(dataSnapshot.child("downvotes").getValue(Long.class).intValue());
                song.setSpotifyID(dataSnapshot.child("spotifyID").getValue(String.class));
                song.setExplicit(dataSnapshot.child("isExplicit").getValue(Boolean.class).booleanValue());
                manipulatedList.addSorted(song);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            if (dataSnapshot.getKey().equals("nextID"))
                return;
            int songID = Integer.parseInt(dataSnapshot.getKey().replace("songID:", ""));
            Song song = new Song();
            song.setSongID(songID);
            //If abfrage redundant?
            int index = -1;
            if ((index = manipulatedList.containsID(song)) != -1) {
                song = (Song) manipulatedList.get(index);
                song.setName(dataSnapshot.child("name").getValue(String.class));
                song.setArtist(dataSnapshot.child("artist").getValue(String.class));
                song.setAlbum(dataSnapshot.child("album").getValue(String.class));
                song.setUpvotes(dataSnapshot.child("upvotes").getValue(Long.class).intValue());
                song.setDownvotes(dataSnapshot.child("downvotes").getValue(Long.class).intValue());
                song.setSpotifyID(dataSnapshot.child("spotifyID").getValue(String.class));
                song.setExplicit(dataSnapshot.child("isExplicit").getValue(Boolean.class).booleanValue());
                Collections.sort(manipulatedList);
                Collections.reverse(manipulatedList);
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            if (dataSnapshot.getKey().equals("nextID"))
                return;
            int songID = Integer.parseInt(dataSnapshot.getKey().replace("songID:", ""));
            Song song = new Song();
            song.setSongID(songID);
            //If abfrage redundant?
            int index = -1;
            if ((index = manipulatedList.containsID(song)) != -1) {
                song = (Song) manipulatedList.get(index);
                manipulatedList.remove(index);
                Collections.sort(manipulatedList);
                Collections.reverse(manipulatedList);
            }
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }

        public void setManipulatedList(SortedLinkedList manipulatedList) {
            this.manipulatedList = manipulatedList;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();

        switch (intent.getAction()) {
            case ACTION_INIT:
                //region action_init
                Intent initResult = new Intent(RESULT_ACTION_INIT);
                int joinedPartyID = extras.getInt(PARTY_ID, -1);
                if(activeParty == joinedPartyID) {
                    currentParty = joinedPartyID;
                    initResult.putExtra(IS_FIRST_TRACK, false);
                    initResult.putExtra(IS_PLAYING, isPlaying);
                    if(isPlaying && currentSong != null) {
                        initResult.putExtra(CURRENT_SONG_NAME, currentSong.getName());
                        initResult.putExtra(CURRENT_SONG_ARTIST, currentSong.getArtist());
                        initResult.putExtra(CURRENT_SONG_ALBUM, currentSong.getAlbum());
                    }
                    LocalBroadcastManager.getInstance(this).sendBroadcast(initResult);
                    break;
                }

                if(currentParty != joinedPartyID) {
                    currentParty = joinedPartyID;
                    //TODO: Exception werfen falls id == -1
                    database = FirebaseDatabase.getInstance();
                    rootRef = database.getReference();
                    partyRef = rootRef.child("party-playlist").child("partyID:" + currentParty);

                    bufferedPlaylist = new SortedLinkedList();
                    //TODO: GGF Ladebalken anzeigen, der indiziert, ob sich mindestens
                    //ein Song in der Playlist befindet
                    bufferedPartyListener = new FirebaseListener();

                    partyRef.addChildEventListener(bufferedPartyListener);

                    initResult.putExtra(IS_FIRST_TRACK, true);
                } else {
                    initResult.putExtra(IS_FIRST_TRACK, false);
                    initResult.putExtra(IS_PLAYING, isPlaying);
                    if(isPlaying && currentSong != null) {
                        initResult.putExtra(CURRENT_SONG_NAME, currentSong.getName());
                        initResult.putExtra(CURRENT_SONG_ARTIST, currentSong.getArtist());
                        initResult.putExtra(CURRENT_SONG_ALBUM, currentSong.getAlbum());
                    }
                }

                LocalBroadcastManager.getInstance(this).sendBroadcast(initResult);
                break;
                //endregion
            case ACTION_PLAY:
                Intent resultIntent1 = new Intent(SERVICE_RESPONSE);
                resultIntent1.putExtra(TYPE, TYPE_PLAY_RESPONSE);
                resultIntent1.putExtra(RESULT_VALUE_PLAY, play());
                LocalBroadcastManager.getInstance(PlayerService.this).sendBroadcast(resultIntent1);
                break;
            case ACTION_PAUSE:
                Intent resultIntent2 = new Intent(SERVICE_RESPONSE);
                resultIntent2.putExtra(TYPE, TYPE_PAUSE_RESPONSE);
                resultIntent2.putExtra(RESULT_VALUE_PAUSE, pause());
                LocalBroadcastManager.getInstance(PlayerService.this).sendBroadcast(resultIntent2);
                break;
            default:
                break;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private boolean play() {
        if(!player.isLoggedIn()) {
            return false;
        }

        boolean isNewParty = false;

        if(activeParty != currentParty) {
            activeParty = currentParty;
            activePlaylist = bufferedPlaylist;
            if(activePartyListener != null)
                activePartyRef.removeEventListener(activePartyListener);
            activePartyListener = bufferedPartyListener;
            activePartyListener.setManipulatedList(activePlaylist);
            activePartyRef = partyRef;
            isNewParty = true;
        }

        return playNext(isNewParty);
    }

    private boolean pause() {
        if(!player.isLoggedIn()) {
            return false;
        }

        player.pause(null);
        isPlaying = false;
        return true;
    }

    private boolean playNext(boolean isNewParty) {
        if (!player.isLoggedIn()) {
            return false;
        }

        player.resume(null);
        /*
        * Es kann passieren, dass resume zwar ein Lied abspielen möchte, der Thread aber zuschnell
        * bei playUri ankommt und stattdessen ein neues Lied spielt, was allerdings nicht der
        * Realität entspricht.
        *
        * Workaroundlösung: Den Thread so lange schlagen legen, dass das Resume ausreichend Zeit
        * hat um sich zu start
        */
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(!isNewParty && player.getPlaybackState().isPlaying) {
            isPlaying = true;
            Intent trackChangedIntent = new Intent(SERVICE_RESPONSE);
            trackChangedIntent.putExtra(TYPE, TYPE_NEW_TRACK_INFORMATION);
            trackChangedIntent.putExtra(CURRENT_SONG_NAME, currentSong.getName());
            trackChangedIntent.putExtra(CURRENT_SONG_ARTIST, currentSong.getArtist());
            trackChangedIntent.putExtra(CURRENT_SONG_ALBUM, currentSong.getAlbum());
            LocalBroadcastManager.getInstance(this).sendBroadcast(trackChangedIntent);
            return true;
        }

        if(activePlaylist.isEmpty()) {
            Intent emptyNotification = new Intent(SERVICE_RESPONSE);
            emptyNotification.putExtra(TYPE, TYPE_IS_EMPTY_NOTIFICATION);
            LocalBroadcastManager.getInstance(this).sendBroadcast(emptyNotification);
            return false;
        }
        Song nextSong = (Song) activePlaylist.getFirst();
        player.playUri(null, "spotify:track:" + nextSong.getSpotifyID(), 0, 0);
        currentSong = nextSong;
        //TODO Song entfernen auch aus Datenbank
        activePartyRef.child("songID:" + currentSong.getSongID()).removeValue();

        isPlaying = true;

        Intent trackChangedIntent = new Intent(SERVICE_RESPONSE);
        trackChangedIntent.putExtra(TYPE, TYPE_NEW_TRACK_INFORMATION);
        trackChangedIntent.putExtra(CURRENT_SONG_NAME, currentSong.getName());
        trackChangedIntent.putExtra(CURRENT_SONG_ARTIST, currentSong.getArtist());
        trackChangedIntent.putExtra(CURRENT_SONG_ALBUM, currentSong.getAlbum());
        LocalBroadcastManager.getInstance(this).sendBroadcast(trackChangedIntent);

        return true;
    }

    private void removeAllFirebaseListener() {
        if(activePartyListener != null) {
            activePartyRef.removeEventListener(activePartyListener);
        }

        if(bufferedPartyListener != null) {
            partyRef.removeEventListener(bufferedPartyListener);
        }

        Log.d(DEBUG_TAG, "All FirebaseListener got removed");
    }

    @Override
    public void onDestroy() {
        //Vllt ersetzen durch awaitDestroy?
        removeAllFirebaseListener();
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d(DEBUG_TAG, "" + playerEvent);

        switch (playerEvent) {
            case kSpPlaybackNotifyPause:
                Log.d(DEBUG_TAG, "Pause Event happend");
                //player.resume(null);
                break;
            case kSpPlaybackNotifyTrackDelivered:
                playNext(false);
                break;
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {

    }
}
