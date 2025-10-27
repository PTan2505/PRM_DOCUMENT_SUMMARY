package com.example.prm_ai.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApiService {
    @POST("v1beta/models/gemini-pro:generateContent")
    Call<GeminiApiResponse> generateContent(@Query("key") String apiKey, @Body GeminiApiRequest request);
}
