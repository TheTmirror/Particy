package com.mocoteam1.musicmatch.streamingservice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.mocoteam1.musicmatch.R;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

/**
 * Created by Tristan on 04.01.2018.
 */

public class SpotifyLogin extends AppCompatActivity {

    private static final String DEBUG_TAG = SpotifyLogin.class.getSimpleName();

    public static final String CLIENT_ID = "9d348edc95004d878a4e4cdbc0db66a2";
    private static final String REDIRECT_URI = "moco-spotify-app://callback";
    private static final int REQUEST_CODE = 23071;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"streaming", "playlist-read-private", "playlist-read-collaborative"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);

            Intent resultIntent = new Intent();

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    Log.d(DEBUG_TAG, "Einloggen erfolgreich");
                    resultIntent.putExtra(IStreamingService.LOGIN_RESULT, true);
                    resultIntent.putExtra(getString(R.string.spotify_access_token), response.getAccessToken());
                    resultIntent.putExtra(getString(R.string.spotify_access_token_expire_time), response.getExpiresIn() * 1000);
                    setResult(RESULT_OK, resultIntent);
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Log.d(DEBUG_TAG, "Einloggen fehlgeschlafen");
                    resultIntent.putExtra(IStreamingService.LOGIN_RESULT, false);
                    setResult(RESULT_OK, resultIntent);
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
                    Log.d(DEBUG_TAG, "Sollte nicht vorkommen");
                    setResult(RESULT_CANCELED);
            }

            finish();
        }

    }
}
