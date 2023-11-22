package com.assist.dojeon.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.assist.dojeon.activity.MainActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.assist.dojeon.R;
import com.assist.dojeon.dto.Dojeon;
import com.assist.dojeon.dto.User;
import com.assist.dojeon.util.UserInfo;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;


public class SettingFragment extends Fragment {

    int type;
    Dojeon dojeon;
    User user;

    public int[] mTimes;
    public int mRange;

    public boolean isAlarm;
    int dojeonId;
    private static final int MY_PERMISSION_STORAGE = 1111;
    TimePicker picker;
    ImageView toggle_alarm;

    CardView alarm;
    CardView range;
    NumberPicker rangePicker;
    TextView tv_done;
    TextView tv_range;
    SlidingUpPanelLayout layout;

    public SettingFragment(int type) {
        // Required empty public constructor
        this.type = type;
        mTimes = new int[]{7,0};
        mRange = 30;
        isAlarm = true;
        this.dojeonId = -1;
    }


    public void setDojeonId(int dojeonId) {
        this.dojeonId = dojeonId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        alarm = view.findViewById(R.id.alarm);
        range = view.findViewById(R.id.range);
        picker = view.findViewById(R.id.timepicker);
        rangePicker = view.findViewById(R.id.picker);
        tv_done = view.findViewById(R.id.done);
        tv_range = view.findViewById(R.id.tv_range);
        ImageView back = view.findViewById(R.id.back);
        toggle_alarm = view.findViewById(R.id.toggle_alarm);
        layout = view.findViewById(R.id.sliding_layout);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setBack(getActivity());
            }
        });

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            alarm.setTag(false);
            toggle_alarm.setImageResource(R.drawable.toggle_off);
            picker.setVisibility(View.GONE);
        }else{
            if (!isAlarm){
                alarm.setTag(false);
                toggle_alarm.setImageResource(R.drawable.toggle_off);
                picker.setVisibility(View.GONE);
            }else{
                alarm.setTag(true);
                toggle_alarm.setImageResource(R.drawable.toggle_on);
                picker.setVisibility(View.VISIBLE);
            }
        }





        String[] values = new String[29];
        for (int i = 2; i <= 30; i++) {
            values[i-2] = String.valueOf(i);
        }

        tv_range.setText(mRange + "일");

        rangePicker.

        rangePicker.setMinValue(2);
        rangePicker.setMaxValue(30);
        rangePicker.setWrapSelectorWheel(true);
        rangePicker.setValue(mRange);


        layout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        layout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean b = (boolean) view.getTag();
                isAlarm = !b;

                if (checkPermission()){
                    if (b){
                        toggle_alarm.setImageResource(R.drawable.toggle_off);
                        picker.setVisibility(View.GONE);
                        if (dojeon != null){
                            dojeon.alarmId = 0;
                            saveDojeon(dojeon);
                        }
                    }else{
                        toggle_alarm.setImageResource(R.drawable.toggle_on);
                        picker.setVisibility(View.VISIBLE);
                        if (dojeon != null){
                            Random random = new Random();
                            dojeon.alarmId = random.nextInt(10000000);
                            saveDojeon(dojeon);
                        }
                    }
                    view.setTag(!b);
                }

            }
        });

        range.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        });

        tv_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                tv_range.setText(rangePicker.getValue() + "일");
                mRange = rangePicker.getValue();

                if (dojeon != null){
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(dojeon.start);
                    calendar.add(Calendar.DAY_OF_MONTH, rangePicker.getValue()-1);
                    dojeon.end = calendar.getTime();
                    dojeon.range = mRange;
                    saveDojeon(dojeon);
                }
            }
        });

        picker.setHour(mTimes[0]);
        picker.setMinute(mTimes[1]);
        picker.setDescendantFocusability(TimePicker.FOCUS_BLOCK_DESCENDANTS);

        if (dojeonId != -1) {
            getData(dojeonId);
        }else {
            picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                @Override
                public void onTimeChanged(TimePicker view, int i, int i1) {
                    mTimes = new int[]{i, i1};
                    if (dojeon != null){
                        dojeon.setAlarmTime(i, i1);
                        saveDojeon(dojeon);
                    }
                }
            });
        }

        return view;
    }

    public void getData(int dojeonId){
        if (UserInfo.getLogin(getActivity())){
            alarm.setClickable(false);
            range.setClickable(false);
            picker.setEnabled(false);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(UserInfo.getID(getActivity())).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    alarm.setClickable(true);
                    range.setClickable(true);
                    picker.setEnabled(true);

                    if (task.isSuccessful() && task.getResult().exists()) {
                        user = task.getResult().toObject(User.class);

                        Dojeon doj = UserInfo.getDojeon(user, dojeonId);
                        if (doj != null){
                            SettingFragment.this.dojeon = doj;
                            picker.setHour(dojeon.alarmHour);
                            picker.setMinute(dojeon.alarmMinute);


                            if (dojeon.alarmId == 0){
                                alarm.setTag(false);
                                toggle_alarm.setImageResource(R.drawable.toggle_off);
                                picker.setVisibility(View.GONE);
                            }else{
                                alarm.setTag(true);
                                toggle_alarm.setImageResource(R.drawable.toggle_on);
                                picker.setVisibility(View.VISIBLE);
                            }
                            rangePicker.setValue(dojeon.range);
                            tv_range.setText(dojeon.range + "일");
                        }
                    }

                    picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                        @Override
                        public void onTimeChanged(TimePicker view, int i, int i1) {
                            mTimes = new int[]{i, i1};
                            if (dojeon != null){
                                dojeon.setAlarmTime(i, i1);
                                saveDojeon(dojeon);
                            }
                        }
                    });
                }
            });

        }else{

            if (!UserInfo.getID(getActivity()).equals("")) {
                user = UserInfo.getUser(getActivity());
                Dojeon doj = UserInfo.getDojeon(user, dojeonId);
                if (doj != null) {
                    if (doj.type == type) {
                        SettingFragment.this.dojeon = doj;

                        if (dojeon.alarmId == 0) {
                            alarm.setTag(false);
                            isAlarm = false;
                            toggle_alarm.setImageResource(R.drawable.toggle_off);
                            picker.setVisibility(View.GONE);
                        } else {
                            alarm.setTag(true);
                            isAlarm = true;
                            toggle_alarm.setImageResource(R.drawable.toggle_on);
                            picker.setVisibility(View.VISIBLE);

                            picker.setHour(dojeon.alarmHour);
                            picker.setMinute(dojeon.alarmMinute);
                        }
                        rangePicker.setValue(dojeon.range);
                        tv_range.setText(dojeon.range + "일");
                    }
                }
            }
            picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                @Override
                public void onTimeChanged(TimePicker view, int i, int i1) {
                    mTimes = new int[]{i, i1};
                    if (dojeon != null){
                        dojeon.setAlarmTime(i, i1);
                        saveDojeon(dojeon);
                    }
                }
            });

        }


    }

    private boolean checkPermission(){
        boolean isCheck = true;
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            isCheck = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, MY_PERMISSION_STORAGE);
            }
            Toast toast= Toast.makeText(getContext(), "기능 사용을 위한 알람 권한 동의가 필요합니다.", Toast.LENGTH_SHORT); toast.show();
            isAlarm = false;
        }
        return isCheck;
    }


    public static void setBack(FragmentActivity activity){
        FragmentManager manager = activity.getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        if (manager.getBackStackEntryCount() > 1) {
            Fragment fragment = manager.findFragmentById(R.id.parent);
            if (fragment != null) {
                transaction.remove(fragment).commit();
                manager.popBackStack();
            }
        }
    }

    public void saveDojeon(Dojeon dojeon){
        for (Dojeon doj : user.getDojeons()) {
            if (doj.type == dojeon.type && doj.start.getTime() == dojeon.start.getTime() && doj.end.getTime() == dojeon.end.getTime()){
                int alarm = doj.alarmId;
                doj.alarmId = dojeon.alarmId;
                doj.setAlarmTime(dojeon.alarmHour, dojeon.alarmMinute);
                doj.range = dojeon.range;
                doj.end = dojeon.end;
                if (UserInfo.getLogin(getContext())){
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("users").document(user.uid).set(user);
                }else{
                    UserInfo.setUser(getContext(), user);
                }
                UserInfo.removeNotice(getContext(), alarm);
                if (dojeon.alarmId != 0) {
                    UserInfo.setNotice(getContext(), doj);
                }
                break;
            }
        }
    }
}