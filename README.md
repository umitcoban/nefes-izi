# Nefes İzi

Nefes İzi; sigara tüketimini, kullanıcı tarafından girilen ürün değerlerinden hesaplanan tahmini emisyonları, maliyeti ve kişisel sağlık notlarını cihaz üzerinde tutan native Android uygulamasıdır.

Uygulama local-first çalışır:

- kullanıcı hesabı yoktur,
- internet izni yoktur,
- analitik veya izleme SDK’sı yoktur,
- Android bulut yedeklemesi kapalıdır,
- kayıtların doğruluk kaynağı cihazdaki Room veritabanıdır.

> Uygulama tıbbi teşhis veya tedavi amacı taşımaz. Gösterilen emisyonlar kişisel emilim değil, kayıtlı ürün değerlerinin matematiksel toplamıdır.

## Teknoloji yığını

- Kotlin
- Jetpack Compose ve Material 3
- Navigation Compose
- Room
- Preferences DataStore
- Hilt
- Kotlin Coroutines ve Flow
- Gradle Version Catalog

## Gereksinimler

- Android Studio’nun güncel kararlı sürümü
- JDK 17 veya uyumlu Android Studio JDK’sı
- Android SDK 36.1
- Android 8.0 / API 26 veya üzeri cihaz ya da emülatör

## Çalıştırma

Projeyi Android Studio ile açıp `app` konfigürasyonunu çalıştırabilir veya terminalden debug APK üretebilirsin:

```bash
./gradlew assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Doğrulama komutları

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
```

Room şeması değiştiğinde schema JSON dosyası `app/schemas/` altında Git’e eklenmelidir. Destructive migration kullanılmaz.

## Mimari

Kod şu an tek Android modülü içinde, feature ve core paket sınırlarıyla düzenlenmiştir:

```text
app/src/main/java/com/umityasincoban/nefesizi/
├── core/
│   ├── common
│   ├── data
│   ├── database
│   ├── di
│   └── domain
├── feature/
│   ├── onboarding
│   ├── today
│   ├── records
│   ├── health
│   ├── analytics
│   ├── products
│   └── settings
└── ui/
```

- Compose ekranları DAO’ya doğrudan erişmez.
- ViewModel’lar repository ve domain kurallarını kullanır.
- Ürün değerleri ve fiyatlar sigara kaydına snapshot olarak yazılır.
- Bilinmeyen kimyasal veya fiyat değerleri `0` kabul edilmez.
- Para değerleri kayan noktalı sayı yerine mikro para birimi olarak saklanır.

## Veri ve fiyat geçmişi

Bir ürünün fiyatı veya kimyasal değeri değiştiğinde eski sigara kayıtları değiştirilmez. Yeni kayıtlar, olay tarihinde yürürlükte olan ürün revizyonunun değerlerini snapshot olarak alır.

Ayrıntılı geliştirme sırası ve edge-case kararları:

- [Implementasyon yol haritası](docs/IMPLEMENTATION_ROADMAP.md)
- [Proje incelemesi ve hedef mimari](docs/PROJECT_REVIEW_AND_ARCHITECTURE.md)

## Bilinen eksikler

Kayıt detay düzenleme, gelişmiş dönem analizleri, JSON/CSV yedekleme, bildirimler, biyometrik kilit ve release sertleştirme yol haritasındaki sırayla geliştirilmektedir.
