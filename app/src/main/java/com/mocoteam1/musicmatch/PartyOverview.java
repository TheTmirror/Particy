package com.mocoteam1.musicmatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.mocoteam1.musicmatch.entities.Party;
import com.mocoteam1.musicmatch.entities.StreamingServicePlaylist;
import com.mocoteam1.musicmatch.helper.PartyAdapter;
import com.mocoteam1.musicmatch.helper.Math;
import com.mocoteam1.musicmatch.streamingservice.IStreamingService;
import com.mocoteam1.musicmatch.streamingservice.Spotify;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * TODO: Lange Ladezeiten fix
 * Problem gefunden: Die langen Ladezeiten kommen nicht durch die Serverinteraktion zustande,
 * sondern durch das dauerhafte entfernen und hinzufügen der Listeners. Mir ist aufgefallen,
 * dass wenn man die Listeners nicht entfernt bei nächsten mal, sofern nur die selben Listerns
 * gebraucht werden, schneller geladen wird. Mögliche Lösung: Nur die veränderten Listerns entfernen
 * und erst beim Schließen des Programmes alle Listerns entfernen.
 *
 * TODO: Ggf muss das hinufügen und entfernen der Listeners in die onResume / onPause Methode verlegt werden
 */
public class PartyOverview extends AppCompatActivity {

    private static final String DEBUG_TAG = PartyOverview.class.getSimpleName();

    //Firebase Auth
    FirebaseAuth auth;
    FirebaseUser user;

    //Firebase Database
    FirebaseDatabase database;
    DatabaseReference rootRef;
    DatabaseReference userRootRef;

    //FirebaseDatabaseListener
    ChildEventListener partyListener;
    HashMap<Integer, ValueEventListener> listeners;

    //PartyInformation
    PartyAdapter adapter;
    HashMap<Integer, Party> parties;
    ListView partiesListView;

    //StreamingService
    IStreamingService streamingService;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.logout:
                auth.signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            case R.id.verwaltung:
                Intent i = new Intent(this, AccountManagement.class);
                startActivity(i);
                return true;
            /*case R.id.play:
                streamingService = new Spotify(this);
                streamingService.playSong("spotify:track:0oq3HqazmZgAjs4UTCqeTz");
                return true;
            case R.id.playlist:
                streamingService = new Spotify(this);
                List<StreamingServicePlaylist> playlists = streamingService.getPlaylists();
                Log.d(DEBUG_TAG, "Something");
                return true;
            case R.id.validate:
                SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_shared_preferences), MODE_PRIVATE);
                String accessToken = sharedPreferences.getString(getString(R.string.spotify_access_token), null);
                Log.d(DEBUG_TAG, "AccessToken is valid: " + Spotify.isAccessTokenValid(this));
                return true;*/
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().setTitle(null);
        setContentView(R.layout.activity_party_overview);

        listeners = new HashMap<Integer, ValueEventListener>();
        parties = new HashMap<Integer, Party>();

        partiesListView = (ListView) findViewById(R.id.partyOverviewListView);

        partiesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int partyID = Math.safeLongToInt(adapterView.getItemIdAtPosition(i));

                Intent intent = new Intent(PartyOverview.this, PartyDetailview.class);
                intent.putExtra(PartyDetailview.PARTY_ID, partyID);

