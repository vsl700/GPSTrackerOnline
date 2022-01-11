package com.vasciie.gpstrackeronline.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.activities.LoginWayActivity;
import com.vasciie.gpstrackeronline.activities.MainActivity;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LocationsListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LocationsListFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // The access to the activity functions and public application fields
    // (like the location dataset)
    public static MainActivity main;

    private RecyclerView recyclerView;
    private RecyclerViewAdapterLocations rvAdapter;


    public LocationsListFragment() {
        main = MainActivity.currentMainActivity;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LocationsListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LocationsListFragment newInstance(String param1, String param2) {
        LocationsListFragment fragment = new LocationsListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_locations_list, container, false);

        Button goBackListBtn = v.findViewById(R.id.goBackList);
        goBackListBtn.setOnClickListener(view -> {
            main.getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(LoginWayActivity.loggedInCaller ? R.id.fragmentContainerView2 : R.id.fragmentContainerView, ButtonsFragment.class, null)
                    .commit();

            main.removeLookupMarkers();
        });

        Button currentLocBtn = v.findViewById(R.id.currentLoc2);
        currentLocBtn.setOnClickListener(view -> main.moveMapCamera(false));


        if(recyclerView == null)
            recyclerView = v.findViewById(R.id.recyclerView);

        rvAdapter = new RecyclerViewAdapterLocations(main, MainActivity.images, MainActivity.capTimes);
        recyclerView.setAdapter(rvAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(main));

        main.setupLookupMarkers();

        return v;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        rvAdapter.onDestroy();
    }
}