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
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.Arrays;

/** 自检：贴卡后把卡片里读到的原始内容显示出来（用来判断是卡的问题还是分发的问题） */
public class CardCheckActivity extends Activity implements NfcAdapter.ReaderCallback {

    private NfcAdapter adapter;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pad = (int) (getResources().getDisplayMetrics().density * 20);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);
        setContentView(scroll);

        TextView head = new TextView(this);
        head.setTextSize(22);
        head.setGravity(Gravity.CENTER_HORIZONTAL);
        head.setText("请贴一下卡片");
        root.addView(head);

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, pad, 0, 0);
        status.setText("正在等待卡片…\n（这一页只是查看卡片内容，不会拨号）");
        root.addView(status);

        adapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.enableReaderMode(this, this,
                    NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B
                            | NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V, null);
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
        StringBuilder sb = new StringBuilder();
        byte[] id = tag.getId();
        StringBuilder uid = new StringBuilder();
        for (byte b : id) {
            uid.append(String.format("%02X:", b));
        }
        sb.append("UID：").append(uid.length() > 0 ? uid.substring(0, uid.length() - 1) : "(无)").append("\n");
        sb.append("技术：");
        for (String t : tag.getTechList()) {
            int i = t.lastIndexOf('.');
            sb.append(i >= 0 ? t.substring(i + 1) : t).append(" ");
        }
        sb.append("\n\n");

        Ndef ndef = Ndef.get(tag);
        NdefMessage msg = null;
        if (ndef != null) {
            try {
                ndef.connect();
                sb.append("NDEF：可读");
                sb.append(ndef.isWritable() ? "，可写" : "，只读");
                sb.append("，容量 ").append(ndef.getMaxSize()).append(" 字节\n");
                msg = ndef.getNdefMessage();
                ndef.close();
            } catch (Exception e) {
                sb.append("NDEF 读取失败：").append(e.getClass().getSimpleName()).append("\n");
            }
        } else {
            sb.append("NDEF：这张卡没有 NDEF 数据（或不是 NDEF 卡）\n");
        }

        if (msg == null) {
            sb.append("\n结论：卡里没有号码，需要重新写入。\n");
        } else {
            sb.append("记录条数：").append(msg.getRecords().length).append("\n\n");
            int idx = 0;
            for (NdefRecord r : msg.getRecords()) {
                idx++;
                String uri = "";
                try {
                    if (r.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(r.getType(), NdefRecord.RTD_URI)) {
                        byte[] p = r.getPayload();
                        String head = "";
                        if (p.length > 0) {
                            int pre = p[0] & 0xFF;
                            if (pre == 0x01) head = "http://www.";
                            else if (pre == 0x02) head = "https://www.";
                            else if (pre == 0x03) head = "http://";
                            else if (pre == 0x04) head = "https://";
                            else if (pre == 0x05) head = "tel:";
                        }
                        uri = head + new String(p, 1, Math.max(0, p.length - 1), Charset.forName("UTF-8"));
                    } else if (TapCall.isAarForUs(r)) {
                        uri = "Android 应用记录(AAR)：本应用 —— 贴卡会打开「贴卡拨号」";
                    } else if (r.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE
                            && "android.com:pkg".equals(new String(r.getType(), Charset.forName("UTF-8")))) {
                        uri = "Android 应用记录(AAR)："
                                + new String(r.getPayload(), Charset.forName("UTF-8"));
                    } else {
                        uri = "[非 URI 记录 tnf=" + r.getTnf() + "] " + new String(r.getPayload(), Charset.forName("UTF-8"));
                    }
                } catch (Exception e) {
                    uri = "(解析失败)";
                }
                sb.append("第 ").append(idx).append(" 条：").append(uri).append("\n");
                String n = TapCall.numberFromUri(uri);
                sb.append("   解析号码：").append(n.length() > 0 ? n : "（不是号码）").append("\n");
            }
            String first = "";
            try {
                first = TapCall.numberFromUri(uriOf(msg.getRecords()[0]));
            } catch (Exception ignored) {
            }
            sb.append("\n结论：");
            if (first.length() > 0) {
                sb.append("第一条记录是可识别号码 ").append(first).append("，系统应该能分发（若仍无反应，问题在后台弹出权限或 App 未安装）");
            } else {
                sb.append("第一条记录不是 tel: 号码，系统识别不了 → 请重新用「写入卡片」写一次");
            }
        }
        sb.append("\n\n（再贴一次可重新读取）");

        final String text = sb.toString();
        runOnUiThread(new Runnable() {
            public void run() {
                status.setText(text);
            }
        });
    }

    private String uriOf(NdefRecord r) {
        try {
            if (r.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(r.getType(), NdefRecord.RTD_URI)) {
                byte[] p = r.getPayload();
                if (p.length == 0) {
                    return "";
                }
                int pre = p[0] & 0xFF;
                String head = pre == 0x05 ? "tel:" : "";
                return head + new String(p, 1, p.length - 1, Charset.forName("UTF-8"));
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}
