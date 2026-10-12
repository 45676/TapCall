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
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 关于：版本、备案号位置、隐私政策入口、开源地址 */
public class AboutActivity extends Activity {

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
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setText("关于 贴卡拨号");
        root.addView(title);

        String version = "?";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
        }

        TextView info = new TextView(this);
        info.setTextSize(15);
        info.setPadding(0, dp(14), 0, dp(14));
        info.setText("版本：v" + version + "\n\n"
                + "一个完全离线的家庭辅助拨号工具：贴卡 → 直接拨打设定号码。\n"
                + "· 不联网（未申请网络权限）\n"
                + "· 无广告、无统计、无第三方 SDK\n"
                + "· 号码只保存在本机和卡片里\n\n"
                + "应用备案号：（尚未上架，如未来在国内应用商店发布，此处展示备案号）\n\n"
                + "开源仓库：（如发布到 GitHub，此处填写仓库地址）\n"
                + "开源许可：GNU GPL-3.0（可自由使用/修改，分发修改版需同样开源）\n\n"
                + "联系方式：（此处填写开发者联系方式）");
        root.addView(info);

        Button policy = new Button(this);
        policy.setTextSize(16);
        policy.setAllCaps(false);
        policy.setText("查看隐私政策");
        policy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AboutActivity.this, PrivacyActivity.class));
            }
        });
        root.addView(policy);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
