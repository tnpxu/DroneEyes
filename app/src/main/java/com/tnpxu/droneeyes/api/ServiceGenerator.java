package com.tnpxu.droneeyes.api;

import com.squareup.okhttp.OkHttpClient;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

/**
 * Created by tnpxu on 11/12/2558.
 */
public class ServiceGenerator {

    public static final String API_BASE_URL = "http://parkingserver.cloudapp.net:3000";
    //public static final String API_BASE_URL = "http://192.168.43.185:3000";

    private static OkHttpClient httpClient = new OkHttpClient();
    private static Retrofit.Builder builder =
            new Retrofit.Builder()
                    .baseUrl(API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create());

    public static <S> S createService(Class<S> serviceClass) {
        Retrofit retrofit = builder.client(httpClient).build();
        return retrofit.create(serviceClass);
    }
}
