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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.germainz.identiconizer.Config;
import com.germainz.identiconizer.ContactInfo;
import com.germainz.identiconizer.identicons.Identicon;
import com.germainz.identiconizer.identicons.IdenticonFactory;
import com.germainz.identiconizer.identicons.IdenticonUtils;

import java.util.ArrayList;

/**
 * The contact-photo writing logic, shared by {@link IdenticonCreationService} (user-started
 * batches) and {@link NewContactsJobService} (automatic, for newly added contacts).
 * Must be called off the main thread.
 */
public class IdenticonCreator {

    public interface ProgressListener {
        void onContact(String name);
    }

    private final Context mContext;
    private final ContentResolver mResolver;
    private final ProgressListener mListener;
    private final Identicon mIdenticon;

    final ArrayList<ContactInfo> mInsertErrors = new ArrayList<>();
    final ArrayList<ContactInfo> mUpdateErrors = new ArrayList<>();

    public IdenticonCreator(Context context, ProgressListener listener) {
        mContext = context.getApplicationContext();
        mResolver = mContext.getContentResolver();
        mListener = listener;
        mIdenticon = IdenticonFactory.makeIdenticon(mContext);
    }

    public boolean hasErrors() {
        return !mInsertErrors.isEmpty() || !mUpdateErrors.isEmpty();
    }

    /**
     * @param updateExisting if false, only contacts without any photo get one. If true,
     *                       contacts that already have an identicon get a fresh one too
     *                       (useful after changing styles).
     */
    public void processContacts(boolean updateExisting) {
        Cursor cursor = getContacts();
        if (cursor == null) return;
        try {
            while (cursor.moveToNext()) {
                final int rawContactId = cursor.getInt(0);
                final String name = cursor.getString(1);
                final long photoId = cursor.getLong(2);
                if (TextUtils.isEmpty(name)) continue;
                final byte[] photo = photoId > 0 ? getContactPhotoBlob(photoId) : null;
                if (photoId <= 0 || photo == null
                        || (updateExisting && IdenticonUtils.isIdenticon(photo))) {
                    generateIdenticon(rawContactId, name);
                }
            }
        } finally {
            cursor.close();
        }
    }

    public void processContacts(ArrayList<ContactInfo> contactInfos) {
        for (ContactInfo contactInfo : contactInfos)
            generateIdenticon(contactInfo.nameRawContactId, contactInfo.contactName);
    }

    private Cursor getContacts() {
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        String[] projection = new String[]{
                "name_raw_contact_id",
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_ID
        };
        String selection = ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '1'";
        if (Config.getInstance(mContext).shouldIgnoreContactVisibility())
            selection = null;
        String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
        return mResolver.query(uri, projection, selection, null, sortOrder);
    }

    private byte[] getContactPhotoBlob(long photoId) {
        String[] projection = new String[]{ContactsContract.Data.DATA15};
        String where = ContactsContract.Data._ID + " = ? AND "
                + ContactsContract.Data.MIMETYPE + " = ?";
        String[] args = {String.valueOf(photoId),
                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE};
        Cursor cursor = mResolver.query(ContactsContract.Data.CONTENT_URI, projection, where, args, null);
        if (cursor == null) return null;
        try {
            return cursor.moveToFirst() ? cursor.getBlob(0) : null;
        } finally {
            cursor.close();
        }
    }

    private void generateIdenticon(int rawContactId, String name) {
        if (TextUtils.isEmpty(name)) return;
        if (mListener != null) mListener.onContact(name);
        final byte[] identiconImage = mIdenticon.generateIdenticonByteArray(name);
        if (identiconImage != null)
            setContactPhoto(identiconImage, rawContactId, name);
    }

    private void setContactPhoto(byte[] bytes, int rawContactId, String name) {
        final String selection = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND "
                + ContactsContract.Data.MIMETYPE + " = ?";
        final String[] selectionArgs = new String[]{
                String.valueOf(rawContactId),
                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE};

        boolean hasPhotoRow = false;
        Cursor cursor = mResolver.query(ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.Data._ID}, selection, selectionArgs, null);
        if (cursor != null) {
            hasPhotoRow = cursor.moveToFirst();
            cursor.close();
        }

        ContentValues values = new ContentValues();
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Data.IS_PRIMARY, 1);
        values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
        values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes);
        values.put("skip_processing", "skip_processing");
        // Not using applyBatch: large identicons (e.g. Spirograph at 720x720) can approach
        // the 1 MB binder transaction limit on their own.
        if (hasPhotoRow) {
            if (mResolver.update(ContactsContract.Data.CONTENT_URI, values, selection, selectionArgs) != 1)
                mUpdateErrors.add(new ContactInfo(rawContactId, name, bytes.length));
        } else {
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
            if (mResolver.insert(ContactsContract.Data.CONTENT_URI, values) == null)
                mInsertErrors.add(new ContactInfo(rawContactId, name, bytes.length));
        }
    }
}
