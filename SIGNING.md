# Android release signing

نام برند انتشار: `App-BotStore`

شناسه نصب Android که برای سازگاری آپدیت ثابت می‌ماند: `ir.asteam.telegrambotstore`

Alias جدید کلید خصوصی: `app-botstore-release`

SHA-256 certificate fingerprint ثابت:

`01:9E:A1:CC:37:F2:3B:26:3C:AA:4F:5A:D5:08:F0:54:4D:E9:DB:CD:F9:C9:58:AF:3C:72:D0:E7:50:AF:4D:69`

## چرا خود کلید تغییر نمی‌کند؟

Android فقط زمانی یک APK جدید را به‌عنوان Update نسخه نصب‌شده قبول می‌کند که `applicationId` و certificate امضا با نسخه قبلی سازگار باشند. بنابراین برای rebrand، Alias کلید از نام قدیمی به `app-botstore-release` تغییر می‌کند، اما material رمزنگاری و certificate همان کلید قبلی باقی می‌ماند.

فایل JKS خصوصی و رمزهای آن نباید در مخزن عمومی GitHub commit شوند.
