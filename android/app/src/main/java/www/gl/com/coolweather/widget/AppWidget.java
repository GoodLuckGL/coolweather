package www.gl.com.coolweather.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import www.gl.com.coolweather.MainActivity;
import www.gl.com.coolweather.R;
import www.gl.com.coolweather.bean.Daily;
import www.gl.com.coolweather.bean.District;
import www.gl.com.coolweather.bean.Realtime;
import www.gl.com.coolweather.bean.Skycon;
import www.gl.com.coolweather.bean.Temperature;
import www.gl.com.coolweather.bean.WeatherBean;
import www.gl.com.coolweather.net.ApiService;
import www.gl.com.coolweather.utils.DateUtils;
import www.gl.com.coolweather.utils.TranslationUtils;


public class AppWidget extends AppWidgetProvider {

    private static final String ACTION_UPDATE = "action_update";

    private final int NO_LOCATION = 0x01;

    private final int UPDATING = 0x02;

    private final int UPDATE_SUCCESS = 0x03;

    private final int UPDATE_FAIL = 0x04;

    public static int getWeatherIcon(String weather) {
        int iconId;
        switch (weather) {
            case "CLEAR_DAY":
            case "CLEAR_NIGHT": {
                iconId = R.drawable.sunny_ic;
            }
            break;
            case "PARTLY_CLOUDY_DAY":
            case "PARTLY_CLOUDY_NIGHT":
                iconId = R.drawable.cloud_ic;
                break;
            case "CLOUDY":
                iconId = R.drawable.nosun_ic;
                break;
            case "WIND":
                iconId = R.drawable.wind_ic;
                break;
            case "HAZE":
                iconId = R.drawable.haze_ic;
                break;
            case "RAIN":
                iconId = R.drawable.rain_ic;
                break;
            case "SNOW":
                iconId = R.drawable.snow_ic;
                break;
            default:
                iconId = R.drawable.sunny_ic;
                break;
        }
        return iconId;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        if (ACTION_UPDATE.equals(action)) {
            getWeatherData(context);
        }
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        getWeatherData(context);
    }

    void getWeatherData(final Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE);
        String locationDistrictBean = sharedPreferences.getString("flutter.location_district", "");
        final District district = new Gson().fromJson(locationDistrictBean, District.class);
        if (district == null) {
            // 显示没有定位遮罩
            updateAppWidget(NO_LOCATION, context, "未知", null);
            return;
        }

