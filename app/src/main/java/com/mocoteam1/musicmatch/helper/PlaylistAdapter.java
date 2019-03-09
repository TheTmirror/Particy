package com.mocoteam1.musicmatch.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mocoteam1.musicmatch.PartyDetailview;
import com.mocoteam1.musicmatch.R;
import com.mocoteam1.musicmatch.entities.Song;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Tristan on 12.12.2017.
 */

public class PlaylistAdapter extends BaseAdapter{

    private static final String DEBUG_TAG = PlaylistAdapter.class.getSimpleName();

    private Context context;
    private HashMap<Integer, Song> container;
    private int partyID;
    private int radius;

    //Firebase
    FirebaseDatabase database;
    DatabaseReference rootRef;
    DatabaseReference longitudeRef;
    DatabaseReference latitudeRef;
    DatabaseReference radiusRef;

    //FirebaseListener
    ValueEventListener longitudeListener;
    ValueEventListener latitudeListener;
    ValueEventListener radiusListener;

    //Location
    private double longitude;
    private double latitude;

    public PlaylistAdapter(Context context, HashMap<Integer, Song> container, int partyID) {
        this.context = context;
        this.container = container;
        this.partyID = partyID;

        database = FirebaseDatabase.getInstance();
        rootRef = database.getReference();
        longitudeRef = rootRef.child("party-meta").child("partyID:" + this.partyID).child("location").child("length");
        latitudeRef = rootRef.child("party-meta").child("partyID:" + this.partyID).child("location").child("width");
        radiusRef = rootRef.child("party-settings").child("partyID:" + this.partyID).child("radius");

        longitudeListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    longitude = dataSnapshot.getValue(Double.class).doubleValue();
                } catch(NullPointerException ex) {
                    Log.w(DEBUG_TAG, "Diese NullPointer Exception kann auftreten.\n" +
                            "Der Stacktrace wird geprintet um zu überprüfen ob es sich wirklich" +
                            " um den löschen Fall handelt.\n");
                    ex.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        latitudeListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    latitude = dataSnapshot.getValue(Double.class).doubleValue();
                } catch(NullPointerException ex) {
                    Log.w(DEBUG_TAG, "Diese NullPointer Exception kann auftreten.\n" +
                            "Der Stacktrace wird geprintet um zu überprüfen ob es sich wirklich" +
                            " um den löschen Fall handelt.\n");
                    ex.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        radiusListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    radius = dataSnapshot.getValue(Long.class).intValue();
                } catch (NullPointerException ex) {
                    Log.w(DEBUG_TAG, "Diese NullPointer Exception kann auftreten.\n" +
                            "Der Stacktrace wird geprintet um zu überprüfen ob es sich wirklich" +
                            " um den löschen Fall handelt.\n");
                    ex.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        longitudeRef.addValueEventListener(longitudeListener);
        latitudeRef.addValueEventListener(latitudeListener);
        radiusRef.addValueEventListener(radiusListener);
    }

    @Override
    public int getCount() {
        return container.size();
    }

    @Override
    public Song getItem(int i) {
        Iterator it = container.entrySet().iterator();
        HashMap.Entry<Integer, Song> result = null;
        for(int j = 0; j <= i ; j++)
            result = (HashMap.Entry<Integer, Song>) it.next();
        return result.getValue();
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).getSongID();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        final int index = i;

        if (view == null) {
            view = LayoutInflater.from(context).
                    inflate(R.layout.party_playlist_listview_item, viewGroup, false);
        }

        TextView name = (TextView) view.findViewById(R.id.partyPlaylistListviewItemName);
        TextView artist = (TextView) view.findViewById(R.id.partyPlaylistListViewItemArtist);
        TextView album = (TextView) view.findViewById(R.id.partyPlaylistListviewItemAlbum);
        TextView votesum = (TextView) view.findViewById(R.id.partyPlaylistListviewItemVotesum);

        final ImageButton upvoteButton = (ImageButton) view.findViewById(R.id.partyPlaylistListviewItemUpvoteButton);
        final ImageButton downvoteButton = (ImageButton) view.findViewById(R.id.partyPlaylistListviewItemDownvoteButton);

