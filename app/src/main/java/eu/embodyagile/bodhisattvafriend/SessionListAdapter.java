package eu.embodyagile.bodhisattvafriend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.embodyagile.bodhisattvafriend.data.SessionEntity;

public class SessionListAdapter extends RecyclerView.Adapter<SessionListAdapter.VH> {

    public interface Callbacks {
        void onDeleteClicked(SessionEntity session);
    }

    private final Callbacks callbacks;
    private final List<SessionEntity> items = new ArrayList<>();
    private final DateFormat dateFmt = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private final DateFormat timeFmt = DateFormat.getTimeInstance(DateFormat.SHORT);

    public SessionListAdapter(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void setItems(List<SessionEntity> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged(); // OK for e-ink paging (full refresh)
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SessionEntity s = items.get(position);

        Date d = new Date(s.timestamp);
        h.tvDate.setText(dateFmt.format(d));
        h.tvTime.setText(timeFmt.format(d));

        // Practice shown as ID to keep it simple.
        // If you want name: resolve via PracticeRepository here or in Activity.
        h.tvPractice.setText(s.practiceId != null ? s.practiceId : "-");

        int minutes = safeMinutes(s);
        h.tvMinutes.setText(minutes + " min");

        h.btnDelete.setOnClickListener(v -> callbacks.onDeleteClicked(s));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvPractice, tvMinutes;
        ImageButton btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.row_date);
            tvTime = itemView.findViewById(R.id.row_time);
            tvPractice = itemView.findViewById(R.id.row_practice);
            tvMinutes = itemView.findViewById(R.id.row_minutes);
            btnDelete = itemView.findViewById(R.id.row_delete);
        }
    }

    private static int safeMinutes(SessionEntity s) {
        // Prefer actualMillis if present; otherwise plannedMinutes.
        // Adjust if your entity uses different field names.
        if (s.actualDurationMs > 0) {
            return Math.max(0, Math.round(s.actualDurationMs / 60000f));
        }
        return Math.max(0, (int) s.plannedMinutes);
    }
}
