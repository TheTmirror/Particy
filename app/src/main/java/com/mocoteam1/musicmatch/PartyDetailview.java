package com.mocoteam1.musicmatch;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.mocoteam1.musicmatch.entities.Party;
import com.mocoteam1.musicmatch.entities.PartySettings;
import com.mocoteam1.musicmatch.entities.Song;
import com.mocoteam1.musicmatch.helper.PlaylistAdapter;
import com.mocoteam1.musicmatch.proto.PlayerService;
import com.mocoteam1.musicmatch.streamingservice.Spotify;
import com.spotify.sdk.android.player.Player;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * TODO: Siehe PartyOverview Topic to do
 *
 * TODO: Überprüfen wann der erste Call von ValueEvent/ChildEvent stattfindet.
 * Ziel: Das Trennen von Reaktion/Abruf von Werten aus der Datenbank und der Systemlogik
 * Momentane Vermutung: Zuerst wird die Methode des Lifecycleabschnitts (onStart etc..) zu
 * ende ausgeführt. Dann wird NACH dem Beenden des Lifecycles und VOR dem Start des nächsten Lifecycles
 * der Call ausgefüht. Nicht sicher - nur Vermutung. Scheinbar geht dies nur für child Events!
 */
public class PartyDetailview extends AppCompatActivity {

    private static final String DEBUG_TAG = PartyDetailview.class.getSimpleName();

    public static final String PARTY_ID = "partyID";
    public static final String PARTY_OWNER = "partyOwner";
    private static final int ACCESS_FINE_LOCATION_RQ_CODE = 2307;

    private boolean wait = true;

    private String userID;
    private String userName;
    //private int partyID;
    private PartySettings settings;

    private Party party;

    //FirebaseAuth
    FirebaseAuth auth;
    FirebaseUser user;

    //FirebaseDatabase
    FirebaseDatabase database;
    DatabaseReference rootRef;
    DatabaseReference partyMetaRef;
    DatabaseReference partySettingsRef;
    DatabaseReference partyPlaylistRef;
    DatabaseReference userPartiesRef;

    //Firebase Listener
    ValueEventListener partyMetaRefChildNameListener;
    ValueEventListener partyMetaRefChildOwnerListener;
    ChildEventListener partySettingsRefListener;
    ChildEventListener partyPlaylistRefListener;

    //Playliste
    ListView playlistView;
    HashMap<Integer, Song> songs;
    PlaylistAdapter adapter;

    //View
    ImageButton playPartyButton;

    //Receiver
    LinkedList<BroadcastReceiver> receivers = new LinkedList<BroadcastReceiver>();

    //Location
    public static final String CURRENT_LONGITUDE = "currentLongitude";
    public static final String CURRENT_LATITUDE = "currentLatitude";
    private LocationManager locationManager;
    private String bestLocationProvider;
    private LocationListener locationListener;

