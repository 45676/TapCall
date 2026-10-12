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

import android.app.Activity;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Toast;

/** 贴卡后被系统拉起：解析卡里的号码并直接拨出（无界面） */
public class CallTagActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Prefs.isPrivacyAccepted(this)) {
            toast("请先打开「贴卡拨号」，阅读并同意隐私政策");
            finish();
            return;
        }
        toast("贴卡拨号：已收到卡片");
        // 刚写完卡时卡片往往还贴在写入手机上，冷却期内忽略，避免写入手机自己拨出去
        long sinceWrite = System.currentTimeMillis() - Prefs.getLastWriteAt(this);
        if (sinceWrite >= 0 && sinceWrite < 20000) {
            toast("刚写完这张卡，已忽略这次贴卡（把卡拿开即可）");
            finish();
            return;
        }

        if (isWakeCard(getIntent())) {
            toast("唤醒卡：正在打开待机页");
            startActivity(new android.content.Intent(this, StandbyActivity.class)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return;
        }

        String number = extractNumber(getIntent());
        if (number.length() == 0) {
            number = TapCall.normalize(Prefs.getNumber(this));
        }
        if (number.length() == 0) {
            toast("这张卡里没有号码，请先用「贴卡拨号」写入");
            finish();
            return;
        }
        String how = Dialer.call(this, number);
        if (Dialer.BY_FAIL.equals(how)) {
            toast("拨号失败，请检查拨号权限");
        } else {
            toast("正在拨号（" + how + "）：" + number);
        }
        finish();
    }

    /** 唤醒卡：卡里只有 tapcall:standby，不含号码 */
    private boolean isWakeCard(android.content.Intent intent) {
        if (intent == null || !NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            return false;
        }
        Parcelable[] raw = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (raw == null) {
            return false;
        }
        for (Parcelable p : raw) {
            if (p instanceof NdefMessage && TapCall.messageIsWake((NdefMessage) p)) {
                return true;
            }
        }
        return false;
    }

    private String extractNumber(android.content.Intent intent) {
        if (intent == null || !NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            return "";
        }
        Parcelable[] raw = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (raw == null) {
            return "";
        }
        for (Parcelable p : raw) {
            if (p instanceof NdefMessage) {
                String n = TapCall.numberFromMessage((NdefMessage) p);
                if (n.length() > 0) {
                    return n;
                }
            }
        }
        return "";
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
