/*
 * TapCall - a simple NFC tap-to-dial tool for family use
 * Copyright (C) 2026 TapCall contributors
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.family.tapcall;

import android.content.Context;
import android.content.SharedPreferences;

/** 保存号码与写卡模式 */
public final class Prefs {
    private static final String NAME = "tapcall";
    private static final String KEY_NUMBER = "number";
    private static final String KEY_MODE = "mode";
    private static final String KEY_LAST_WRITE = "lastWrite";
    private static final String KEY_DIAL_MODE = "dialMode";
    private static final String KEY_AUTO_STANDBY = "autoStandby";
    private static final String KEY_SKIP_AUTO = "skipAuto";
    private static final String KEY_PRIVACY = "privacyAccepted";
    private static final String KEY_PERM_ASKED = "permAsked";

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String getNumber(Context c) {
        return sp(c).getString(KEY_NUMBER, "");
    }

    public static void setNumber(Context c, String v) {
        sp(c).edit().putString(KEY_NUMBER, v).apply();
    }

    public static int getMode(Context c) {
        return sp(c).getInt(KEY_MODE, TapCall.MODE_FAMILY);
    }

    public static void setMode(Context c, int v) {
        sp(c).edit().putInt(KEY_MODE, v).apply();
    }

    /** 上次写卡完成的时间：写完后卡片通常还贴在写入手机上，用它做冷却，避免写入手机自己拨号 */
    public static long getLastWriteAt(Context c) {
        return sp(c).getLong(KEY_LAST_WRITE, 0L);
    }

    public static void setLastWriteAt(Context c, long v) {
        sp(c).edit().putLong(KEY_LAST_WRITE, v).apply();
    }

    /** 拨号方式：0=自动(逐个尝试) 1=Telecom.placeCall 2=ACTION_CALL 3=系统拨号盘 */
    public static int getDialMode(Context c) {
        return sp(c).getInt(KEY_DIAL_MODE, 0);
    }

    public static void setDialMode(Context c, int v) {
        sp(c).edit().putInt(KEY_DIAL_MODE, v).apply();
    }

    /** 打开 App 时是否自动进入待机模式（老人机建议开） */
    public static boolean isAutoStandby(Context c) {
        return sp(c).getBoolean(KEY_AUTO_STANDBY, true);
    }

    public static void setAutoStandby(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_AUTO_STANDBY, v).apply();
    }

    /** 从待机页手动退出时置位，避免主界面立刻又自动进入待机 */
    public static boolean consumeSkipAuto(Context c) {
        boolean v = sp(c).getBoolean(KEY_SKIP_AUTO, false);
        if (v) {
            sp(c).edit().putBoolean(KEY_SKIP_AUTO, false).apply();
        }
        return v;
    }

    public static void setSkipAuto(Context c) {
        sp(c).edit().putBoolean(KEY_SKIP_AUTO, true).apply();
    }

    /** 是否已同意隐私政策（商店合规要求：同意之前不得申请权限） */
    public static boolean isPrivacyAccepted(Context c) {
        return sp(c).getBoolean(KEY_PRIVACY, false);
    }

    public static void setPrivacyAccepted(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_PRIVACY, v).apply();
    }

    /** 是否已经弹过一次拨号权限申请（避免每次回到前台反复弹） */
    public static boolean isPermAsked(Context c) {
        return sp(c).getBoolean(KEY_PERM_ASKED, false);
    }

    public static void setPermAsked(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_PERM_ASKED, v).apply();
    }
}
