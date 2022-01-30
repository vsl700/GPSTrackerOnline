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
    private final int selectionIndex;


    public RecyclerViewAdapterPhones(MainActivityCaller main, String[] phones, int selectionIndex){
        this.main = main;
        this.phones = phones;
        this.selectionIndex = selectionIndex;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(main);
        View view = inflater.inflate(R.layout.text_row_item2, parent, false);
        return new ViewHolder(view, main);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.button.setText(phones[position]);
        if(position == selectionIndex)
            holder.select();
    }

    @Override
    public int getItemCount() {
        return phones.length;
    }

    public void onDestroy(){
        ViewHolder.previouslyClicked = null;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public Button button;

        private static ViewHolder previouslyClicked;
        private MainActivityCaller main;


        public ViewHolder(@NonNull View itemView, MainActivityCaller main) {
            super(itemView);
            this.main = main;

            button = itemView.findViewById(R.id.phone_item_button);

            button.setOnClickListener(view -> {
                System.out.println(button.getText() + ": CLICK!");

                main.onPhoneSelected(getAdapterPosition());
                if(previouslyClicked != null){
                    if(previouslyClicked.equals(this))
                        return;

                    previouslyClicked.deselect();
                }

                select();
            });
        }

        private void select() {
            previouslyClicked = this;
            button.setBackgroundColor(button.getHighlightColor());
        }

        private void deselect() {
            button.setBackgroundColor(0xffffffff);
        }
    }
}
