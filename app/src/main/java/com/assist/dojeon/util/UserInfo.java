package com.assist.dojeon.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.assist.dojeon.R;
import com.assist.dojeon.activity.LoginActivity;
import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.kakao.sdk.user.UserApiClient;
import com.assist.dojeon.dto.Dojeon;
import com.assist.dojeon.dto.User;
import com.assist.dojeon.receiver.NotificationReceiver;
import com.assist.dojeon.view.LoadingView;
import com.nhn.android.naverlogin.OAuthLogin;

import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class UserInfo {
    public static String getID(Context context){
        SharedPreferences preferences = context.getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        return preferences.getString("uid", "");
    }

    public static void setUser(Context context, User user){
        Gson gson = new Gson();
        SharedPreferences.Editor editor = context.getSharedPreferences("UserInfo", Context.MODE_PRIVATE).edit();
        editor.putString("user", gson.toJson(user));
        editor.apply();
    }

    public static User getUser(Context context){
        SharedPreferences preferences = context.getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        String json = preferences.getString("user", "");
        if (json.equals("")){
            String idByANDROID_ID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            User user = new User(idByANDROID_ID, "Guest" + randomChar() + randomChar() + new Random().nextInt(100000), null);
            enrollUser(context, user.uid, user.name, false);
            return user;
        }else{
            Gson gson = new Gson();
            return gson.fromJson(json, User.class);
        }
    }


    public static void enrollUser(Context context, String uid, String name, boolean isLogin){
        SharedPreferences.Editor editor = context.getSharedPreferences("UserInfo", Context.MODE_PRIVATE).edit();
        editor.putString("uid", uid);
        editor.putString("name", name);
        editor.putBoolean("isLogin", isLogin);
        editor.apply();
    }

    public static boolean getLogin(Context context){
        SharedPreferences preferences = context.getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        return preferences.getBoolean("isLogin", false);
    }

    public static void logout(Context context){
        SharedPreferences.Editor editor = context.getSharedPreferences("UserInfo", Context.MODE_PRIVATE).edit();
        editor.remove("uid");
        editor.remove("name");
        editor.remove("isLogin");
        editor.apply();

        logoutKakao();
        logoutGoogle(context);
        FirebaseAuth.getInstance().signOut();
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if(accessToken != null) {
            LoginManager.getInstance().logOut();
        }
        logoutNaver(context);
        removeAllNotice(context);


        //비회원으로 전환
        String idByANDROID_ID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        User user = new User(idByANDROID_ID, "Guest" + randomChar() + randomChar() + new Random().nextInt(100000), null);
        User lastUser = getUser(context);
        if (lastUser != null){
            user.addAllDojeon(lastUser.getDojeons());
        }
        enrollUser(context, user.uid, user.name, false);
        setUser(context, user);
    }

    private static void logoutNaver(Context context){
        OAuthLogin mOAuthLoginModule = OAuthLogin.getInstance();
        mOAuthLoginModule.init(
                context
                , context.getString(R.string.naver_client_id)
                ,context.getString(R.string.naver_client_secret)
                ,context.getString(R.string.naver_client_name)
                //,OAUTH_CALLBACK_INTENT
                // SDK 4.1.4 버전부터는 OAUTH_CALLBACK_INTENT변수를 사용하지 않습니다.
        );
        mOAuthLoginModule.logoutAndDeleteToken(context);
    }

    private static void logoutKakao(){ UserApiClient.getInstance().logout(new Function1<Throwable, Unit>() { @Override public Unit invoke(Throwable throwable) { return null; } }); }

    private static void logoutGoogle(Context context){
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
        mGoogleSignInClient.signOut();
    }

    private static char randomChar() {
        Random r = new Random();
        return (char)(r.nextInt(26) + 'A');
    }


    //회원 전용
    public static void saveData(User user, double res, Context context, LoadingView loadingView, int type, int range, int[] time, boolean isAlarm, Uri uri){

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String message = "";
        Calendar calendar = Calendar.getInstance();

        Dojeon dojeon = getDojeon(user, calendar, type);
        int alarmId = 0;
        boolean isExist = false;

        if (dojeon == null){

            message = String.format("새로운 도전을 하셨군요! 설정된 기간은 %d일입니다.", range);

            Date start = calendar.getTime();
            calendar.add(Calendar.DAY_OF_MONTH, range-1);
            calendar.set(Calendar.HOUR_OF_DAY, 11);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            Date end = calendar.getTime();

            dojeon = new Dojeon(new Random().nextInt(10000000), type, start, end, range);

            if (isAlarm) {
                Random random = new Random();
                alarmId = random.nextInt(10000000);
                dojeon.setAlarmId(alarmId);
                dojeon.setAlarmTime(time[0], time[1]);

                setNotice(context, dojeon);
            }



            calendar.add(Calendar.DAY_OF_MONTH, -range + 1);
            dojeon.check(calendar, res);
            user.addDojeon(dojeon);

            calendar.set(Calendar.HOUR_OF_DAY, time[0]);
            calendar.set(Calendar.MINUTE, time[1]);
        }else{
            message = String.format("%d월 %d일 도전 완료!", calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.DAY_OF_MONTH));
            dojeon.check(calendar, res);

            calendar.set(Calendar.HOUR_OF_DAY, dojeon.alarmHour);
            calendar.set(Calendar.MINUTE, dojeon.alarmMinute);
            isExist = true;
        }

        if (uri == null){
            String finalMessage1 = message;

            db.collection("users").document(user.uid).set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    loadingView.stop();
                    if (task.isSuccessful()){
                        Toast.makeText(context, finalMessage1, Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(context, "오류가 발생했습니다. 다시 저장해주세요.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }else{
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();

            final StorageReference ref = storageRef.child("users").child(user.getUid()).child(System.currentTimeMillis() + ".jpg");
            UploadTask uploadTask = ref.putFile(uri);


            String finalMessage = message;
            Dojeon finalDojeon = dojeon;

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
                        finalDojeon.addImage(Calendar.getInstance(), downloadUri);
                        db.collection("users").document(user.uid).set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                loadingView.stop();
                                if (task.isSuccessful()){
                                    Toast.makeText(context, finalMessage, Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(context, "오류가 발생했습니다. 다시 저장해주세요.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }else {
                        Toast.makeText(context, "오류가 발생했습니다. 다시 저장해주세요.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }

    //비회원 전용
    public static void saveData(User user, double res, Context context, LoadingView loadingView, int type, int range, int[] time, boolean isAlarm){
        String message;
        Calendar calendar = Calendar.getInstance();

        Dojeon dojeon = getDojeon(user, calendar, type);
        int alarmId = 0;

        if (dojeon == null){
            message = String.format("새로운 도전을 하셨군요! 설정된 기간은 %d일입니다.", range);

            Date start = calendar.getTime();
            calendar.add(Calendar.DAY_OF_MONTH, range-1);
            calendar.set(Calendar.HOUR_OF_DAY, 11);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);

            Date end = calendar.getTime();

            dojeon = new Dojeon(new Random().nextInt(10000000), type, start, end, range);
            Log.d("Calen", String.valueOf(isAlarm));
            if (isAlarm) {
                Random random = new Random();
                alarmId = random.nextInt(10000000);
                dojeon.setAlarmId(alarmId);
                dojeon.setAlarmTime(time[0], time[1]);
                setNotice(context, dojeon);
            }

            calendar.add(Calendar.DAY_OF_MONTH, -range + 1);

            dojeon.check(calendar, res);
            user.addDojeon(dojeon);

            calendar.set(Calendar.HOUR_OF_DAY, time[0]);
            calendar.set(Calendar.MINUTE, time[1]);
        }else{
            message = String.format("%d월 %d일 도전 완료!", calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.DAY_OF_MONTH));
            dojeon.check(calendar, res);
            calendar.set(Calendar.HOUR_OF_DAY, dojeon.alarmHour);
            calendar.set(Calendar.MINUTE, dojeon.alarmMinute);
        }

        loadingView.stop();
        setUser(context, user);

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

    }

    public static Dojeon getDojeon(User user, Calendar calendar, int type){
        if (user == null || user.getDojeons() == null) return null;
        for (Dojeon dojeon : user.getDojeons()) {
            long time = calendar.getTimeInMillis();
            if (dojeon.type == type && dojeon.start.getTime() <= time && dojeon.end.getTime() >= time){
                return dojeon;
            }
        }
        return null;
    }

    public static Dojeon getDojeon(User user, int id){
        if (user == null || user.getDojeons() == null) return null;
        for (Dojeon dojeon : user.getDojeons()) {
            if (dojeon.dojeonId == id){
                return dojeon;
            }
        }
        return null;
    }

    public static ArrayList<Dojeon> getProceedDojeon(User user){
        ArrayList<Dojeon> doj = new ArrayList<>();
        if (user == null || user.getDojeons() == null) return doj;

        for (Dojeon dojeon : user.getDojeons()) {
            long time = System.currentTimeMillis();
            if (dojeon.start.getTime() <= time && dojeon.end.getTime() >= time && dojeon.resMap.size() < dojeon.range){
                doj.add(dojeon);
            }
        }
        return doj;
    }
    public static ArrayList<Dojeon> getEndDojeon(User user){
        ArrayList<Dojeon> doj = new ArrayList<>();
        if (user == null || user.getDojeons() == null) return doj;

        for (Dojeon dojeon : user.getDojeons()) {
            long time = System.currentTimeMillis();
            if (dojeon.end.getTime() < time || dojeon.resMap.size() == dojeon.range){
                doj.add(dojeon);
            }
        }
        return doj;
    }

    public static int getSumDojeon(User user){
        int sum = 0;
        if (user == null || user.getDojeons() == null) return sum;
        for (Dojeon dojeon : user.getDojeons()) {
            sum += dojeon.timeMap.size();
        }
        return sum;
    }


    public static void removeNotice(Context context, int alarmId){
        AlarmManager mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent receiverIntent = new Intent(context, NotificationReceiver.class);
        PendingIntent mAlarmIntent = PendingIntent.getBroadcast(context, alarmId, receiverIntent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        mAlarmMgr.cancel(mAlarmIntent);
        mAlarmIntent.cancel();
    }

    public static void removeAllNotice(Context context){
        String uid = UserInfo.getID(context);
        if (uid.equals("")) return;

        if (getLogin(context)) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users").document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        User user = task.getResult().toObject(User.class);
                        if (user != null) {
                            ArrayList<Dojeon> dojeons = getProceedDojeon(user);
                            for (Dojeon dojeon : dojeons) {
                                if (dojeon.alarmId == 0) continue;
                                removeNotice(context, dojeon.alarmId);
                            }
                        }
                    }
                }
            });
        }
    }


    public static void getNotice(Context context){
        String uid = UserInfo.getID(context);
        if (uid.equals("")) return;

        if (!getLogin(context)){
            User user = getUser(context);
            if (user != null) {
                ArrayList<Dojeon> dojeons = getProceedDojeon(user);
                for(Dojeon dojeon : dojeons){
                    if (dojeon.alarmId == 0) continue;
                    removeNotice(context, dojeon.alarmId);
                    setNotice(context, dojeon);
                }
            }
        }else{
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users").document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()){
                        User user = task.getResult().toObject(User.class);
                        if (user != null) {
                            ArrayList<Dojeon> dojeons = getProceedDojeon(user);
                            for(Dojeon dojeon : dojeons){
                                if (dojeon.alarmId == 0) continue;
                                removeNotice(context, dojeon.alarmId);
                                setNotice(context, dojeon);
                            }
                        }
                    }
                }
            });
        }

    }


    public static void setNotice(Context context, Dojeon dojeon) {

        //알람을 수신할 수 있도록 하는 리시버로 인텐트 요청

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dojeon.start);
        calendar.set(Calendar.HOUR_OF_DAY, dojeon.alarmHour);
        calendar.set(Calendar.MINUTE, dojeon.alarmMinute);

        Log.d("Calen", calendar.getTime().toString());


        AlarmManager mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent receiverIntent = new Intent(context, NotificationReceiver.class);
        receiverIntent.putExtra("dojeon", dojeon);

        PendingIntent mAlarmIntent = PendingIntent.getBroadcast(context, dojeon.alarmId, receiverIntent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);


        //알람시간 설정
        //param 1)알람의 타입
        //param 2)알람이 울려야 하는 시간(밀리초)을 나타낸다.
        //param 3)알람이 울릴 때 수행할 작업을 나타냄
        mAlarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, mAlarmIntent);
    }


    public static void saveDojeon(double res, int type, Context context, int range, int[] time, boolean isAlarm, Uri uri){
        LoadingView loadingView = new LoadingView(context);
        loadingView.show("저장 중...");

        String idByANDROID_ID =
                Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (getID(context).equals("")){
            User user = new User(idByANDROID_ID, "Guest" + randomChar() + randomChar() + new Random().nextInt(100000), null);
            saveData(user, res, context, loadingView, type, range, time, isAlarm);
            enrollUser(context, user.uid, user.name, false);
        }else{
            if (!getLogin(context)){
                saveData(getUser(context), res, context, loadingView, type, range, time, isAlarm);
            }else{
                db.collection("users").document(UserInfo.getID(context)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()){
                            User user;

                            if (task.getResult().exists()){
                                user = task.getResult().toObject(User.class);
                            }else{
                                user = new User(idByANDROID_ID, "Guest" + randomChar() + randomChar() + new Random().nextInt(100000), null);
                            }

                            saveData(user, res, context, loadingView, type, range, time, isAlarm, uri);
                        }else{
                            loadingView.stop();
                            Toast.makeText(context, "오류가 발생했습니다. 다시 저장해주세요.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }
}
