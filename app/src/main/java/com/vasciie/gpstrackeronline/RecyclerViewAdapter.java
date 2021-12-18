package com.vasciie.gpstrackeronline;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    MainActivity main;
    LinkedList<String> capTimes; // Using LinkedList to improve performance while tracking
    LinkedList<Integer> images;


    public RecyclerViewAdapter(MainActivity main, LinkedList<Integer> images, LinkedList<String> capTimes){
        this.main = main;
        this.images = images;
        this.capTimes = capTimes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(main);
        View view = inflater.inflate(R.layout.text_row_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.capTimeText.setText(capTimes.get(position));
        holder.image.setImageResource(main.imageIds.get(images.get(position)));
        holder.main = main;
    }

    @Override
    public int getItemCount() {
        return images.size();
    }



    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView capTimeText;
        public ImageView image;
        public MainActivity main;
        public CardView cardView;

        // I set the selection color that way so that I can easily set the color
        // through the layout designer, and after memorizing the color I remove it
        private final ColorStateList selectionColor;

        private static ViewHolder previouslyClicked;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            capTimeText = itemView.findViewById(R.id.text_time);
            image = itemView.findViewById(R.id.imageView);

            cardView = itemView.findViewById(R.id.list_item);
            selectionColor = cardView.getCardBackgroundColor();
            deselect(); // Make it with normal color (when not selected)
            cardView.setOnClickListener(view -> {
                main.lookupLocation(getAdapterPosition());

                if(previouslyClicked != null){
                    if(previouslyClicked.equals(this)) {
                        return;
                    }else {
                        previouslyClicked.deselect();
                    }
                }

                cardView.setCardBackgroundColor(selectionColor);

                previouslyClicked = this;
            });
        }

        public void deselect(){
            cardView.setCardBackgroundColor(0xffffffff);
        }
    }

}
