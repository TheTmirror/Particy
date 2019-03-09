package com.mocoteam1.musicmatch;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mocoteam1.musicmatch.entities.Party;

import java.util.LinkedList;

public class JoinPartyExample extends AppCompatActivity {

    private static final String DEBUG_TAG = JoinPartyExample.class.getSimpleName();
    private ChildEventListener listener;

    private String userID;
    private String userName;

    //FirebaseAuth
    FirebaseAuth auth;
    FirebaseUser user;

    //Firebase Database
    DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_join_party_example);

        //Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        rootRef = database.getReference();

    }

    @Override
    protected void onStart() {
        super.onStart();

        //FirebaseAuth Init
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        userID = user.getUid();
        userName = user.getEmail();

        final EditText nameText = (EditText) findViewById(R.id.joinPartyExampleNameEditText);
        final EditText ownerText = (EditText) findViewById(R.id.joinPartyExampleOwnerEditText);
        Button joinButton = (Button) findViewById(R.id.joinPartyExampleJoinButton);

        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String name = nameText.getText().toString();
                final String owner = ownerText.getText().toString();

                final Workaround w = new Workaround();
                w.setPartyExists(false);

                //TODO: Überprüfen ob singleEventListener auch entfernt werden müssen
                listener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        if(dataSnapshot.getKey().equals("nextPartyID"))
                            return;
                        if(w.isPartyExists())
                            return;
                        rootRef.child("party-meta").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                w.setChildren((int) dataSnapshot.getChildrenCount());
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                        final int children = w.getChildren();
                        final Party party = new Party();
                        party.setPartyID(Integer.parseInt(dataSnapshot.getKey().replace("partyID:", "")));
                        DatabaseReference ref = rootRef.child("party-meta").child("partyID:" + party.getPartyID()).child("name");
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                party.setPartyName(dataSnapshot.getValue(String.class));

                                if(party.getPartyOwner() != null) {
                                    if(party.getPartyOwner().equals(owner) && party.getPartyName().equals(name)) {
                                        w.setPartyExists(true);

                                        rootRef.child("user").child("userID:" + userID).child("parties").child("partyID:" + party.getPartyID()).setValue(true);

                                        Intent intent = new Intent(JoinPartyExample.this, PartyDetailview.class);
                                        intent.putExtra("partyID", party.getPartyID());

                                        startActivity(intent);
                                        finish();
                                    }

                                    if(w.getCounter() == children - 1) {
                                        //Party wurde nicht gefunden
                                        Log.d(DEBUG_TAG, "Die Party wurde nicht gefunden");
                                        finish();
                                    } else
                                        w.incCounter();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                        ref = rootRef.child("party-meta").child("partyID:" + party.getPartyID()).child("owner");
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                party.setPartyOwner(dataSnapshot.getValue(String.class));

                                if(party.getPartyName() != null) {
                                    if(party.getPartyOwner().equals(owner) && party.getPartyName().equals(name)) {
                                        w.setPartyExists(true);

                                        rootRef.child("user").child("userID:" + userID).child("parties").child("partyID:" + party.getPartyID()).setValue(true);

                                        Intent intent = new Intent(JoinPartyExample.this, PartyDetailview.class);
                                        intent.putExtra("partyID", party.getPartyID());

                                        startActivity(intent);
                                        finish();
                                    }

                                    if(w.getCounter() == children - 1) {
                                        //Party wurde nicht gefunden
                                        Log.d(DEBUG_TAG, "Die Party wurde nicht gefunden");
                                        finish();
                                    } else
                                        w.incCounter();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                };

                rootRef.child("party-meta").addChildEventListener(listener);
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();

        if(listener != null)
            rootRef.child("party-meta").removeEventListener(listener);
    }

    private class Workaround {

        private boolean partyExists;
        private int counter = 0;
        private int children = 0;

        public boolean isPartyExists() {
            return partyExists;
        }

        public void setPartyExists(boolean partyExists) {
            this.partyExists = partyExists;
        }

        public int getCounter() {
            return counter;
        }

        public void setCounter(int counter) {
            this.counter = counter;
        }

        public void incCounter() {
            counter = counter + 1;
        }

        public int getChildren() {
            return children;
        }

        public void setChildren(int children) {
            this.children = children;
        }
    }

}
