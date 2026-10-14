package com.alexeygrigorev.applist;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Home screen replacement: just an alphabetical list of every launchable app.
public class MainActivity extends Activity {
    private final List<ResolveInfo> apps = new ArrayList<>();
    private Adapter adapter;
    private ListView list;

    private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reload();
        }
    };

    // Bring the list back to the front when the screen goes off, so waking
    // the phone always lands on the list instead of the last-used app.
    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent home = new Intent(MainActivity.this, MainActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(home);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        list = new ListView(this);
        list.setBackgroundColor(0xFF000000);
        list.setFitsSystemWindows(true);
        adapter = new Adapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            ResolveInfo ri = apps.get(position);
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        });
        setContentView(list);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(packagesChanged, f);
        registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        reload();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packagesChanged);
        unregisterReceiver(screenOff);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        list.setSelection(0);
    }

    @Override
    public void onBackPressed() {
        // Stay on the list.
    }

    private void reload() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(main, 0);
        found.removeIf(ri -> ri.activityInfo.packageName.equals(getPackageName()));
        Collections.sort(found, new ResolveInfo.DisplayNameComparator(pm));
        apps.clear();
        apps.addAll(found);
        adapter.notifyDataSetChanged();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private class Adapter extends BaseAdapter {
        @Override public int getCount() { return apps.size(); }
        @Override public Object getItem(int p) { return apps.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int position, View convert, ViewGroup parent) {
            LinearLayout row;
            if (convert == null) {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(16), dp(10), dp(16), dp(10));
                ImageView icon = new ImageView(MainActivity.this);
                row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
                TextView label = new TextView(MainActivity.this);
                label.setTextColor(0xFFFFFFFF);
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                label.setPadding(dp(16), 0, 0, 0);
                row.addView(label);
            } else {
                row = (LinearLayout) convert;
            }
            ResolveInfo ri = apps.get(position);
            PackageManager pm = getPackageManager();
            ((ImageView) row.getChildAt(0)).setImageDrawable(ri.loadIcon(pm));
            ((TextView) row.getChildAt(1)).setText(ri.loadLabel(pm));
            return row;
        }
    }
}
