package com.example.weatherwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONException;
import org.json.JSONObject;

public class WidgetWeather extends AppWidgetProvider {
    public static String city_name = "Казань";
    private static final String SYNC_CLICKED = "automaticWidgetSyncButtonClick";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
        Log.d("widget_test", "updateAppWidget");
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("widget_test", "onUpdate");
        //Создаются объекты RemoteViews и ComponentName. RemoteViews используется для управления макетом виджета, а ComponentName представляет имя класса виджета.
        RemoteViews remoteViews;
        ComponentName watchWidget;

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
        watchWidget = new ComponentName(context, WidgetWeather.class);
        //выполняет назначение действия для виджета, которое будет выполнено при нажатии на элемент
        remoteViews.setOnClickPendingIntent(R.id.update_btn, getPendingSelfIntent(context, SYNC_CLICKED));
        appWidgetManager.updateAppWidget(watchWidget, remoteViews);

        WeatherData weatherdata = new WeatherData(context);

        for (int appWidgetId : appWidgetIds) {
            //Устанавливается видимость прелоадера
            remoteViews.setViewVisibility(R.id.loading_indicator, View.VISIBLE);
            //Устанавливается видимость содержимого виджета
            remoteViews.setViewVisibility(R.id.widget_content, View.GONE);
            //Обновляется виджет с помощью менеджера виджетов
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

            weatherdata.setOnDownloadedWeather(jsonString -> {
                setWeather(jsonString, context);
                // Скрываем прелоудер после обновления
                //Устанавливается слушатель для события загрузки данных о погоде. Когда данные загружены, выполняется код внутри лямбда-выражения.
                remoteViews.setViewVisibility(R.id.loading_indicator, View.GONE);
                remoteViews.setViewVisibility(R.id.widget_content, View.VISIBLE);
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
            });
            weatherdata.getWeather(city_name);
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (SYNC_CLICKED.equals(intent.getAction())) {
            WeatherData weatherdata = new WeatherData(context);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            RemoteViews remoteViews;
            ComponentName watchWidget;

            remoteViews = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
            watchWidget = new ComponentName(context, WidgetWeather.class);

            Log.d("widget_test", "Click");
            remoteViews.setViewVisibility(R.id.loading_indicator, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.widget_content, View.GONE);
            appWidgetManager.updateAppWidget(watchWidget, remoteViews);

            weatherdata.setOnDownloadedWeather(jsonString -> {
                setWeather(jsonString, context.getApplicationContext());
                remoteViews.setViewVisibility(R.id.loading_indicator, View.GONE);
                remoteViews.setViewVisibility(R.id.widget_content, View.VISIBLE);
                appWidgetManager.updateAppWidget(watchWidget, remoteViews);
            });

            weatherdata.getWeather(city_name);

            appWidgetManager.updateAppWidget(watchWidget, remoteViews);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Log.d("widget_test", "onEnabled");
    }

    @Override
    public void onDisabled(Context context) {
    }

    public void setWeather(String jsonString, Context context) throws JSONException {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
        ComponentName thisWidget = new ComponentName(context, WidgetWeather.class);

        Log.d("widget_test", "setWeather");

        JSONObject jsonObject = new JSONObject(jsonString);

        String city = jsonObject.getString("name");
        remoteViews.setTextViewText(R.id.widget_city, city);

        Log.d("widget_test", "city " + city);

        int temp_value = (int) Math.round(jsonObject.getJSONObject("main").getDouble("temp"));
        String temp = (temp_value < 0 ? "-" : "") + temp_value + "°C";
        remoteViews.setTextViewText(R.id.widget_temp, temp);

        String like = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");
        like = like.substring(0, 1).toUpperCase() + like.substring(1);
        remoteViews.setTextViewText(R.id.widget_like, like);

        String icon = jsonObject.getJSONArray("weather").getJSONObject(0).getString("icon");

        String iconUrl = "https://openweathermap.org/img/wn/" + icon + "@4x.png";

        Glide.with(context)
                .load(iconUrl)
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        remoteViews.setImageViewBitmap(R.id.widget_img, ((BitmapDrawable) resource).getBitmap());
                        appWidgetManager.updateAppWidget(thisWidget, remoteViews);
                    }
                });

        appWidgetManager.updateAppWidget(thisWidget, remoteViews);
    }
}
