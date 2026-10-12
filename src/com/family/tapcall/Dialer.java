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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import java.util.List;

/** 拨号策略：Telecom 直拨 → ACTION_CALL → 系统拨号盘（顺序由主界面的「拨号方式」决定） */
public final class Dialer {
    public static final String BY_PLACE = "Telecom直拨";
    public static final String BY_CALL = "ACTION_CALL";
    public static final String BY_DIAL = "系统拨号盘";
    public static final String BY_FAIL = "失败";

    private Dialer() {
    }

    public static boolean hasCallPermission(Context ctx) {
        return Build.VERSION.SDK_INT < 23
                || ctx.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
    }

    /** 返回实际使用的通道名 */
    public static String call(Context ctx, String number) {
        Uri uri = Uri.parse("tel:" + TapCall.normalize(number));
        int mode = Prefs.getDialMode(ctx);
        boolean granted = hasCallPermission(ctx);
        if (!granted) {
            return openDialer(ctx, uri) ? BY_DIAL : BY_FAIL;
        }
        if (mode == 1 || mode == 0) {
            if (placeCall(ctx, uri)) {
                return BY_PLACE;
            }
            if (mode == 1) {
                return openDialer(ctx, uri) ? BY_DIAL : BY_FAIL;
            }
        }
        if (mode == 2 || mode == 0) {
            if (startCall(ctx, uri)) {
                return BY_CALL;
            }
            if (mode == 2) {
                return openDialer(ctx, uri) ? BY_DIAL : BY_FAIL;
            }
        }
        return openDialer(ctx, uri) ? BY_DIAL : BY_FAIL;
    }

    /** 走 Telecom 服务；指定第一张卡，避免双卡"每次询问"弹窗 */
    private static boolean placeCall(Context ctx, Uri uri) {
        if (Build.VERSION.SDK_INT < 23) {
            return false;
        }
        try {
            TelecomManager tm = (TelecomManager) ctx.getSystemService(Context.TELECOM_SERVICE);
            if (tm == null) {
                return false;
            }
            Bundle extras = new Bundle();
            List<PhoneAccountHandle> accounts = tm.getCallCapablePhoneAccounts();
            if (accounts != null && accounts.size() > 0) {
                extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accounts.get(0));
            }
            tm.placeCall(uri, extras);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean startCall(Context ctx, Uri uri) {
        try {
            Intent call = new Intent(Intent.ACTION_CALL, uri);
            call.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(call);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean openDialer(Context ctx, Uri uri) {
        try {
            Intent d = new Intent(Intent.ACTION_DIAL, uri);
            d.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(d);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
