package com.vasciie.gpstrackeronline.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vasciie.gpstrackeronline.R;
import com.vasciie.gpstrackeronline.activities.MainActivityCaller;

import java.util.ArrayList;

public class RecyclerViewAdapterPhones extends RecyclerView.Adapter<RecyclerViewAdapterPhones.ViewHolder> {

    private final MainActivityCaller main;
    private final String[] phones;


    public RecyclerViewAdapterPhones(MainActivityCaller main, String[] phones){
        this.main = main;
        this.phones = phones;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(main);
        View view = inflater.inflate(R.layout.text_row_item2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.button.setText(phones[position]);
    }

    @Override
    public int getItemCount() {
        return phones.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public Button button;

        private static ViewHolder previouslyClicked;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            button = itemView.findViewById(R.id.phone_item_button);


            button.setOnClickListener(view -> {
                System.out.println(button.getText() + ": CLICK!");

                if(previouslyClicked != null){
                    if(previouslyClicked.equals(this))
                        return;

                    previouslyClicked.deselect();
                }

                previouslyClicked = this;
                button.setBackgroundColor(button.getHighlightColor());
            });
        }

        public void deselect() {
            button.setBackgroundColor(0xffffffff);
        }
    }
}
