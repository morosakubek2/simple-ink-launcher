/*
 * Simple Ink Launcher
 * Copyright (C) 2019  Dmitriy Simbiriatin <dmitriy.simbiriatin@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ds.simple.ink.launcher;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.net.wifi.WifiManager;


import androidx.preference.PreferenceManager;

import org.ds.simple.ink.launcher.apps.ApplicationRepository;
import org.ds.simple.ink.launcher.apps.IconsThemeRepository;
import org.ds.simple.ink.launcher.settings.ApplicationSettings;
import org.ds.simple.ink.launcher.utils.IntentUtils;

import lombok.val;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.content.Intent.ACTION_PACKAGE_ADDED;
import static android.content.Intent.ACTION_PACKAGE_FULLY_REMOVED;

public class LauncherApplication extends Application {
    private IconsThemeRepository iconsThemeRepository;
    private ApplicationSettings applicationSettings;
    private ApplicationRepository applicationRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.launcher_preferences, false);

        iconsThemeRepository = IconsThemeRepository.from(this);
        applicationSettings = ApplicationSettings.from(this);
        applicationRepository = ApplicationRepository.from(this);

        applicationSettings.registerIconsThemeChangeListener(iconsThemeRepository);
        iconsThemeRepository.registerOnIconsThemeLoadListener(applicationRepository);

        val iconsThemePackage = applicationSettings.getIconsTheme();
        val currentIconsTheme = iconsThemeRepository.loadTheme(iconsThemePackage);
        applicationRepository.setIconsTheme(currentIconsTheme);

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(applicationSettings);

        registerApplicationEventsReceiver();
        startDefaultReaderAppIfEnabled();
    }

    public final IconsThemeRepository getIconsThemeRepository() {
        return iconsThemeRepository;
    }

    public final ApplicationSettings getApplicationSettings() {
        return applicationSettings;
    }

    public final ApplicationRepository getApplicationRepository() {
        return applicationRepository;
    }

    private void startDefaultReaderAppIfEnabled() {
        if (applicationSettings.isReaderApplicationAutoStartEnabled()) {
            val componentName = applicationSettings.getDefaultReaderApplicationComponentName();
            val intent = getPackageManager().getLaunchIntentForPackage(componentName.getPackageName());
            if (intent != null) {
                startActivity(intent);
            }
        }
    }

    private void registerApplicationEventsReceiver() {
        val packageFilter = new IntentFilter();
        packageFilter.addDataScheme("package");
        packageFilter.addAction(ACTION_PACKAGE_ADDED);
        packageFilter.addAction(ACTION_PACKAGE_FULLY_REMOVED);
        registerReceiver(new ApplicationEventsReceiver(), packageFilter);

        val batteryFilter = new IntentFilter();
        batteryFilter.addAction(ACTION_BATTERY_CHANGED);
        registerReceiver(new BatteryEventsReceiver(), batteryFilter);

        val wifiStateFilter = new IntentFilter();
        wifiStateFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(new WifiEventsReceiver(), wifiStateFilter);
    }

    private class ApplicationEventsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            applicationRepository.notifyAboutApplicationEvent();

            if (ACTION_PACKAGE_FULLY_REMOVED.equals(intent.getAction())) {
                val packageName = IntentUtils.packageNameFrom(intent.getDataString());
                applicationSettings.notifyApplicationRemoved(packageName);
            }
        }
    }

    private class BatteryEventsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            int pct = level * 100 / (int)scale;
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            applicationSettings.notifyBatteryLevelChanged(pct, isCharging);
        }
    }

    private class WifiEventsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            boolean isEnabled = false;

            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (info != null && info.isConnectedOrConnecting()) {
                isEnabled = true;
            }

            applicationSettings.notifyWifiStateChanged(isEnabled);
        }
    }
}