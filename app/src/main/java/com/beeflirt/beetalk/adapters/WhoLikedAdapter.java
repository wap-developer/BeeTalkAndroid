package com.beeflirt.beetalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.utils.Constants;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class WhoLikedAdapter extends RecyclerView.Adapter<WhoLikedAdapter.VH> {

    public interface Listener {
        void onClick(Map<String, Object> item);
    }

    private final List<Map<String, Object>> items;
    private final Listener listener;

    public WhoLikedAdapter(List<Map<String, Object>> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_who_liked, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Map<String, Object> item = items.get(position);
        boolean blurred = asBool(item.get("blurred"));
        boolean superLike = asBool(item.get("is_super_like"));

        String name = asString(item.get("name"), "Someone");
        h.tvName.setText(name);

        String meta;
        if (blurred) {
            meta = "Unlock Premium to see profile";
        } else {
            Object age = item.get("age");
            Object city = item.get("city");
            StringBuilder sb = new StringBuilder();
            if (age != null) sb.append(asInt(age)).append(" yrs");
            if (city != null && !String.valueOf(city).isEmpty()) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(city);
            }
            if (sb.length() == 0) sb.append("Likes you");
            meta = sb.toString();
        }
        h.tvMeta.setText(meta);

        h.tvBadge.setVisibility(superLike ? View.VISIBLE : View.GONE);
        h.viewBlur.setVisibility(blurred ? View.VISIBLE : View.GONE);

        String photo = asString(item.get("primary_photo"), null);
        if (photo != null && !photo.isEmpty()) {
            String url = photo.startsWith("http") ? photo : Constants.IMAGE_BASE_URL + photo;
            Glide.with(h.itemView.getContext())
                    .load(url)
                    .centerCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(h.ivPhoto);
        } else {
            h.ivPhoto.setImageResource(R.drawable.ic_person);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CircleImageView ivPhoto;
        View viewBlur;
        TextView tvName, tvMeta, tvBadge;

        VH(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            viewBlur = itemView.findViewById(R.id.view_blur);
            tvName = itemView.findViewById(R.id.tv_name);
            tvMeta = itemView.findViewById(R.id.tv_meta);
            tvBadge = itemView.findViewById(R.id.tv_badge);
        }
    }

    private static boolean asBool(Object o) {
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() == 1;
        return false;
    }

    private static int asInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    private static String asString(Object o, String def) {
        return o == null ? def : String.valueOf(o);
    }
}
