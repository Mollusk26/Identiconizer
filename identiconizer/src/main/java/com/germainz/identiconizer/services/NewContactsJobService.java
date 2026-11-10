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

package com.germainz.identiconizer.services;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.germainz.identiconizer.Config;
import com.germainz.identiconizer.Notifications;

/**
 * Replaces the old always-running ContactsObserverService, which Android 8+ kills in the
 * background. The system wakes this job when the contacts database changes; if a contact
 * with a higher ID than we've seen before exists, contacts without a photo get an identicon.
 * <p>
 * Content-URI jobs fire once, so the job reschedules itself after each run. They also can't
 * be persisted across reboots, so BootCompletedReceiver schedules it again at boot.
 */
public class NewContactsJobService extends JobService {

    private static final int JOB_ID = 4242;

    public static void schedule(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js == null) return;
        JobInfo job = new JobInfo.Builder(JOB_ID,
                new ComponentName(context, NewContactsJobService.class))
                .addTriggerContentUri(new JobInfo.TriggerContentUri(
                        ContactsContract.Contacts.CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                // Wait for changes to settle. Gives sync adapters (e.g. DAVx5) time to set a
                // contact's real photo before we'd add an identicon. See upstream issue #13.
                .setTriggerContentUpdateDelay(5_000)
                .setTriggerContentMaxDelay(60_000)
                .build();
        js.schedule(job);
    }

    public static void cancel(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js != null) js.cancel(JOB_ID);
    }

    /** Remembers the current highest contact ID so existing contacts aren't treated as new. */
    public static void resetBaseline(Context context) {
        Config.getInstance(context).setMaxContactID(queryMaxContactId(context));
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        final Config config = Config.getInstance(this);
        if (!config.isEnabled()) return false;
        if (checkSelfPermission(Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        new Thread(() -> {
            try {
                int maxId = queryMaxContactId(this);
                if (maxId > config.getMaxContactID()) {
                    IdenticonCreator creator = new IdenticonCreator(this, null);
                    creator.processContacts(false);
                    if (creator.hasErrors())
                        Notifications.showErrors(this, creator.mInsertErrors, creator.mUpdateErrors);
                }
                if (maxId != config.getMaxContactID())
                    config.setMaxContactID(maxId);
            } finally {
                if (config.isEnabled()) schedule(this);
                jobFinished(params, false);
            }
        }, "NewContactsJob").start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // Interrupted (e.g. quota). Reschedule so we still catch the change later.
        if (Config.getInstance(this).isEnabled()) schedule(this);
        return false;
    }

    private static int queryMaxContactId(Context context) {
        Cursor cursor;
        try {
            cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts._ID}, null, null,
                    ContactsContract.Contacts._ID + " DESC LIMIT 1");
        } catch (SecurityException e) {
            return 0; // contacts permission not granted (yet)
        }
        if (cursor == null) return 0;
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }
}
