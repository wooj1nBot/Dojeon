package com.assist.dojeon.fragment;

import static com.assist.dojeon.fragment.SettingFragment.setBack;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.assist.dojeon.R;
import com.assist.dojeon.dto.Dojeon;
import com.assist.dojeon.dto.User;
import com.assist.dojeon.util.OnSingleClickListener;
import com.assist.dojeon.util.UserInfo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


public class MeditateFragment extends Fragment {


    HistoryFragment fragment;
    FirebaseFirestore db;
    private final int STATE_STOP = 0;
    private final int STATE_START = 1;
    private final int STATE_COMPLETE = 2;
    private final int STATE_SAVE = 3;
    private final int STATE_PAUSE = 4;

    private int state = STATE_STOP;

    private Timer appTimer;

    private long time = 0;

    private Handler handler;

    private final int DEFAULT_CHECK_TIME = 16;

    SettingFragment settingFragment;
    int currentId;
    TextView tv_min ;
    TextView tv_sec ;
    TextView tv_mic;

    MaterialCardView start;
    TextView tv_start;
    CardView history ;

    public MeditateFragment(){
        settingFragment = new SettingFragment(Dojeon.TYPE_MEDITATE);
        fragment = new HistoryFragment(Dojeon.TYPE_MEDITATE);
        currentId = -1;
    }
    public MeditateFragment(int currentId){
        this.currentId = currentId;
        settingFragment = new SettingFragment(Dojeon.TYPE_MEDITATE);
        fragment = new HistoryFragment(Dojeon.TYPE_MEDITATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_meditate, container, false);
        db = FirebaseFirestore.getInstance();

        tv_min = view.findViewById(R.id.tv_min);
        tv_sec = view.findViewById(R.id.tv_sec);
        tv_mic = view.findViewById(R.id.tv_mic);

        start = view.findViewById(R.id.start);
        tv_start = view.findViewById(R.id.tv_start);
        history = view.findViewById(R.id.history);

        handler = new Handler();

        TextView home = view.findViewById(R.id.home);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                state = STATE_STOP;
                setBack(getActivity());
            }
        });

        history.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if(appTimer != null){
                    appTimer.cancel();
                }

                state = STATE_PAUSE;

                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.parent, fragment);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });

        ImageView setting = view.findViewById(R.id.setting);

        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(appTimer != null){
                    appTimer.cancel();
                }

                state = STATE_PAUSE;

                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.parent, settingFragment);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });

        start.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (state == STATE_STOP || state == STATE_PAUSE){
                    //정지한 상태
                    TimerTask purposeTask = new TimerTask() {
                        @Override
                        public void run() {
                            time++;
                            long t = time;
                            long mic = t % 60;
                            t /= 60;
                            long sec = t % 60;
                            t /= 60;
                            long min = t;

                            if (min >= 5){
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        state = STATE_COMPLETE;
                                        start.setCardBackgroundColor(Color.parseColor("#7198AD"));
                                        start.setStrokeWidth(0);
                                        tv_start.setText("완료하기");
                                        tv_start.setTextColor(Color.WHITE);
                                    }
                                });
                            }

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (state != STATE_COMPLETE) {
                                        state = STATE_START;
                                    }
                                    String text = String.format("%02d", min);
                                    tv_min.setText(text);
                                    text = String.format("%02d", sec);
                                    tv_sec.setText(text);
                                    text = String.format("%02d", mic);
                                    tv_mic.setText(text);
                                }
                            });

                        }
                    };

                    appTimer = new Timer();
                    appTimer.schedule(purposeTask, 0, DEFAULT_CHECK_TIME);

                    start.setCardBackgroundColor(Color.WHITE);
                    start.setStrokeWidth(3);
                    start.setStrokeColor(Color.parseColor("#3C3C3C"));
                    tv_start.setText("일시 정지");
                    tv_start.setTextColor(Color.parseColor("#3C3C3C"));
                }
                else if (state == STATE_START){
                    if(appTimer != null){
                        appTimer.cancel();
                    }

                    state = STATE_PAUSE;

                    start.setCardBackgroundColor(Color.parseColor("#3C3C3C"));
                    start.setStrokeWidth(0);
                    tv_start.setText("시작하기");
                    tv_start.setTextColor(Color.WHITE);
                }
                else if (state == STATE_SAVE){
                    //이미 저장됨
                    state = STATE_STOP;
                    currentId = -1;
                    time = 0;
                    String text = String.format("%02d", 0);
                    tv_min.setText(text);
                    text = String.format("%02d", 0);
                    tv_sec.setText(text);
                    text = String.format("%02d", 0);
                    tv_mic.setText(text);

                    start.setCardBackgroundColor(Color.parseColor("#3C3C3C"));
                    start.setStrokeWidth(0);
                    tv_start.setText("시작하기");
                    tv_start.setTextColor(Color.WHITE);
                }

                else if (state == STATE_COMPLETE){
                    //5분 넘음 -> 저장 가능

                    if(appTimer != null){
                        appTimer.cancel();
                    }

                    tv_start.setText("다시 시작");
                    state = STATE_SAVE;

                    UserInfo.saveDojeon(time, Dojeon.TYPE_MEDITATE, getActivity(), settingFragment.mRange, settingFragment.mTimes, settingFragment.isAlarm, null);
                }
            }
        });

        Calendar c = Calendar.getInstance();
        String key = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH);

        if (currentId != -1){
            fragment.setDojeonId(currentId);
            settingFragment.setDojeonId(currentId);

            if (!UserInfo.getID(getActivity()).equals("")) {
                if (!UserInfo.getLogin(getActivity())) {
                    Dojeon dojeon = UserInfo.getDojeon(UserInfo.getUser(getContext()), currentId);
                    if (dojeon != null) {
                        if (dojeon.resMap.containsKey(key)) {
                            state = STATE_SAVE;
                            long t = dojeon.resMap.get(key).longValue();
                            long mic = t % 60;
                            t /= 60;
                            long sec = t % 60;
                            t /= 60;
                            long min = t;

                            String text = String.format("%02d", min);
                            tv_min.setText(text);
                            text = String.format("%02d", sec);
                            tv_sec.setText(text);
                            text = String.format("%02d", mic);
                            tv_mic.setText(text);

                            tv_start.setText("다시 시작");
                            start.setCardBackgroundColor(Color.parseColor("#7198AD"));
                            start.setStrokeWidth(0);
                            tv_start.setTextColor(Color.WHITE);
                        }
                    }
                } else {
                    db.collection("users").document(UserInfo.getID(getActivity())).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                User user;
                                if (task.getResult().exists()) {
                                    user = task.getResult().toObject(User.class);
                                    Dojeon dojeon = UserInfo.getDojeon(user, currentId);
                                    if (dojeon != null && dojeon.resMap.containsKey(key)) {
                                        state = STATE_SAVE;
                                        long t = dojeon.resMap.get(key).longValue();
                                        long mic = t % 60;
                                        t /= 60;
                                        long sec = t % 60;
                                        t /= 60;
                                        long min = t;

                                        String text = String.format("%02d", min);
                                        tv_min.setText(text);
                                        text = String.format("%02d", sec);
                                        tv_sec.setText(text);
                                        text = String.format("%02d", mic);
                                        tv_mic.setText(text);

                                        tv_start.setText("다시 시작");
                                        start.setCardBackgroundColor(Color.parseColor("#7198AD"));
                                        start.setStrokeWidth(0);
                                        tv_start.setTextColor(Color.WHITE);
                                    }
                                }
                            }
                        }
                    });
                }
            }

        }else{
            if (!UserInfo.getID(getActivity()).equals("")) {
                if (!UserInfo.getLogin(getActivity())) {
                    if (time == 0){
                        ArrayList<Dojeon> dojeons = UserInfo.getProceedDojeon(UserInfo.getUser(getActivity()));
                        for (Dojeon dojeon : dojeons) {
                            if (dojeon.type == Dojeon.TYPE_MEDITATE) {
                                currentId = dojeon.dojeonId;
                                fragment.setDojeonId(dojeon.dojeonId);
                                settingFragment.setDojeonId(dojeon.dojeonId);

                                if (dojeon.resMap.containsKey(key)){
                                    state = STATE_SAVE;
                                    long t = dojeon.resMap.get(key).longValue();
                                    long mic = t % 60;
                                    t /= 60;
                                    long sec = t % 60;
                                    t /= 60;
                                    long min = t;

                                    String text = String.format("%02d", min);
                                    tv_min.setText(text);
                                    text = String.format("%02d", sec);
                                    tv_sec.setText(text);
                                    text = String.format("%02d", mic);
                                    tv_mic.setText(text);

                                    tv_start.setText("다시 시작");
                                    start.setCardBackgroundColor(Color.parseColor("#7198AD"));
                                    start.setStrokeWidth(0);
                                    tv_start.setTextColor(Color.WHITE);

                                }

                                break;
                            }
                        }
                    }

                } else {
                    if (time == 0){
                        setting.setClickable(false);

                        db.collection("users").document(UserInfo.getID(getActivity())).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    setting.setClickable(true);

                                    User user;
                                    if (task.getResult().exists()) {
                                        user = task.getResult().toObject(User.class);
                                        ArrayList<Dojeon> proceed = UserInfo.getProceedDojeon(user);
                                        for (Dojeon dojeon : proceed) {
                                            if (dojeon.type == Dojeon.TYPE_MEDITATE) {
                                                currentId = dojeon.dojeonId;
                                                fragment.setDojeonId(dojeon.dojeonId);
                                                settingFragment.setDojeonId(dojeon.dojeonId);

                                                if (dojeon.resMap.containsKey(key)){
                                                    state = STATE_SAVE;
                                                    long t = dojeon.resMap.get(key).longValue();
                                                    long mic = t % 60;
                                                    t /= 60;
                                                    long sec = t % 60;
                                                    t /= 60;
                                                    long min = t;

                                                    String text = String.format("%02d", min);
                                                    tv_min.setText(text);
                                                    text = String.format("%02d", sec);
                                                    tv_sec.setText(text);
                                                    text = String.format("%02d", mic);
                                                    tv_mic.setText(text);

                                                    tv_start.setText("다시 시작");
                                                    start.setCardBackgroundColor(Color.parseColor("#7198AD"));
                                                    start.setStrokeWidth(0);
                                                    tv_start.setTextColor(Color.WHITE);
                                                }
                                                break;
                                            }
                                        }

                                    }
                                }
                            }
                        });
                    }

                }
            }
        }

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        state = STATE_STOP;
        time = 0;
        currentId = -1;

        if(appTimer != null){
            appTimer.cancel();
            appTimer = null;
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        if (state == STATE_PAUSE){
            long t = time;
            long mic = t % 60;
            t /= 60;
            long sec = t % 60;
            t /= 60;
            long min = t;


            String text = String.format("%02d", min);
            tv_min.setText(text);
            text = String.format("%02d", sec);
            tv_sec.setText(text);
            text = String.format("%02d", mic);
            tv_mic.setText(text);

            start.setCardBackgroundColor(Color.parseColor("#3C3C3C"));
            start.setStrokeWidth(0);
            tv_start.setText("다시 시작");
            tv_start.setTextColor(Color.WHITE);
        }
    }
}