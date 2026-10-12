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
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 贴在卡片上写入号码 */
public class WriteTagActivity extends Activity implements NfcAdapter.ReaderCallback {
    public static final String EXTRA_MODE = "mode";

    private NfcAdapter adapter;
    private TextView status;
    private String number;
    private int mode;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        number = TapCall.normalize(Prefs.getNumber(this));
        mode = getIntent().getIntExtra(EXTRA_MODE, TapCall.MODE_FAMILY);

        int pad = (int) (getResources().getDisplayMetrics().density * 24);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView head = new TextView(this);
        head.setTextSize(24);
        head.setGravity(Gravity.CENTER_HORIZONTAL);
        head.setText("请把卡片贴到手机背面");
        root.addView(head);

        status = new TextView(this);
        status.setTextSize(18);
        status.setPadding(0, pad, 0, 0);
        status.setText(number.length() == 0
                ? "没有号码：请先返回上一页输入并保存号码"
                : "正在等待卡片…\n\n要写入的号码：" + number
                        + "\n\n（写成功后请把卡片拿开，几秒内本机不会拨号）");
        root.addView(status);
        setContentView(root);

        adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter == null) {
            status.setText("这台手机没有 NFC");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && number.length() > 0) {
            adapter.enableReaderMode(this, this,
                    NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B
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
        String result;
        try {
            NdefMessage msg = TapCall.buildMessage(number, mode);
            int need = msg.toByteArray().length;
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    result = "这张卡是只读的，写不进去";
                } else if (ndef.getMaxSize() < need) {
                    result = "卡片容量不够：需要 " + need + " 字节，卡片只有 " + ndef.getMaxSize() + " 字节";
                } else {
                    ndef.writeNdefMessage(msg);
                    result = "写入成功！可以拿去测试了";
                }
                ndef.close();
            } else {
                NdefFormatable fmt = NdefFormatable.get(tag);
                if (fmt == null) {
                    result = "这张卡不支持 NDEF 写入（可能是门禁卡、MIFARE Classic 或已被锁定）";
                } else {
                    fmt.connect();
                    fmt.format(msg);
                    fmt.close();
                    result = "格式化并写入成功！可以拿去测试了";
                }
            }
        } catch (Exception e) {
            result = "写入失败：" + e.getClass().getSimpleName() + "（把卡贴稳一点，再试一次）";
        }

        if (result.startsWith("写入成功") || result.startsWith("格式化并写入成功")) {
            Prefs.setLastWriteAt(this, System.currentTimeMillis());
        }

        final String shown = result;
        runOnUiThread(new Runnable() {
            public void run() {
                status.setText(shown);
                if (shown.startsWith("写入成功") || shown.startsWith("格式化并写入成功")) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, 2500);
                } else {
                    busy = false;
                }
            }
        });
    }
}
