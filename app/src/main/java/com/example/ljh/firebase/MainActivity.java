package com.example.ljh.firebase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v7.widget.AppCompatButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.StorageReference;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private FirebaseStorage mFirebaseStorage;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        // button_fetch 버튼이 눌리면 실행되게 한다.
        Button mFetch = (Button) findViewById(R.id.button_fetch);
        mFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFetchButton(v);
            }
        });

        Button mLogout = (Button) findViewById(R.id.button_logout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLogoutButton(v);
            }
        });


        mFirebaseStorage = FirebaseStorage.getInstance();
        // RemoteConfig 초기화 부분
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // Create Remote Config Setting to enable developer mode.
        // Fetching configs from the server is normally limited to 5 requests per hour.
        // Enabling developer mode allows many more requests to be made per hour, so developers
        // can test different config values during development.
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();

        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);

        displayConfig();
        displayImage();
    }

    // Storage에서 이미지 읽어서 표시하는 부분
    private void displayImage() {
        // Create a storage reference from our app
        StorageReference storageRef = mFirebaseStorage.getReferenceFromUrl("gs://jonghoon-753d2.appspot.com/겨울.PNG");
        storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Log.d(TAG, "getBytes Success");
                // Use the bytes to display the image

                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                ImageView iv = (ImageView)findViewById(R.id.imageView);
                iv.setImageBitmap(bmp);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                Log.d(TAG, "getBytes Failed");
            }
        });
    }

    // 설정을 읽는 부분
    private void displayConfig() {
        Boolean cheat_enabled = mFirebaseRemoteConfig.getBoolean("cheat_enabled");
        ((TextView)findViewById(R.id.textView_cheat)).setText("cheat_enabled=" + cheat_enabled);
        long price = mFirebaseRemoteConfig.getLong("your_price");
        ((TextView)findViewById(R.id.textView_price)).setText("your_price is " + price);
    }

    // 설정을 로컬로 가져오는 부분
    public void onFetchButton(View v) {
        long cacheExpiration = 3600; // 1 hour in seconds.
        // If in developer mode cacheExpiration is set to 0 so each fetch will retrieve values from
        // the server.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Fetch Succeeded");
                            // Once the config is successfully fetched it must be activated before newly fetched
                            // values are returned.
                            mFirebaseRemoteConfig.activateFetched();
                        } else {
                            Log.d(TAG, "Fetch failed");
                        }
                        displayConfig();
                    }
                });
    }

    public void onLogoutButton(View v) {
        mAuth.signOut();
        finish();
    }

}
