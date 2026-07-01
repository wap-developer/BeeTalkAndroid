package com.beeflirt.beetalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.models.GreetingSlide;

import java.util.List;

public class GreetingSlideAdapter extends RecyclerView.Adapter<GreetingSlideAdapter.SlideViewHolder> {

    private List<GreetingSlide> slides;

    public GreetingSlideAdapter(List<GreetingSlide> slides) {
        this.slides = slides;
    }

    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_greeting_slide, parent, false);
        return new SlideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        GreetingSlide slide = slides.get(position);
        holder.ivImage.setImageResource(slide.getImageResId());
        holder.tvTitle.setText(slide.getTitle());
        holder.tvDescription.setText(slide.getDescription());
    }

    @Override
    public int getItemCount() {
        return slides.size();
    }

    static class SlideViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;
        TextView tvDescription;

        public SlideViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_slide_image);
            tvTitle = itemView.findViewById(R.id.tv_slide_title);
            tvDescription = itemView.findViewById(R.id.tv_slide_description);
        }
    }
}
