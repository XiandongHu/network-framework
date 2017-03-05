package com.example.huxiandong.network.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import okhttp3.Cookie;

/**
 * Created by huxiandong
 * on 17/3/4.
 */

class CacheCookie {

    static List<CacheCookie> decorateCookies(Collection<Cookie> cookies) {
        List<CacheCookie> cacheCookies = new ArrayList<>(cookies.size());
        for (Cookie cookie : cookies) {
            cacheCookies.add(new CacheCookie(cookie));
        }
        return cacheCookies;
    }

    private Cookie mCookie;

    private CacheCookie(Cookie cookie) {
        mCookie = cookie;
    }

    Cookie getCookie() {
        return mCookie;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CacheCookie)) {
            return false;
        }

        CacheCookie that = (CacheCookie) other;
        return that.mCookie.name().equals(mCookie.name())
                && that.mCookie.domain().equals(mCookie.domain())
                && that.mCookie.path().equals(mCookie.path())
                && that.mCookie.secure() == mCookie.secure()
                && that.mCookie.hostOnly() == mCookie.hostOnly();
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + mCookie.name().hashCode();
        hash = 31 * hash + mCookie.domain().hashCode();
        hash = 31 * hash + mCookie.path().hashCode();
        hash = 31 * hash + (mCookie.secure() ? 0 : 1);
        hash = 31 * hash + (mCookie.hostOnly() ? 0 : 1);
        return hash;
    }

}
