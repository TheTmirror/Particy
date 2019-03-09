package com.mocoteam1.musicmatch;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.mocoteam1.musicmatch.entities.Party;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mocoteam1.musicmatch.entities.Song;
import com.mocoteam1.musicmatch.entities.StreamingServicePlaylist;
import com.mocoteam1.musicmatch.helper.CreatePartyPlaylistAdapter;
import com.mocoteam1.musicmatch.streamingservice.DummyStreamingService;
import com.mocoteam1.musicmatch.streamingservice.IStreamingService;
import com.mocoteam1.musicmatch.streamingservice.Spotify;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: Progressbar muss noch hinzugefügt werden damit der Nutzer nicht verwirrt ist
public class CreateParty extends AppCompatActivity {

    private static final String DEBUG_TAG = CreateParty.class.getSimpleName();

    private static final int ACCESS_FINE_LOCATION_RQ_CODE = 2307;

    private String userID;
    private String userName;
    private int nextPartyID;
    private LocationManager locationManager;
    private String bestLocationProvider;
    private LocationListener locationListener;

    private StreamingServicePlaylist selectedPlaylist;
    private boolean askedAlreadyThisInstance = false;

    //FirebaseAuth
    FirebaseAuth auth;

    //Firebase References
    FirebaseDatabase database;
    DatabaseReference rootRef;
    DatabaseReference userPartiesRef;
    DatabaseReference partyMetaRef;
    DatabaseReference partyPlaylistRef;
    DatabaseReference partySettingsRef;

    //FirebaseDatabase Listener
    ValueEventListener nextPartyIDListener;