                startActivity(intent);
            }
        });

        //TODO: Die Darstellung der FloatingActionButtons muss noch bearbeitet werden
        FloatingActionButton createNewPartyButton = (FloatingActionButton) findViewById(R.id.partyOverviewCreateNewPartyButton);
        FloatingActionButton joinExistingPartyButton = (FloatingActionButton) findViewById(R.id.partyOverviewJoinExistingPartyButton);

        createNewPartyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(PartyOverview.this, CreateParty.class);
                startActivity(i);
            }
        });

        joinExistingPartyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(PartyOverview.this, JoinPartyExample.class);
                startActivity(i);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        //Datenbankinit
        database = FirebaseDatabase.getInstance();
        rootRef = database.getReference().getRoot();
        userRootRef = rootRef.child("user").child("userID:" + user.getUid());

        //ViewInit
        adapter = new PartyAdapter(this, parties, user.getEmail());
        partiesListView.setAdapter(adapter);

        /**
         * TODO: EDIT: Workaround gefunden. Nun muss aber jedes mal die kompletten Partys neu geladen
         * werden. Das muss anders gehen!
         *
         *
         * Dies klappt nicht um die Partys neu zuladen.
         * Das Problem ist, dass die HashMap nicht geleert wird sondern eine neue Instanz erstellt
         * wird. Somit kennt der Adapter den Container nicht mehr.
         */
        //Damit die Parties resettet werden. Muss auch anders gehen.
        //Verliere ich dadurch ggf Performance?
        //parties = new HashMap<Integer, PartyDetailview>();
        parties.clear();
        listeners.clear();

        partyListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final int partyID = Integer.parseInt(dataSnapshot.getKey().replace("partyID:", ""));
                if(!parties.containsKey(partyID)) {
                    Party p = new Party();
                    parties.put(partyID, p);
                    Log.d(DEBUG_TAG, "Die Party mit der ID " + partyID + " wurde der HashMap hinzugefügt");
                }

                if(!listeners.containsKey(partyID)) {
                    final ValueEventListener listener = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            /**
                             * TODO: Drüber nachdenken ob dies die beste Lösung ist
                             *
                             * Auf diese Art und Weise kümmert sich der User selbst um die
                             * Konsistenz seiner Daten wenn jemand anderes eine Party entfernt in
                             * der er ist, entfernt er selbst alle Details zu der Party, damit es
                             * nicht zu einer Inkonsistenz kommt. Damit hat der Host der Party nicht
                             * die Aufgabe des Management, sondern es wird die Fähigkeit der
                             * Datenbank genutzt.
                             *
                             * Wenn der dataSnapshot.getValue() == null entspricht wurde das Element entfernt
                             * TODO: Muss der Listener dann auch entfernt werden?
                             * Oder wurde er schon entfernt als das Objekt entfernt wurde
                             */
                            if(dataSnapshot.getValue() == null) {
                                userRootRef.child("parties").child("partyID:" + partyID).removeValue();
                                //parties.remove(partyID);
                                /**
                                 * TODO: Hier dann ggf zugehörigen Listener entfernen
                                 * rootRef.child("party-meta").child("partyID:" + partyID).removeValueEventListener(this);
                                 */
                                //listeners.remove(partyID);
                                //adapter.notifyDataSetChanged();
                                return;
                            }

                            Party p = parties.get(partyID);
                            String name = dataSnapshot.child("name").getValue(String.class);
                            String owner = dataSnapshot.child("owner").getValue(String.class);
                            DataSnapshot locationSnapshot = dataSnapshot.child("location");
                            double length = locationSnapshot.child("length").getValue(Double.class).doubleValue();
                            double width = locationSnapshot.child("width").getValue(Double.class).doubleValue();
                            p.setPartyID(partyID);
                            p.setPartyName(name);
                            p.setPartyOwner(owner);
                            p.setLength(length);
                            p.setWidth(width);
                            Log.d(DEBUG_TAG, "Party " + p.getPartyID() + " wurde komplett gefüllt");
                            //TODO: Dies vllt mit childCounter nur einmal aufrufen -> bessere Performance
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    };
                    listeners.put(partyID, listener);
                    rootRef.child("party-meta").child("partyID:" + partyID).addValueEventListener(listener);
                    Log.d(DEBUG_TAG, "Der zur Party " + partyID + " gehörende ValueEventListener (Meta-Infos) wurde dynamisch hinzugefügt");
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                /**
                 * Dies sollte eigentlich nicht passieren
                 *
                 * TODO: Passende Expception entwerfen
                 */
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                int partyID = Integer.parseInt(dataSnapshot.getKey().replace("partyID:", ""));
                if(parties.containsKey(partyID)) {
                    parties.remove(partyID);
                    adapter.notifyDataSetChanged();
                }
                rootRef.child("party-meta").child("partyID:" + partyID).removeEventListener(listeners.get(partyID));
                listeners.remove(partyID);

                Log.d(DEBUG_TAG,"Die Party " + partyID + " und alle zugehörigen Listener wurde erfolgreich entfernt");
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
                /**
                 * Dies sollte eigentlich nicht passieren
                 *
                 * TODO: Passende Expception entwerfen
                 */
            }
        };

        userRootRef.child("parties").addChildEventListener(partyListener);
        Log.d(DEBUG_TAG, "Der Listener um die Party - Zugehörigkeit des Users zu erfahren wurde hinzugefügt");
    }

    @Override
    protected void onStop() {
        super.onStop();

        removeFirebaseDatabaseListeners();
    }

    @Override
    protected void onDestroy() {
        if(streamingService != null)
            streamingService.onDestroy();
        super.onDestroy();
    }

    private void removeFirebaseDatabaseListeners() {
        DatabaseReference partyMetaRef = rootRef.child("party-meta");
        Iterator it = listeners.entrySet().iterator();
        while(it.hasNext()) {
            HashMap.Entry<Integer, ValueEventListener> pair = (HashMap.Entry<Integer, ValueEventListener>) it.next();
            Integer partyID = (Integer) pair.getKey();
            ValueEventListener listener = pair.getValue();
            partyMetaRef.child("partyID:" + partyID).removeEventListener(listener);
        }

        userRootRef.child("parties").removeEventListener(partyListener);

        Log.d(DEBUG_TAG, "All FirebaseDatabase Listeners removed");
    }
}
