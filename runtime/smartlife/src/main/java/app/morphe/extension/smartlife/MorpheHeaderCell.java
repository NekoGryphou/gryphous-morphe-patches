package app.morphe.extension.smartlife;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.thingclips.smart.personal.setting.plug.cell.base.BaseArcherCell;

public final class MorpheHeaderCell extends BaseArcherCell {
    @Override public RecyclerView.v t(ViewGroup parent) {
        int layout = parent.getResources().getIdentifier("baselist_delegate_text_title", "layout",
                parent.getContext().getPackageName());
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new RecyclerView.v(view) {};
    }

    @Override public void s(RecyclerView.v holder) {
        ((TextView) holder.itemView).setText("Morphe");
    }
}
