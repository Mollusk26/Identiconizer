/*
 * Original work Copyright (C) 2013 The ChameleonOS Open Source Project
 * Modified work Copyright (C) 2013-2014 GermainZ@xda-developers.com
 * Modified work Copyright (C) 2026 Identiconizer contributors
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

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.provider.ContactsContract;

import androidx.core.app.ServiceCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.germainz.identiconizer.ContactInfo;
import com.germainz.identiconizer.Notifications;
import com.germainz.identiconizer.R;

import java.util.ArrayList;

/**
 * Runs a user-started batch (all contacts, or a list picked in ContactsListActivity) as a
 * foreground service so it survives the user leaving the app. Always started while the app
 * is in the foreground, which Android 12+ requires for starting foreground services.
 */
@SuppressWarnings("deprecation") // IntentService: still functional and the simplest fit here.
public class IdenticonCreationService extends IntentService {
    private static final String TAG = "IdenticonCreationService";
    private static final int SERVICE_NOTIFICATION_ID = 8675309;

    public IdenticonCreationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        final String title = getString(R.string.identicons_creation_service_running_title);
        ServiceCompat.startForeground(this, SERVICE_NOTIFICATION_ID,
                Notifications.progress(this, title,
                        getString(R.string.identicons_creation_service_running_summary)),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);

        IdenticonCreator creator = new IdenticonCreator(this, name ->
                Notifications.update(this, SERVICE_NOTIFICATION_ID, title,
                        getString(R.string.identicons_creation_service_contact_summary, name)));

        if (intent.hasExtra("contactsList")) {
            ArrayList<ContactInfo> contactsList = intent.getParcelableArrayListExtra("contactsList");
            if (contactsList != null) creator.processContacts(contactsList);
        } else {
            creator.processContacts(intent.getBooleanExtra("updateExisting", true));
        }

        if (creator.hasErrors())
            Notifications.showErrors(this, creator.mInsertErrors, creator.mUpdateErrors);
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("CONTACTS_UPDATED"));
        getContentResolver().notifyChange(ContactsContract.Data.CONTENT_URI, null);
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
    }
}
