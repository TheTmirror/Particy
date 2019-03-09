package com.mocoteam1.musicmatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.mocoteam1.musicmatch.streamingservice.IStreamingService;
import com.mocoteam1.musicmatch.streamingservice.Spotify;

import java.util.Calendar;
import java.util.Date;

public class AccountManagement extends AppCompatActivity {

    private static final String DEBUG_TAG = AccountManagement.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SupportActionBar verstecken
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        // ContentView setzen
        setContentView(R.layout.activity_account_management);

        // Bei Buttonklick Spotify Account verbinden
        Button addSpotify = (Button) findViewById(R.id.accountManagementAddSpotify);
        addSpotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Spotify Account verbinden
                IStreamingService spotify = new Spotify(AccountManagement.this);
                spotify.login(IStreamingService.LOGIN_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == IStreamingService.LOGIN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (data.getBooleanExtra(IStreamingService.LOGIN_RESULT, false)) {

                    SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_shared_preferences), MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(getString(R.string.spotify_access_token), data.getStringExtra(getString(R.string.spotify_access_token)));
                    editor.putInt(getString(R.string.spotify_access_token_expire_time), data.getIntExtra(getString(R.string.spotify_access_token_expire_time), 0));
                    Date currentTime = Calendar.getInstance().getTime();
                    Log.d(DEBUG_TAG, "Saved Time: " + currentTime.getTime());
                    editor.putLong(getString(R.string.spotify_access_token_save_time), currentTime.getTime());
                    editor.commit();

                    Spotify spotify = new Spotify(this);
                    Spotify.UserIDTask getUserId = spotify.new UserIDTask();
                    getUserId.execute(data.getStringExtra(getString(R.string.spotify_access_token)));

                    //Notification dass der Account erfolgreich verbunden wurde
                    Toast notification = Toast.makeText(this, getString(R.string.account_management_successfully_added_spotify), Toast.LENGTH_LONG);
                    notification.show();

                    //TODO: Datenbankinformationen hinzufügen
                    Log.d(DEBUG_TAG, "Datenbankdaten wurden erstellt");

                    // Activity beenden
                    this.finish();
                } else {
                    //TODO: Fehlermeldung für Nutzer einfügen
                    Log.d(DEBUG_TAG, "Verknüpfung ist fehlgeschlagen");
                }
            }
        }
    }
}