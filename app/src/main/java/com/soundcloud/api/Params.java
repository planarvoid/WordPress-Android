package com.soundcloud.api;

/**
 * Request parameters for various objects.
 */
public interface Params {
    /**
     * see <a href="http://developers.soundcloud.com/docs/api/tracks">developers.soundcloud.com/docs/api/tracks</a>
     */
    interface Track {
        String PUBLIC = "public";
        String PRIVATE = "private";
    }

    /**
     * see <a href="http://developers.soundcloud.com/docs/api/users">developers.soundcloud.com/docs/api/users</a>
     */
    interface User {
        String NAME = "user[username]";
        String FULLNAME = "user[full_name]";
        String DESCRIPTION = "user[description]";
        String CITY = "user[city]";
        String PERMALINK = "user[permalink]";
        String DISCOGS_NAME = "user[discogs_name]";
        String MYSPACE_NAME = "user[myspace_name]";
        String WEBSITE = "user[website]";
        String WEBSITE_TITLE = "user[website_title]";
        String EMAIL = "user[email]";
        String PASSWORD = "user[password]";
        String PASSWORD_CONFIRMATION = "user[password_confirmation]";
        String TERMS_OF_USE = "user[terms_of_use]";
        String AVATAR = "user[avatar_data]";
        String GENDER = "user[gender]";
        String CUSTOM_GENDER = "user[custom_gender]";
        String DATE_OF_BIRTH_MONTH = "user[date_of_birth][month]";
        String DATE_OF_BIRTH_YEAR = "user[date_of_birth][year]";
    }

    /**
     * see <a href="http://developers.soundcloud.com/docs/api/comments">developers.soundcloud.com/docs/api/comments</a>
     */
    interface Comment {
        String BODY = "comment[body]";
        String TIMESTAMP = "comment[timestamp]";
        String REPLY_TO = "comment[reply_to]";
    }
}
