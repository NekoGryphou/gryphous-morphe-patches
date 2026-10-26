package com.thingclips.smart.archer.api.extension;
import com.thingclips.smart.archer.api.cell.IArcherCell;
public final class CustomCell {
    private final String name;
    private final IArcherCell cell;
    public CustomCell(String name, IArcherCell cell) { this.name = name; this.cell = cell; }
    public String b() { return name; }
    public IArcherCell a() { return cell; }
}