        name.setText(getItem(i).getName());
        artist.setText(getItem(i).getArtist());
        album.setText(getItem(i).getAlbum());
        votesum.setText("" + getItem(i).getVotesum());

        upvoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isInRange()) {
                    OutOfRangeNotification outOfRangeNotification = new OutOfRangeNotification();
                    outOfRangeNotification.show(((Activity)context).getFragmentManager(), "OutOfRangeNotification");
                    return;
                }

                Song song = getItem(index);
                song.setUpvotes(song.getUpvotes() + 1);
                //TODO: Die Ref "tiefer im Baum" ansprechen.
                rootRef.child("party-playlist").child("partyID:" + partyID).child("songID:" + song.getSongID()).child("upvotes").setValue(song.getUpvotes());
                upvoteButton.setEnabled(false);
                downvoteButton.setEnabled(false);
                notifyDataSetChanged();
            }
        });

        downvoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isInRange()) {
                    OutOfRangeNotification outOfRangeNotification = new OutOfRangeNotification();
                    outOfRangeNotification.show(((Activity)context).getFragmentManager(), "OutOfRangeNotification");
                    return;
                }

                Song song = getItem(index);
                song.setDownvotes(song.getDownvotes() + 1);
                //TODO: Die Ref "tiefer im Baum" ansprechen.
                rootRef.child("party-playlist").child("partyID:" + partyID).child("songID:" + song.getSongID()).child("downvotes").setValue(song.getDownvotes());
                upvoteButton.setEnabled(false);
                downvoteButton.setEnabled(false);
                notifyDataSetChanged();
            }
        });

        return view;
    }

    private boolean isInRange() {
        /**
         * TODO: SharedPrefrences Resetten
         * Die SharedPreferences enthalten immer die zuletzt gespeicherte Location und gleichen
         * diese mit der PartyLocation ab. Wenn allerdings in eine neue Party gejoint wird, der
         * LocationManager aber noch keine Messung der aktuellen Location durchgeführt hat und dann
         * gevotet wird, wird der zuletzt gespeicherte Locationwert in den SharedPreferences genommen,
         * was möglicherweise zu falschen Werten führt und unter Umständen deshalb fälschlicherweise
         * nicht gevotet werden kann.
         */
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_shared_preferences), Context.MODE_PRIVATE);
        Location now = new Location("");
        now.setLatitude(sharedPreferences.getFloat(PartyDetailview.CURRENT_LATITUDE, -1));
        now.setLongitude(sharedPreferences.getFloat(PartyDetailview.CURRENT_LONGITUDE, -1));

        Location party = new Location("");
        party.setLatitude(latitude);
        party.setLongitude(longitude);
        Log.d(DEBUG_TAG, "Radius: " + radius);
        if(party.distanceTo(now) > radius) {
            Log.d(DEBUG_TAG, "Out of range");
            Log.d(DEBUG_TAG, "Party - Latitude: " + party.getLatitude() + " Longitude: " + party.getLongitude());
            Log.d(DEBUG_TAG, "Now - Latitude: " + now.getLatitude() + " Longitude: " + now.getLongitude());
            Log.d(DEBUG_TAG, "DISTANCE: " + party.distanceTo(now));
            return false;
        } else {
            return true;
        }
    }

    public void removeFirebaseDatabaseListener() {
        longitudeRef.removeEventListener(longitudeListener);
        latitudeRef.removeEventListener(latitudeListener);
        radiusRef.removeEventListener(radiusListener);
    }

    public static class OutOfRangeNotification extends DialogFragment {

        public OutOfRangeNotification() {

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            alertBuilder.setTitle(getString(R.string.playlist_adapter_out_of_range_notification_title));
            alertBuilder.setMessage(getString(R.string.playlist_adapter_out_of_range_notification_message));

            alertBuilder.setNegativeButton(getString(R.string.playlist_adapter_out_of_range_notification_understood_button_text), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //getActivity().finish();
                }
            });

            AlertDialog alertDialog = alertBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            return alertDialog;
        }
    }
}
