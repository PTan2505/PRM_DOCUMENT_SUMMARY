package com.example.prm_ai.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApiService {
    // ✅ Sử dụng gemini-2.5-flash (model mới nhất, tháng 10/2024)
    @POST("v1beta/models/gemini-2.5-pro:generateContent")
    Call<GeminiApiResponse> generateContent(
            @Query("key") String apiKey,
            @Body GeminiApiRequest request
    );

    // Các model khác:
    // gemini-2.5-pro:generateContent (mạnh nhất)
    // gemini-1.5-flash:generateContent (cũ hơn nhưng ổn định)
}