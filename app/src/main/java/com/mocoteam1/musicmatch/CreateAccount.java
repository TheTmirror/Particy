package com.mocoteam1.musicmatch;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class CreateAccount extends AppCompatActivity {

    private static final String DEBUG_TAG = CreateAccount.class.getSimpleName();

    public static final String PASSWORD = "password";
    public static final String EMAIL = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_create_account);

        final EditText emailText = (EditText) findViewById(R.id.createAccountEmail);
        final EditText passwordText = (EditText) findViewById(R.id.createAccountPassword);
        final EditText repeatedPasswordText = (EditText) findViewById(R.id.createAccountRepeatedPassword);
        Button createAccountButton = (Button) findViewById(R.id.createAccountCreateButton);

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailText.getText().toString();
                String password = passwordText.getText().toString();
                String repeatedPassword = repeatedPasswordText.getText().toString();

                TextView errorText = (TextView) findViewById(R.id.createAccountErrorText);

                if(!password.equals(repeatedPassword)) {
                    errorText.setText(getString(R.string.create_account_error_text_password_no_match));
                    errorText.setVisibility(View.VISIBLE);
                    return;
                } else if(!email.contains("@")){
                    errorText.setText(getString(R.string.create_account_error_text_email));
                    errorText.setVisibility(View.VISIBLE);
                    return;
                } else if(password.length() < 6) {
                    errorText.setText(getString(R.string.create_account_error_text_password_to_short));
                    errorText.setVisibility(View.VISIBLE);
                    return;
                }

                Intent resultIntent = new Intent();
                resultIntent.putExtra(EMAIL, email);
                resultIntent.putExtra(PASSWORD, password);
                setResult(RESULT_OK, resultIntent);
                finish();

                Log.d(DEBUG_TAG, "Account was created according to our guidelines");
            }
        });
    }
}
