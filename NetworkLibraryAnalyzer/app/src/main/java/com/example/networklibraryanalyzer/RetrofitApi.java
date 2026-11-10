package com.example.networklibraryanalyzer;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HTTP;
import retrofit2.http.Url;

public interface RetrofitApi {

    @HTTP(method = "GET", hasBody = false)
    Call<ResponseBody> get(@Url String url);

    @HTTP(method = "POST", hasBody = true)
    Call<ResponseBody> post(@Url String url, @Body RequestBody body);

    @HTTP(method = "PUT", hasBody = true)
    Call<ResponseBody> put(@Url String url, @Body RequestBody body);

    @HTTP(method = "PATCH", hasBody = true)
    Call<ResponseBody> patch(@Url String url, @Body RequestBody body);

    @HTTP(method = "DELETE", hasBody = false)
    Call<ResponseBody> delete(@Url String url);

    @HTTP(method = "DELETE", hasBody = true)
    Call<ResponseBody> deleteWithBody(@Url String url, @Body RequestBody body);

    @HTTP(method = "HEAD", hasBody = false)
    Call<ResponseBody> head(@Url String url);

    @HTTP(method = "OPTIONS", hasBody = false)
    Call<ResponseBody> options(@Url String url);
}