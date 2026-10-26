package com.thingclips.smart.archer.api.bean;
import java.util.List;
public class ArcherConfigGroupData {
    private String name;
    private List<Items> items;
    private Header header;
    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
    public void setItems(List<Items> items) { this.items = items; }
    public List<Items> getItems() { return items; }
    public void setHeader(Header header) { this.header = header; }
    public Header getHeader() { return header; }
    public static class Items {
        private String cellType;
        public void setCellType(String type) { cellType = type; }
        public String getCellType() { return cellType; }
    }
    public static class Header {
        private String cellType;
        public void setCellType(String type) { cellType = type; }
        public String getCellType() { return cellType; }
    }
}
