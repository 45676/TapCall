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
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 主界面：设置号码、写卡、测试拨号、检查权限 */
public class MainActivity extends Activity {
    private static final int REQ_CALL = 1001;

    private EditText numberBox;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pad = dp(20);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("贴卡拨号");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView intro = new TextView(this);
        intro.setTextSize(15);
        intro.setPadding(0, dp(12), 0, dp(16));
        intro.setText("1) 输入要拨打的号码，点「保存号码」\n"
                + "2) 点「写入卡片」，把卡片贴到手机背面\n"
                + "3) 以后手机贴上这张卡，就会直接拨出这个号码");
        root.addView(intro);

        numberBox = new EditText(this);
        numberBox.setInputType(InputType.TYPE_CLASS_PHONE);
        numberBox.setHint("例如 13800138000");
        numberBox.setTextSize(24);
        numberBox.setText(Prefs.getNumber(this));
        root.addView(numberBox);

        root.addView(button("保存号码", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        }));
        root.addView(button("① 写入卡片（家里人手机：贴卡直接拨出）", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                write(TapCall.MODE_FAMILY);
            }
        }));
        root.addView(button("② 写入卡片（通用：任何手机弹出拨号盘）", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                write(TapCall.MODE_UNIVERSAL);
            }
        }));
        root.addView(button("★ 写入唤醒卡（贴一下自动进待机页）", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
                if (Prefs.getNumber(MainActivity.this).length() < 3) {
                    return;
                }
                Prefs.setMode(MainActivity.this, TapCall.MODE_WAKE);
                Intent i = new Intent(MainActivity.this, WriteTagActivity.class);
                i.putExtra(WriteTagActivity.EXTRA_MODE, TapCall.MODE_WAKE);
                startActivity(i);
            }
        }));
        root.addView(button("⚡ 待机模式（开着这页，贴卡即拨）", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, StandbyActivity.class));
            }
        }));
        root.addView(button("关于 / 隐私政策", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        }));
        root.addView(button("开关：打开 App 自动进待机", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean v2 = !Prefs.isAutoStandby(MainActivity.this);
                Prefs.setAutoStandby(MainActivity.this, v2);
                refresh();
                toast(v2 ? "已开启：打开 App 直接进待机模式" : "已关闭自动待机");
            }
        }));
        root.addView(button("检查卡片内容（不拨号）", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CardCheckActivity.class));
            }
        }));
        root.addView(button("切换拨号方式（当前：自动）", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int m = (Prefs.getDialMode(MainActivity.this) + 1) % 4;
                Prefs.setDialMode(MainActivity.this, m);
                refresh();
                toast("拨号方式：" + dialModeName(m));
            }
        }));
        root.addView(button("测试拨号", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testCall();
            }
        }));
        root.addView(button("授权拨号权限", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCallPermission();
            }
        }));
        root.addView(button("打开 NFC 设置", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                } catch (Exception e) {
                    toast("无法打开 NFC 设置");
                }
            }
        }));

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, dp(18), 0, 0);
        root.addView(status);

        // 商店合规：必须先让用户阅读并同意隐私政策，之后才申请权限
        if (!Prefs.isPrivacyAccepted(this)) {
            startActivity(new Intent(this, PrivacyActivity.class));
            return;
        }
        if (!hasCallPermission()) {
            requestCallPermissionOnce();
        }

        // 老人机常用场景：打开 App 就直接进待机模式，贴卡即拨
        if (Prefs.isAutoStandby(this) && !Prefs.consumeSkipAuto(this)) {
            startActivity(new Intent(this, StandbyActivity.class));
        }
    }

    /** 权限只在"已同意隐私政策"之后申请，且只自动弹一次 */
    private void requestCallPermissionOnce() {
        if (!Prefs.isPermAsked(this)) {
            Prefs.setPermAsked(this, true);
            requestCallPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        if (Prefs.isPrivacyAccepted(this) && !hasCallPermission()) {
            requestCallPermissionOnce();
        }
    }

    private Button button(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        b.setLayoutParams(lp);
        return b;
    }

    private void save() {
        String n = TapCall.normalize(numberBox.getText().toString());
        if (n.length() < 3) {
            toast("号码太短，请重新输入");
            return;
        }
        numberBox.setText(n);
        Prefs.setNumber(this, n);
        toast("已保存：" + n);
        refresh();
    }

    private void write(int mode) {
        save();
        String n = Prefs.getNumber(this);
        if (n.length() < 3) {
            return;
        }
        Prefs.setMode(this, mode);
        Intent i = new Intent(this, WriteTagActivity.class);
        i.putExtra(WriteTagActivity.EXTRA_MODE, mode);
        startActivity(i);
    }

    private void testCall() {
        String n = TapCall.normalize(numberBox.getText().toString());
        if (n.length() < 3) {
            toast("请先输入号码");
            return;
        }
        if (!hasCallPermission()) {
            requestCallPermission();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + n)));
        } catch (Exception e) {
            toast("拨号失败：" + e.getMessage());
        }
    }

    private boolean hasCallPermission() {
        return Build.VERSION.SDK_INT < 23
                || checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCallPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[] { Manifest.permission.CALL_PHONE }, REQ_CALL);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refresh();
    }

    private String dialModeName(int m) {
        if (m == 1) {
            return "只用 Telecom 直拨";
        }
        if (m == 2) {
            return "只用 ACTION_CALL";
        }
        if (m == 3) {
            return "只用系统拨号盘（按一下）";
        }
        return "自动（依次尝试）";
    }

    private void refresh() {
        StringBuilder sb = new StringBuilder();
        sb.append("拨号权限：").append(hasCallPermission() ? "已授权 ✓" : "未授权 ✗（贴卡只能到拨号盘）");
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter == null) {
            sb.append("\nNFC：这台手机不支持");
        } else {
            sb.append("\nNFC：").append(adapter.isEnabled() ? "已开启 ✓" : "未开启 ✗");
        }
        String n = Prefs.getNumber(this);
        sb.append("\n当前号码：").append(n.length() == 0 ? "（未设置）" : n);
        sb.append("\n拨号方式：").append(dialModeName(Prefs.getDialMode(this)));
        sb.append("\n自动待机：").append(Prefs.isAutoStandby(this) ? "开 ✓" : "关");
        sb.append("\n版本：").append(versionName());
        status.setText(sb.toString());
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
