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
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 隐私政策：商店合规要求——用户同意之前不得申请任何权限 */
public class PrivacyActivity extends Activity {

    /** 隐私政策正文（纯本地应用：不收集、不联网、无第三方 SDK） */
    public static final String POLICY = "贴卡拨号 隐私政策（版本 1.1.0，生效日期：2026-10-12）\n\n一、我们收集什么\n本应用不收集、不上传、不共享任何个人信息。\n· 未申请联网权限（没有 INTERNET 权限），应用完全离线运行；\n· 未集成任何第三方 SDK（无统计、无广告、无崩溃上报）。\n\n二、信息保存在哪里\n· 你输入的号码、拨号方式等设置，只保存在本机应用私有目录，卸载应用即删除；\n· 卡里的号码保存在 NFC 卡片本身，不经过任何服务器。\n\n三、申请的权限与用途\n· NFC（android.permission.NFC）：读取卡片内容，或被卡片唤醒；\n· 拨打电话（android.permission.CALL_PHONE）：贴卡后直接拨出你设置的号码；\n· 以上权限只用于实现对应功能，不作他用。\n\n四、未成年人\n本应用是家庭辅助工具，建议在成年人指导下使用。\n\n五、联系方式\n（此处填写开发者联系方式或开源仓库地址）\n\n六、政策变更\n本政策如有更新，将随应用版本一并更新并在本页展示。";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pad = dp(20);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setText("隐私政策");
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        TextView body = new TextView(this);
        body.setTextSize(15);
        body.setPadding(0, dp(12), 0, dp(12));
        body.setText(POLICY);
        scroll.addView(body);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(slp);
        root.addView(scroll);

        Button agree = new Button(this);
        agree.setTextSize(18);
        agree.setAllCaps(false);
        agree.setText("同意并继续");
        agree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setPrivacyAccepted(PrivacyActivity.this, true);
                Toast.makeText(PrivacyActivity.this, "已同意隐私政策", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        root.addView(agree);

        Button disagree = new Button(this);
        disagree.setTextSize(15);
        disagree.setAllCaps(false);
        disagree.setText("不同意");
        disagree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setPrivacyAccepted(PrivacyActivity.this, false);
                Toast.makeText(PrivacyActivity.this,
                        "未同意隐私政策，贴卡拨号功能不会启用；可随时重新打开应用阅读并同意",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
        root.addView(disagree);

        setContentView(root);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
