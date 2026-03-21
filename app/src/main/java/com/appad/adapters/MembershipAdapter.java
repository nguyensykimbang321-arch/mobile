package com.appad.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.models.ArtistMembership;
import com.appad.utils.ImageUrlUtils;
import com.bumptech.glide.Glide;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MembershipAdapter extends RecyclerView.Adapter<MembershipAdapter.MembershipViewHolder> {

    private Context context;
    private List<ArtistMembership> membershipList;

    public MembershipAdapter(Context context, List<ArtistMembership> membershipList) {
        this.context = context;
        this.membershipList = membershipList;
    }

    @NonNull
    @Override
    public MembershipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_membership, parent, false);
        return new MembershipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MembershipViewHolder holder, int position) {
        ArtistMembership membership = membershipList.get(position);
        
        if (membership.getArtist() != null) {
            holder.txtArtistName.setText(membership.getArtist().getName());
            Glide.with(context)
                    .load(ImageUrlUtils.fixUrl(membership.getArtist().getImageUrl()))
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.imgAvatar);
        } else {
            holder.txtArtistName.setText("Nghệ sĩ #" + membership.getArtistId());
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.txtPricePaid.setText(formatter.format(membership.getPricePaid() != null ? membership.getPricePaid() : 0));

        // Format and Check Status
        try {
            String status = membership.getStatus();
            String expiryStr = membership.getExpiryDate();
            boolean isExpiredByDate = false;

            if (expiryStr != null) {
                String dateToShow = expiryStr.split("T")[0];
                holder.txtExpiry.setText("Hết hạn: " + dateToShow);
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date expiryDate = sdf.parse(dateToShow);
                if (expiryDate != null && expiryDate.before(new Date())) {
                    isExpiredByDate = true;
                }
            }

            if ("expired".equalsIgnoreCase(status) || "inactive".equalsIgnoreCase(status) || isExpiredByDate) {
                holder.txtStatus.setText("Hết hạn");
                holder.txtStatus.setTextColor(Color.parseColor("#EF4444"));
            } else {
                holder.txtStatus.setText("Đang hoạt động");
                holder.txtStatus.setTextColor(Color.parseColor("#4CAF50"));
            }
        } catch (Exception e) {
            holder.txtExpiry.setText("Hết hạn: " + membership.getExpiryDate());
            holder.txtStatus.setText("Đang hoạt động");
        }
    }

    @Override
    public int getItemCount() {
        return membershipList.size();
    }

    public static class MembershipViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtArtistName, txtStatus, txtExpiry, txtPricePaid;

        public MembershipViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgArtistAvatar);
            txtArtistName = itemView.findViewById(R.id.txtArtistName);
            txtStatus = itemView.findViewById(R.id.txtMembershipStatus);
            txtExpiry = itemView.findViewById(R.id.txtExpiryDate);
            txtPricePaid = itemView.findViewById(R.id.txtPricePaid);
        }
    }
}
