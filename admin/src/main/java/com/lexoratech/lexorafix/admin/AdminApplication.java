package com.lexoratech.lexorafix.admin;

import android.app.Application;
import com.lexoratech.lexorafix.admin.utils.ThemeUtils;

public class AdminApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Ensure Theme is evaluated immediately on app process creation
        // By default, this ensures MODE_NIGHT_FOLLOW_SYSTEM works accurately 
        // across process restarts from system settings changes.
        ThemeUtils.applyTheme(this);
    }
}
