package com.assist.dojeon.fragment;

import static android.app.Activity.RESULT_OK;

import static com.assist.dojeon.fragment.SettingFragment.setBack;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.assist.dojeon.util.OnSingleClickListener;
import com.assist.dojeon.view.LoadingView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.assist.dojeon.R;
import com.assist.dojeon.dto.Dojeon;
import com.assist.dojeon.dto.Feature;
import com.assist.dojeon.dto.Image;
import com.assist.dojeon.dto.Request;
import com.assist.dojeon.dto.Requests;
import com.assist.dojeon.dto.Responses;
import com.assist.dojeon.dto.TextAnnotation;
import com.assist.dojeon.dto.User;
import com.assist.dojeon.util.UserInfo;
import com.jaygoo.widget.OnRangeChangedListener;
import com.jaygoo.widget.RangeSeekBar;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.FileCallback;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Engine;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;


public class WeightFragment extends Fragment {
    private static final int MY_PERMISSION_CAMERA = 1211;


    public static final String WEIGHT_PATH = "JPEG_" + "WEIGHT";

    ProgressBar progressBar;
    ImageView imageView, shutter;
    ImageView iv_zoom;
    TextView retry, save, value;
    LinearLayout layout;
    RangeSeekBar zoom;

    Uri resultUri;

    private FirebaseFirestore db;

    HistoryFragment fragment;

    int currentId;

    SettingFragment settingFragment;

    private CameraView camera;
    ImageView setting;
    CardView history;


    public WeightFragment() {
        settingFragment = new SettingFragment(Dojeon.TYPE_WEIGHT);
        fragment = new HistoryFragment(Dojeon.TYPE_WEIGHT);
        this.currentId = -1;
    }

