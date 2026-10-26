package com.thingclips.smart.personal.setting.plug.cell.base;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.thingclips.smart.archer.api.cell.IArcherCell;
public abstract class BaseArcherCell implements IArcherCell {
    public abstract void s(RecyclerView.v holder);
    public abstract RecyclerView.v t(ViewGroup parent);
}
