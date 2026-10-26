package app.morphe.extension.smartlife;

import android.content.Context;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.thingclips.smart.personal.setting.plug.cell.base.SwitchTextCell;

/** Inherits the exact row inflation and holder used by Smart Life's SoundCell. */
public final class MorpheSwitchCell extends SwitchTextCell {
    private final int option;

    public MorpheSwitchCell(int option) { this.option = option; }

    @Override public void s(RecyclerView.v holder) {
        View row = holder.itemView;
        Context context = row.getContext();
        Preferences.initialize(context);
        TextView title = (TextView) row.findViewById(id(context, "menu_list_title"));
        final CompoundButton button = (CompoundButton) row.findViewById(id(context, "sb_subtitle"));
        String label = Options.label(option, context.getResources().getConfiguration().locale.getLanguage());
        title.setText(label);
        // RecyclerView may rebind a holder previously assigned to another option.
        button.setOnCheckedChangeListener(null);
        button.setChecked(Preferences.hidden(option));
        button.setContentDescription(label);
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton view, boolean checked) {
                Preferences.set(option, checked);
            }
        });
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { button.toggle(); }
        });
    }

    static int id(Context context, String name) {
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }
}
