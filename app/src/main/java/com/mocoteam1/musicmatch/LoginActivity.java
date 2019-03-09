package com.mocoteam1.musicmatch;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mocoteam1.musicmatch.streamingservice.IStreamingService;
import com.mocoteam1.musicmatch.streamingservice.Spotify;

//import org.mindrot.jbcrypt.BCrypt;

public class LoginActivity extends AppCompatActivity {

    private static final String DEBUG_TAG = LoginActivity.class.getSimpleName();

    private static final int CREATE_ACCOUNT_RQ_CODE = 2307;

    //FirebaseAuth
    private FirebaseAuth auth;

    //FirebaseData
    private FirebaseDatabase database;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_login);

        Button loginButton = (Button) findViewById(R.id.loginActivityOkButton);
        Button createAccountButton = (Button) findViewById(R.id.loginActitvityCreateAccountButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText emailText = (EditText) findViewById(R.id.loginActivityUsername);
                EditText passwordText = (EditText) findViewById(R.id.loginActivityPassword);

                String email = emailText.getText().toString();
                String password = passwordText.getText().toString();

                try {
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = auth.getCurrentUser();
                                Intent intent = new Intent(LoginActivity.this, PartyOverview.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);

                                Log.d(DEBUG_TAG, "User " + user.getUid() + " logged in");
                            } else {
                                //TODO: Richtige Fehlermeldung erstellen
                                TextView errorText = (TextView) findViewById(R.id.loginActivityLoginError);
                                errorText.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                } catch (IllegalArgumentException ex) {
                    TextView errorText = (TextView) findViewById(R.id.loginActivityLoginError);
                    errorText.setVisibility(View.VISIBLE);
                }
            }
        });

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, CreateAccount.class);
                startActivityForResult(intent, CREATE_ACCOUNT_RQ_CODE);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        if(auth.getCurrentUser() != null) {
            Intent intent = new Intent(this, PartyOverview.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            Log.d(DEBUG_TAG, "Existing Token was used to log in");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CREATE_ACCOUNT_RQ_CODE)
            if(resultCode == RESULT_OK) {
                Bundle intentExtras = data.getExtras();
                final String email = intentExtras.getString(CreateAccount.EMAIL);
                String password = intentExtras.getString(CreateAccount.PASSWORD);

                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();

                            DatabaseReference rootRef = database.getReference();
                            DatabaseReference userRef = rootRef.child("user").child("userID:" + user.getUid());

                            //Create Database Entry
                            userRef.child("name").setValue(user.getEmail());
                            Log.d(DEBUG_TAG, "Database entries for new user " + user.getUid() + " were created");

                            Intent intent = new Intent(LoginActivity.this, PartyOverview.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);

                            Log.d(DEBUG_TAG, "User account with UID " + user.getUid() + " was successfully created");
                        } else {
                            //TODO: Richtige Fehlermeldung erstellen
                            Toast notification = Toast.makeText(LoginActivity.this, getString(R.string.login_activity_account_creation_error_text), Toast.LENGTH_LONG);
                            notification.show();
                        }
                    }
                });
            }
    }
}
