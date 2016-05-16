package com.tnpxu.droneeyes.api;

import com.squareup.okhttp.RequestBody;

import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;

/**
 * Created by tnpxu on 11/12/2558.
 */
public interface SendingPhotoApi {
    @Multipart
    @POST("/api/upload/register")
    Call<ResData> uploadRegister(
            @Part("drone1\"; filename=\"drone1.jpg\" ") RequestBody file,
            @Part("description") String description,
            @Part("token") String token,
            @Part("hello") SendingPhotoData data
            );

    @Multipart
    @POST("/api/upload/service")
    Call<ResData> uploadService(
            @Part("drone1\"; filename=\"drone1.jpg\" ") RequestBody file,
            @Part("description") String description,
            @Part("token") String token,
            @Part("hello") SendingPhotoData data
            );
}

/*** (( SENDING PATTERN EXAMPLE))
 final File photo = new File(mDataPath);

 final ProgressDialog pDialog;
 pDialog = ProgressDialog.show(LabActivity.this, "Sending", "Please wait");

 String description = "Hello, Team alpha this is Drone eiei";
 SendingPhotoApi service = ServiceGenerator.createService(SendingPhotoApi.class);
 RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), photo);

 Call<String> call = service.upload(requestBody, description);
 call.enqueue(new Callback<String>() {
    @Override
    public void onResponse(Response<String> response, Retrofit retrofit) {
    Log.v("Upload", "success");
    pDialog.dismiss();
    CameraActivity.isChecking = false;
    finish();
    }

    @Override
    public void onFailure(Throwable t) {
    Log.e("Upload", t.getMessage());
    pDialog.dismiss();
    CameraActivity.isChecking = false;
    finish();
    }
 });
 ***/
