package androidx.recyclerview.widget;
import android.view.View;
public class RecyclerView {
    public abstract static class v {
        public final View itemView;
        public v(View view) { itemView = view; }
    }
}
