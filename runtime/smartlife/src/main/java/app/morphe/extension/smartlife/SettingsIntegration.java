package app.morphe.extension.smartlife;

import com.thingclips.smart.archer.api.bean.ArcherConfigGroupData;
import com.thingclips.smart.archer.api.extension.CustomCell;
import java.util.ArrayList;
import java.util.List;

/** Adds one native Archer group to both locally and remotely configured Settings lists. */
public final class SettingsIntegration {
    private static final String GROUP = "morphe_settings";
    private static final String HEADER = "morphe_header";
    private SettingsIntegration() {}

    public static List<ArcherConfigGroupData> groups(List<ArcherConfigGroupData> input) {
        List<ArcherConfigGroupData> output = new ArrayList<>();
        if (input != null) for (ArcherConfigGroupData group : input) {
            if (!GROUP.equals(group.getName())) output.add(group);
        }
        if (Options.mask() == 0) return output;
        ArcherConfigGroupData group = new ArcherConfigGroupData();
        group.setName(GROUP);
        ArcherConfigGroupData.Header header = new ArcherConfigGroupData.Header();
        header.setCellType(HEADER);
        group.setHeader(header);
        List<ArcherConfigGroupData.Items> items = new ArrayList<>();
        for (int i = 0; i < Options.KEYS.length; i++) if (Options.available(i)) {
            ArcherConfigGroupData.Items item = new ArcherConfigGroupData.Items();
            item.setCellType(Options.KEYS[i]);
            items.add(item);
        }
        group.setItems(items);
        // Keep the app's account and general settings at their original positions.
        output.add(group);
        return output;
    }

    public static List<CustomCell> cells(List<CustomCell> input) {
        List<CustomCell> output = new ArrayList<>();
        if (input != null) for (CustomCell cell : input) {
            boolean ours = HEADER.equals(cell.b());
            for (String key : Options.KEYS) ours |= key.equals(cell.b());
            if (!ours) output.add(cell);
        }
        if (Options.mask() == 0) return output;
        output.add(new CustomCell(HEADER, new MorpheHeaderCell()));
        for (int i = 0; i < Options.KEYS.length; i++) if (Options.available(i)) {
            output.add(new CustomCell(Options.KEYS[i], new MorpheSwitchCell(i)));
        }
        return output;
    }
}