        // 显示正在刷新遮罩
        updateAppWidget(UPDATING, context, district.name, null);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.caiyunapp.com/v2/TwsDo9aQUYewFhV8/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);
        Call<WeatherBean> call = apiService.weather(district.longitude, district.latitude);
        call.enqueue(new Callback<WeatherBean>() {
            @Override
            public void onResponse(@NonNull Call<WeatherBean> call, @NonNull Response<WeatherBean> response) {
                WeatherBean weatherBean = response.body();
                updateAppWidget(UPDATE_SUCCESS, context, district.name, weatherBean);
            }

            @Override
            public void onFailure(@NonNull Call<WeatherBean> call, @NonNull Throwable t) {
                updateAppWidget(UPDATE_FAIL, context, district.name, null);
            }
        });
    }

    void updateAppWidget(int status, Context context, String district, WeatherBean weatherBean) {
        ComponentName componentName = new ComponentName(context, AppWidget.class);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget);

        // 打开APP首页的Intent
        Intent launchIntent = new Intent(context, MainActivity.class);
        PendingIntent launchPendingIntent = PendingIntent.getActivity(context, 0, launchIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        // 点击刷新的Intent
        Intent updateIntent = new Intent(ACTION_UPDATE);
        updateIntent.setClass(context, AppWidget.class);
        PendingIntent updatePendingIntent = PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        if (status == NO_LOCATION) {
            remoteViews.setTextViewText(R.id.widget_update_mask_tv, "请点击打开APP开启定位");
            remoteViews.setTextViewText(R.id.widget_district_tv, district);
            remoteViews.setViewVisibility(R.id.widget_update_mask_ll, View.VISIBLE);
            remoteViews.setOnClickPendingIntent(R.id.widget_update_mask_ll, launchPendingIntent);
        } else if (status == UPDATING) {
//            remoteViews.setTextViewText(R.id.widget_update_mask_tv, "正在更新");
            remoteViews.setViewVisibility(R.id.widget_update_mask_ll, View.VISIBLE);
        } else if (status == UPDATE_SUCCESS) {
            showAppWidgetData(remoteViews, district, weatherBean);
            remoteViews.setOnClickPendingIntent(R.id.widget_fl, launchPendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.widget_update_fl, updatePendingIntent);
        } else if (status == UPDATE_FAIL) {
            remoteViews.setViewVisibility(R.id.widget_update_mask_ll, View.GONE);
            remoteViews.setTextViewText(R.id.widget_update_time_tv, "更新失败");
            remoteViews.setOnClickPendingIntent(R.id.widget_update_fl, updatePendingIntent);
        }
        AppWidgetManager.getInstance(context).updateAppWidget(componentName, remoteViews);
    }

    private void showAppWidgetData(RemoteViews remoteViews, String district, WeatherBean weatherBean) {
        Realtime realtime = weatherBean.result.realtime;
        Daily daily = weatherBean.result.daily;

        remoteViews.setTextViewText(R.id.widget_district_tv, district);
        remoteViews.setTextViewText(R.id.widget_weather_tv, TranslationUtils.getWeatherDesc(realtime.skycon));
        remoteViews.setTextViewText(R.id.widget_temperature_tv, toInt(realtime.temperature) + "°");

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.CHINA);
        String updateDate = sdf.format(new Date(weatherBean.server_time * 1000));
        remoteViews.setTextViewText(R.id.widget_update_time_tv, updateDate + " 更新");

        remoteViews.setTextViewText(R.id.widget_max_min_tv, toInt(daily.temperature.get(0).max)
                + " / " + toInt(daily.temperature.get(0).min) + "°");

        sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

        Skycon skycon1 = daily.skycon.get(1);
        Skycon skycon2 = daily.skycon.get(2);
        Skycon skycon3 = daily.skycon.get(3);

        Temperature temperature1 = daily.temperature.get(1);
        Temperature temperature2 = daily.temperature.get(2);
        Temperature temperature3 = daily.temperature.get(3);

        try {
            Date date2 = sdf.parse(skycon2.date);
            Date date3 = sdf.parse(skycon3.date);
            Calendar calendar = Calendar.getInstance();
            if (date2 != null && date3 != null) {
                remoteViews.setTextViewText(R.id.widget_forecast_day_1_tv, "明天");
                remoteViews.setImageViewResource(R.id.widget_forecast_icon_1_iv, getWeatherIcon(skycon1.value));
                remoteViews.setTextViewText(R.id.widget_forecast_temperature_1_iv, toInt(temperature1.max)
                        + " / " + toInt(temperature1.min) + "°");

                calendar.setTime(date2);
                remoteViews.setTextViewText(R.id.widget_forecast_day_2_tv, DateUtils.getWeekday(
                        calendar.get(Calendar.DAY_OF_WEEK) - 1));
                remoteViews.setImageViewResource(R.id.widget_forecast_icon_2_iv, getWeatherIcon(skycon2.value));
                remoteViews.setTextViewText(R.id.widget_forecast_temperature_2_iv, toInt(temperature2.max)
                        + " / " + toInt(temperature2.min) + "°");

                calendar.setTime(date3);
                remoteViews.setTextViewText(R.id.widget_forecast_day_3_tv, DateUtils.getWeekday(calendar.get(
                        Calendar.DAY_OF_WEEK) - 1));
                remoteViews.setImageViewResource(R.id.widget_forecast_icon_3_iv, getWeatherIcon(skycon3.value));
                remoteViews.setTextViewText(R.id.widget_forecast_temperature_3_iv, toInt(temperature3.max)
                        + " / " + toInt(temperature3.min) + "°");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        remoteViews.setViewVisibility(R.id.widget_update_mask_ll, View.GONE);
    }

    int toInt(double value) {
        return (int) (value + 0.5);
    }
}
