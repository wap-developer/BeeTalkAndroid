package com.beeflirt.beetalk.utils;

public class Constants {
    // API Configuration
    // public static final String BASE_URL = "https://beetalk.xuaxin.org/api/"; // production
    public static final String BASE_URL = "http://192.168.2.57:8080/api/"; // local device/dev

    // Image/Upload Base URL (without /api/)
    // public static final String IMAGE_BASE_URL = "https://beetalk.xuaxin.org/"; // production
    public static final String IMAGE_BASE_URL = "http://192.168.2.57:8080/"; // local device/dev

    // SharedPreferences Keys
    public static final String PREF_NAME = "BeeTalkLite_Prefs";
    public static final String LEGACY_PREF_NAME = "TinderKW_Prefs"; // Old name, used for migration
    public static final String KEY_TOKEN = "jwt_token";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";

    // Timezone Configuration
    public static final String APP_TIMEZONE = "Asia/Jakarta";

}
