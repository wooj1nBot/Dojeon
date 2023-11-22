package com.assist.dojeon.activity;

import static com.assist.dojeon.fragment.WeightFragment.WEIGHT_PATH;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.assist.dojeon.R;
import com.assist.dojeon.dto.Dojeon;
import com.assist.dojeon.fragment.HistoryFragment;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.common.KakaoSdk;
import com.kakao.sdk.common.util.Utility;
import com.kakao.sdk.user.UserApiClient;
import com.kakao.sdk.user.model.User;
import com.assist.dojeon.util.OnSingleClickListener;
import com.assist.dojeon.util.UserInfo;
import com.assist.dojeon.view.LoadingView;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class LoginActivity extends AppCompatActivity {

    GoogleSignInClient mGoogleSignInClient;
    CallbackManager callbackManager;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    Function2<OAuthToken, Throwable, Unit> callback;

    int RC_SIGN_IN = 9001; //구글 로그인을 감지하기 위한 변수
    OAuthLogin mOAuthLoginModule;

    LoadingView loadingView;

    OAuthLoginHandler mOAuthLoginHandler;

    @SuppressLint("HandlerLeak")

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestProfile()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        callbackManager = CallbackManager.Factory.create();


        callback = new Function2<OAuthToken, Throwable, Unit>() {
            @Override
            public Unit invoke(OAuthToken oAuthToken, Throwable throwable) {
                if (oAuthToken == null) {
                    Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    loadingView.stop();
                    return null;
                }
                handleKakaoLogin();
                return null;
            }
        };


        mOAuthLoginModule = OAuthLogin.getInstance();
        mOAuthLoginModule.init(
                LoginActivity.this
                ,getString(R.string.naver_client_id)
                ,getString(R.string.naver_client_secret)
                ,getString(R.string.naver_client_name)
                //,OAUTH_CALLBACK_INTENT
                // SDK 4.1.4 버전부터는 OAUTH_CALLBACK_INTENT변수를 사용하지 않습니다.
        );

       mOAuthLoginHandler = new OAuthLoginHandler() {


            @Override
            public void run(boolean success) {
                if (success) {
                    Log.d("logkkk", "sc1");
                    new RequestApiTask(LoginActivity.this, mOAuthLoginModule, loadingView).execute();
                } else {
                    Log.d("logkkk", String.valueOf(mOAuthLoginModule.getLastErrorCode(LoginActivity.this).toString()));
                    Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    loadingView.stop();
                }
            };
        };


        TextView skip = findViewById(R.id.skip);
        skip.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                finish();
            }
        });


        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

    }

    public class RequestApiTask extends AsyncTask<Void, Void, String> {
        private final Context mContext;
        private final OAuthLogin mOAuthLoginModule;
        private LoadingView loadingView;

        public RequestApiTask(Context mContext, OAuthLogin mOAuthLoginModule, LoadingView loadingView) {
            this.mContext = mContext;
            this.mOAuthLoginModule = mOAuthLoginModule;
            this.loadingView = loadingView;
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected String doInBackground(Void... params) {
            String url = "https://openapi.naver.com/v1/nid/me";
            String at = mOAuthLoginModule.getAccessToken(mContext);
            return mOAuthLoginModule.requestApi(mContext, at, url);
        }

        protected void onPostExecute(String content) {
            try {
                JSONObject loginResult = new JSONObject(content);
                if (loginResult.getString("resultcode").equals("00")){
                    JSONObject response = loginResult.getJSONObject("response");
                    String id = response.getString("id");
                    String name = response.getString("name");
                    String profile = response.getString("profile_image");
                    String email = null;
                    if (!response.isNull("email")){
                        email = response.getString("email");
                    }

                    login(id, name, profile, email, loadingView);
                }else{
                    Log.d("logkkk", "sc2");
                    loadingView.stop();
                    Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }

            } catch (JSONException e) {
                Log.d("logkkk", "sc3");
                loadingView.stop();
                Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }


    public void onClick(View v){

        loadingView = new LoadingView(this);
        loadingView.show("로그인 중...");

        if (v.getId() == R.id.google){
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
        if (v.getId() == R.id.facebook){
            LoginManager loginManager = LoginManager.getInstance();
            loginManager.logInWithReadPermissions(LoginActivity.this,
                    Collections.singletonList("public_profile"));
            loginManager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

                private ProfileTracker mProfileTracker;

                @Override
                public void onSuccess(LoginResult loginResult) {
                    mProfileTracker = new ProfileTracker() {
                        @Override
                        protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                            handleFacebookAccessToken(loginResult.getAccessToken(), currentProfile);
                            mProfileTracker.stopTracking();
                        }
                    };
                }

                @Override
                public void onCancel() {
                        loadingView.stop();
                    // ...
                }

                @Override
                public void onError(FacebookException error) {
                    loadingView.stop();
                    // ...
                }
            });
        }
        if (v.getId() == R.id.kakao){
            if(UserApiClient.getInstance().isKakaoTalkLoginAvailable(LoginActivity.this)){
                //카카오톡이 설치되어 있을 경우 -> 앱에서 로그인
                UserApiClient.getInstance().loginWithKakaoTalk(LoginActivity.this, callback);
            }else{ //카카오톡이 설치되어 있지 않다면 -> 웹에서 로그인
                UserApiClient.getInstance().loginWithKakaoAccount(LoginActivity.this, callback);
            }
        }
        if (v.getId() == R.id.naver){
            mOAuthLoginModule.startOauthLoginActivity(LoginActivity.this, mOAuthLoginHandler);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }


    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {

            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Uri profile = account.getPhotoUrl();
            String email = account.getEmail();
            String name = account.getDisplayName();
            String token = account.getIdToken();

            AuthCredential credential = GoogleAuthProvider.getCredential(token, null);
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                loadingView.stop();
                                Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (profile != null) {
                                login(user.getUid(), name, email, profile.toString(), loadingView);
                            }else {
                                login(user.getUid(), name, email,null, loadingView);
                            }
                        }
                    });
        } catch (ApiException e) {
            loadingView.stop();
            Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    private void handleFacebookAccessToken(AccessToken token, Profile profile) {

        String name = profile.getName();
        Uri profile_uri = profile.getPictureUri();
        // id로 유저 정보 관리

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            loadingView.stop();
                            Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (profile_uri != null) {
                            login(user.getUid(), name, null,profile_uri.toString(), loadingView);
                        }else {
                            login(user.getUid(), name,  null,null, loadingView);
                        }
                    }
                });
    }

    private void handleKakaoLogin(){
        UserApiClient.getInstance().me(new Function2<User, Throwable, Unit>() {
            @Override
            public Unit invoke(User user, Throwable throwable) {
                //로그인 되었다면
                if(user != null){
                    long id = user.getId();
                    String nickname = user.getKakaoAccount().getProfile().getNickname();
                    String email = user.getKakaoAccount().getEmail();
                    String profile_uri = user.getKakaoAccount().getProfile().getProfileImageUrl();
                    login(String.valueOf(id), nickname, email, profile_uri, loadingView);
                }
                // 안되어 있으면
                else{
                    loadingView.stop();
                    Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
                return null;
            }
        });
    }






    private void login(String id, String name, String email, String profile, LoadingView loadingView){

        com.assist.dojeon.dto.User user;

        if (!UserInfo.getID(LoginActivity.this).equals("")){
            user = UserInfo.getUser(LoginActivity.this);
            user.setUid(id);
            user.setName(name);
            user.setEmail(email);
            user.setProfile(profile);
        }else {
            user = new com.assist.dojeon.dto.User(id, name, email);
            user.setProfile(profile);
        }

        db.collection("users").document(id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()){
                    com.assist.dojeon.dto.User al = task.getResult().toObject(com.assist.dojeon.dto.User.class);
                    al.addAllDojeon(user.getDojeons());

                    db.collection("users").document(id).set(al).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            loadingView.stop();
                            if (task.isSuccessful()){
                                uploadFiles(user);
                                UserInfo.enrollUser(LoginActivity.this, user.uid, user.name, true);
                                Toast.makeText(LoginActivity.this, "다시 오신 것을 환영합니다!", Toast.LENGTH_SHORT).show();
                                UserInfo.getNotice(LoginActivity.this);
                                finish();
                            }else{
                                Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else{
                    db.collection("users").document(id).set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            loadingView.stop();
                            if (task.isSuccessful()){
                                uploadFiles(user);
                                UserInfo.enrollUser(LoginActivity.this, user.uid, user.name, true);
                                Toast.makeText(LoginActivity.this, "로그인에 성공했습니다.", Toast.LENGTH_SHORT).show();
                                UserInfo.getNotice(LoginActivity.this);
                                finish();
                            }else{
                                Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }


    public void uploadFiles(com.assist.dojeon.dto.User user){

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        for (Dojeon dojeon : user.getDojeons()){
            for (String key : dojeon.timeMap.keySet()){

                String imageFileName = WEIGHT_PATH + "_" + key;
                File file = new File(getFilesDir(), imageFileName + ".jpg");

                if (file.exists()) {
                    Log.d("file", file.getName());

                    Uri uri = Uri.fromFile(file);
                    final StorageReference ref = storageRef.child("users").child(user.getUid()).child(System.currentTimeMillis() + ".jpg");
                    UploadTask uploadTask = ref.putFile(uri);

                    Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }

                            // Continue with the task to get the download URL
                            return ref.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloadUri = task.getResult();
                                db.collection("users").document(user.getUid()).update("dojeons", FieldValue.arrayRemove(dojeon));
                                Calendar c = Calendar.getInstance();
                                c.setTime(dojeon.timeMap.get(key));
                                dojeon.addImage(c, downloadUri);
                                db.collection("users").document(user.getUid()).update("dojeons", FieldValue.arrayUnion(dojeon));
                            }
                        }
                    });
                }
            }
        }


    }








}