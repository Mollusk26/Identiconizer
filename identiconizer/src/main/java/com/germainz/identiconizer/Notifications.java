/*
 * Copyright (C) 2026 Identiconizer contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.germainz.identiconizer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

/** Notification channels and builders shared by the services. */
public final class Notifications {

    public static final String CHANNEL_PROGRESS = "progress";
    public static final String CHANNEL_ERRORS = "errors";
    public static final int ERROR_NOTIFICATION_ID = 8675310;

    private Notifications() {
    }

    public static void ensureChannels(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        nm.createNotificationChannel(new NotificationChannel(CHANNEL_PROGRESS,
                context.getString(R.string.channel_progress), NotificationManager.IMPORTANCE_LOW));
        nm.createNotificationChannel(new NotificationChannel(CHANNEL_ERRORS,
                context.getString(R.string.channel_errors), NotificationManager.IMPORTANCE_DEFAULT));
    }

    public static Notification progress(Context context, String title, String text) {
        ensureChannels(context);
        Intent intent = new Intent(context, IdenticonsSettings.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(context, CHANNEL_PROGRESS)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_settings_identicons)
                .setContentIntent(contentIntent)
                .build();
    }

    public static void update(Context context, int id, String title, String text) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(id, progress(context, title, text));
    }

    public static void showErrors(Context context, ArrayList<ContactInfo> insertErrors,
                                  ArrayList<ContactInfo> updateErrors) {
        ensureChannels(context);
        Intent intent = new Intent(context, ErrorsListActivity.class);
        intent.putParcelableArrayListExtra("insertErrors", insertErrors);
        intent.putParcelableArrayListExtra("updateErrors", updateErrors);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String contentText = context.getString(R.string.sql_error_notification_text,
                insertErrors.size() + updateErrors.size());
        Notification notice = new NotificationCompat.Builder(context, CHANNEL_ERRORS)
                .setSmallIcon(R.drawable.ic_settings_identicons)
                .setContentTitle(context.getString(R.string.sql_error_notification_title))
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setAutoCancel(true)
                .build();
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(ERROR_NOTIFICATION_ID, notice);
    }
}
