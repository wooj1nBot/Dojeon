package com.assist.dojeon.fragment;

import static com.assist.dojeon.fragment.SettingFragment.setBack;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.assist.dojeon.R;
import com.assist.dojeon.activity.LoginActivity;
import com.assist.dojeon.activity.MainActivity;
import com.assist.dojeon.dto.Dojeon;
import com.assist.dojeon.dto.User;
import com.assist.dojeon.util.OnSingleClickListener;
import com.assist.dojeon.util.UserInfo;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match

    TextView tv_complete;
    TextView tv_sum;

    RecyclerView rc;

    public boolean isProfile = true;

    public ProfileFragment(boolean isProfile) {
        // Required empty public constructor
        this.isProfile = isProfile;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        ImageView back = view.findViewById(R.id.back);
        ImageView logout= view.findViewById(R.id.logout);
        TextView tv_logo = view.findViewById(R.id.logo);
        CardView cardView = view.findViewById(R.id.panel);

        back.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                setBack(getActivity());
            }
        });

        logout.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                final PopupMenu popupMenu = new PopupMenu(getContext(),logout);
                getActivity().getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.parent, new HomeFragment());
                        transaction.commitAllowingStateLoss();

                        UserInfo.logout(getContext());
                        Intent intent = new Intent(getContext(), LoginActivity.class);
                        startActivity(intent);
                        return false;
                    }
                });
                popupMenu.show();
            }
        });

        if (isProfile){
            cardView.setVisibility(View.VISIBLE);
            back.setVisibility(View.GONE);
            logout.setVisibility(View.VISIBLE);
            tv_logo.setVisibility(View.VISIBLE);
        }else{
            cardView.setVisibility(View.GONE);
            back.setVisibility(View.VISIBLE);
            logout.setVisibility(View.GONE);
            tv_logo.setVisibility(View.GONE);
        }

        CircleImageView profile = view.findViewById(R.id.profile);
        TextView tv_name = view.findViewById(R.id.tv_name);
        tv_complete = view.findViewById(R.id.tv_complete);
        tv_sum = view.findViewById(R.id.tv_sum);

        rc = view.findViewById(R.id.rc);

        String uid = UserInfo.getID(getContext());

        if (UserInfo.getLogin(getContext())){
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(uid).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()){
                        User user = documentSnapshot.toObject(User.class);
                        if (user.profile != null){
                            Glide.with(getActivity()).load(Uri.parse(user.profile)).into(profile);
                        }else{
                            profile.setImageResource(R.mipmap.ic_launcher);
                        }
                        tv_name.setText(user.name);
                        setDojeonList(user);
                    }
                }
            });
        }else {
            setDojeonList(UserInfo.getUser(getContext()));
        }

        return view;
    }



    public void setDojeonList(User user){
        ArrayList<Dojeon> proceed = UserInfo.getProceedDojeon(user);
        ArrayList<Dojeon> end = UserInfo.getEndDojeon(user);
        tv_complete.setText(String.format("완료한 도전 : %d개", end.size()));
        tv_sum.setText(String.format("총 도전 기간 : %d일", UserInfo.getSumDojeon(user)));

        ListAdopter listAdopter = new ListAdopter(proceed, end);
        rc.setLayoutManager(new LinearLayoutManager(getContext()));
        rc.setAdapter(listAdopter);
    }

    public class ListAdopter extends RecyclerView.Adapter<ListAdopter.ViewHolder> {

        ArrayList<Dojeon> proceed;
        ArrayList<Dojeon> complete;
        Context context;
        FragmentManager manager;

        ListAdopter(ArrayList<Dojeon> proceed, ArrayList<Dojeon> complete){
            this.proceed = proceed;
            this.complete = complete;
            this.manager = getActivity().getSupportFragmentManager();
        }

        @NonNull
        @Override
        public ListAdopter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) ;
            View view = inflater.inflate(R.layout.dojeon_list_item, parent, false);
            return new ListAdopter.ViewHolder(view);
        }



        @Override
        public void onBindViewHolder(@NonNull ListAdopter.ViewHolder holder, int position) {

            Dojeon dojeon;

            if (position >= proceed.size()){
                dojeon = complete.get(position - proceed.size());
                holder.item.setTag(new Object[]{dojeon, true});
            }else{
                dojeon = proceed.get(position);
                holder.item.setTag(new Object[]{dojeon, false});
            }

            holder.item.setOnClickListener(new OnSingleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    Object[] objects = (Object[]) v.getTag();
                    Dojeon dojeon = (Dojeon) objects[0];
                    boolean isEnd = (boolean) objects[1];

                    Fragment fragment = null;

                    if (!isEnd) {
                        if (dojeon.type == Dojeon.TYPE_MEDITATE) {
                            fragment = new MeditateFragment(dojeon.dojeonId);
                        }
                        if (dojeon.type == Dojeon.TYPE_WEIGHT) {
                            fragment = new WeightFragment(dojeon.dojeonId);
                        }
                    }else{
                        HistoryFragment historyFragment = new HistoryFragment(dojeon.type);
                        historyFragment.setDojeonId(dojeon.dojeonId);
                        fragment = historyFragment;
                    }
                    if (fragment == null) return;

                    FragmentTransaction transaction = manager.beginTransaction();
                    transaction.replace(R.id.parent, fragment);
                    transaction.addToBackStack(null);
                    transaction.commitAllowingStateLoss();
                }
            });

            if (proceed.size() > 0){
                if (position == 0){
                    holder.tv_title.setVisibility(View.VISIBLE);
                    holder.tv_title.setText("진행중인 도전");

                    holder.history.setVisibility(View.VISIBLE);
                    holder.tv_history.setVisibility(View.VISIBLE);
                    holder.tv_pro.setText(String.valueOf(proceed.size()));
                    holder.tv_com.setText(String.valueOf(complete.size()));

                }
                else if (position == proceed.size()) {
                    holder.tv_title.setVisibility(View.VISIBLE);
                    holder.tv_title.setText("완료한 도전");

                    holder.history.setVisibility(View.GONE);
                    holder.tv_history.setVisibility(View.GONE);
                }else{
                    holder.tv_title.setVisibility(View.GONE);
                    holder.history.setVisibility(View.GONE);
                    holder.tv_history.setVisibility(View.GONE);
                }
            }else{
                if (position == 0) {
                    holder.tv_title.setVisibility(View.VISIBLE);
                    holder.tv_title.setText("완료한 도전");

                    holder.history.setVisibility(View.VISIBLE);
                    holder.tv_history.setVisibility(View.VISIBLE);
                    holder.tv_pro.setText(String.valueOf(proceed.size()));
                    holder.tv_com.setText(String.valueOf(complete.size()));
                }else{
                    holder.tv_title.setVisibility(View.GONE);
                    holder.history.setVisibility(View.GONE);
                    holder.tv_history.setVisibility(View.GONE);
                }
            }


            holder.image_type.setImageResource(Dojeon.TYPES_IMAGE[dojeon.type]);
            holder.tv_type.setText(Dojeon.TYPES[dojeon.type]);

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

            if (dojeon.end.getTime() < System.currentTimeMillis() || dojeon.range == dojeon.resMap.size()){
                holder.tv_proceed.setText(String.format("%d일 중 %d회 도전 완료", dojeon.range, dojeon.resMap.size()));
            }else{
                Log.d("diff" , String.valueOf(diffDays));
                holder.tv_proceed.setText(String.format("%d일 째 도전 중", dif));
            }
            int per = (int) ((dojeon.resMap.size() / (float) dojeon.range) * 100);

            int cnt = dojeon.resMap.size();
            cnt = Math.min(cnt, dojeon.range);
            holder.tv_percent.setText(String.format("달성률 : %d %% (%d 일 / %d 일)", per, cnt, dojeon.range));

            SimpleDateFormat format = new SimpleDateFormat("yyyy / MM / dd");

            holder.tv_start.setText("시작일 : " + format.format(dojeon.start));

            holder.tv_end.setText("종료일 : " + format.format(dojeon.end));
        }

        @Override
        public int getItemCount() {
            return proceed.size() + complete.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            CardView item;

            ImageView image_type;
            TextView tv_type;
            TextView tv_proceed;
            TextView tv_percent;
            TextView tv_start;
            TextView tv_end;

            TextView tv_title;

            CardView history;
            TextView tv_history;
            TextView tv_pro;
            TextView tv_com;

            ViewHolder(View itemView) {
                super(itemView);
                item = itemView.findViewById(R.id.item);
                image_type = itemView.findViewById(R.id.image);
                tv_type = itemView.findViewById(R.id.tv_type);
                tv_proceed = itemView.findViewById(R.id.tv_proceed);
                tv_percent = itemView.findViewById(R.id.tv_percent);
                tv_start = itemView.findViewById(R.id.tv_start);
                tv_end = itemView.findViewById(R.id.tv_end);
                tv_title = itemView.findViewById(R.id.tv_title);

                history = itemView.findViewById(R.id.history);
                tv_history = itemView.findViewById(R.id.tv_history);
                tv_pro = itemView.findViewById(R.id.pro);
                tv_com = itemView.findViewById(R.id.com);
            }
        }


    }


}