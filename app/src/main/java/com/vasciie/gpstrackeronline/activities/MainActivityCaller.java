package com.vasciie.gpstrackeronline.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.SupportMapFragment;
import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.fragments.ButtonsFragment;
import com.vasciie.gpstrackeronline.fragments.RecyclerViewAdapterLocations;
import com.vasciie.gpstrackeronline.fragments.RecyclerViewAdapterPhones;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public class MainActivityCaller extends MainActivity {

    private RecyclerView recyclerView;
    private RecyclerViewAdapterPhones rvAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_caller);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.mapView2, SupportMapFragment.class, null)
                    .commit();

            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragmentContainerView2, ButtonsFragment.class, null)
                    .commit();
        }

        getSupportFragmentManager().addFragmentOnAttachListener(this::onAttachFragment);


        recyclerView = findViewById(R.id.phones_list);

        rvAdapter = new RecyclerViewAdapterPhones(this, new String[] {"phone1", "phone2", "phone3", "phone4", "phone5", "phone6"});
        recyclerView.setAdapter(rvAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void onAttachFragment(FragmentManager fragmentManager, Fragment fragment) {
        if(fragment instanceof SupportMapFragment){
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView2))
                    .getMapAsync(this);
        }
        else if(fragment instanceof ButtonsFragment){
            if(lookupMarker != null) {
                lookupMarker.remove();
                lookupMarker = null;
            }
        }
    }
}