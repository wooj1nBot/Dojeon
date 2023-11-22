package com.assist.dojeon.receiver;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.assist.dojeon.R;
import com.assist.dojeon.activity.MainActivity;
import com.assist.dojeon.dto.Dojeon;
import com.assist.dojeon.util.UserInfo;

import java.util.Calendar;

public class NotificationReceiver extends BroadcastReceiver {

    NotificationManager manager;
    NotificationCompat.Builder builder;

    //오레오 이상은 반드시 채널을 설정해줘야 Notification이 작동함
    private static String CHANNEL_ID = "channel_dojeon";
    private static String CHANNEL_NAME = "Dojeon Notification";


    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        if (intent.getAction() != null) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                UserInfo.getNotice(context);
                return;
            }
            if(action.equals(Intent.ACTION_PACKAGE_REPLACED)){
                // Broadcast Action: A new version of an application package has been installed, replacing an existing version that was previously installed.
                // 새로운 버전의 앱 패키지가 설치 되거나 업데이트 되었을 때

                if (intent.getDataString().contains(context.getPackageName())){
                    // do something...
                    UserInfo.getNotice(context);
                    return;
                }
            }
        }

        Dojeon dojeon = (Dojeon) intent.getSerializableExtra("dojeon");

        if (System.currentTimeMillis() > dojeon.end.getTime()){
            UserInfo.removeNotice(context, dojeon.alarmId);
            return;
        }

        builder = null;

        //푸시 알림을 보내기위해 시스템에 권한을 요청하여 생성
        manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        //안드로이드 오레오 버전 대응

        builder = new NotificationCompat.Builder(context, CHANNEL_ID);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            manager.createNotificationChannel(
                    new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            );

        }

        //알림창 클릭 시 지정된 activity 화면으로 이동
        Intent intent2 = new Intent(context, MainActivity.class);
        intent2.putExtra("dojeon", dojeon);
        intent2.setAction(Intent.ACTION_MAIN);
        intent2.addCategory(Intent.CATEGORY_LAUNCHER);
        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // FLAG_UPDATE_CURRENT ->
        // 설명된 PendingIntent가 이미 존재하는 경우 유지하되, 추가 데이터를 이 새 Intent에 있는 것으로 대체함을 나타내는 플래그입니다.
        // getActivity, getBroadcast 및 getService와 함께 사용
        PendingIntent pendingIntent = PendingIntent.getActivity(context, dojeon.alarmId, intent2,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);


        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);


        Calendar start = Calendar.getInstance();
        start.setTime(dojeon.getStart());
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);

        long  diffSec = (c.getTimeInMillis() - start.getTimeInMillis()) / 1000; //초 차이
        long  diffDays = diffSec / (24*60*60); //일자수 차이

        int dif = (int) (diffDays + 1);

        String title = Dojeon.TYPES[dojeon.type] + String.format(" 도전 %d일 차 입니다.", dif);
        String value = "오늘도 힘차게 도전해주세요!";

        //알림창 제목
        builder.setContentTitle(title);
        builder.setContentText(value);
        //알림창 아이콘
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        //알림창 터치시 자동 삭제
        builder.setAutoCancel(true);

        builder.setContentIntent(pendingIntent);

        //푸시알림 빌드
        Notification notification = builder.build();

        //NotificationManager를 이용하여 푸시 알림 보내기
        manager.notify(911, notification);
    }
}