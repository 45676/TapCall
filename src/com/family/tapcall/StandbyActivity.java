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
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.nfc.NdefMessage;
import android.nfc.tech.Ndef;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 待机模式：让这一页一直开着，App 处于前台时直接读卡并拨号。
 * 这样拨号不是"后台应用发起的"，可以绕开 MIUI 的拨号二次确认。
 *
 * 退出方式：长按底部大按钮。返回键与左右滑返回手势都已屏蔽，防止老人误退出。
 */
public class StandbyActivity extends Activity implements NfcAdapter.ReaderCallback {

    private NfcAdapter adapter;
    private TextView status;
    private TextView last;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setText("请把卡片贴到手机背面");
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setTextSize(15);
        hint.setGravity(Gravity.CENTER_HORIZONTAL);
        hint.setPadding(0, dp(12), 0, 0);
        hint.setText("流程：先贴唤醒卡进入本页 → 再贴要拨的那张卡");
        root.addView(hint);

        status = new TextView(this);
        status.setTextSize(19);
        status.setPadding(0, dp(16), 0, dp(8));
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        status.setText("等待卡片…");
        root.addView(status);

        last = new TextView(this);
        last.setTextSize(16);
        last.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(last);

        // ===== 退出按钮（主操作，放大） =====
        Button exit = new Button(this);
        exit.setText("长按这里退出待机");
        exit.setTextSize(24);
        exit.setAllCaps(false);
        exit.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(88));
        elp.topMargin = dp(24);
        exit.setLayoutParams(elp);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(StandbyActivity.this,
                        "为防止误触，请长按这个按钮 1 秒以上再松手", Toast.LENGTH_SHORT).show();
            }
        });
        exit.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Prefs.setSkipAuto(StandbyActivity.this);
                finish();
                return true;
            }
        });
        root.addView(exit);

        TextView tip = new TextView(this);
        tip.setTextSize(12);
        tip.setGravity(Gravity.CENTER_HORIZONTAL);
        tip.setPadding(0, dp(8), 0, 0);
        tip.setText("返回键与左右滑返回手势已屏蔽，只能长按上面的大按钮退出");
        root.addView(tip);

        // ===== 固定屏幕（次要操作：小号红色按钮） =====
        Button lock = new Button(this);
        lock.setText("🔒 固定屏幕");
        lock.setTextSize(12);
        lock.setAllCaps(false);
        GradientDrawable red = new GradientDrawable();
        red.setColor(0xFFD32F2F);
        red.setCornerRadius(dp(6));
        lock.setBackground(red);
        lock.setTextColor(0xFFFFFFFF);
        lock.setMinWidth(0);
        lock.setMinimumWidth(0);
        lock.setMinHeight(0);
        lock.setMinimumHeight(0);
        lock.setPadding(dp(18), dp(6), dp(18), dp(6));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.topMargin = dp(26);
        llp.gravity = Gravity.CENTER_HORIZONTAL;
        lock.setLayoutParams(llp);
        lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startLockTask();
                    Toast.makeText(StandbyActivity.this,
                            "已固定在本页。解除：按住返回键（或上滑并按住）→ 解锁屏幕",
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(StandbyActivity.this,
                            "还没开启屏幕固定。请先到 设置 → 安全（或更多设置）→ 屏幕固定 打开，再回来点这个按钮",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        root.addView(lock);

        // ===== 解除固定屏幕的说明 =====
        TextView unlockTip = new TextView(this);
        unlockTip.setTextSize(12);
        unlockTip.setGravity(Gravity.CENTER_HORIZONTAL);
        unlockTip.setPadding(0, dp(8), 0, 0);
        unlockTip.setTextColor(0xFFD32F2F);
        unlockTip.setText("解除固定屏幕的方式：\n"
                + "1) 按住返回键（全面屏：上滑并按住）→ 出现提示后解锁屏幕；\n"
                + "2) 或到 设置 → 安全（更多设置）→ 屏幕固定 里关闭。\n"
                + "开启前需先在系统里打开「屏幕固定」开关。");
        root.addView(unlockTip);

        setContentView(root);
        adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter == null) {
            status.setText("这台手机没有 NFC");
        }

        // Android 13+ 预测式返回：注册一个"什么都不做"的回调，挡住返回手势
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    new android.window.OnBackInvokedCallback() {
                        @Override
                        public void onBackInvoked() {
                            Toast.makeText(StandbyActivity.this,
                                    "返回已屏蔽，请长按大按钮退出待机",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    /** 老式返回键/手势：同样屏蔽 */
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "返回已屏蔽，请长按大按钮退出待机", Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Prefs.isPrivacyAccepted(this)) {
            Toast.makeText(this, "请先打开「贴卡拨号」，阅读并同意隐私政策", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (adapter != null) {
            adapter.enableReaderMode(this, this,
                    NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B
                            | NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V
                            | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null) {
            adapter.disableReaderMode(this);
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        if (busy) {
            return;
        }
        busy = true;
        String number = "";
        boolean wake = false;
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            try {
                ndef.connect();
                NdefMessage msg = ndef.getNdefMessage();
                ndef.close();
                wake = TapCall.messageIsWake(msg);
                number = TapCall.numberFromMessage(msg);
            } catch (Exception ignored) {
            }
        }
        if (wake) {
            runOnUiThread(new Runnable() {
                public void run() {
                    status.setText("已经是待机模式，直接贴拨号卡即可");
                    busy = false;
                }
            });
            return;
        }
        if (number.length() == 0) {
            number = TapCall.normalize(Prefs.getNumber(this));
        }
        final String n = number;
        if (n.length() == 0) {
            runOnUiThread(new Runnable() {
                public void run() {
                    status.setText("这张卡里没有号码，也没有设置默认号码");
                    busy = false;
                }
            });
            return;
        }
        final String how = Dialer.call(this, n);
        runOnUiThread(new Runnable() {
            public void run() {
                status.setText("已拨号：" + n);
                last.setText("通道：" + how + "（" + new java.text.SimpleDateFormat("HH:mm:ss")
                        .format(new java.util.Date()) + "）");
                busy = false;
            }
        });
    }
}
