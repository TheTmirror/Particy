package com.mocoteam1.musicmatch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mocoteam1.musicmatch.entities.Song;
import com.mocoteam1.musicmatch.helper.SearchResultAdapter;
import com.mocoteam1.musicmatch.streamingservice.IStreamingService;
import com.mocoteam1.musicmatch.streamingservice.Spotify;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SearchSongSample extends AppCompatActivity {

    private static final String DEBUG_TAG = SearchSongSample.class.getSimpleName();
    private int nextID;

    //FirebaseDatabase
    FirebaseDatabase database;
    DatabaseReference rootRef;
    DatabaseReference partyPlaylistRef;

    //FirebaseDatabase Listener
    ValueEventListener nextIDListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_search_song_sample);

        Bundle intentExtras = getIntent().getExtras();
        int partyID = intentExtras.getInt(PartyDetailview.PARTY_ID);

        //Firebase
        database = FirebaseDatabase.getInstance();
        rootRef = database.getReference();
        partyPlaylistRef = rootRef.child("party-playlist").child("partyID:" + partyID);


        final EditText nameText = (EditText) findViewById(R.id.searchSongSampleNameEditText);
        final ListView searchResultsListView = (ListView) findViewById(R.id.searchSongResultListView);
        Button seachSong = (Button) findViewById(R.id.searchSongSampleSearchButton);

        nameText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchResultsListView.setVisibility(View.GONE);
            }
        });

        seachSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IStreamingService spotify = new Spotify(SearchSongSample.this);
                List<Song> searchResults = spotify.getSong(nameText.getText().toString());

                searchResultsListView.setAdapter(new SearchResultAdapter(SearchSongSample.this, searchResults));
                searchResultsListView.setVisibility(View.VISIBLE);
            }
        });

        searchResultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Song selectedSong = (Song) adapterView.getItemAtPosition(i);
                selectedSong.setSongID(nextID);

                Map newSong = new HashMap();
                newSong.put("nextID", nextID + 1);
                newSong.put("/songID:" + selectedSong.getSongID() + "/name", selectedSong.getName());
                newSong.put("/songID:" + selectedSong.getSongID() + "/artist", selectedSong.getArtist());
                newSong.put("/songID:" + selectedSong.getSongID() + "/album", selectedSong.getAlbum());
                newSong.put("/songID:" + selectedSong.getSongID() + "/upvotes", selectedSong.getUpvotes());
                newSong.put("/songID:" + selectedSong.getSongID() + "/downvotes", selectedSong.getDownvotes());
                newSong.put("/songID:" + selectedSong.getSongID() + "/spotifyID", selectedSong.getSpotifyID());
                newSong.put("/songID:" + selectedSong.getSongID() + "/isExplicit", selectedSong.isExplicit());
                partyPlaylistRef.updateChildren(newSong);

                finish();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        nextIDListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                nextID = dataSnapshot.getValue(Long.class).intValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        partyPlaylistRef.child("nextID").addValueEventListener(nextIDListener);
    }

    private void removeFirebaseDatabaseListener() {
        partyPlaylistRef.child("nextID").removeEventListener(nextIDListener);

        Log.d(DEBUG_TAG, "All FirebaseDatabase Listener removed");
    }

    @Override
    protected void onStop() {
        super.onStop();

        removeFirebaseDatabaseListener();
    }

    public static Song[] getMultipleSampleData() {
        Song[] songs = new Song[10];

        Song s = new Song();
        s.setSongID(1);
        s.setName("Werte");
        s.setArtist("Prinz Pi");
        s.setAlbum("Im Westen nix Neues");
        songs[s.getSongID() - 1] = s;

        s = new Song();
        s.setSongID(2);
        s.setName("Rebell");
        s.setArtist("Die Ärzte");
        s.setAlbum("Super 8");
        songs[s.getSongID() - 1] = s;

        s = new Song();
        s.setSongID(3);
        s.setName("Tagen wie diesen");
        s.setArtist("Toten Hosen");
        s.setAlbum("Ballast der Republik");
        songs[s.getSongID() - 1] = s;

        s = new Song();
        s.setSongID(4);
        s.setName("Walpurgisnacht");
        s.setArtist("K.I.Z");
        s.setAlbum("Hahnenkamp");
        songs[s.getSongID() - 1] = s;

        s = new Song();
        s.setSongID(5);
        s.setName("In the End");
        s.setArtist("Linkin Park");
        s.setAlbum("Hybrid Theorie");
        songs[s.getSongID() - 1] = s;

        s = new Song();
        s.setSongID(6);
        s.setName("In Too Deep");
        s.setArtist("Sum 41");
        s.setAlbum("All Killer, No Filler");
        songs[s.getSongID() - 1] = s;

        s = new Song();
        s.setSongID(7);
        s.setName("crushcrushcrush");
        s.setArtist("Paramore");
        s.setAlbum("R!OT");
        songs[s.getSongID() - 1] = s;

        s = new Song();
        s.setSongID(8);
        s.setName("Sorry, you're not a winner");
        s.setArtist("Enter Shikari");
        s.setAlbum("Take To The Skies");
        songs[s.getSongID() - 1] = s;

        s = new Song();
        s.setSongID(9);
        s.setName("Numb");
        s.setArtist("Linkin Park");
        s.setAlbum("Meteora");
        songs[s.getSongID() - 1] = s;

        s = new Song();
        s.setSongID(10);
        s.setName("Still Waiting");
        s.setArtist("Sum 41");
        s.setAlbum("Does This Look Infected?");
        songs[s.getSongID() - 1] = s;

        return songs;
    }
}
