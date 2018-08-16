package com.example.gryffindor.sos.data;

import android.provider.BaseColumns;

public final class SOSContract {
    public static final class usersEntry implements BaseColumns
    {
        public final static String TABLE_NAME="users";

        public final static String _ID =BaseColumns._ID;
        public final static String COLUMN_USER_ID="userID";
        public final static String COLUMN_USER_MESSAGE="message";

    }
    public static final class contactsEntry implements BaseColumns
    {
        public final static String TABLE_NAME="contacts";

        public final static String _ID =BaseColumns._ID;
        public final static String COLUMN_CONTACT_PHONE ="phone";
        public final static String COLUMN_CONTACT_NAME ="name";
        public final static String COLUMN_CONTACT_REFERENCE_TO_USER ="idReference";

    }
}
