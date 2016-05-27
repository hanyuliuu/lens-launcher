package nickrout.lenslauncher.adapter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.makeramen.dragsortadapter.DragSortAdapter;
import com.makeramen.dragsortadapter.NoForegroundShadowBuilder;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import nickrout.lenslauncher.R;
import nickrout.lenslauncher.model.App;
import nickrout.lenslauncher.model.AppPersistent;

/**
 * Created by rish on 26/5/16.
 */
public class ArrangerDragDropAdapter extends DragSortAdapter<ArrangerDragDropAdapter.MainViewHolder> {

    public static final String TAG = "ArrangerDragDropAdapter";
    private final List<App> appData;
    private RecyclerView mRecyclerView;
    private Context mContext;

    public ArrangerDragDropAdapter(Context mContext, RecyclerView recyclerView, List<App> appData) {
        super(recyclerView);
        this.mContext = mContext;
        this.appData = appData;
        this.mRecyclerView = recyclerView;
    }

    public App getItemForPosition(int position) {
        return appData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return appData.get(position).getID();
    }

    @Override
    public int getPositionForId(long id) {
        for (int i = 0; i < appData.size(); i++)
            if (appData.get(i).getID() == ((int) id))
                return i;
        return -1;
    }

    @Override
    public boolean move(int fromPosition, int toPosition) {
        appData.add(toPosition, appData.remove(fromPosition));
        return true;
    }

    @Override
    public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.element_app_arranger, parent, false);
        final MainViewHolder holder = new MainViewHolder(mContext, ArrangerDragDropAdapter.this, view);
        view.setOnLongClickListener(holder);
        holder.setOnClickListeners();
        return holder;
    }

    @Override
    public void onBindViewHolder(final MainViewHolder holder, final int position) {
        App app = getItemForPosition(position);

        // NOTE: check for getDraggingId() match to set an "invisible space" while dragging
        holder.mContainer.setVisibility(getDraggingId() == getPositionForId(position) ? View.INVISIBLE : View.VISIBLE);
        holder.mContainer.postInvalidate();

        holder.setAppElement(app);
    }

    @Override
    public long getDraggingId() {
        return super.getDraggingId();
    }

    @Override
    public int getItemCount() {
        return appData.size();
    }

    @Override
    public void onDrop() {
        super.onDrop();
    }

    public List<App> getAppData() {
        return appData;
    }

    public static class MainViewHolder extends DragSortAdapter.ViewHolder implements View.OnLongClickListener, PopupMenu.OnMenuItemClickListener {

        @Bind(R.id.element_app_container)
        CardView mContainer;

        @Bind(R.id.element_app_label)
        TextView mLabel;

        @Bind(R.id.element_app_icon)
        ImageView mIcon;

        @Bind(R.id.element_app_hide)
        ImageView mToggleAppVisibility;

        @Bind(R.id.element_app_menu)
        ImageView mMenu;

        private App mApp;
        private Context mContext;

        public MainViewHolder(Context context, final ArrangerDragDropAdapter adapter, View itemView) {
            super(adapter, itemView);
            ButterKnife.bind(this, itemView);
            this.mContext = context;
        }

        @Override
        public boolean onLongClick(@NonNull View v) {
            startDrag();
            return true;
        }

        @Override
        public View.DragShadowBuilder getShadowBuilder(View itemView, Point touchPoint) {
            return new NoForegroundShadowBuilder(itemView, touchPoint);
        }

        public void setAppElement(App app) {
            this.mApp = app;
            mLabel.setText(mApp.getLabel());
            mIcon.setImageBitmap(mApp.getIcon());
            boolean isAppVisible = AppPersistent.getAppVisibilityForPackage(mApp.getPackageName().toString());
            if (isAppVisible) {
                mToggleAppVisibility.setImageResource(R.drawable.ic_visible);
            } else {
                mToggleAppVisibility.setImageResource(R.drawable.ic_invisible);
            }
            mContainer.postInvalidate();
        }

        public void toggleAppVisibility(App app) {
            boolean isAppVisible = AppPersistent.getAppVisibilityForPackage(app.getPackageName().toString());
            AppPersistent.setAppVisibilityForPackage(app.getPackageName().toString(), !isAppVisible);
            if (isAppVisible) {
                Snackbar.make(mContainer, app.getLabel() + " is now hidden", Snackbar.LENGTH_LONG).show();
                mToggleAppVisibility.setImageResource(R.drawable.ic_invisible);
            } else {
                Snackbar.make(mContainer, app.getLabel() + " is now visible", Snackbar.LENGTH_LONG).show();
                mToggleAppVisibility.setImageResource(R.drawable.ic_visible);
            }
        }

        public void setOnClickListeners() {
            mToggleAppVisibility.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    printAllPersistent();
                    if (mApp != null)
                        toggleAppVisibility(mApp);
                    else
                        Snackbar.make(mContainer, "Error in Opening App", Snackbar.LENGTH_LONG).show();
                }
            });

            mMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popupMenu = new PopupMenu(mContext, view);
                    popupMenu.setOnMenuItemClickListener(MainViewHolder.this);
                    popupMenu.inflate(R.menu.menu_app_arranger);
                    popupMenu.show();
                }
            });
        }

        public void printAllPersistent() {
            for (AppPersistent appPersistent : AppPersistent.listAll(AppPersistent.class)) {
                if (!appPersistent.isAppVisible())
                    Log.d(TAG, appPersistent.toString());
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_item_uninstall:
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + mApp.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                        mContext.startActivity(intent);
                    }
                    return true;
            }
            return false;
        }
    }
}