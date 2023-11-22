package com.assist.dojeon.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.assist.dojeon.R;
import com.assist.dojeon.dto.User;
import com.assist.dojeon.util.OnSingleClickListener;
import com.assist.dojeon.util.UserInfo;
import com.assist.dojeon.view.LoadingView;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.checkerframework.checker.units.qual.C;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.bootpay.android.Bootpay;
import kr.co.bootpay.android.constants.BootpayBuildConfig;
import kr.co.bootpay.android.events.BootpayEventListener;
import kr.co.bootpay.android.models.BootExtra;
import kr.co.bootpay.android.models.BootItem;
import kr.co.bootpay.android.models.BootUser;
import kr.co.bootpay.android.models.Payload;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class BillingActivity extends AppCompatActivity {

    User user;
    Map<String, Object> setting;
    SlidingUpPanelLayout layout;

    String SUBS_ID_MONTH = "subscription_dojeon_1";
    String SUBS_ID_YEAR = "subscription_dojeon_12";
    String SUBS_ID_ONCE = "subscription_dojeon_once";
    String[] subscription_id = {SUBS_ID_MONTH, SUBS_ID_YEAR, SUBS_ID_ONCE};

    int BILL_STATE_WAIT = 1;
    int BILL_STATE_ERROR = 2;
    int BILL_STATE_CANCEL = 3;
    int BILL_STATE_GRACE = 4;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing);

        String uid = UserInfo.getID(this);

        CardView cd_sub = findViewById(R.id.cd_sub);

        handler = new Handler();

        LinearLayout loadview = findViewById(R.id.load);
        LottieAnimationView animationView = findViewById(R.id.animation_view);
        TextView title = findViewById(R.id.tv_title);

        TextView tv_sub_type = findViewById(R.id.sub_type);
        TextView tv_sub_price = findViewById(R.id.sub_price);
        TextView tv_sub_txt = findViewById(R.id.sub_type_txt);
        TextView tv_sub_date = findViewById(R.id.sub_date);
        ImageView iv_sub_state = findViewById(R.id.check_sub);
        TextView tv_sub_state = findViewById(R.id.sub_state);
        TextView tv_sub_cancel = findViewById(R.id.sub_cancel);

        CardView cd1 = findViewById(R.id.cd1);
        ImageView check1 = findViewById(R.id.check1);

        cd1.setTag(subscription_id[0]);
        CardView cd2 = findViewById(R.id.cd2);
        ImageView check2 = findViewById(R.id.check2);

        cd2.setTag(subscription_id[1]);
        CardView cd3 = findViewById(R.id.cd3);
        ImageView check3 = findViewById(R.id.check3);

        cd3.setTag(subscription_id[2]);


        CardView purchase = findViewById(R.id.purchase);
        layout = findViewById(R.id.sliding_layout);

        layout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        layout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        purchase.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                layout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        });

        TextView skip = findViewById(R.id.skip);
        skip.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                finish();
            }
        });

        ImageView close = findViewById(R.id.close);
        close.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        if (UserInfo.getLogin(this)) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users").document(uid).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                    if (error != null) return;
                    if (value != null && value.exists()) {
                        check1.setImageTintList(null);
                        check2.setImageTintList(null);
                        check3.setImageTintList(null);
                        cd1.setTag(subscription_id[0]);
                        cd2.setTag(subscription_id[1]);
                        cd3.setTag(subscription_id[2]);

                        user = value.toObject(User.class);
                        if (user.bill != null && user.bill.subscription_id != null){
                            cd_sub.setVisibility(View.VISIBLE);

                            if (user.bill.subscription_id.equals(SUBS_ID_ONCE)){
                                check3.setImageTintList(ColorStateList.valueOf(Color.parseColor("#1F72A0")));
                                cd3.setTag(null);

                                db.collection("subscription").document(user.bill.subscription_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                        Map<String, Object> map = documentSnapshot.getData();
                                        long price = (long) map.get("price");
                                        tv_sub_type.setText("평생 구독");
                                        tv_sub_txt.setText("일시불");
                                        tv_sub_price.setText(price+"");
                                        tv_sub_state.setText("구독 중");
                                        iv_sub_state.setImageResource(R.drawable.check);
                                        iv_sub_state.setImageTintList(ColorStateList.valueOf(Color.parseColor("#1F72A0")));
                                        tv_sub_date.setVisibility(View.INVISIBLE);
                                        tv_sub_cancel.setVisibility(View.GONE);
                                        purchase.setVisibility(View.GONE);
                                    }
                                });
                            }else {

                                if (user.bill.state != BILL_STATE_CANCEL && user.bill.id != null){

                                    if (user.bill.subscription_id.equals(SUBS_ID_MONTH)){
                                        check1.setImageTintList(ColorStateList.valueOf(Color.parseColor("#1F72A0")));
                                        cd1.setTag(null);
                                    }else{
                                        check2.setImageTintList(ColorStateList.valueOf(Color.parseColor("#1F72A0")));
                                        cd2.setTag(null);
                                    }

                                    db.collection("task").document(user.bill.id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            if (documentSnapshot.exists()) {
                                                Map<String, Object> map = documentSnapshot.getData();
                                                Timestamp date = (Timestamp) map.get("reserve_execute_at");
                                                SimpleDateFormat format = new SimpleDateFormat("yyyy년 MM월 dd일");
                                                tv_sub_date.setText("결제 예정일 : " + format.format(date.toDate()));
                                            }
                                        }
                                    });

                                    db.collection("subscription").document(user.bill.subscription_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            Map<String, Object> map = documentSnapshot.getData();
                                            long interval = (long) map.get("interval");
                                            long price = (long) map.get("price");

                                            if (interval == 1){
                                                tv_sub_type.setText("월간 구독");
                                                tv_sub_txt.setText("월");
                                            }else if (interval == 12){
                                                tv_sub_type.setText("연간 구독");
                                                tv_sub_txt.setText("년");
                                            }
                                            tv_sub_price.setText(price+"");
                                        }
                                    });
                                    if (user.bill.state == BILL_STATE_WAIT){
                                        tv_sub_state.setText("구독 중");
                                        iv_sub_state.setImageResource(R.drawable.check);
                                        iv_sub_state.setImageTintList(ColorStateList.valueOf(Color.parseColor("#1F72A0")));

                                    }else {
                                        tv_sub_state.setText("결제 유예");
                                        iv_sub_state.setImageResource(R.drawable.warning);
                                        iv_sub_state.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F9A825")));
                                    }

                                    tv_sub_cancel.setOnClickListener(new OnSingleClickListener() {
                                        @Override
                                        public void onSingleClick(View v) {
                                            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                            final View mdialog = inflater.inflate(R.layout.purchase_cancel_dialog, null);
                                            AlertDialog.Builder buider = new AlertDialog.Builder(BillingActivity.this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
                                            buider.setView(mdialog);
                                            buider.setCancelable(false);
                                            Dialog dialog = buider.create();
                                            MaterialCardView cancel = mdialog.findViewById(R.id.cancel);
                                            MaterialCardView ok = mdialog.findViewById(R.id.ok);

                                            ok.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    db.collection("task").document(user.bill.id).delete();
                                                    db.collection("users").document(uid).update("bill.id", null,
                                                            "bill.subscription_id", null,
                                                            "bill.state", BILL_STATE_CANCEL).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {
                                                            Toast.makeText(BillingActivity.this, "구독이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                    dialog.dismiss();
                                                }
                                            });
                                            cancel.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    dialog.dismiss();

                                                }
                                            });

                                            dialog.show();
                                            Window window = dialog.getWindow();
                                            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                                            window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
                                        }
                                    });
                                    loadview.setVisibility(View.GONE);
                                    animationView.cancelAnimation();

                                    cd_sub.setVisibility(View.VISIBLE);
                                    title.setVisibility(View.VISIBLE);
                                }else{
                                    loadview.setVisibility(View.VISIBLE);
                                    animationView.playAnimation();

                                    cd_sub.setVisibility(View.GONE);
                                    title.setVisibility(View.GONE);
                                }
                            }
                        }else{
                            loadview.setVisibility(View.VISIBLE);
                            animationView.playAnimation();

                            cd_sub.setVisibility(View.GONE);
                            title.setVisibility(View.GONE);
                        }

                    }
                }
            });
            db.collection("subscription").document("subscription_setting").get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    setting = documentSnapshot.getData();
                }
            });
        }else{
            Intent intent = new Intent(BillingActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }


        cd1.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (v.getTag() == null){
                    Toast.makeText(BillingActivity.this, "이미 구독한 상품입니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                purchase((String) v.getTag());
            }
        });
        cd2.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (v.getTag() == null){
                    Toast.makeText(BillingActivity.this, "이미 구독한 상품입니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                purchase((String) v.getTag());
            }
        });
        cd3.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (v.getTag() == null){
                    Toast.makeText(BillingActivity.this, "이미 구독한 상품입니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String id = String.valueOf(v.getTag());
                Log.d("wwwid", id);

                LoadingView loadingView = new LoadingView(BillingActivity.this);
                loadingView.show("결제 중...");

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("subscription").document(id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            payment_once(task.getResult().getData(), loadingView);
                        }else{
                            loadingView.stop();
                            Toast.makeText(BillingActivity.this, "결제 모듈 불러오기에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

    }

    public void purchase(String id){
        if (user.bill != null && user.bill.billing_key != null){

            BillUser billUser = new BillUser();
            billUser.email = BillingActivity.this.user.email;
            billUser.username = BillingActivity.this.user.name;

            Bill bill = new Bill(null, BillingActivity.this.user.uid,
                    null, billUser);
            bill.setSubscription_id(id);

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View mdialog = inflater.inflate(R.layout.purchase_change_dialog, null);
            AlertDialog.Builder buider = new AlertDialog.Builder(BillingActivity.this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
            buider.setView(mdialog);
            buider.setCancelable(false);
            Dialog dialog = buider.create();
            TextView tv_sub = mdialog.findViewById(R.id.textView6);

            MaterialCardView cancel = mdialog.findViewById(R.id.cancel);
            MaterialCardView ok = mdialog.findViewById(R.id.ok);

            if (user.bill.id != null){
                tv_sub.setText("해당 구독 상품으로 교체하시겠습니까?");
            }

            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                        LoadingView loadingView = new LoadingView(BillingActivity.this);
                        loadingView.show("결제 중...");

                        if (user.bill.id != null){
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            db.collection("task").document(user.bill.id).delete();
                            db.collection("users").document(user.uid).update("bill.id", null,
                                    "bill.subscription_id", null).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    try {
                                        re_purchase(bill).enqueue(new Callback<String>() {
                                            @Override
                                            public void onResponse(Call<String> call, Response<String> response) {
                                                if (response.code() == 200){
                                                    layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                                                    Toast.makeText(BillingActivity.this, "결제 성공!", Toast.LENGTH_SHORT).show();
                                                }else{
                                                    Toast.makeText(BillingActivity.this, "결제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                                }
                                                loadingView.stop();
                                            }

                                            @Override
                                            public void onFailure(Call<String> call, Throwable t) {
                                                loadingView.stop();
                                                Toast.makeText(BillingActivity.this, "결제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                            }

                                        });
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            });
                        }else{
                            try {
                                re_purchase(bill).enqueue(new Callback<String>() {
                                    @Override
                                    public void onResponse(Call<String> call, Response<String> response) {
                                        if (response.code() == 200){
                                            layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                                            Toast.makeText(BillingActivity.this, "결제 성공!", Toast.LENGTH_SHORT).show();
                                        }else{
                                            Toast.makeText(BillingActivity.this, "결제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                        }
                                        loadingView.stop();
                                    }

                                    @Override
                                    public void onFailure(Call<String> call, Throwable t) {
                                        Log.d("error", t.toString());
                                        loadingView.stop();
                                        Toast.makeText(BillingActivity.this, "결제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                    }

                                });
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }


                    dialog.dismiss();

                }
            });
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();

                }
            });

            dialog.show();
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

        }else{
            LoadingView loadingView = new LoadingView(BillingActivity.this);
            loadingView.show("결제 중...");

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("subscription").document(id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        if (id.equals(SUBS_ID_ONCE)){
                            payment_once(task.getResult().getData(), loadingView);
                        }else{
                            payment(task.getResult().getData(), loadingView);
                        }
                    }else{
                        Toast.makeText(BillingActivity.this, "결제 모듈 불러오기에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    class Bill{
        @Expose
        @SerializedName("receipt_id")
        String receipt_id;

        @Expose
        @SerializedName("uid")
        String uid;

        @Expose
        @SerializedName("purchased_at")
        String purchased_at;

        @Expose
        @SerializedName("subscription_id")
        String subscription_id;

        @Expose
        @SerializedName("user")
        BillUser user;


        public Bill(String receipt_id,  String uid, String purchased_at, BillUser user){
            this.receipt_id = receipt_id;
            this.uid = uid;
            this.purchased_at = purchased_at;
            this.user = user;
        }

        public void setSubscription_id(String subscription_id) {
            this.subscription_id = subscription_id;
        }
    }

    class BillUser{
        @Expose
        @SerializedName("username")
        String username;

        @Expose
        @SerializedName("email")
        String email;
    }

    public interface RetrofitService {
        // @GET( EndPoint-자원위치(URI) )
        @POST(".") //HTTP 메서드 및 URL
        @Headers("Content-Type: application/json")
        //Requests 타입의 DTO 데이터와 API 키를 요청
        Call<String> getPosts(@Body Bill request);
    }

    private Call<String> get_billing_key(Bill bill) throws JSONException {
        // Create the arguments to the callable function.
        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://get-billing-key-szdhekdb6q-du.a.run.app/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        RetrofitService retrofitService = retrofit.create(RetrofitService.class);
        return retrofitService.getPosts(bill);
    }

    private Call<String> re_purchase(Bill bill) throws JSONException {
        // Create the arguments to the callable function.
        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://re-purchase-szdhekdb6q-du.a.run.app/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        RetrofitService retrofitService = retrofit.create(RetrofitService.class);
        return retrofitService.getPosts(bill);
    }


    public void payment_once(Map<String, Object> subscription, LoadingView loadingView){
        BootUser user = new BootUser();
        user.setEmail( BillingActivity.this.user.email);
        user.setUsername( BillingActivity.this.user.name);


        BootExtra extra = new BootExtra()
                .setCardQuota((String) subscription.get("cardQuota")); // 일시불, 2개월, 3개월 할부 허용, 할부는 최대 12개월까지 사용됨 (5만원 이상 구매시 할부허용 범위)


        Payload payload = new Payload();
        payload.setApplicationId((String) setting.get("applicationId"))
                .setOrderId((String) subscription.get("order_id"))
                .setOrderName((String) subscription.get("order_name"))
                .setPrice((double) ((long) subscription.get("price")))
                .setUser(user)
                .setExtra(extra);


        Bootpay.init(getSupportFragmentManager(), getApplicationContext())
                .setPayload(payload)
                .setEventListener(new BootpayEventListener() {
                    @Override
                    public void onCancel(String data) {
                        loadingView.stop();
                        Toast.makeText(BillingActivity.this, "결제를 취소했습니다.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String data) {
                        loadingView.stop();
                        Toast.makeText(BillingActivity.this, "결제 에러", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onClose() {
                        loadingView.stop();
                        Bootpay.removePaymentWindow();
                    }

                    @Override
                    public void onIssued(String data) {
                        loadingView.stop();
                        Toast.makeText(BillingActivity.this, "결제 에러", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public boolean onConfirm(String data) {
                        loadingView.stop();
                        return true;
                    }

                    @Override
                    public void onDone(String data) {
                        loadingView.stop();
                         FirebaseFirestore db = FirebaseFirestore.getInstance();
                         db.collection("users").document(UserInfo.getID(BillingActivity.this)).update("bill.subscription_id", subscription.get("subscription_id")).addOnSuccessListener(new OnSuccessListener<Void>() {
                             @Override
                             public void onSuccess(Void unused) {
                                 layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                                 Toast.makeText(BillingActivity.this, "결제 완료!", Toast.LENGTH_SHORT).show();
                             }
                         });
                    }
                }).requestPayment();
    }


    public void payment(Map<String, Object> subscription, LoadingView loadingView) {
        BootUser user = new BootUser();
        user.setEmail( BillingActivity.this.user.email);
        user.setUsername( BillingActivity.this.user.name);


        BootExtra extra = new BootExtra()
                .setCardQuota((String) subscription.get("cardQuota")); // 일시불, 2개월, 3개월 할부 허용, 할부는 최대 12개월까지 사용됨 (5만원 이상 구매시 할부허용 범위)

        List items = new ArrayList<>();
        BootItem item1 = new BootItem().setName((String) subscription.get("order_name")).setId((String) subscription.get("order_id"))
                .setQty(1).setPrice((double) ((long) subscription.get("price")));
        items.add(item1);

        Payload payload = new Payload();
        payload.setApplicationId((String) setting.get("applicationId"))
                .setOrderName((String) subscription.get("order_name"))
                .setPg((String) setting.get("pg"))
                .setMethod((String) setting.get("method"))
                .setSubscriptionId((String) subscription.get("subscription_id"))
                .setPrice((double) ((long) subscription.get("price")))
                .setUser(user)
                .setExtra(extra)
                .setItems(items);


        Bootpay.init(getSupportFragmentManager(), getApplicationContext())
                .setPayload(payload)
                .setEventListener(new BootpayEventListener() {
                    @Override
                    public void onCancel(String data) {
                        loadingView.stop();
                        Toast.makeText(BillingActivity.this, "결제를 취소했습니다.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String data) {
                        loadingView.stop();
                        Toast.makeText(BillingActivity.this, "자동 결제 등록 에러", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onClose() {
                        loadingView.stop();
                        Bootpay.removePaymentWindow();
                    }

                    @Override
                    public void onIssued(String data) {
                        loadingView.stop();
                        Toast.makeText(BillingActivity.this, "자동 결제 등록 에러", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public boolean onConfirm(String data) {
                        loadingView.stop();
                        return true;
                    }

                    @Override
                    public void onDone(String data) {
                            loadingView.stop();
                            layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    LoadingView loadingView = new LoadingView(BillingActivity.this);
                                    loadingView.show("구독 등록 중...");
                                    try {
                                    JSONObject jsonObject = new JSONObject(data);
                                    JSONObject d = jsonObject.getJSONObject("data");
                                    JSONObject receipt = d.getJSONObject("receipt_data");

                                    String receipt_id = d.getString("receipt_id");
                                    String purchased_at = receipt.getString("purchased_at");

                                    System.out.println(receipt_id);

                                    BillUser billUser = new BillUser();
                                    billUser.email = BillingActivity.this.user.email;
                                    billUser.username = BillingActivity.this.user.name;

                                    Bill bill = new Bill(receipt_id, BillingActivity.this.user.uid,
                                            purchased_at, billUser);

                                    get_billing_key(bill).enqueue(new Callback<String>() {
                                            @Override
                                            public void onResponse(Call<String> call, Response<String> response) {
                                                loadingView.stop();
                                                if (response.code() == 200){
                                                    layout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                                                    Toast.makeText(BillingActivity.this, "결제 완료!", Toast.LENGTH_SHORT).show();
                                                }else{
                                                    Toast.makeText(BillingActivity.this, "구독 등록 실패: 관리자에게 문의 부탁드립니다.", Toast.LENGTH_SHORT).show();
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<String> call, Throwable t) {
                                                Log.d("error", t.toString());
                                                loadingView.stop();
                                                Toast.makeText(BillingActivity.this, "구독 등록 실패: 관리자에게 문의 부탁드립니다.", Toast.LENGTH_SHORT).show();
                                            }
                                    });
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            });


                    }
                }).requestSubscription();
    }



}