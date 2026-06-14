# 📱 تطبيق Android - نظام توزيع المهام

## ما يفعله التطبيق
- يتصل بسيرفر NAS على نفس الشبكة بالـ IP
- يعرض كل الطلبات مع إمكانية الفلترة والرد
- **يرن ويُنبه تلقائياً** عند وصول طلب جديد أو رد
- يعمل في الخلفية حتى لو أغلقت التطبيق

---

## خطوات البناء بـ Android Studio

### 1. تثبيت Android Studio
حمّل من: https://developer.android.com/studio
اختر النسخة الأحدث وثبّتها.

### 2. افتح المشروع
- افتح Android Studio
- اختر **Open** (مش New Project)
- اختر مجلد `TaskManagerApp`
- انتظر حتى ينتهي Gradle من تحميل الـ dependencies (دقيقتين تقريباً)

### 3. ابنِ الـ APK
- من القائمة: **Build → Build Bundle(s)/APK(s) → Build APK(s)**
- أو من الـ Terminal الداخلي:
  ```
  ./gradlew assembleDebug
  ```
- الملف هيكون في:
  ```
  app/build/outputs/apk/debug/app-debug.apk
  ```

### 4. ثبّت على الموبايل
**طريقة 1 - USB:**
```
adb install app/build/outputs/apk/debug/app-debug.apk
```

**طريقة 2 - ملف مباشر:**
- انقل الـ APK للموبايل (WhatsApp / Google Drive / USB)
- افتح الملف وثبّته
- لازم تفعّل "السماح بمصادر غير معروفة" من الإعدادات

---

## إعداد التطبيق على الموبايل

### أول تشغيل
1. سيطلب **عنوان IP** السيرفر: `192.168.1.100`
2. و**رقم البورت**: `8090`
3. اضغط **اتصال** ← سيتحقق من السيرفر
4. ادخل اسم المستخدم وكلمة المرور

### متطلبات الشبكة
- الموبايل والـ NAS **على نفس الـ Wi-Fi**
- أو إذا كنت خارج البيت، يلزم VPN أو Port Forwarding

---

## كيف يعمل الـ Polling
التطبيق يسأل السيرفر كل **دقيقة** عن طلبات جديدة.
- لو ظهر طلب جديد ← يرن ويطلع إشعار
- لو فيه رد جديد من شخص آخر ← إشعار تاني
- الخدمة تشتغل في الخلفية حتى بعد إعادة تشغيل الموبايل

---

## هيكل الملفات
```
TaskManagerApp/
├── app/src/main/
│   ├── java/com/taskmanager/app/
│   │   ├── ApiClient.kt        ← تواصل مع السيرفر
│   │   ├── Prefs.kt            ← حفظ الإعدادات
│   │   ├── NotificationHelper.kt ← إشعارات الصوت
│   │   ├── PollService.kt      ← خدمة الاستطلاع الخلفية
│   │   ├── BootReceiver.kt     ← إعادة التشغيل بعد الريستارت
│   │   ├── MainActivity.kt     ← الشاشة الرئيسية
│   │   ├── SetupActivity.kt    ← إعداد الـ IP
│   │   ├── TaskAdapter.kt      ← قائمة الطلبات
│   │   └── ThreadAdapter.kt    ← رسائل الطلب
│   ├── res/layout/             ← تصاميم الشاشات
│   └── AndroidManifest.xml     ← إعدادات التطبيق
└── build.gradle                ← تبعيات المشروع
```
