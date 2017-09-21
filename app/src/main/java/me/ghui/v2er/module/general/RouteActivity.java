package me.ghui.v2er.module.general;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import me.ghui.v2er.general.Navigator;
import me.ghui.v2er.module.create.CreateTopicActivity;
import me.ghui.v2er.module.drawer.dailyhot.DailyHotActivity;
import me.ghui.v2er.module.home.MainActivity;
import me.ghui.v2er.network.UrlInterceptor;
import me.ghui.v2er.util.LightStatusBarUtils;

/**
 * Created by ghui on 30/06/2017.
 */

public class RouteActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        LightStatusBarUtils.setLightStatusBar(getWindow(), true);
        super.onCreate(savedInstanceState);
        route();
        finish();
    }

    private void route() {
        Uri data = getIntent().getData();
        if (data == null) {
            Navigator.from(this).to(MainActivity.class).start();
            return;
        }

        switch (data.getScheme()) {
            case "https":
            case "http":
                UrlInterceptor.openWapPage(data.getPath(), this);
                return;
            case "v2er":
                v2erRoute(data);
                return;
        }
    }

    private void v2erRoute(Uri data) {
        switch (data.getPath()) {
            case "/daily_hot":
                Navigator.from(this).to(DailyHotActivity.class).start();
                break;
            case "/create_topic":
                Navigator.from(this).to(CreateTopicActivity.class).start();
                break;
        }
    }
}