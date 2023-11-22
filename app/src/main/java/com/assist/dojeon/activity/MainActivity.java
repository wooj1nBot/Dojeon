package com.assist.dojeon.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreenViewProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.splashscreen.SplashScreen;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.assist.dojeon.R;
import com.assist.dojeon.dto.Dojeon;
import com.assist.dojeon.fragment.HomeFragment;
import com.assist.dojeon.fragment.MeditateFragment;
import com.assist.dojeon.fragment.ProfileFragment;
import com.assist.dojeon.fragment.WeightFragment;
import com.assist.dojeon.util.UserInfo;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    FragmentManager manager;
    HomeFragment homeFragment;
    ProfileFragment profileFragment;


    TextView tv1, tv2, tv3;
    ImageView iv1, iv2, iv3;
    private final int REQUEST_IMAGE_CAPTURE = 69;
    private long pressedTime;



    private static final int MY_PERMISSION_STORAGE = 1111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv1 = findViewById(R.id.tv1);
        tv2 = findViewById(R.id.tv2);
        tv3 = findViewById(R.id.tv3);

        iv1 = findViewById(R.id.iv1);
        iv2 = findViewById(R.id.iv2);
        iv3 = findViewById(R.id.iv3);

        homeFragment = new HomeFragment();
        profileFragment = new ProfileFragment(true);

        FirebaseApp.initializeApp(/*context=*/ this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance());

        checkPermission();


        manager = getSupportFragmentManager();
        manager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                if (f instanceof HomeFragment) {
                    iv2.setImageResource(R.drawable.home_filled);
                    tv2.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

                    iv1.setImageResource(R.drawable.profile);
                    tv1.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                }
                if (f instanceof ProfileFragment) {
                    ProfileFragment p = (ProfileFragment) f;
                    if (p.isProfile) {
                        iv1.setImageResource(R.drawable.profile_filled);
                        tv1.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                        iv2.setImageResource(R.drawable.home);
                        tv2.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                    }
                }
            }
        }, true);
        initFragment();

        Intent intent = getIntent();
        Dojeon dojeon = (Dojeon) intent.getSerializableExtra("dojeon");
        if (dojeon != null){

            Fragment fragment = null;

            if (dojeon.type == Dojeon.TYPE_MEDITATE) {
                fragment = new MeditateFragment(dojeon.dojeonId);
            }
            if (dojeon.type == Dojeon.TYPE_WEIGHT) {
                fragment = new WeightFragment(dojeon.dojeonId);
            }
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.parent, fragment);
            transaction.addToBackStack(null);
            transaction.commitAllowingStateLoss();
        }

    }
    public void initFragment(){
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(R.id.parent, homeFragment);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }

    private void checkPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, MY_PERMISSION_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int i = 0; i < permissions.length; i++){
            if (permissions[i].equals(Manifest.permission.POST_NOTIFICATIONS)){

                if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS)) {
                        // 사용자가 거부만 선택한 경우에는 앱을 다시 실행하여 허용을 선택하면 앱을 사용할 수 있습니다.
                        Toast toast= Toast.makeText(MainActivity.this, "기능 사용을 위한 알람 권한 동의가 필요합니다.", Toast.LENGTH_LONG); toast.show();
                    }else {

                        Toast toast=Toast.makeText(MainActivity.this, "기능 사용을 위한 알람 권한 동의가 필요합니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다.", Toast.LENGTH_LONG); toast.show();
                        // “다시 묻지 않음”을 사용자가 체크하고 거부를 선택한 경우에는 설정(앱 정보)에서 퍼미션을 허용해야 앱을 사용할 수 있습니다.
                    }
                }else{

                }
            }

            if (Objects.equals(permissions[i], Manifest.permission.CAMERA)) {

                if (grantResults[i] == PackageManager.PERMISSION_GRANTED ){
                    if (homeFragment.weightFragment != null) {
                        homeFragment.weightFragment.openCamera();
                    }
                }else {
                    if (homeFragment.weightFragment != null) {
                        homeFragment.weightFragment.closeCamera();
                    }
                    // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                        // 사용자가 거부만 선택한 경우에는 앱을 다시 실행하여 허용을 선택하면 앱을 사용할 수 있습니다.

                        Toast toast= Toast.makeText(this, "기능 사용을 위한 카메라 권한 동의가 필요합니다.", Toast.LENGTH_SHORT); toast.show();
                    }else {

                        Toast toast=Toast.makeText(this, "기능 사용을 위한 카메라 권한 동의가 필요합니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다.", Toast.LENGTH_SHORT); toast.show();
                        // “다시 묻지 않음”을 사용자가 체크하고 거부를 선택한 경우에는 설정(앱 정보)에서 퍼미션을 허용해야 앱을 사용할 수 있습니다.
                    }
                }
            }
        }


    }


    public void onClick(View v){
        FragmentTransaction transaction = manager.beginTransaction();

        if (v.getId() == R.id.mypage) {
            if (UserInfo.getLogin(MainActivity.this)){
                transaction.replace(R.id.parent, profileFragment);
                transaction.commitAllowingStateLoss();
            }else{
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        } else if (v.getId() == R.id.home) {
            transaction.replace(R.id.parent, homeFragment);
            transaction.commitAllowingStateLoss();

        } else if (v.getId() == R.id.premium) {
            Intent intent = new Intent(MainActivity.this, BillingActivity.class);
            startActivity(intent);
        }
    }

    public void setBack(){
        FragmentTransaction transaction = manager.beginTransaction();
        if (manager.getBackStackEntryCount() > 1) {
            Fragment fragment = manager.findFragmentById(R.id.parent);
            if (fragment != null) {
                transaction.remove(fragment).commit();
                manager.popBackStack();
            }
        }
    }

    @Override
    public void onBackPressed() {
        FragmentTransaction transaction = manager.beginTransaction();
        if (manager.getBackStackEntryCount() > 1) {
            Fragment fragment = manager.findFragmentById(R.id.parent);
            if (fragment != null) {
                transaction.remove(fragment).commit();
                manager.popBackStack();
            }
        }else{
            if ( pressedTime == 0 ) {
                Toast.makeText(MainActivity.this, " 한 번 더 누르면 종료됩니다." , Toast.LENGTH_LONG).show();
                pressedTime = System.currentTimeMillis();
            }
            else {
                int seconds = (int) (System.currentTimeMillis() - pressedTime);

                if ( seconds > 2000 ) {
                    Toast.makeText(MainActivity.this, " 한 번 더 누르면 종료됩니다." , Toast.LENGTH_LONG).show();
                    pressedTime = 0 ;
                }
                else {
                    super.onBackPressed();
                    finish(); // app 종료 시키기
                }
            }
        }

    }


}