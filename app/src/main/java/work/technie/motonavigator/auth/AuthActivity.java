package work.technie.motonavigator.auth;

/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) 2017 Anupam Das
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import work.technie.motonavigator.R;
import work.technie.motonavigator.data.MotorContract;

/**
 * Created by anupam on 29/10/16.
 */

public class AuthActivity extends BaseActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = "AuthActivity";
    private static final int RC_SIGN_IN = 9001;
    private static final int ANONYMOUS_SIGN_IN = 0;
    private static final int GOOGLE_SIGN_IN = 1;
    private static final int EMAIL_PASSWORD_SIGN_IN = 2;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private GoogleApiClient mGoogleApiClient;

    private EditText mEmailField;
    private EditText mPasswordField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        mEmailField = (EditText) findViewById(R.id.field_email);
        mPasswordField = (EditText) findViewById(R.id.field_password);

        findViewById(R.id.google_sign_in_button).setOnClickListener(this);
        findViewById(R.id.guest_sign_in_button).setOnClickListener(this);
        findViewById(R.id.email_sign_in_button).setOnClickListener(this);
        findViewById(R.id.email_create_account_button).setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, getString(R.string.onAuthStateChanges_signed_in) + user.getUid());
                } else {
                    Log.d(TAG, getString(R.string.onAuthStateChanges_sign_out));
                }
            }
        };
    }

    private void signInAnonymously() {
        showProgressDialog();
        // [START signin_anonymously]
        final Activity mActivity = this;

        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, getString(R.string.signInAnonym_complete) + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, getString(R.string.signInAnonym), task.getException());
                            Toast.makeText(AuthActivity.this, R.string.authentication_failed,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putInt(getString(R.string.login_mode), ANONYMOUS_SIGN_IN);
                            editor.apply();

                            Intent intent = new Intent(mActivity, work.technie.motonavigator.activity.BaseActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }

                        // [START_EXCLUDE]
                        hideProgressDialog();



                        // [END_EXCLUDE]
                    }
                });
        // [END signin_anonymously]
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);

                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(getString(R.string.login_mode), GOOGLE_SIGN_IN);
                editor.apply();


                final Uri waypointUri = MotorContract.Waypoints.buildWaypointUri();
                this.getContentResolver().delete(waypointUri,null,null);

                final Uri stepsUri = MotorContract.Steps.buildStepUri();
                this.getContentResolver().delete(stepsUri,null,null);

                if(mAuth.getCurrentUser() != null) {
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference waypointsRef = database.getReference(mAuth.getCurrentUser().getUid())
                            .child("STEPS");
                    waypointsRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                                ContentValues contentValue = new ContentValues();
                                contentValue.put(MotorContract.Steps.ROUTE_ID, messageSnapshot.child(MotorContract.Steps.ROUTE_ID).getValue().toString());
                                contentValue.put(MotorContract.Steps.BEARING_BEFORE, messageSnapshot.child(MotorContract.Steps.BEARING_BEFORE).getValue().toString());
                                contentValue.put(MotorContract.Steps.BEARING_AFTER, messageSnapshot.child(MotorContract.Steps.BEARING_AFTER).getValue().toString());
                                contentValue.put(MotorContract.Steps.LOCATION_LAT, messageSnapshot.child(MotorContract.Steps.LOCATION_LAT).getValue().toString());
                                contentValue.put(MotorContract.Steps.LOCATION_LONG, messageSnapshot.child(MotorContract.Steps.LOCATION_LONG).getValue().toString());
                                contentValue.put(MotorContract.Steps.TYPE, messageSnapshot.child(MotorContract.Steps.TYPE).getValue().toString());
                                contentValue.put(MotorContract.Steps.INSTRUCTION, messageSnapshot.child(MotorContract.Steps.INSTRUCTION).getValue().toString());
                                contentValue.put(MotorContract.Steps.MODE, messageSnapshot.child(MotorContract.Steps.MODE).getValue().toString());
                                contentValue.put(MotorContract.Steps.DURATION, messageSnapshot.child(MotorContract.Steps.DURATION).getValue().toString());
                                contentValue.put(MotorContract.Steps.NAME, messageSnapshot.child(MotorContract.Steps.NAME).getValue().toString());
                                contentValue.put(MotorContract.Steps.DISTANCE, messageSnapshot.child(MotorContract.Steps.DISTANCE).getValue().toString());

                                getApplicationContext().getContentResolver().insert(stepsUri, contentValue);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError firebaseError) {
                        }
                    });

                    DatabaseReference stepRef = database.getReference(mAuth.getCurrentUser().getUid())
                            .child("WAYPOINTS");
                    stepRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                                ContentValues contentValue = new ContentValues();
                                contentValue.put(MotorContract.Waypoints.ROUTE_ID, messageSnapshot.child(MotorContract.Waypoints.ROUTE_ID).getValue().toString());
                                contentValue.put(MotorContract.Waypoints.START_NAME, messageSnapshot.child(MotorContract.Waypoints.START_NAME).getValue().toString());
                                contentValue.put(MotorContract.Waypoints.START_LAT, messageSnapshot.child(MotorContract.Waypoints.START_LAT).getValue().toString());
                                contentValue.put(MotorContract.Waypoints.START_LONG, messageSnapshot.child(MotorContract.Waypoints.START_LONG).getValue().toString());
                                contentValue.put(MotorContract.Waypoints.DEST_NAME, messageSnapshot.child(MotorContract.Waypoints.DEST_NAME).getValue().toString());
                                contentValue.put(MotorContract.Waypoints.DEST_LAT, messageSnapshot.child(MotorContract.Waypoints.DEST_LAT).getValue().toString());
                                contentValue.put(MotorContract.Waypoints.DEST_LONG, messageSnapshot.child(MotorContract.Waypoints.DEST_LONG).getValue().toString());
                                contentValue.put(MotorContract.Waypoints.MODE, messageSnapshot.child(MotorContract.Waypoints.MODE).getValue().toString());
                                contentValue.put(MotorContract.Waypoints.ROUTE_DURATION, messageSnapshot.child(MotorContract.Waypoints.ROUTE_DURATION).getValue().toString());
                                contentValue.put(MotorContract.Waypoints.ROUTE_DISTANCE, messageSnapshot.child(MotorContract.Waypoints.ROUTE_DISTANCE).getValue().toString());

                                getApplicationContext().getContentResolver().insert(waypointUri, contentValue);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError firebaseError) {
                        }
                    });
                }

                Intent intent = new Intent(this, work.technie.motonavigator.activity.BaseActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(intent);

            } else {
                // Google Sign In failed, update UI appropriately
                // [START_EXCLUDE]
                // [END_EXCLUDE]
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, getString(R.string.firebase_google) + acct.getId());
        showProgressDialog();

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, getString(R.string.signInWithCredential_complete) + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, getString(R.string.signInWithCredential), task.getException());
                            Toast.makeText(AuthActivity.this, getString(R.string.authentication_failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                        // [START_EXCLUDE]
                        hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut() {
        mAuth.signOut();

        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                    }
                });
    }

    private void revokeAccess() {
        // Firebase sign out
        mAuth.signOut();

        // Google revoke access
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, getString(R.string.connection_failed) + connectionResult);
        Toast.makeText(this, R.string.google_play_error, Toast.LENGTH_SHORT).show();
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = mEmailField.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmailField.setError(getString(R.string.required));
            valid = false;
        } else {
            mEmailField.setError(null);
        }

        String password = mPasswordField.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError(getString(R.string.required));
            valid = false;
        } else {
            mPasswordField.setError(null);
        }

        return valid;
    }

    private void createAccount(String email, String password) {
        Log.d(TAG, getString(R.string.createAccount) + email);
        if (!validateForm()) {
            return;
        }

        final Activity mActivity = this;

        showProgressDialog();

        // [START create_user_with_email]
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, getString(R.string.createUserWithEmail_complete) + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(AuthActivity.this, R.string.auth_failed,
                                    Toast.LENGTH_SHORT).show();
                        } else {

                            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putInt(getString(R.string.login_mode), EMAIL_PASSWORD_SIGN_IN);
                            editor.apply();

                            Intent intent = new Intent(mActivity, work.technie.motonavigator.activity.BaseActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }

                        // [START_EXCLUDE]
                        hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
        // [END create_user_with_email]
    }

    private void signInEmail(String email, String password) {
        Log.d(TAG, getString(R.string.sign_in) + email);
        if (!validateForm()) {
            return;
        }

        final Activity mActivity = this;
        showProgressDialog();

        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, getString(R.string.signEmail_complete) + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, getString(R.string.signEmail_failed), task.getException());
                            Toast.makeText(AuthActivity.this, R.string.auth_failed,
                                    Toast.LENGTH_SHORT).show();
                        }

                        // [START_EXCLUDE]
                        if (!task.isSuccessful()) {
                            //failed
                        } else {
                            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putInt(getString(R.string.login_mode), EMAIL_PASSWORD_SIGN_IN);
                            editor.apply();

                            Intent intent = new Intent(mActivity, work.technie.motonavigator.activity.BaseActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                        hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
        // [END sign_in_with_email]
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();

        if (i == R.id.guest_sign_in_button) {
            signInAnonymously();
        } else if (i == R.id.google_sign_in_button) {
            signIn();
        } else if (i == R.id.email_create_account_button) {
            createAccount(mEmailField.getText().toString(), mPasswordField.getText().toString());
        } else if (i == R.id.email_sign_in_button) {
            signInEmail(mEmailField.getText().toString(), mPasswordField.getText().toString());
        }
    }
}
