# -*- coding: utf-8 -*-
"""把 classes.dex 塞进 aapt2 生成的 APK 包里（不依赖 Gradle）"""
import sys, zipfile

src, dex, out = sys.argv[1], sys.argv[2], sys.argv[3]
zin = zipfile.ZipFile(src, 'r')
zout = zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED)
for item in zin.infolist():
    if item.filename == 'classes.dex':
        continue
    zout.writestr(item, zin.read(item.filename))
zin.close()
zout.write(dex, 'classes.dex')
zout.close()
print('classes.dex added ->', out)
