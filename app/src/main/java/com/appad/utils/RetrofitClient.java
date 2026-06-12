package com.appad.utils;

import com.appad.services.ApiService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.Map;

public class RetrofitClient {
    // IP MÁY ẢO (Emulator) - Mặc định
    // private static final String BASE_URL = "http://10.0.2.2:5000/"; 
    
    // CHẠY TRÊN CLOUD (Railway) - Thay bằng link bạn vừa lấy ở bước trên
    private static final String BASE_URL = "https://backend-production-c44f.up.railway.app/"; 
    
    // IP TEST LOCAL (Comment lại khi dùng Railway)
    // private static final String BASE_URL_LOCAL = "http://192.168.1.xxx:5000/"; 

    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        System.out.println("RetrofitClient: Calling URL: " + original.url());
                        SessionManager sm = SessionManager.getInstance(com.appad.MusicApplication.getInstance());
                        String token = (sm != null) ? sm.getToken() : null;
                        
                        if (token != null && !token.isEmpty()) {
                            System.out.println("RetrofitClient: Adding Authorization header with token: " + token.substring(0, Math.min(token.length(), 10)) + "...");
                            Request request = original.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .method(original.method(), original.body())
                                    .build();
                            return chain.proceed(request);
                        } else {
                            System.out.println("RetrofitClient: No token found in SessionManager");
                        }
                        
                        return chain.proceed(original);
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