    //Streaming Service
    IStreamingService spotify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_create_party);

        final ToggleButton usePlaylistToggle = (ToggleButton) findViewById(R.id.createPartyUsePlaylistToggle);
        usePlaylistToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                FrameLayout playlistLayout = (FrameLayout) findViewById(R.id.createPartyPlaylistOverviewFrameLayout);
                if(usePlaylistToggle.isChecked())
                    playlistLayout.setVisibility(View.VISIBLE);
                else {
                    playlistLayout.setVisibility(View.GONE);
                    selectedPlaylist = null;
                }
            }
        });

        ListView playlistsView = (ListView) findViewById(R.id.createPartyPlaylistListView);
        TextView playlistText = (TextView) findViewById(R.id.createPartyUsePlaylistText);
        ToggleButton playlistToggleButton = (ToggleButton) findViewById(R.id.createPartyUsePlaylistToggle);

        //Seeeeeeeeeeeehr schlechte Performance! Das muss anders getan werden

        if(Spotify.isAccessTokenValid(this)) {
            spotify = new Spotify(this);
            playlistsView.setAdapter(new CreatePartyPlaylistAdapter(spotify.getPlaylists(), this));
            playlistsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Log.d(DEBUG_TAG, "Entered");
                    selectedPlaylist = (StreamingServicePlaylist) adapterView.getItemAtPosition(i);
                    Log.d(DEBUG_TAG, selectedPlaylist.getName() + " got selected");
                }
            });

            playlistText.setVisibility(View.VISIBLE);
            playlistToggleButton.setVisibility(View.VISIBLE);
        } else {
            playlistText.setVisibility(View.GONE);
            playlistToggleButton.setVisibility(View.GONE);
        }

        Button partyErstellen = (Button) findViewById(R.id.createPartyConfirmButton);
        partyErstellen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = ((TextView) findViewById(R.id.createPartyNameEntryText)).getText().toString();
                String owner = userName;

                int radius = Integer.parseInt(((EditText) findViewById(R.id.createPartyRadiusEntryText)).getText().toString());
                boolean addAllowed = ((ToggleButton) findViewById(R.id.createPartyAddAllowedToggle)).isChecked();
                Log.d(DEBUG_TAG, "isCheck: " + addAllowed);

                //Get PartyLocation
                /**
                 * TODO: Schwerwiegend!!!! Empty Location Cache abfangen
                 * Es kann passieren, dass das Abfragen von getLastKnowLocation einen Null Wert
                 * als Location zurückliefert. Dies ist der Fall, wenn bisher kein Location Wert
                 * zurückgeliefert gecached wurde, also wenn der Cache leer ist. Wann wird der Cache
                 * geleert? JEDES MAL wenn der Standort an und ausgeschaltet wird. Klar man kann
                 * abfangen ob die Location Null ist, aber wie erzwingt man dann eine Location?
                 *
                 * Bevor das Problem behoben wird: Die Applikation SOLL mit einer NULL Pointer Exception
                 * abstürzen!
                 */
                Location location = getCurrentLocation();
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();

                //Create PartyDetailview
                Party p = new Party();
                p.setPartyName(name);
                p.setPartyOwner(owner);
                p.setLength(longitude);
                p.setWidth(latitude);

                Map newParty = new HashMap();
                newParty.put("user/userID:" + userID + "/parties/partyID:" + nextPartyID, true);
                newParty.put("party-meta/partyID:" + nextPartyID + "/name", p.getPartyName());
                newParty.put("party-meta/partyID:" + nextPartyID + "/owner", p.getPartyOwner());
                newParty.put("party-meta/partyID:" + nextPartyID + "/location/length", p.getLength());
                newParty.put("party-meta/partyID:" + nextPartyID + "/location/width", p.getWidth());
                newParty.put("party-meta/partyID:" + nextPartyID + "/hash", "xxxxx" + nextPartyID);

                /*
                *Kann wahrscheinlich (99%) raus
                if (((ToggleButton) findViewById(R.id.createPartyUsePlaylistToggle)).isChecked()) {
                    //Falls eine Playlist als Basis genommen wird
                } else {
                    //partyPlaylistRef.child("partyID:" + NEXT_PARTY_KEY).setValue(false);
                }
                */

                newParty.put("party-settings/partyID:" + nextPartyID + "/addAllowed", addAllowed);
                newParty.put("party-settings/partyID:" + nextPartyID + "/radius", radius);

                if(usePlaylistToggle.isChecked() && selectedPlaylist != null) {
                    newParty.put("party-playlist/partyID:" + nextPartyID + "/nextID", selectedPlaylist.getTotalTracks() + 1);
                    for(Song s : selectedPlaylist.updateAndGetSongs(CreateParty.this)) {
                        s.setUpvotes(0);
                        s.setDownvotes(0);
                        //DatabaseReference partyPlaylistSongRef = partyPlaylistRef.child("partyID:" + nextPartyID).child("songID:" + s.getSongID());
                        String subRef = "party-playlist/partyID:" + nextPartyID + "/songID:" + s.getSongID();
                        newParty.put(subRef + "/name", s.getName());
                        newParty.put(subRef + "/artist", s.getArtist());
                        newParty.put(subRef + "/album", s.getAlbum());
                        newParty.put(subRef + "/upvotes", s.getUpvotes());
                        newParty.put(subRef + "/downvotes", s.getDownvotes());

                        //New Informations
                        newParty.put(subRef + "/spotifyID", s.getSpotifyID());
                        newParty.put(subRef + "/isExplicit", s.isExplicit());
                    }
                } else {

                    newParty.put("party-playlist/partyID:" + nextPartyID + "/nextID", 1);
                }

                newParty.put("party-meta/nextPartyID", nextPartyID + 1);

                rootRef.updateChildren(newParty);

                //Nachdem die PartyDetailview komplett erstellt und gesichert ist
                Intent i = new Intent(CreateParty.this, PartyDetailview.class);
                i.putExtra("partyID", nextPartyID);
                startActivity(i);
                finish();

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == ACCESS_FINE_LOCATION_RQ_CODE) {
            Log.d(DEBUG_TAG, "Called");
            if((grantResults.length > 0) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeLocationContent();
            } else {
                ExplanationDialog explanationDialog = new ExplanationDialog();
                explanationDialog.show(getFragmentManager(), "ExplanationDialog");
            }
        }
    }

    private Location getCurrentLocation() {
        //TODO: Überlegen: Wie geht man hier vor?
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        } else {
            return locationManager.getLastKnownLocation(bestLocationProvider);
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
        if(bestLocationProvider == null ||bestLocationProvider.equals("") || bestLocationProvider.equals("passive")) {
            NoLocationProviderDialog noLocationProviderDialog = new NoLocationProviderDialog();
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
    protected void onStart() {
        super.onStart();

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        userID = user.getUid();
        userName = user.getEmail();

        database = FirebaseDatabase.getInstance();
        rootRef = database.getReference();
        userPartiesRef = rootRef.child("user").child("userID:"+userID).child("parties");
        partyMetaRef = rootRef.child("party-meta");
        partyPlaylistRef = rootRef.child("party-playlist");
        partySettingsRef = rootRef.child("party-settings");

        nextPartyIDListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue(Long.class) == null)
                    nextPartyID = 1;
                else
                    nextPartyID = dataSnapshot.getValue(Long.class).intValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        partyMetaRef.child("nextPartyID").addValueEventListener(nextPartyIDListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                NoLocationProviderDialog noLocationProviderDialog = new NoLocationProviderDialog();
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
    protected void onPause() {
        super.onPause();

        if(locationManager != null)
            locationManager.removeUpdates(locationListener);
    }

    private void removeFirebaseDatabaseListener() {

        partyMetaRef.child("nextPartyID").removeEventListener(nextPartyIDListener);

        Log.d(DEBUG_TAG, "All FirebaseDatabaseListener removed");

    }

    @Override
    protected void onStop() {
        super.onStop();

        removeFirebaseDatabaseListener();
    }

    public static class ExplanationDialog extends DialogFragment {

        public ExplanationDialog() {

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(R.string.create_party_explanation_title);
            alertBuilder.setMessage(R.string.create_party_explanation_message);
            alertBuilder.setNegativeButton(R.string.create_party_explanation_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    getActivity().finish();
                    return;
                }
            });

            AlertDialog alertDialog = alertBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            return alertDialog;
        }
    }

    public static class NoLocationProviderDialog extends DialogFragment {

        public NoLocationProviderDialog() {

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(R.string.create_party_no_location_provider_title);
            alertBuilder.setMessage(R.string.create_party_no_location_provider_message);
            alertBuilder.setNegativeButton(R.string.create_party_no_location_provider_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    getActivity().finish();
                    return;
                }
            });

            AlertDialog alertDialog = alertBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            return alertDialog;
        }
    }
}