    //Workaround
    private boolean isAboutToClose;
    private boolean askedAlreadyThisInstance = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_party_detailview);

        Bundle intentExtras = getIntent().getExtras();
        int partyID = intentExtras.getInt(PARTY_ID);

        party = new Party();
        settings = new PartySettings();
        songs = new HashMap<Integer, Song>();

        party.setPartyID(partyID);
        party.setSettings(settings);
        party.setPlaylist(songs);

        playlistView = (ListView) findViewById(R.id.partyPlaylistListview);
        adapter = new PlaylistAdapter(this, songs, party.getPartyID());
        playlistView.setAdapter(adapter);

        playPartyButton = (ImageButton) findViewById(R.id.partyPlayParty);

        isAboutToClose = false;
    }

    //TODO: Firebase Resources NotFound Exception abfangen
    @Override
    protected void onStart() {
        super.onStart();

        //FirebaseAuth Init
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        userID = user.getUid();
        userName = user.getEmail();

        //FirebaseDatabase Init
        database = FirebaseDatabase.getInstance();
        rootRef = database.getReference();
        partyMetaRef = rootRef.child("party-meta").child("partyID:" + party.getPartyID());
        partySettingsRef = rootRef.child("party-settings").child("partyID:" + party.getPartyID());
        partyPlaylistRef = rootRef.child("party-playlist").child("partyID:" + party.getPartyID());
        userPartiesRef = rootRef.child("user").child("userID:" + userID).child("parties");

        /**
         * TODO: Das signaliesieren wenn die Party beendet wird wenn jemand gerade im Party Bildschirm ist gefällt mir nicht.
         * Ich würde dies lieber über das auslösen eines Events lösen. Allerdings würden die Listener der Party trotzdem reagieren,
         * da es ValueEventListener sind und dass remove als changeEvent erkennen. Lieber
         * ChildEventListener? Beobachten diese ggf zu viel an Daten? Noch andere Möglichkeiten in Betracht ziehen?
         */
        partyMetaRefChildNameListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                /**
                 * Party wurde gelöscht von jemand anderes während man
                 * sich in der Party befindet
                 */
                if(dataSnapshot.getValue() == null) {
                    PartyClosedNotification notification = new PartyClosedNotification();
                    notification.show(getFragmentManager(), "PartyClosedNotification");
                    isAboutToClose = true;
                    return;
                }

                party.setPartyName(dataSnapshot.getValue(String.class));
                ((TextView) findViewById(R.id.partyNameText)).setText(party.getPartyName());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        partyMetaRefChildOwnerListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                /**
                 * Party wurde gelöscht von jemand anderes während man
                 * sich in der Party befindet
                 *
                 * Ist nötig, da sonst die Listener auf nicht vorhandene Werte reagieren wollen
                 */
                if(isAboutToClose)
                    return;

                party.setPartyOwner(dataSnapshot.getValue(String.class));
                ((TextView) findViewById(R.id.partyOwnerText)).setText(party.getPartyOwner());

                //region ownermode

                /**
                 * Ownermode
                 *
                 * Passende Berechtigungen aktivieren wenn der User gleichzeitig der Owner der
                 * Party ist
                 */
                if(party.getPartyOwner().equals(userName)) {
                    ImageButton showQRCodeButton = (ImageButton) findViewById(R.id.partyShowQRCodeButton);
                    //TODO: Wieder aktivieren nachdem Lizenzfragen geklärt sind
                    showQRCodeButton.setVisibility(View.GONE);
                    showQRCodeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent i = new Intent(PartyDetailview.this, ShowQRCode.class);
                            i.putExtra(PARTY_ID, party.getPartyID());
                            i.putExtra(PARTY_OWNER, party.getPartyOwner());
                            startActivity(i);
                        }
                    });

                    ImageButton showUserButton = (ImageButton) findViewById(R.id.partyShowUserButton);
                    showUserButton.setVisibility(View.GONE);
                    showUserButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //TODO: Um diese Funktionalität zu enablen muss erst die Datenbank noch angepasst werden
                        }
                    });

                    ImageButton deleteParty = (ImageButton) findViewById(R.id.partyQuitPartyButton);
                    deleteParty.setVisibility(View.VISIBLE);
                    deleteParty.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            /**
                             * TODO: *Solved* Prüfen ob bei entfernen der Knoten auch die Listeners mit
                             * entfernt werden. Wenn dem nämlich nicht so ist, werden diese erst in
                             * onStop gelöscht was zu spät ist. Sie müssen dann davor hier gelöscht
                             * werden und zwar VOR dem entfernen der Values
                             *
                             * EDIT: Scheinbar müssen Sie entfernt werden, da die Aktivity sonst versucht
                             * wert mit equals zu überprüfen die es absolut nicht mehr gibt.
                             * => NullPointerException
                             */

                            removeFirebaseDatabaseListeners();

                            partyMetaRef.removeValue();
                            partyPlaylistRef.removeValue();
                            partySettingsRef.removeValue();
                            userPartiesRef.child("partyID:" + party.getPartyID()).removeValue();

                            Log.d(DEBUG_TAG, "Party wurde erfolgreich entfernt");

                            //Notification dass die Party erfolgreich entfern wurde
                            Toast notification = Toast.makeText(PartyDetailview.this, getString(R.string.party_party_remove_success_text), Toast.LENGTH_LONG);
                            notification.show();

                            /**
                             * TODO: Notification-Event auslösen
                             * Die anderen Clients müssen darüber informiert werden, dass es diese
                             * Party nicht mehr gibt. Dies kann vernachlässigt werden, wenn in den
                             * Klassen PartyOverview und PartyDetailview per Listener (ChildRemoved)
                             * auf das Event reagiert wird.
                             */

                            finish();
                        }
                    });

                    if(Spotify.isAccessTokenValid(PartyDetailview.this)) {
                        setAndInitBroadcastsForPlayer();

                        Intent startService = new Intent(PartyDetailview.this, PlayerService.class);
                        startService.setAction(PlayerService.ACTION_INIT);
                        startService.putExtra(PlayerService.PARTY_ID, party.getPartyID());
                        startService(startService);
                    } else {
                        //TODO: Was tun wenn das AccessToken nicht valid ist
                    }

                    //endregion
                } else {
                    /**
                     * Usermode
                     * Passende Berechtigungen werden aktiviert
                     */
                    ImageButton exitParty = (ImageButton) findViewById(R.id.partyQuitPartyButton);
                    exitParty.setVisibility(View.VISIBLE);
                    exitParty.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            userPartiesRef.child("partyID:" + party.getPartyID()).removeValue();

                            Log.d(DEBUG_TAG, "User " + userID + " hat erfolgreich die Party " + party.getPartyID() + " verlassen");

                            //Notification
                            Toast notification = Toast.makeText(PartyDetailview.this, getString(R.string.party_party_leave_success_text), Toast.LENGTH_LONG);
                            notification.show();

                            /**
                             * TODO: Bidirektionale Abhängigkeit aus der DB entfernen
                             * In Zukunft wird es auch eine Liste geben, ind er alle Teilnehmer einer
                             * Party nachgehalten sind (für den schnelleren Access aus Sicht des Hosts).
                             * Dort muss der User auch gelöscht werden
                             */

                            finish();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        partySettingsRefListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.getKey().equals("addAllowed")) {
                    if(settings != null)
                        settings.setAddAllowed(dataSnapshot.getValue(Boolean.class).booleanValue());
                } else if(dataSnapshot.getKey() == "radius") {
                    if(settings != null)
                        settings.setRadius(dataSnapshot.getValue(Long.class).intValue());
                } else {
                    //throw new Exception("This should not happen!");
                }

                if(Spotify.isAccessTokenValid(PartyDetailview.this) && (settings.isAddAllowed() || (party.getPartyOwner() != null && party.getPartyOwner().equals(userName)))) {
                    ImageButton addSongButton = (ImageButton) findViewById(R.id.partyAddSongButton);
                    addSongButton.setVisibility(View.VISIBLE);
                    addSongButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent i = new Intent(PartyDetailview.this, SearchSongSample.class);
                            i.putExtra(PARTY_ID, party.getPartyID());
                            startActivity(i);
                        }
                    });
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                //Ist dies sinnvoll die Methode abzubilden?
                onChildAdded(dataSnapshot, s);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                /**
                 * Dies sollte eigentlich nicht passieren
                 *
                 * TODO: Passende Expception entwerfen
                 */
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                /**
                 * Dies sollte eigentlich nicht passieren
                 *
                 * TODO: Passende Expception entwerfen
                 */
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        partyPlaylistRefListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.getKey().equals("nextID"))
                    return;
                int songID = Integer.parseInt(dataSnapshot.getKey().replace("songID:",""));
                Log.d(DEBUG_TAG, "songID: " + songID);
                if(!songs.containsKey(songID)) {
                    Song song = new Song();
                    song.setSongID(songID);
                    song.setName(dataSnapshot.child("name").getValue(String.class));
                    song.setArtist(dataSnapshot.child("artist").getValue(String.class));
                    song.setAlbum(dataSnapshot.child("album").getValue(String.class));
                    song.setUpvotes(dataSnapshot.child("upvotes").getValue(Long.class).intValue());
                    song.setDownvotes(dataSnapshot.child("downvotes").getValue(Long.class).intValue());
                    songs.put(songID, song);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.getKey().equals("nextID"))
                    return;
                int songID = Integer.parseInt(dataSnapshot.getKey().replace("songID:",""));
                Log.d(DEBUG_TAG, "songID: " + songID);
                if(songs.containsKey(songID)) {
                    Song song = songs.get(songID);
                    song.setSongID(songID);
                    song.setName(dataSnapshot.child("name").getValue(String.class));
                    song.setArtist(dataSnapshot.child("artist").getValue(String.class));
                    song.setAlbum(dataSnapshot.child("album").getValue(String.class));
                    song.setUpvotes(dataSnapshot.child("upvotes").getValue(Long.class).intValue());
                    song.setDownvotes(dataSnapshot.child("downvotes").getValue(Long.class).intValue());
                    songs.put(songID, song);
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e(DEBUG_TAG, "Dies sollte nicht passieren");
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getKey().equals("nextID"))
                    return;
                int songID = Integer.parseInt(dataSnapshot.getKey().replace("songID:",""));
                Log.d(DEBUG_TAG, "songID: " + songID);
                if(songs.containsKey(songID)) {
                    songs.remove(songID);
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e(DEBUG_TAG, "Dies sollte nicht passieren");
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        };

        partyMetaRef.child("name").addValueEventListener(partyMetaRefChildNameListener);
        partyMetaRef.child("owner").addValueEventListener(partyMetaRefChildOwnerListener);
        partySettingsRef.addChildEventListener(partySettingsRefListener);
        partyPlaylistRef.addChildEventListener(partyPlaylistRefListener);

        Log.d(DEBUG_TAG, "Alle Datenbank Listener wurden korrekt angehangen");
    }

    private void setAndInitBroadcastsForPlayer() {
        //region PlayButtonListener
        final View.OnClickListener playClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startPlayingIntent = new Intent(PartyDetailview.this, PlayerService.class);
                startPlayingIntent.setAction(PlayerService.ACTION_PLAY);
                startService(startPlayingIntent);
            }
        };

        final View.OnClickListener pauseClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startPlayingIntent = new Intent(PartyDetailview.this, PlayerService.class);
                startPlayingIntent.setAction(PlayerService.ACTION_PAUSE);
                startService(startPlayingIntent);
            }
        };
        //endregion

        //region NewTrackInformationReceiver
        final IntentFilter newTrackInformationFilter = new IntentFilter(PlayerService.SERVICE_RESPONSE);
        final BroadcastReceiver newTrackInformationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();

                switch (extras.getString(PlayerService.TYPE)) {
                    case PlayerService.TYPE_NEW_TRACK_INFORMATION:
                        TextView currentSongName = (TextView) findViewById(R.id.partyCurrentSongName);
                        TextView currentSongArtist = (TextView) findViewById(R.id.partyCurrentSongArtist);
                        TextView currentSongAlbum = (TextView) findViewById(R.id.partyCurrentSongAlbum);

                        if(extras.getString(PlayerService.CURRENT_SONG_NAME, null) != null) {
                            currentSongName.setText(extras.getString(PlayerService.CURRENT_SONG_NAME));
                            currentSongArtist.setText(extras.getString(PlayerService.CURRENT_SONG_ARTIST, null));
                            currentSongAlbum.setText(extras.getString(PlayerService.CURRENT_SONG_ALBUM, null));

                            if(currentSongName.getVisibility() != View.VISIBLE)
                                currentSongName.setVisibility(View.VISIBLE);
                            if(currentSongArtist.getVisibility() != View.VISIBLE)
                                currentSongArtist.setVisibility(View.VISIBLE);
                            if(currentSongAlbum.getVisibility() != View.VISIBLE)
                                currentSongAlbum.setVisibility(View.VISIBLE);
                        }
                        break;
                }
            }
        };
        //endregion

        //region PlayReceiver
        //Der Receiver sollte nicht mehr Informationen enthalten
        final IntentFilter playFilter = new IntentFilter(PlayerService.SERVICE_RESPONSE);
        final BroadcastReceiver playReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();

                switch (extras.getString(PlayerService.TYPE)) {
                    case PlayerService.TYPE_PLAY_RESPONSE:
                        if(extras.getBoolean(PlayerService.RESULT_VALUE_PLAY, false)) {
                            ImageButton mediaControllButton = (ImageButton) findViewById(R.id.partyPlayParty);
                            TextView currentSongName = (TextView) findViewById(R.id.partyCurrentSongName);
                            TextView currentSongArtist = (TextView) findViewById(R.id.partyCurrentSongArtist);
                            TextView currentSongAlbum = (TextView) findViewById(R.id.partyCurrentSongAlbum);

                            mediaControllButton.setImageDrawable(getDrawable(R.drawable.pause));
                            mediaControllButton.setOnClickListener(pauseClickListener);
                            if (mediaControllButton.getVisibility() != View.VISIBLE) {
                                mediaControllButton.setVisibility(View.VISIBLE);
                            }

                            if (currentSongName.getVisibility() != View.VISIBLE)
                                currentSongName.setVisibility(View.VISIBLE);
                            if (currentSongArtist.getVisibility() != View.VISIBLE)
                                currentSongArtist.setVisibility(View.VISIBLE);
                            if (currentSongAlbum.getVisibility() != View.VISIBLE)
                                currentSongAlbum.setVisibility(View.VISIBLE);
                        } else {
                            //TODO: Fehlermeldung
                        }
                        break;
                }
            }
        };
        //endregion

        //region emptyReceiver
        final IntentFilter emptyFilter = new IntentFilter(PlayerService.SERVICE_RESPONSE);
        final BroadcastReceiver emptyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent){
                Bundle extras = intent.getExtras();

                switch (extras.getString(PlayerService.TYPE)) {
                    case PlayerService.TYPE_IS_EMPTY_NOTIFICATION:
                        EmptyPlaylistNotification notification = new EmptyPlaylistNotification();
                        notification.show(getFragmentManager(), "EmptyPlaylistNotification");
                        break;
                    default:
                        break;
                }
            }
        };
        //endregion

        //region PauseReceiver
        //Der Receiver sollte nicht mehr Informationen enthalten
        final IntentFilter pauseFilter = new IntentFilter(PlayerService.SERVICE_RESPONSE);
        final BroadcastReceiver pauseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();

                switch (extras.getString(PlayerService.TYPE)) {
                    case PlayerService.TYPE_PAUSE_RESPONSE:
                        ImageButton mediaControllButton = (ImageButton) findViewById(R.id.partyPlayParty);

                        mediaControllButton.setImageDrawable(getDrawable(R.drawable.play));
                        mediaControllButton.setOnClickListener(playClickListener);
                        if(mediaControllButton.getVisibility() != View.VISIBLE) {
                            mediaControllButton.setVisibility(View.VISIBLE);
                        }
                        break;
                }
            }
        };
        //endregion

        //region initReceiver  und registieren der Broadcasts
        IntentFilter initFilter = new IntentFilter(PlayerService.RESULT_ACTION_INIT);
        BroadcastReceiver initReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(PartyDetailview.this);

                if(!extras.getBoolean(PlayerService.RESULT_VALUE_ACTION_INIT, true)) {
                    PlayerInitError playerInitError = new PlayerInitError();
                    playerInitError.show(getFragmentManager(), "PlayerInitError");
                    broadcastManager.unregisterReceiver(this);
                    return;
                }

                ImageButton mediaControllButton = (ImageButton) findViewById(R.id.partyPlayParty);
                TextView currentSongName = (TextView) findViewById(R.id.partyCurrentSongName);
                TextView currentSongArtist = (TextView) findViewById(R.id.partyCurrentSongArtist);
                TextView currentSongAlbum = (TextView) findViewById(R.id.partyCurrentSongAlbum);

                if(extras.getBoolean(PlayerService.IS_FIRST_TRACK)) {
                    mediaControllButton.setImageDrawable(getDrawable(R.drawable.play));
                    mediaControllButton.setOnClickListener(playClickListener);
                    if(mediaControllButton.getVisibility() != View.VISIBLE) {
                        mediaControllButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    if(extras.getBoolean(PlayerService.IS_PLAYING)) {
                        mediaControllButton.setImageDrawable(getDrawable(R.drawable.pause));
                        mediaControllButton.setOnClickListener(pauseClickListener);
                        if (mediaControllButton.getVisibility() != View.VISIBLE) {
                            mediaControllButton.setVisibility(View.VISIBLE);
                        }

                        if(extras.getString(PlayerService.CURRENT_SONG_NAME, null) != null) {
                            currentSongName.setText(extras.getString(PlayerService.CURRENT_SONG_NAME));
                            currentSongArtist.setText(extras.getString(PlayerService.CURRENT_SONG_ARTIST, null));
                            currentSongAlbum.setText(extras.getString(PlayerService.CURRENT_SONG_ALBUM, null));

                            if(currentSongName.getVisibility() != View.VISIBLE)
                                currentSongName.setVisibility(View.VISIBLE);
                            if(currentSongArtist.getVisibility() != View.VISIBLE)
                                currentSongArtist.setVisibility(View.VISIBLE);
                            if(currentSongAlbum.getVisibility() != View.VISIBLE)
                                currentSongAlbum.setVisibility(View.VISIBLE);
                        }
                    } else {
                        mediaControllButton.setImageDrawable(getDrawable(R.drawable.play));
                        mediaControllButton.setOnClickListener(playClickListener);
                        if(mediaControllButton.getVisibility() != View.VISIBLE) {
                            mediaControllButton.setVisibility(View.VISIBLE);
                        }
                    }
                }

                broadcastManager.registerReceiver(playReceiver, playFilter);
                broadcastManager.registerReceiver(pauseReceiver, pauseFilter);
                broadcastManager.registerReceiver(newTrackInformationReceiver, newTrackInformationFilter);
                broadcastManager.registerReceiver(emptyReceiver, emptyFilter);
                receivers.addFirst(emptyReceiver);
                receivers.addFirst(playReceiver);
                receivers.addFirst(pauseReceiver);
                receivers.addFirst(newTrackInformationReceiver);
                broadcastManager.unregisterReceiver(this);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(initReceiver, initFilter);
        //endregion
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        /**
         * TODO: Überprüfen ob der Listener im Service untergebracht ist
         * Das Überprüfen und Ausführen des locationListeners muss außerhalb des UI Thread
         * stattfinden. Tut es das?
         */
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_shared_preferences), MODE_PRIVATE);
                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                //TODO: Safe cast von double nach float überlegen
                sharedPreferencesEditor.putFloat(CURRENT_LONGITUDE, (float)location.getLongitude());
                sharedPreferencesEditor.putFloat(CURRENT_LATITUDE, (float)location.getLatitude());
                sharedPreferencesEditor.commit();
                //Log.d(DEBUG_TAG, "Location saved in SharedPrefences - Longitude: " + (float) location.getLongitude() + " Latitude: " + (float) location.getLatitude());
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                CreateParty.NoLocationProviderDialog noLocationProviderDialog = new CreateParty.NoLocationProviderDialog();
                noLocationProviderDialog.show(getFragmentManager(), "NoLocationProviderDialog");
            }
        };

        if(!askedAlreadyThisInstance) {
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_RQ_CODE);
                askedAlreadyThisInstance = true;
            } else {
                initializeLocationContent();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == ACCESS_FINE_LOCATION_RQ_CODE) {
            if((grantResults.length > 0) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeLocationContent();
            } else {
                CreateParty.ExplanationDialog explanationDialog = new CreateParty.ExplanationDialog();
                explanationDialog.show(getFragmentManager(), "ExplanationDialog");
            }
        }
    }

    private void initializeLocationContent() {

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setBearingRequired(false);

        bestLocationProvider = locationManager.getBestProvider(criteria, true);

        //TODO: Herausfinden was passiert wenn es keinen Location Provider gibt. Kommt null oder "" zurück?
        if(bestLocationProvider == null || bestLocationProvider.equals("") || bestLocationProvider.equals("passive")) {
            CreateParty.NoLocationProviderDialog noLocationProviderDialog = new CreateParty.NoLocationProviderDialog();
            noLocationProviderDialog.show(getFragmentManager(), "NoLocationProviderDialog");
        }
        //TODO: Dies als else machen?
        try {
            locationManager.requestLocationUpdates(bestLocationProvider, 0, 0, locationListener);
        } catch(SecurityException ex) {
            Log.e(DEBUG_TAG, "Dieser Fall sollte nicht eintreten. Deshalb als Excpetion");
            ex.printStackTrace();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        if(locationManager != null)
            locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        removeBroadcastReceivers();
        adapter.removeFirebaseDatabaseListener();
        removeFirebaseDatabaseListeners();
    }

    //TODO: AUFRUFEN!
    private void removeBroadcastReceivers() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        for(BroadcastReceiver receiver : receivers) {
            broadcastManager.unregisterReceiver(receiver);
        }
    }

    private void removeFirebaseDatabaseListeners() {
        partyMetaRef.child("name").removeEventListener(partyMetaRefChildNameListener);
        partyMetaRef.child("owner").removeEventListener(partyMetaRefChildOwnerListener);
        partySettingsRef.removeEventListener(partySettingsRefListener);
        partyPlaylistRef.removeEventListener(partyPlaylistRefListener);

        Log.d(DEBUG_TAG, "All FirebaseDatabase Listeners removed");
    }

    public static class PartyClosedNotification extends DialogFragment {

        public PartyClosedNotification() {

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getString(R.string.party_party_closed_notification_title));
            alertBuilder.setMessage(getString(R.string.party_party_closed_notification_message));

            alertBuilder.setNegativeButton(getString(R.string.party_party_closed_notification_understood_button_text), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    getActivity().finish();
                }
            });

            AlertDialog alertDialog = alertBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            return alertDialog;
        }
    }

    public static class EmptyPlaylistNotification extends DialogFragment {

        public EmptyPlaylistNotification() {

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getString(R.string.party_empty_playlist_notification_title));
            alertBuilder.setMessage(getString(R.string.party_empty_playlist_notification_message));

            alertBuilder.setNegativeButton(getString(R.string.party_empty_playlist_notification_understood_button_text), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent stopService = new Intent(getActivity(), PlayerService.class);
                    getActivity().stopService(stopService);
                }
            });

            AlertDialog alertDialog = alertBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            return alertDialog;
        }

    }

    public static class PlayerInitError extends DialogFragment {

        public PlayerInitError() {

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getString(R.string.party_player_init_error_title));
            alertBuilder.setMessage(getString(R.string.party_player_init_error_message));

            alertBuilder.setNegativeButton(getString(R.string.party_party_player_init_error_understood_button_text), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent stopService = new Intent(getActivity(), PlayerService.class);
                    getActivity().stopService(stopService);
                }
            });

            AlertDialog alertDialog = alertBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            return alertDialog;
        }
    }

}
