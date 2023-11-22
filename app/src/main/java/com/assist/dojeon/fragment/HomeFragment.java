package com.assist.dojeon.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.assist.dojeon.R;
import com.assist.dojeon.util.OnSingleClickListener;
import com.assist.dojeon.util.UserInfo;


public class HomeFragment extends Fragment {

    public WeightFragment weightFragment;
    MeditateFragment meditateFragment;
    ProfileFragment profileFragment;

    public HomeFragment() {
        // Required empty public constructor
        weightFragment = new WeightFragment();
        meditateFragment = new MeditateFragment();
        profileFragment = new ProfileFragment(false);

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view =  inflater.inflate(R.layout.fragment_home, container, false);

        CardView card_weight = view.findViewById(R.id.card_weight);
        LinearLayout weight = view.findViewById(R.id.weight);
        CardView card_meditate = view.findViewById(R.id.card_medi);
        LinearLayout meditate = view.findViewById(R.id.medi);
        CardView history = view.findViewById(R.id.history);

        FragmentManager manager = getActivity().getSupportFragmentManager();



        card_meditate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.parent, meditateFragment);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });

        meditate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.parent, meditateFragment);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });

        card_weight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.parent, weightFragment);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });
        weight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.parent, weightFragment);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });

        history.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (!UserInfo.getID(getContext()).equals("")) {
                    FragmentTransaction transaction = manager.beginTransaction();
                    transaction.replace(R.id.parent, profileFragment);
                    transaction.addToBackStack(null);
                    transaction.commitAllowingStateLoss();
                }
            }
        });


        return view;

    }
}