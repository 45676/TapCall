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

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

/** 号码解析与 NDEF 记录构造 */
public final class TapCall {
    /** 自家模式使用的自定义 scheme：卡里写 tapcall:13800138000 */
    public static final String SCHEME = "tapcall";
    public static final String PREFIX = SCHEME + ":";

    /** 家里人的手机（装了本 App）：贴卡直接拨出 */
    public static final int MODE_FAMILY = 0;
    /** 通用模式：只写 tel: 记录，任何手机贴卡都会弹出拨号盘（需按一下） */
    public static final int MODE_UNIVERSAL = 1;
    /** 唤醒卡：卡里不存号码，贴一下只负责打开 App 并进入待机页 */
    public static final int MODE_WAKE = 2;
    /** 唤醒卡里的 URI（旧格式，仍兼容识别） */
    public static final String WAKE_URI = PREFIX + "standby";
    /** 本应用包名：唤醒卡里的 AAR 记录用它 */
    public static final String PACKAGE_NAME = "com.family.tapcall";

    private TapCall() {
    }

    /** 只保留号码里合法的字符：数字、开头的 +、* # , ; */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') {
                sb.append(ch);
            } else if (ch == '+' && sb.length() == 0) {
                sb.append(ch);
            } else if (ch == '*' || ch == '#' || ch == ',' || ch == ';') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /** 构造要写进卡片的 NDEF 消息 */
    public static NdefMessage buildMessage(String number, int mode) {
        if (mode == MODE_WAKE) {
            // 唤醒卡用 AAR（Android 应用记录）：系统级"打开这个包"，比自定义 URI 可靠，
            // 锁屏、App 被清理掉、刚重启都能拉起来。
            return new NdefMessage(new NdefRecord[] {
                    NdefRecord.createApplicationRecord(PACKAGE_NAME),
                    NdefRecord.createUri(WAKE_URI) });
        }
        String n = normalize(number);
        NdefRecord universal = NdefRecord.createUri("tel:" + n);
        if (mode == MODE_UNIVERSAL) {
            return new NdefMessage(new NdefRecord[] { universal });
        }
        // tel: 放第一条：任何手机都能识别（没装 App 会弹拨号盘），装了本 App 则由本 App 直接拨出。
        // 自定义 scheme 放第二条：给以后扩展用，不影响第一条的分发。
        NdefRecord family = NdefRecord.createUri(PREFIX + n);
        return new NdefMessage(new NdefRecord[] { universal, family });
    }

    /** 是不是唤醒卡 */
    public static boolean isWakeUri(String uri) {
        return uri != null && WAKE_URI.equals(uri.trim());
    }

    /** 是不是指向本应用的 AAR（android.com:pkg）记录 */
    public static boolean isAarForUs(NdefRecord r) {
        try {
            if (r.getTnf() != NdefRecord.TNF_EXTERNAL_TYPE) {
                return false;
            }
            String type = new String(r.getType(), java.nio.charset.Charset.forName("UTF-8"));
            if (!"android.com:pkg".equals(type)) {
                return false;
            }
            String pkg = new String(r.getPayload(), java.nio.charset.Charset.forName("UTF-8"));
            return PACKAGE_NAME.equals(pkg.trim());
        } catch (Exception e) {
            return false;
        }
    }

    /** 这条 NDEF 消息是不是唤醒卡（AAR 或旧的 URI 格式都算） */
    public static boolean messageIsWake(NdefMessage msg) {
        if (msg == null) {
            return false;
        }
        for (NdefRecord r : msg.getRecords()) {
            if (isWakeUri(uriOf(r)) || isAarForUs(r)) {
                return true;
            }
        }
        return false;
    }

    /** 从整条 NDEF 消息里找出号码（按记录顺序找第一条能识别的） */
    public static String numberFromMessage(NdefMessage msg) {
        if (msg == null) {
            return "";
        }
        for (NdefRecord r : msg.getRecords()) {
            String n = numberFromUri(uriOf(r));
            if (n.length() > 0) {
                return n;
            }
        }
        return "";
    }

    /** 取出 URI 型记录的完整 URI（还原 NDEF URI 前缀缩写） */
    public static String uriOf(NdefRecord r) {
        try {
            if (r.getTnf() == NdefRecord.TNF_WELL_KNOWN
                    && java.util.Arrays.equals(r.getType(), NdefRecord.RTD_URI)) {
                byte[] p = r.getPayload();
                if (p == null || p.length == 0) {
                    return "";
                }
                int pre = p[0] & 0xFF;
                String head = "";
                if (pre == 0x01) {
                    head = "http://www.";
                } else if (pre == 0x02) {
                    head = "https://www.";
                } else if (pre == 0x03) {
                    head = "http://";
                } else if (pre == 0x04) {
                    head = "https://";
                } else if (pre == 0x05) {
                    head = "tel:";
                }
                return head + new String(p, 1, p.length - 1, java.nio.charset.Charset.forName("UTF-8"));
            }
            if (r.getTnf() == NdefRecord.TNF_ABSOLUTE_URI) {
                return new String(r.getType(), java.nio.charset.Charset.forName("UTF-8"));
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /** 从卡片记录里的 URI 解析号码 */
    public static String numberFromUri(String uri) {
        if (uri == null) {
            return "";
        }
        if (uri.startsWith(PREFIX)) {
            return normalize(uri.substring(PREFIX.length()));
        }
        if (uri.startsWith("tel:")) {
            return normalize(uri.substring(4));
        }
        return "";
    }
}
