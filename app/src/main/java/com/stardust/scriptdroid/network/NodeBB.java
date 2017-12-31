package com.stardust.scriptdroid.network;

import android.content.Context;
import android.util.Log;

import com.google.gson.GsonBuilder;
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.stardust.scriptdroid.R;
import com.stardust.scriptdroid.network.api.ConfigApi;
import com.stardust.scriptdroid.network.entity.config.Config;
import com.stardust.scriptdroid.network.util.WebkitCookieManagerProxy;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Stardust on 2017/9/20.
 */

public class NodeBB {

    public static final String BASE_URL = "http://www.autojs.org/";
    private static final NodeBB sInstance = new NodeBB();
    private static final String LOG_TAG = "NodeBB";
    private Config mConfig;


    private Retrofit mRetrofit;

    NodeBB() {
        mRetrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder()
                        .setLenient()
                        .create()))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(new OkHttpClient.Builder()
                        .cookieJar(new WebkitCookieManagerProxy())
                        .build())
                .build();
    }

    public static NodeBB getInstance() {
        return sInstance;
    }

    public Retrofit getRetrofit() {
        return mRetrofit;
    }

    public Observable<Config> getConfig() {
        if (mConfig == null) {
            return mRetrofit.create(ConfigApi.class)
                    .getConfig()
                    .doOnNext(config -> mConfig = config);
        }
        return Observable.just(mConfig);

    }

    public void invalidateConfig() {
        mConfig = null;
    }

    public static String getErrorMessage(Throwable e, Context context, String defaultMsg) {
        if (!(e instanceof HttpException)) {
            return defaultMsg;
        }
        HttpException httpException = (HttpException) e;
        ResponseBody body = httpException.response().errorBody();
        if (body == null)
            return defaultMsg;
        try {
            String errorMessage = getErrorMessage(context, httpException, body.string());
            return errorMessage == null ? defaultMsg : errorMessage;
        } catch (IOException e1) {
            e1.printStackTrace();
            return defaultMsg;
        }
    }

    private static String getErrorMessage(Context context, HttpException error, String errorBody) {
        if (errorBody == null)
            return null;
        if (errorBody.contains("invalid-login-credentials")) {
            return context.getString(R.string.nodebb_error_invalid_login_credentials);
        }
        if (errorBody.contains("change_password_error_match")) {
            return context.getString(R.string.nodebb_error_change_password_error_match);

        }
        if (errorBody.contains("change_password_error_length")) {
            return context.getString(R.string.nodebb_error_change_password_error_length);
        }
        if (errorBody.contains("email-taken")) {
            return context.getString(R.string.nodebb_error_email_taken);
        }
        if (error.code() == 403) {
            return context.getString(R.string.nodebb_error_forbidden);
        }
        Log.d(LOG_TAG, "unknown error: " + errorBody, error);
        return null;
    }

    public static String url(String relativePath) {
        return BASE_URL + relativePath;
    }

    public static CharSequence getErrorMessage(Throwable error, Context context, int defaultMsg) {
        return getErrorMessage(error, context, context.getString(defaultMsg));
    }
}