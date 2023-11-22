package com.assist.dojeon.fragment;

import static com.assist.dojeon.fragment.SettingFragment.setBack;
import static com.assist.dojeon.fragment.WeightFragment.WEIGHT_PATH;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jaygoo.widget.RangeSeekBar;
import com.assist.dojeon.R;
import com.assist.dojeon.dto.Dojeon;
import com.assist.dojeon.dto.User;
import com.assist.dojeon.util.UserInfo;
import com.assist.dojeon.view.RangeCalenderView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class HistoryFragment extends Fragment {

    int type;
    User user;

    TextView tv_title;
    TextView tv_start;
    TextView tv_end;
    GridView gridView;
    RangeSeekBar seekBar;
    TextView tv_date ;
    TextView tv_proceed ;
    TextView tv_progress;
    RangeCalenderView calenderView ;
    TextView tv_label ;
    LineChart chart;
    RecyclerView rc;
    TextView tv_doj;

    Timer timer;

    int dojeonId;

    public HistoryFragment(int type) {
        // Required empty public constructor
        this.type = type;
        this.dojeonId = -1;
    }

    public void setDojeonId(int dojeonId) {
        this.dojeonId = dojeonId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment



        View view =  inflater.inflate(R.layout.fragment_history, container, false);

        tv_title = view.findViewById(R.id.tv_title);
        tv_start = view.findViewById(R.id.tv_start);
        tv_end = view.findViewById(R.id.tv_end);
        gridView = view.findViewById(R.id.grid);
        rc = view.findViewById(R.id.rc);
        seekBar = view.findViewById(R.id.seekbar);
        tv_date = view.findViewById(R.id.tv_date);
        tv_proceed = view.findViewById(R.id.tv_preceed);
        tv_progress = view.findViewById(R.id.tv_progress);
        calenderView = view.findViewById(R.id.calender);
        tv_label = view.findViewById(R.id.ylabel);
        chart = view.findViewById(R.id.chart);
        ImageView back = view.findViewById(R.id.back);
        tv_doj = view.findViewById(R.id.tv_doj);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBack(getActivity());
            }
        });

        if (type == Dojeon.TYPE_WEIGHT){
            tv_label.setText("몸무게 (kg)");
            rc.setVisibility(View.GONE);
            gridView.setVisibility(View.VISIBLE);
        } else if (type == Dojeon.TYPE_MEDITATE) {
            tv_label.setText("시간 (분)");
            rc.setVisibility(View.VISIBLE);
            gridView.setVisibility(View.GONE);
        }

        seekBar.setIndicatorTextStringFormat("%s%%");
        seekBar.setEnabled(false);

        if (UserInfo.getLogin(getContext())){

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(UserInfo.getID(getContext())).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                    if (task.isSuccessful() && task.getResult().exists()) {
                        user = task.getResult().toObject(User.class);
                        setData();
                    }

                }
            });
        }else {
            user = UserInfo.getUser(getContext());
            setData();
        }

        return view;
    }

    public void setAnim(RangeSeekBar seekBar, float progress){
        Log.d("progess", String.valueOf(progress));

        int time = 10;
        int period = 650;
        float tick = progress * (time / (float) period);
        timer = new Timer();
        final float[] sum = {0};
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                // 코드 작성
                if (sum[0] >= progress){
                    timer.cancel();
                    return;
                }
                sum[0] += tick;
                seekBar.setProgress(sum[0]);
            }
        };
        timer.schedule(timerTask, 0, time);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null){
            timer.cancel();
        }
    }

    public void setData(){

        SimpleDateFormat format = new SimpleDateFormat("yyyy / MM / dd");
        tv_title.setText("진행 중인 도전 없음");
        tv_start.setVisibility(View.GONE);
        tv_end.setVisibility(View.GONE);
        tv_proceed.setText(String.format("%d일 째 도전 중", 0));
        tv_date.setText(format.format(Calendar.getInstance().getTime()));

        if (user == null) return;

        Dojeon dojeon;
        if (dojeonId != -1){
            dojeon = UserInfo.getDojeon(user, dojeonId);
        }else{
            dojeon = UserInfo.getDojeon(user, Calendar.getInstance(), type);
        }

        calenderView.setDojeon(dojeon, type);

        if (dojeon != null){

            ArrayList<Item> items = new ArrayList<>();

            tv_start.setVisibility(View.VISIBLE);
            tv_end.setVisibility(View.VISIBLE);
            tv_title.setText(Dojeon.TYPES[dojeon.type]);
            tv_start.setText("도전 시작일 : " + format.format(dojeon.start));
            tv_end.setText("도전 종료일 : " + format.format(dojeon.end));

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    seekBar.setRange(0, dojeon.range);

                    seekBar.setIndicatorText(String.valueOf( (int) (( (float) dojeon.resMap.size() / dojeon.range) * 100)));

                    setAnim(seekBar, dojeon.resMap.size());
                }
            }, 100);


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



            if (dojeon.end.getTime() < System.currentTimeMillis()){
                tv_doj.setText("완료한 도전");
                tv_proceed.setText(String.format("%d일 중 %d회 도전 완료", dojeon.range, dojeon.resMap.size()));
            }else{
                tv_doj.setText("진행중인 도전");
                tv_proceed.setText(String.format("%d일 째 도전 중", dif));
            }


            tv_progress.setText(String.format("%d   /   %d", dojeon.resMap.size(), dojeon.range));
            setChart(chart, dojeon);

            for (String key : dojeon.timeMap.keySet()){
                Date time = dojeon.timeMap.get(key);
                Item item = null;
                if (type == Dojeon.TYPE_WEIGHT){
                    if (dojeon.imageMap.containsKey(key)){
                        item = new Item(Uri.parse(dojeon.imageMap.get(key)), time, dojeon.resMap.get(key));
                        items.add(item);
                    }else{
                        String imageFileName = WEIGHT_PATH + "_" + key;
                        File file = new File(getActivity().getFilesDir(), imageFileName + ".jpg");
                        if (file.exists()) {
                            Uri dest = Uri.fromFile(file);
                            item = new Item(dest, time, dojeon.resMap.get(key));
                            items.add(item);
                        }
                    }
                }else if (type == Dojeon.TYPE_MEDITATE){
                    item = new Item(null, time, dojeon.resMap.get(key));
                    items.add(item);
                }


            }

            items.sort(new Comparator<Item>() {
                @Override
                public int compare(Item item, Item t1) {
                    return Long.compare(t1.time.getTime(), item.time.getTime());
                }
            });


            if (type == Dojeon.TYPE_WEIGHT) {

                GridViewAdapter adapter = new GridViewAdapter(items);
                gridView.setAdapter(adapter);

                int totalHeight = (items.size() / 3);

                if (items.size() % 3 != 0) {
                    totalHeight++;
                }

                ViewGroup.LayoutParams params = gridView.getLayoutParams();

                Display display = getActivity().getWindowManager().getDefaultDisplay();  // in Activity
                /* getActivity().getWindowManager().getDefaultDisplay() */ // in Fragment
                Point size = new Point();
                display.getRealSize(size); // or getSize(size)
                int width = size.x;
                int height = size.y;

                params.height = width / 3 * totalHeight;

                gridView.setLayoutParams(params);
                gridView.requestLayout();

            }else{
                ListAdopter adopter = new ListAdopter(items);
                rc.setLayoutManager(new LinearLayoutManager(getContext()));
                rc.setAdapter(adopter);
            }
        }


    }

    public void setChart(LineChart lineChart, Dojeon dojeon){

        ArrayList<Date> days = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dojeon.start);
        Date end = dojeon.end;




        while (calendar.getTime().getTime() <= end.getTime()){
            String key = calendar.get(Calendar.YEAR)+"-"+(calendar.get(Calendar.MONTH) + 1)+"-"+calendar.get(Calendar.DAY_OF_MONTH);
            days.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }


        lineChart.getLegend().setEnabled(false);
        lineChart.setExtraBottomOffset(15f); // 간격
        lineChart.getDescription().setEnabled(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // x축 데이터 표시 위치
        xAxis.setGranularity(1f);
        xAxis.setYOffset(12f);
        xAxis.setTextSize(14f);
        xAxis.setTextColor(Color.parseColor("#777777"));
        xAxis.setAxisLineColor(Color.parseColor("#D9D9D9"));

        YAxis yAxisLeft = lineChart.getAxisLeft();
        yAxisLeft.setXOffset(12f);
        yAxisLeft.setTextSize(14f);
        yAxisLeft.setTextColor(Color.parseColor("#777777"));
        yAxisLeft.setDrawAxisLine(true);
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setGridColor(Color.parseColor("#D9D9D9"));
        yAxisLeft.setAxisLineColor(Color.parseColor("#D9D9D9"));

        YAxis yAxisRight = lineChart.getAxisRight();
        yAxisRight.setEnabled(false);

        xAxis.setLabelCount(days.size());
        xAxis.setValueFormatter(new ValueFormatter() {

            @Override
            public String getFormattedValue(float value) {
                Date date = days.get((int) value);
                if (date.getDate() == 1){
                  return (date.getMonth() + 1) + "/" + date.getDate();
                }
                return String.valueOf(date.getDate());
            }
        });


        lineChart.setData(createChartData(dojeon));

        lineChart.setVisibleXRangeMaximum(8); // allow 5 values to be displayed

        lineChart.invalidate();

    }

    private LineData createChartData(Dojeon dojeon) {

        ArrayList<Entry> entry1 = new ArrayList<>(); // 앱1

        Calendar c = Calendar.getInstance();
        c.setTime(dojeon.start);
        Date end = dojeon.end;

        ArrayList<Integer> colors = new ArrayList<>();
        ArrayList<Integer> colors2 = new ArrayList<>();

        int color = Color.parseColor("#7198AD");


        while (c.getTime().getTime() <= end.getTime()){

            String key = c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH) + 1)+"-"+c.get(Calendar.DAY_OF_MONTH);
            c.add(Calendar.DAY_OF_MONTH, 1);
            String key2 = c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH) + 1)+"-"+c.get(Calendar.DAY_OF_MONTH);
            c.add(Calendar.DAY_OF_MONTH, -1);

            if (dojeon.resMap.containsKey(key)){
                colors2.add(color);
                if (!dojeon.resMap.containsKey(key2)){
                    colors.add(Color.TRANSPARENT);
                }else{
                    colors.add(color);
                }
                if (dojeon.type == Dojeon.TYPE_WEIGHT) {
                    entry1.add(new Entry(entry1.size(), dojeon.resMap.get(key).floatValue()));
                }else{
                    float t = (long) dojeon.resMap.get(key).floatValue();
                    t /= 60f;
                    t /= 60f;
                    float min = t;
                    entry1.add(new Entry(entry1.size(), min));
                }

            }else{
                colors.add(Color.TRANSPARENT);
                colors2.add(Color.TRANSPARENT);
                entry1.add(new Entry(entry1.size(), 0));
            }

            c.add(Calendar.DAY_OF_MONTH, 1);

        }


        LineData d = new LineData();

        LineDataSet set = new LineDataSet(entry1, "몸무게");
        set.setColors(colors);
        set.setCircleColors(colors2);

        set.setLineWidth(1.5f);
        set.setDrawCircleHole(false);
        set.setCircleRadius(4f);
        set.setFillColor(color);
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setDrawValues(false);

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        d.addDataSet(set);

        return d;
    }

    class Item{

        Uri uri;
        Date time;
        double value;


        Item(Uri uri, Date time, double value){
            this.uri = uri;
            this.time = time;
            this.value = value;
        }

    }


    public class ListAdopter extends RecyclerView.Adapter<ListAdopter.ViewHolder> {

        ArrayList<Item> items;
        Context context;

        ListAdopter(ArrayList<Item> items){
            this.items = items;
        }

        @NonNull
        @Override
        public ListAdopter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) ;
            View view = inflater.inflate(R.layout.meditate_item, parent, false);
            return new ListAdopter.ViewHolder(view);
        }



        @Override
        public void onBindViewHolder(@NonNull ListAdopter.ViewHolder holder, int position) {

            Item item = items.get(position);

            long t = (long) item.value;
            long mic = t % 60;
            t /= 60;
            long sec = t % 60;
            t /= 60;
            long min = t;

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy / MM / dd");
            holder.tv_date.setText(simpleDateFormat.format(item.time));
            holder.tv_time.setText(String.format("%d 분 %d초", min, sec));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            TextView tv_date;
            TextView tv_time;

            ViewHolder(View itemView) {
                super(itemView);
                tv_date = itemView.findViewById(R.id.tv_date);
                tv_time = itemView.findViewById(R.id.tv_time);
            }
        }


    }


    class GridViewAdapter extends BaseAdapter {
        ArrayList<Item> items;

        GridViewAdapter(ArrayList<Item> items){
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        public void addItem(Item item) {
            items.add(item);
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            final Context context = viewGroup.getContext();
            final Item item = items.get(position);

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.grid_weight_item, viewGroup, false);
                LinearLayout info = convertView.findViewById(R.id.info);
                TextView tv_value = convertView.findViewById(R.id.tv_value);
                TextView tv_date = convertView.findViewById(R.id.tv_date);
                TextView tv_time = convertView.findViewById(R.id.tv_time);
                ImageView imageView = convertView.findViewById(R.id.imageview);
                ProgressBar pb = convertView.findViewById(R.id.pb);

                tv_value.setText(String.format("%.1f kg", item.value));
                SimpleDateFormat format = new SimpleDateFormat("yyyy / MM / dd");
                SimpleDateFormat format2 = new SimpleDateFormat("kk : mm");

                tv_date.setText(format.format(item.time));
                tv_time.setText(format2.format(item.time));

                Log.d("uru", item.uri.toString());

                pb.setVisibility(View.VISIBLE);
                Glide.with(getActivity()).load(item.uri).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        pb.setVisibility(View.GONE);
                        return false;
                    }
                }).into(imageView);

                convertView.setTag(true);

                View finalConvertView = convertView;
                convertView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        ViewGroup.LayoutParams params = info.getLayoutParams();
                        params.height = finalConvertView.getHeight();
                    }
                });

                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if ((boolean)view.getTag()) {
                            info.setVisibility(View.VISIBLE);
                            imageView.setVisibility(View.INVISIBLE);
                            view.setTag(false);
                        }else{
                            info.setVisibility(View.INVISIBLE);
                            imageView.setVisibility(View.VISIBLE);
                            view.setTag(true);
                        }
                    }
                });

            } else {
                View view = new View(context);
                view = (View) convertView;
            }


            return convertView;  //뷰 객체 반환
        }
    }
}