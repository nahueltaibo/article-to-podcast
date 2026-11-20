package com.nahueltaibo.pagecast;

import android.content.Context;
import android.content.pm.PackageManager;

final class BuildConfigHolder {
    private BuildConfigHolder() {}

    static String versionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }
}
