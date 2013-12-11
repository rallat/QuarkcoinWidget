package com.israelferrer.quarkcoinwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class PriceBroadcastReceiver extends BroadcastReceiver {

    private static final String CRYPTSY = "http://pubapi.cryptsy.com/api.php?method=singlemarketdata&marketid=71";
    private static final String BTER = "https://bter.com/api/1/ticker/qrk_btc";
    private static final String COINBASE = "https://coinbase.com/api/v1/prices/spot_rate?currency=";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
                views.setViewVisibility(R.id.loading, View.VISIBLE);
                views.setViewVisibility(R.id.quarkcoinImage, View.GONE);
                views.setViewVisibility(R.id.quarkValueInBtc, View.GONE);
                views.setViewVisibility(R.id.quarkValueInCurrency, View.GONE);
                appWidgetManager.updateAppWidget(appWidgetId, views);
                String currency=Prefs.getCurrency(context, appWidgetId);
                if("BTC".equals(currency)){
                     currency ="USD";
                }

                try {
                    String amount = getQuarkValue(context, appWidgetId);
                    String btcValue = getBTCValue(currency);
                    NumberFormat formatter = new DecimalFormat("#.00");
                    Prefs.setLastUpdate(context);
                    views.setTextViewText(R.id.quarkValueInBtc, "BTC" + amount);
                    views.setTextViewText(R.id.quarkValueInCurrency, currency + formatter.format((Float.valueOf(btcValue) * Float.valueOf(amount))));
                } catch (Exception e) {
                    e.printStackTrace();
                    long lastUpdate = Prefs.getLastUpdate(context);
                }
                views.setViewVisibility(R.id.loading, View.GONE);
                views.setViewVisibility(R.id.quarkcoinImage, View.VISIBLE);
                views.setViewVisibility(R.id.quarkValueInBtc, View.VISIBLE);
                views.setViewVisibility(R.id.quarkValueInCurrency, View.VISIBLE);
                appWidgetManager.updateAppWidget(appWidgetId, views);
                Intent priceUpdate = new Intent(context, PriceBroadcastReceiver.class);
                priceUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                PendingIntent pendingPriceUpdate = PendingIntent.getBroadcast(context, appWidgetId, priceUpdate, 0);
                views.setOnClickPendingIntent(R.id.bitcoinParent, pendingPriceUpdate);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }).start();
    }

    private String getQuarkValue(Context context, int widgetId) throws Exception {
        int provider = Prefs.getProvider(context, widgetId);
        String value = "0";
        if (provider == 0) {
            JSONObject obj = getAmount(CRYPTSY);
            value = obj.getJSONObject("return").getJSONObject("markets").getJSONObject("QRK").getString("lasttradeprice");
        } else if (provider == 1) {
            JSONObject obj = getAmount(BTER);
            value = obj.getString("last");
        }
        NumberFormat formatter = new DecimalFormat("#.000000");
        return formatter.format(Double.valueOf(value));
    }

    private String getBTCValue(String currency) throws Exception {
        JSONObject obj = getAmount(COINBASE + currency);
        return obj.getString("amount");
    }

    private JSONObject getAmount(String url) throws Exception {
        HttpGet get = new HttpGet(url);
        HttpClient client = new DefaultHttpClient();
        String result = client.execute(get, new BasicResponseHandler());
        return new JSONObject(result);
    }

}
