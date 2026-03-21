package com.appad.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.adapters.DraggableSongAdapter;

/**
 * ItemTouchHelper.Callback để xử lý kéo thả cho danh sách bài hát
 */
public class SongDragCallback extends ItemTouchHelper.Callback {

    private final DraggableSongAdapter adapter;

    public SongDragCallback(DraggableSongAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        // Cho phép long press để kéo (không cần icon drag handle)
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Chỉ cho phép kéo lên xuống
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Không hỗ trợ swipe
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            // Khi bắt đầu kéo - highlight item
            if (viewHolder != null && viewHolder.itemView != null) {
                viewHolder.itemView.setAlpha(0.85f);
                viewHolder.itemView.setScaleX(1.02f);
                viewHolder.itemView.setScaleY(1.02f);
            }
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        
        // Khi thả - khôi phục trạng thái bình thường
        viewHolder.itemView.setAlpha(1.0f);
        viewHolder.itemView.setScaleX(1.0f);
        viewHolder.itemView.setScaleY(1.0f);
        
        // Thông báo kéo thả hoàn tất
        adapter.onItemMoveFinished();
    }
}
