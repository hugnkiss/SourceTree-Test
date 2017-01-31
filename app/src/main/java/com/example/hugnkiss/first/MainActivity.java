package com.example.hugnkiss.first;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static android.R.attr.data;
import static android.R.attr.x;
import static java.sql.DriverManager.println;

public class MainActivity extends AppCompatActivity {
    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private String mUsername;
    private String mToken;
    private String mUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        //mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    mUID = user.getUid();
                    // User is signed in
                    Log.d("hug", "onAuthStateChanged:signed_in:" + mUID);
                    //Log.d("hug", "onAuthStateChanged:signed_in:" + user.getDisplayName());

                } else {
                    // User is signed out
                    Log.d("hug", "onAuthStateChanged:signed_out");
                }
            }
        };

        // Thread로 웹서버에 접속
        new Thread() {
            public void run() {
                String token = getCustomToken();

                Bundle bun = new Bundle();
                bun.putString("CUSTOMTOKEN", token);
                Message msg = handler.obtainMessage();
                msg.setData(bun);
                handler.sendMessage(msg);
            }
        }.start();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mFirebaseAuth.addAuthStateListener(mAuthListener);
    }

    protected void updateToken() {

        if (mToken != null) {
            //Log.e("hug", mToken);
            mFirebaseAuth.signInWithCustomToken(mToken)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.d("hug", "signInWithCustomToken:onComplete:" + task.isSuccessful());

                            // If sign in fails, display a message to the user. If sign in succeeds
                            // the auth state listener will be notified and logic to handle the
                            // signed in user can be handled in the listener.
                            if (!task.isSuccessful()) {
                                Log.w("hug", "signInWithCustomToken", task.getException());
                            }
                            else {
                                pushDBdata();
                            }
                        }
                    });
        }

        if (mFirebaseUser == null) {
            Log.e("hug", "mFirebaseUser is null");
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            Log.e("hug", "username: " + mUsername);
        }

    }
    public class Order {
        public String deliveredTime;
        public String depositAmount;
        public boolean isDelivered;
        public List<Item> orderItems;

        public String restaurantKey;
        public String vendorKey;

        public Order(){
        }

        public class Item {
            public float count;
            public String itemKey;
            public String name;
            public int totalPrice;
        }
    }

    protected  void pushDBdata() {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders");

        orderRef.child("2017/01").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.e("hug", "onChildAdded: " + dataSnapshot.toString());
                //Order order = dataSnapshot.getValue(Order.class);
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
            });

        /*
        orderRef.child("2017/01/27").push().setValue("star")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d("hug", "order:setValue:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Log.w("hug", "order:setValue", task.getException());
                        }
                    }
                });

        // db에 mUID가 없어도 ref 값이 null이 아님.
        Log.w("hug", "vendRef: " + FirebaseDatabase.getInstance().getReference("vendors/"+mUID).toString());
        DatabaseReference vendRef = FirebaseDatabase.getInstance().getReference("vendors");
        vendRef.child(mUID+"/info").push().setValue("star-1122")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d("hug", "vendors:setValue:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Log.w("hug", "vendors:setValue", task.getException());
                        }
                    }
                });
        */
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthListener);
        }
        // ...
    }

    private String getCustomToken() {
        String token = "";

        URL url = null;
        HttpURLConnection http = null;
        InputStreamReader isr = null;
        BufferedReader br = null;

        try {
            url = new URL("http://10.0.2.2:9000/requestTokenWithPhoneNumber?phoneNumber=01012341001");
            http = (HttpURLConnection) url.openConnection();
            http.setConnectTimeout(10 * 1000);
            http.setReadTimeout(10 * 1000);

            isr = new InputStreamReader(http.getInputStream());
            br = new BufferedReader(isr);

            String str = null;
            while ((str = br.readLine()) != null) {
                token += str + "\n";
            }

        } catch (Exception e) {
            Log.e("hug", e.toString());
        } finally {
        }

        return token;
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            Bundle bun = msg.getData();
            String jString = bun.getString("CUSTOMTOKEN");

            try {
                JSONObject obj = new JSONObject(jString);
                mToken = obj.getString("firebaseCustomAuthToken");
            } catch (JSONException e) {

            }
            //Log.d("hug", "mToken: " + mToken);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateToken();
                }
            });
        }
    };
}