    public WeightFragment(int currentId) {
        this.currentId = currentId;
        settingFragment = new SettingFragment(Dojeon.TYPE_WEIGHT);
        fragment = new HistoryFragment(Dojeon.TYPE_WEIGHT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view =  inflater.inflate(R.layout.fragment_weight, container, false);
        db = FirebaseFirestore.getInstance();
        camera = view.findViewById(R.id.camera);
        shutter = view.findViewById(R.id.shutter);
        progressBar = view.findViewById(R.id.progressBar);
        imageView = view.findViewById(R.id.image);
        retry = view.findViewById(R.id.retry);
        save = view.findViewById(R.id.save);
        value = view.findViewById(R.id.tv_value);
        layout = view.findViewById(R.id.linear);
        TextView home = view.findViewById(R.id.home);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBack(getActivity());
            }
        });
        history = view.findViewById(R.id.history);
        FragmentManager manager = getActivity().getSupportFragmentManager();
        checkPermission();

        history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.parent, fragment);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });
        history.setClickable(false);




        zoom = view.findViewById(R.id.zoom);
        zoom.setIndicatorTextDecimalFormat("0.0 x");
        zoom.setRange(1, 10);
        zoom.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                 camera.setZoom(leftValue / 10f);
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }
        });

        iv_zoom = view.findViewById(R.id.iv_zoom);
        iv_zoom.setTag(false);

        iv_zoom.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (!(boolean) v.getTag()){
                    zoom.setVisibility(View.VISIBLE);
                    iv_zoom.setImageTintList(ColorStateList.valueOf(Color.parseColor("#3C3C3C")));
                }else {
                    zoom.setVisibility(View.GONE);
                    iv_zoom.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                }
                v.setTag(!(boolean) v.getTag());
            }
        });




        shutter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.takePicture();
            }
        });

        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera();
                iv_zoom.setVisibility(View.VISIBLE);
                zoom.setVisibility(View.VISIBLE);
            }
        });



        setting = view.findViewById(R.id.setting);
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.parent, settingFragment);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });

        setting.setClickable(false);


        return view;
    }



    @Override
    public void onStart() {
        super.onStart();

        openCamera();

        if (currentId != -1){
            fragment.setDojeonId(currentId);
            settingFragment.setDojeonId(currentId);
            setting.setClickable(true);
            history.setClickable(true);

            Calendar c = Calendar.getInstance();
            String key = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH);
            String imageFileName = WEIGHT_PATH + "_" + key;

            File file = new File(getActivity().getFilesDir(), imageFileName + ".jpg");

            if (!UserInfo.getID(getActivity()).equals("")) {
                if (!UserInfo.getLogin(getActivity())) {
                    Dojeon dojeon = UserInfo.getDojeon(UserInfo.getUser(getContext()), currentId);
                    if (dojeon != null){
                        if (dojeon.resMap.containsKey(key) && file.exists()) {
                            closeCamera();
                            iv_zoom.setVisibility(View.GONE);
                            zoom.setVisibility(View.GONE);
                            Glide.with(getActivity()).load(file).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(imageView);
                            value.setText(String.format("%.1f kg", dojeon.resMap.get(key)));
                            value.setVisibility(View.VISIBLE);
                            shutter.setVisibility(View.GONE);
                            layout.setVisibility(View.VISIBLE);
                        }
                    }
                }else {

                    db.collection("users").document(UserInfo.getID(getActivity())).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                            if (task.isSuccessful()) {
                                User user;
                                if (task.getResult().exists()) {
                                    user = task.getResult().toObject(User.class);
                                    Dojeon dojeon = UserInfo.getDojeon(user, currentId);
                                    if (dojeon == null) return;

                                    Uri uri = dojeon.getImage(c);
                                    if (uri != null) {
                                        closeCamera();
                                        iv_zoom.setVisibility(View.GONE);
                                        zoom.setVisibility(View.GONE);
                                        progressBar.setVisibility(View.VISIBLE);
                                        Glide.with(getActivity()).load(uri).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).listener(new RequestListener<Drawable>() {
                                            @Override
                                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                                progressBar.setVisibility(View.GONE);
                                                return false;
                                            }

                                            @Override
                                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                progressBar.setVisibility(View.GONE);
                                                return false;
                                            }
                                        }).into(imageView);

                                        shutter.setVisibility(View.GONE);
                                        layout.setVisibility(View.VISIBLE);

                                        value.setText(String.format("%.1f kg", dojeon.resMap.get(key)));
                                        value.setVisibility(View.VISIBLE);
                                    }
                                }
                            }
                        }
                    });
                }
            }
        }else{

            Calendar c = Calendar.getInstance();
            String key = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH);

            if (!UserInfo.getID(getActivity()).equals("")) {
                if (!UserInfo.getLogin(getActivity())) {
                    String imageFileName = WEIGHT_PATH + "_" + key;

                    ArrayList<Dojeon> dojeons = UserInfo.getProceedDojeon(UserInfo.getUser(getActivity()));
                    for (Dojeon dojeon : dojeons) {
                        if (dojeon.type == Dojeon.TYPE_WEIGHT) {
                            currentId = dojeon.dojeonId;
                            fragment.setDojeonId(dojeon.dojeonId);
                            settingFragment.setDojeonId(dojeon.dojeonId);

                            if (dojeon.resMap.containsKey(key)){
                                File file = new File(getActivity().getFilesDir(), imageFileName + ".jpg");
                                if (dojeon.resMap.containsKey(key) && file.exists()) {
                                    closeCamera();
                                    iv_zoom.setVisibility(View.GONE);
                                    zoom.setVisibility(View.GONE);
                                    Glide.with(getActivity()).load(file).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(imageView);
                                    value.setText(String.format("%.1f kg", dojeon.resMap.get(key)));
                                    value.setVisibility(View.VISIBLE);
                                    shutter.setVisibility(View.GONE);
                                    layout.setVisibility(View.VISIBLE);
                                }
                            }
                            break;
                        }
                    }

                    setting.setClickable(true);
                    history.setClickable(true);
                } else {

                    db.collection("users").document(UserInfo.getID(getActivity())).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                            if (task.isSuccessful()) {
                                User user;
                                if (task.getResult().exists()) {
                                    user = task.getResult().toObject(User.class);
                                    ArrayList<Dojeon> proceed = UserInfo.getProceedDojeon(user);
                                    Dojeon current = null;
                                    for (Dojeon dojeon : proceed) {
                                        if (dojeon.type == Dojeon.TYPE_WEIGHT) {
                                            currentId = dojeon.dojeonId;
                                            current = dojeon;
                                            fragment.setDojeonId(dojeon.dojeonId);
                                            settingFragment.setDojeonId(dojeon.dojeonId);
                                            break;
                                        }
                                    }
                                    if (current != null){

                                        Uri uri = current.getImage(c);

                                        if (uri != null) {
                                            closeCamera();
                                            iv_zoom.setVisibility(View.GONE);
                                            zoom.setVisibility(View.GONE);
                                            progressBar.setVisibility(View.VISIBLE);
                                            Glide.with(getActivity()).load(uri).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).listener(new RequestListener<Drawable>() {
                                                @Override
                                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                                    progressBar.setVisibility(View.GONE);
                                                    return false;
                                                }

                                                @Override
                                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                    progressBar.setVisibility(View.GONE);
                                                    return false;
                                                }
                                            }).into(imageView);

                                            shutter.setVisibility(View.GONE);
                                            layout.setVisibility(View.VISIBLE);

                                            value.setText(String.format("%.1f kg", current.resMap.get(key)));
                                            value.setVisibility(View.VISIBLE);
                                        }
                                    }

                                }
                            }
                            setting.setClickable(true);
                            history.setClickable(true);
                        }
                    });
                }
            }
        }
    }



    public void openCamera(){
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (camera == null) return;

            camera.setEngine(Engine.CAMERA2);
            camera.setUseDeviceOrientation(false);
            camera.addCameraListener(new CameraListener() {

                @Override
                public void onZoomChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
                    super.onZoomChanged(newValue, bounds, fingers);
                }

                @Override
                public void onPictureTaken(@NonNull PictureResult result) {
                    super.onPictureTaken(result);

                    result.toBitmap(new BitmapCallback() {
                        @Override
                        public void onBitmapReady(@Nullable Bitmap bitmap) {
                            if (bitmap == null) {
                                Toast.makeText(getContext(), "사진 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (resultUri != null) {
                                new File(resultUri.toString()).delete();
                            }

                            closeCamera();
                            zoom.setVisibility(View.GONE);
                            iv_zoom.setVisibility(View.GONE);
                            Glide.with(getActivity()).load(bitmap).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).
                                    into(imageView);

                            String base = encode(bitmap);
                            getVision(base, bitmap);
                        }
                    });


                }
            });


            imageView.setImageResource(R.drawable.scale);
            imageView.setVisibility(View.GONE);

            layout.setVisibility(View.GONE);
            value.setVisibility(View.GONE);
            shutter.setVisibility(View.VISIBLE);

            camera.setVisibility(View.VISIBLE);
            camera.open();
        }
    }
    public void closeCamera(){
        if (camera == null) return;

        imageView.setVisibility(View.VISIBLE);
        camera.setVisibility(View.GONE);
        camera.close();
    }

    private static File createImageFile(Context context) throws IOException {
        // Create an image file name

        Calendar c = Calendar.getInstance();
        String key = c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH) + 1)+"-"+c.get(Calendar.DAY_OF_MONTH);
        String imageFileName = WEIGHT_PATH + "_" + key;

        return new File(context.getFilesDir(), imageFileName + ".jpg");
    }
    public static String encode(Bitmap bm){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG,100,baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }
    public interface RetrofitService {
        // @GET( EndPoint-자원위치(URI) )
        @POST("v1/images:annotate") //HTTP 메서드 및 URL
        @Headers("Content-Type: application/json; charset=utf-8")
        //Requests 타입의 DTO 데이터와 API 키를 요청
        Call<Responses> getPosts(@Body Requests request, @Query("key") String key);
    }

    public void getVision(String base, Bitmap bitmap){

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://vision.googleapis.com")
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        //JSON 요청 본문 참조 바람. (https://cloud.google.com/vision/docs/ocr?hl=ko#detect_text_in_a_local_image)
        Image image = new Image(base);
        Feature feature = new Feature("TEXT_DETECTION");
        List<Feature> features = new ArrayList<>();
        features.add(feature);
        Request request = new Request(image, features);
        List<Request> requestList = new ArrayList<>();
        requestList.add(request);
        Requests requests = new Requests(requestList);

        RetrofitService retrofitService = retrofit.create(RetrofitService.class);
        Call<Responses> call = retrofitService.getPosts(requests, getString(R.string.API_KEY));

        progressBar.setVisibility(View.VISIBLE);
        call.enqueue(new Callback<Responses>() {
            @Override
            public void onResponse(Call<Responses> call, Response<Responses> response) {
                progressBar.setVisibility(View.GONE);
                if (response.body() != null) {
                    //TextAnnotation 타입의 리스트를 리턴값으로 반환
                    List<TextAnnotation> textAnnotations = response.body().responses.get(0).textAnnotations;
                    if (textAnnotations != null && textAnnotations.size() > 0) {
                        TextAnnotation max = null;
                        for (int i = 0; i < textAnnotations.size(); i++) {
                            TextAnnotation t = textAnnotations.get(i);
                            if (isNumeric(t.description)) {
                                if (max == null) {
                                    max = t;
                                } else {
                                    if (getArea(t) > getArea(max)) {
                                        max = t;
                                    }
                                }
                            }
                        }
                        if (max != null) {
                            success(max.description, bitmap);
                        } else {
                            fail();
                        }
                    } else {
                        fail();
                    }
                }else {
                    fail();
                }
            }

            @Override
            public void onFailure(Call<Responses> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                fail();
            }

        });

    }

    public void fail(){
        shutter.setVisibility(View.GONE);
        layout.setVisibility(View.VISIBLE);
        value.setVisibility(View.GONE);
        Toast.makeText(getActivity(), "체중계 인식에 실패했습니다.", Toast.LENGTH_SHORT).show();
    }

    public void success(String text, Bitmap bitmap){
        double f = Double.parseDouble(text);
        double res = (int) (f * 100) / 100f;

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    File photoFile = createImageFile(getActivity());
                    FileOutputStream out = new FileOutputStream(photoFile);
                    // compress 함수를 사용해 스트림에 비트맵을 저장합니다.
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.close();
                    resultUri = Uri.fromFile(photoFile);

                    if (UserInfo.getLogin(getActivity())){
                        UserInfo.saveDojeon(res, Dojeon.TYPE_WEIGHT, getActivity(), settingFragment.mRange, settingFragment.mTimes, settingFragment.isAlarm, resultUri);
                    }else{
                        UserInfo.saveDojeon(res, Dojeon.TYPE_WEIGHT, getActivity(), settingFragment.mRange, settingFragment.mTimes, settingFragment.isAlarm, null);
                    }
                } catch (IOException e) {
                    Toast.makeText(getContext(), "사진 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        value.setText(String.format("%.1f kg", res));
        shutter.setVisibility(View.GONE);
        layout.setVisibility(View.VISIBLE);
        value.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        closeCamera();
        super.onPause();

    }





    @Override
    public void onDestroy() {
        super.onDestroy();
        camera.destroy();
    }

    public double getArea(TextAnnotation t){
        RectF rectF = t.boundingPoly.getRectF();
        return rectF.width() * rectF.height();
    }

    public static boolean isNumeric(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void checkPermission(){
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, MY_PERMISSION_CAMERA);
        }else{
            openCamera();
        }
    }




}