package com.assist.dojeon.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.assist.dojeon.R;
import com.assist.dojeon.dto.Dojeon;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RangeCalenderView extends RelativeLayout {

    TextView tv_date;
    ImageView prev, next;
    GridView gridView;
    Dojeon dojeon;
    int type;

    Calendar calendar;
    CardView parent;

    int year, month, start, end;

    public RangeCalenderView(Context context) {
        super(context);
        init(context, null);
    }

    public RangeCalenderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RangeCalenderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs){
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.calender_view, this);
        if (attrs != null) {
            //attrs.xml에 정의한 스타일을 가져온다 즉 (attrs.xml에서 발생된 selected 속성이
            // 발생되어 private void setSelected(int select, boolean force)를 호출하게 된다.
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RangeCalenderView);
            year = a.getInteger(R.styleable.RangeCalenderView_year, 2023);
            month = a.getInteger(R.styleable.RangeCalenderView_month, 1);
            start = a.getInteger(R.styleable.RangeCalenderView_start, 1);
            end = a.getInteger(R.styleable.RangeCalenderView_end, 15);
            a.recycle(); // 이용이 끝났으면 recycle() 호출
            calendar = Calendar.getInstance();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        parent = findViewById(R.id.parent);
        tv_date = findViewById(R.id.tv_date);
        prev = findViewById(R.id.prev);
        next = findViewById(R.id.next);
        gridView = findViewById(R.id.gridview);

    }

    private String getMonth(int m){
        switch (m){
            case 1:
                return "January";
            case 2:
                return "February";
            case 3:
                return "March";
            case 4:
                return "April";
            case 5:
                return "May";
            case 6:
                return "June";
            case 7:
                return "July";
            case 8:
                return "August";
            case 9:
                return "September";
            case 10:
                return "October";
            case 11:
                return "November";
            case 12:
                return "December";

        }
        return "January";
    }


    public boolean compare(Date date1, Date date2){
        Date d1 = new Date();
        d1.setYear(date1.getYear());
        d1.setMonth(date1.getMonth());
        d1.setDate(date1.getDate());
        Date d2 = new Date();
        d2.setYear(date2.getYear());
        d2.setMonth(date2.getMonth());
        d2.setDate(date2.getDate());

        return d1.getTime() >= d2.getTime();
    }
    public boolean compare2(Date date1, Date date2){
        Date d1 = new Date();
        d1.setYear(date1.getYear());
        d1.setMonth(date1.getMonth());
        d1.setDate(date1.getDate());
        Date d2 = new Date();
        d2.setYear(date2.getYear());
        d2.setMonth(date2.getMonth());
        d2.setDate(date2.getDate());

        return d1.getTime() > d2.getTime();
    }

    public void setDate(Calendar calendar){

        Calendar c = Calendar.getInstance();
        c.setTime(calendar.getTime());

        ArrayList<Day> days = new ArrayList<>();

        tv_date.setText(getMonth(c.get(Calendar.MONTH)+1) + ", " + c.get(Calendar.YEAR));

        c.set(Calendar.DAY_OF_MONTH, 1);
        int start = c.get(Calendar.DAY_OF_WEEK);

        for(int i = 1; i < start; i++){
            c.add(Calendar.DAY_OF_MONTH, -1);
            if (dojeon != null && dojeon.type == type && compare(c.getTime(), dojeon.start) && !compare2(c.getTime(), dojeon.end)) {
                days.add(new Day(c.getTime(), dojeon, true));
            }else {
                days.add(new Day(c.getTime(), null, true));
            }
        }

        Collections.reverse(days);



        c.setTime(calendar.getTime());
        c.set(Calendar.DAY_OF_MONTH, 1);

        Log.d("cal", c.get(Calendar.YEAR) + "," + c.get(Calendar.MONTH));

        int max = c.getActualMaximum(Calendar.DAY_OF_MONTH);

        for(int i = 1; i <= max; i++){
            if (dojeon != null && dojeon.type == type && compare(c.getTime(), dojeon.start) && !compare2(c.getTime(), dojeon.end)) {
                days.add(new Day(c.getTime(), dojeon, false));
            }else {
                days.add(new Day(c.getTime(), null, false));
            }
            c.add(Calendar.DAY_OF_MONTH, 1);
        }



        int size = max + (start-1);
        int next;
        if(size > 35) {
            next = 42 - size;
            ViewGroup.LayoutParams params = parent.getLayoutParams();
            params.height = ConvertDPtoPX(getContext(), 450);
        }else{
            next = 35 - size;
            ViewGroup.LayoutParams params = parent.getLayoutParams();
            params.height = ConvertDPtoPX(getContext(), 400);
        }

        c.set(Calendar.DAY_OF_MONTH, 1);

        for(int i = 1; i <= next; i++){
            if (dojeon != null && dojeon.type == type && compare(c.getTime(), dojeon.start) && !compare2(c.getTime(), dojeon.end)) {
                days.add(new Day(c.getTime(), dojeon, true));
            }else {
                days.add(new Day(c.getTime(), null, true));
            }
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        c.add(Calendar.MONTH, -1);

        GridViewAdapter adapter = new GridViewAdapter(days);
        gridView.setAdapter(adapter);

        invalidate();
        requestLayout();
    }

    public void setDojeon(Dojeon dojeon, int type){
        this.dojeon = dojeon;
        this.type = type;

        if (calendar != null){
            setDate(calendar);

            prev.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    calendar.add(Calendar.MONTH, -1);
                    setDate(calendar);
                }
            });

            next.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    calendar.add(Calendar.MONTH, 1);
                    setDate(calendar);
                }
            });
        }

    }


    public static int ConvertDPtoPX(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    class Day{
        Date date;
        Dojeon dojeon;
        boolean isOut;

        Day(Date date, Dojeon dojeon, boolean isOut){
            this.date = date;
            this.dojeon = dojeon;
            this.isOut = isOut;
        }
    }

    class GridViewAdapter extends BaseAdapter {
        ArrayList<Day> items;

        GridViewAdapter(ArrayList<Day> items){
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        public void addItem(Day item) {
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
            final Day day = items.get(position);

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.grid_item, viewGroup, false);

                convertView.setTag(day);
                TextView tv_date = convertView.findViewById(R.id.tv_day);
                RelativeLayout parent = convertView.findViewById(R.id.parent);

                if (day.date == null){
                    convertView.setVisibility(INVISIBLE);
                    return convertView;
                }

                ImageView star = convertView.findViewById(R.id.star);

                if (day.dojeon != null){
                    Calendar c = Calendar.getInstance();
                    c.setTime(day.date);
                    tv_date.setTextColor(Color.WHITE);

                    if (day.dojeon.isCheck(c) || day.dojeon.isCheck(c)) {
                        star.setVisibility(View.VISIBLE);
                    } else {
                        star.setVisibility(GONE);
                    }
                }else{
                    tv_date.setTextColor(Color.parseColor("#0F2552"));
                    star.setVisibility(GONE);
                }
                Calendar c = Calendar.getInstance();
                c.setTime(day.date);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);

                if (day.dojeon != null){
                    if (c.get(Calendar.DAY_OF_MONTH) == dojeon.start.getDate()){
                        if (c.getTimeInMillis() < System.currentTimeMillis()) {
                            parent.setBackgroundResource(R.drawable.rectangle1_com);
                        } else {
                            parent.setBackgroundResource(R.drawable.rectangle1);
                        }
                    }else if (c.get(Calendar.DAY_OF_MONTH) == dojeon.end.getDate()){
                        if (c.getTimeInMillis() < System.currentTimeMillis()) {
                            parent.setBackgroundResource(R.drawable.rectangle2_com);
                        } else {
                            parent.setBackgroundResource(R.drawable.rectangle2);
                        }
                    }else {
                        if (c.getTimeInMillis() < System.currentTimeMillis()) {
                            parent.setBackgroundResource(R.drawable.rectangle3_com);
                        } else {
                            parent.setBackgroundResource(R.drawable.rectangle3);
                        }
                    }
                }

                if (day.isOut){
                    convertView.setAlpha(0.4f);
                }


                Log.d("day", day.date.toString());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(day.date);

                if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY){
                  tv_date.setTextColor(Color.parseColor("#D30000"));
                }

                tv_date.setText(day.date.getDate() + "");




            } else {
                View view = new View(context);
                view = (View) convertView;
            }


            return convertView;  //뷰 객체 반환
        }
    }





}
