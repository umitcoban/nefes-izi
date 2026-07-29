# Nefes İzi — Ayrıntılı Implementasyon Yol Haritası

Son güncelleme: 29 Temmuz 2026

## Uygulama ilerleme özeti

- Faz 0 — temel kalite ve dokümantasyon: tamamlandı.
- Faz 1 — ürün revizyonu ve değişmez kayıt snapshot altyapısı: tamamlandı.
- Faz 2 — ürün yönetimi ve yürürlük tarihli fiyat/değer geçmişi: tamamlandı.
- Faz 3 — kayıt ekleme, detay ve düzenleme: tamamlandı.
  Kayıt detayı, manuel geçmiş giriş, düzenleme, çoğaltma, silme onayı/geri alma,
  özel tarih aralığı ve bağlamsal filtreler gerçek Room verisine bağlıdır.
  Aynı revizyondaki düzenleme eski snapshot'ı korur; revizyon aralığı değişirse
  olay tarihindeki revizyon yeniden snapshot alınır. Form alanları
  `SavedStateHandle` ile process death sonrasında geri yüklenir.
- Faz 4 — Bugün ekranı: tamamlandı. Ortalama kayıt aralığı, ilk kayıt saati,
  coverage-aware günlük maliyet ve maruziyet, ürün seçmeli hızlı kayıt,
  kayıt sonrası detay/geri alma, tüm kayıtlar aksiyonu ve görünürlük ayarları
  uygulandı.
- Faz 5 — maliyet ve gelişmiş analiz: tamamlandı. 7/30/90 gün, tüm zamanlar ve
  özel tarih aralığı; eş dönem karşılaştırması, günlük/saatlik/haftalık
  dağılımlar, maliyet coverage'ı, çoklu para birimi, yıllık tahmin, opsiyonel
  uyanma saati ve açıklanabilir insight kuralları uygulandı.
- Faz 6 — sağlık günlüğü: tamamlandı. Geçmiş gün düzenleme, 7/14/30 gün
  özetleri, fizyolojik ölçümler, not geçmişi ve yeterli veri eşikli,
  nedensellik iddiası kurmayan kişisel ilişki özetleri uygulandı.
- Faz 7 — ayarlar ve kişiselleştirme: tamamlandı. Dinamik renk, mantıksal gün
  başlangıcı, haftanın ilk günü, tercih edilen para birimi ve sekme/kart
  görünürlüğü reaktif DataStore tercihleri olarak uygulandı.
- Faz 8 — yerel yedekleme: tamamlandı. Sürümlü JSON yedeği, eksiksiz CSV ZIP,
  SAF dışa/içe aktarma, doğrulama, conflict önizlemesi ve transaction tabanlı
  birleştirme/yerine koyma akışları uygulandı.
- Faz 9 — bildirim ve biyometrik erişim kapısı: tamamlandı. Varsayılan kapalı
  hatırlatmalar, kullanıcı eyleminde izin isteme, unique WorkManager işleri ve
  destek kontrolü yapılan biyometrik kilit uygulandı.
- Faz 10 — kişisel release sertleştirmesi: tamamlandı. Dar ekran kartları
  uyarlanabilir hale getirildi; R8/minify ve resource shrinking açık, imzalı
  kişisel release APK üretildi ve emülatörde smoke test edildi. Mağaza yayınına
  özel kalıcı imza, AAB, mağaza metni ve geniş cihaz matrisi şimdilik ertelendi.

29 Temmuz doğrulaması: `assembleDebug`, `assembleRelease`, `lintRelease`,
`testDebugUnitTest` ve `connectedDebugAndroidTest` başarılıdır. 70 unit test ve
9 emülatör instrumented testi sıfır hata ve sıfır atlanan testle tamamlanmıştır.

## 1. Amaç ve mevcut durum

Bu doküman, mevcut çalışan dikey dilimi tam MVP ve ardından release adayı seviyesine taşımak için uygulanacak sırayı, veri kurallarını, migration stratejisini ve kabul kriterlerini tanımlar.

Bugün çalışan temel:

- onboarding ve ilk ürün oluşturma,
- tek dokunuşla sigara kaydı,
- güncel maruziyet toplamları,
- aranabilir ve güne göre gruplanmış kayıt listesi,
- silme ve geri alma,
- günlük sağlık formu,
- temel 30 günlük analiz,
- ürün ekleme ve varsayılan ürün seçme,
- sistem/açık/koyu tema,
- Room, Hilt, DataStore ve Navigation altyapısı,
- Room `1 → 2` migration,
- debug build, unit test ve lint doğrulaması.

Mevcut uygulama iyi bir ürün çekirdeğidir; ancak aşağıdaki fazlar tamamlanmadan ilk ürün dokümanındaki tüm gereksinimleri karşılayan bir release sayılmayacaktır.

## 2. Değişmez veri ve ürün kuralları

Bu bölümdeki kurallar sonraki bütün implementasyonların sözleşmesidir.

### 2.1 Geçmiş kayıtlar değişmez snapshot kullanır

Bir sigara kaydı oluşturulduğu anda aşağıdaki bilgiler kayıt içine kopyalanır:

- ürün kimliği ve ürün revizyonu kimliği,
- görünen ürün adı,
- sigara başına nikotin, katran ve karbonmonoksit değerleri,
- paket fiyatı ve paket adedi,
- hesaplanmış sigara başı fiyat,
- para birimi,
- veri kaynağı,
- kayıt anındaki saat dilimi.

Ürün daha sonra düzenlense, fiyatı artsa, arşivlense veya adı değişse bile daha önce oluşturulmuş `SmokingRecord` satırları güncellenmez.

Örnek:

1. Paket fiyatı 100 TL iken 10 Temmuz kaydı oluşturulur.
2. Ürün fiyatı 15 Temmuz saat 12:00 itibarıyla 120 TL yapılır.
3. 10 Temmuz kaydının maliyeti 100 TL’lik paket snapshot’ına göre kalır.
4. 15 Temmuz 12:00 sonrasındaki yeni kayıtlar 120 TL’lik fiyatı kullanır.
5. Ürün kartını düzenlemek için geçmiş kayıtlar üzerinde toplu `UPDATE` çalıştırılmaz.

### 2.2 Ürün değerleri yürürlük tarihli revizyonlardır

Yalnızca ürün tablosundaki “güncel fiyat” alanı geçmiş tarihli manuel kayıtlar için yeterli değildir. Bu nedenle ürün kimliği ile değişken değerler ayrılır:

#### `CigaretteProductEntity`

Ürün kimliğini ve yönetim durumunu taşır:

- `id`
- `name`
- `brand`
- `variant`
- `isDefault`
- `isArchived`
- `createdAtEpochMillis`
- `updatedAtEpochMillis`

#### `CigaretteProductRevisionEntity`

Belirli bir andan itibaren geçerli değerleri taşır:

- `id`
- `productId`
- `effectiveFromEpochMillis`
- `nicotineMicrogramsPerCigarette`
- `tarMicrogramsPerCigarette`
- `carbonMonoxideMicrogramsPerCigarette`
- `packPriceMicros`
- `cigarettesPerPack`
- `priceMicrosPerCigarette`
- `currencyCode`
- `valueSource`
- `createdAtEpochMillis`

Kısıtlar:

- Aynı ürün ve aynı `effectiveFromEpochMillis` için tek revizyon bulunur.
- Revizyonlar normal kullanımda güncellenmez; yeni değer için yeni revizyon eklenir.
- Ürün silinmez, varsayılan olarak arşivlenir.
- Arşivlenen ürün geçmiş kayıtlarda görünmeye devam eder.
- Arşivlenen varsayılan ürün varsa varsayılanlık aynı transaction içinde kaldırılır veya başka ürüne aktarılır.

### 2.3 Hangi revizyon kullanılacak?

Yeni kayıt için:

```text
revision =
  ürünün effectiveFrom <= smokedAt olan revizyonları içindeki en yenisi
```

- Hızlı kayıt “şimdi” zamanını kullandığı için güncel revizyon seçilir.
- Geçmiş tarihli kayıt, o tarihte yürürlükte olan revizyonu seçer.
- Seçilen tarihten önce hiçbir revizyon yoksa maliyet ve kimyasal değerler sessizce güncel değerle doldurulmaz.
- Böyle bir durumda kullanıcıya “Bu tarih için kayıtlı ürün değeri yok” gösterilir.
- Kullanıcı açıkça isterse “Mevcut değerleri bu kayıtta kullan” seçeneği sunulur; bu tercih yalnızca o kaydın snapshot’ını etkiler.

Gelecek tarihli kayıt normal akışta engellenir. Daha sonra planlı fiyat revizyonu eklenirse bu ayrı bir ürün yönetimi özelliği olur; sigara kaydı geleceğe oluşturulmaz.

### 2.4 Para hesaplama kuralları

- Para değerleri `Double` veya `Float` ile saklanmaz.
- Veritabanında `Long micros` kullanılır.
- Kullanıcı girişi locale uyumlu `BigDecimal` olarak parse edilir.
- `pricePerCigarette = packPrice / cigarettesPerPack`.
- Bölme sonucu belirlenen mikro para hassasiyetine `HALF_UP` ile yuvarlanır.
- Paket adedi pozitif tam sayı olmalıdır.
- Fiyat boş olabilir; boş fiyat `0` değildir.
- Para birimi ISO 4217 kodu olarak saklanır.
- Farklı para birimleri tek toplamda otomatik toplanmaz. Dönem içinde birden fazla para birimi varsa toplamlar para birimine göre ayrı gösterilir.

### 2.5 Kayıt düzenleme davranışı

| Kullanıcı işlemi | Snapshot davranışı |
|---|---|
| Not, tetikleyici, ruh hâli, ortam veya istek seviyesi değişir | Ürün ve fiyat snapshot’ı korunur |
| Adet veya içilen oran değişir | Aynı snapshot korunur, toplam maliyet ve emisyon yeniden hesaplanır |
| Ürün değişir | Seçilen ürünün kayıt tarihindeki geçerli revizyonu yeniden snapshot alınır |
| Tarih/saat değişir ama aynı revizyon aralığında kalır | Snapshot korunur |
| Tarih/saat başka ürün revizyonu aralığına geçer | Kullanıcıya revizyon farkı gösterilir; varsayılan eylem yeni tarihin revizyonunu kullanmaktır |
| Eski tarihte revizyon bulunamaz | Değerler bilinmiyor kalır veya kullanıcı açıkça mevcut değerleri kullanır |

Kayıt düzenleme tek Room transaction içinde yapılır. `createdAt` korunur, `updatedAt` güncellenir.

### 2.6 Çoğaltma davranışı

“Çoğalt” doğrudan veritabanına kopya yazmaz:

- detay formunu kaynak kaydın adet, ürün ve opsiyonel alanlarıyla açar,
- tarih/saat varsayılan olarak “şimdi” olur,
- ürün revizyonu yeni tarih/saat için tekrar çözülür,
- kullanıcı onayından sonra yeni UUID ile kaydedilir.

Böylece eski fiyat snapshot’ının yanlışlıkla bugünkü kayda taşınması engellenir.

### 2.7 Silme, geri alma ve arşivleme

- Sigara kaydı silindikten sonra snackbar ile kısa süreli geri alma sunulur.
- Geri alma, silinen entity’nin bütün snapshot alanlarını aynen geri yükler.
- Ürünler geçmiş kayıtlarla bağlı olabileceği için fiziksel silme yerine arşivlenir.
- “Tüm verileri sil” ayrı, açık onay ve mümkünse biyometrik doğrulama isteyen geri döndürülemez bir işlemdir.
- Backup içe aktarma dışında `REPLACE` ile sessiz veri ezme yapılmaz.

### 2.8 Zaman ve gün sınırı

- Olay zamanı `Instant` karşılığı epoch millis olarak saklanır.
- Kayıtta olay anındaki `zoneIdSnapshot` bulunur.
- Kullanıcı gün başlangıcını örneğin `04:00` seçebilir.
- Günlük gruplama, seçili gün başlangıcı ve kayıt zone snapshot’ı ile domain katmanında yapılır.
- Saat dilimi ve gün başlangıcı değişikliği kayıtların gerçek zamanını değiştirmez; yalnızca raporlama gruplarını etkileyebilir.
- DST geçişleri için özel testler yazılır.

### 2.9 Bilinmeyen değerler

- Bilinmeyen fiyat veya kimyasal değer `null` kalır.
- Bilinmeyen değer toplamda sıfır gibi sayılmaz.
- Her sonuç `knownCount` ve `unknownCount` taşır.
- Kullanıcıya “7 sigara üzerinden hesaplandı, 3 sigaranın değeri bilinmiyor” biçiminde coverage gösterilir.

## 3. Hedef mimari

Mevcut tek modüllü yapı MVP boyunca korunabilir; fakat paket sınırları netleştirilir:

```text
core/
  database/       Room entity, DAO, migration
  data/           repository implementasyonları, DataStore
  domain/
    model/        UI ve veritabanından bağımsız modeller
    calculation/  maliyet, maruziyet, dönem metrikleri
    usecase/      kayıt ekle/düzenle, ürün revizyonu, import/export
  common/         saat, sayı/para biçimleme, validation

feature/
  today/
  records/
  products/
  health/
  analytics/
  settings/
  backup/
  security/
```

Kurallar:

- Compose ekranı doğrudan DAO çağırmaz.
- ViewModel hesaplama kurallarını kendi içinde çoğaltmaz.
- Hesaplamalar saf domain sınıflarında yapılır.
- `Clock`, `ZoneId` ve UUID üretimi testlerde değiştirilebilir bağımlılıklar olur.
- UI state’leri loading, content, empty ve error durumlarını açıkça taşır.

## 4. Uygulama fazları

### Faz 0 — Mevcut tabanı sabitleme

Amaç: Yeni şema çalışmalarından önce güvenilir başlangıç noktası oluşturmak.

İşler:

- mevcut değişiklikleri kapsamlı ve anlamlı Git commit’lerine ayırmak,
- güncelliğini kaybeden `PROJECT_REVIEW_AND_ARCHITECTURE.md` durum bölümünü yenilemek,
- proje köküne kurulum, build ve test komutlarını içeren `README.md` eklemek,
- şablon `ExampleUnitTest` ve `ExampleInstrumentedTest` dosyalarını kaldırmak,
- ortak `Clock`, dispatcher ve UUID sağlayıcılarını eklemek,
- hata/snackbar olay modelini tek kullanımlık event yapısına taşımak,
- Room ve DataStore hata durumları için kullanıcıya gösterilebilir hata modeli oluşturmak.

Kabul kriterleri:

- `assembleDebug`, `testDebugUnitTest` ve `lintDebug` başarılı,
- çalışma ağacında hangi değişikliğin hangi faza ait olduğu açık,
- dokümantasyon mevcut uygulamayı doğru tarif ediyor.

### Faz 1 — Ürün revizyonu ve maliyet veri temeli

Amaç: Fiyat ve ürün değeri değişikliklerinin geçmişi bozmamasını garanti etmek.

Şema:

- `CigaretteProductRevisionEntity` eklenir.
- `SmokingRecordEntity` içine nullable `productRevisionIdSnapshot` ve gerekirse `valueSourceSnapshot` eklenir.
- Sağlık entity’sine sonraki faz için tansiyon ve kilo kolonları eklenebilir.
- İlgili indeksler:
  - `(productId, effectiveFromEpochMillis)`
  - `smokedAtEpochMillis`
  - `trigger`, `mood`

Migration `2 → 3`:

1. Revision tablosunu oluştur.
2. Her mevcut ürün için mevcut değerlerden başlangıç revizyonu üret.
3. Başlangıç revizyonunun yürürlük zamanını ürün `createdAt` değeri yap.
4. Mevcut smoking record snapshot’larını kesinlikle yeniden hesaplama.
5. Mümkünse record–revision ilişkisini yalnızca güvenle eşleştirilebilen satırlarda doldur; diğerlerinde nullable bırak.
6. Eski ürün değer kolonlarını tek migration içinde hemen kaldırmak yerine bir geçiş sürümünde tut; veri doğrulandıktan sonraki migration’da sadeleştir.

Domain işleri:

- `ResolveProductRevisionAtTimeUseCase`
- `CreateSmokingRecordUseCase`
- `UpdateSmokingRecordUseCase`
- `CalculatePricePerCigaretteUseCase`
- locale uyumlu para parser/formatter
- validation sonuç modeli: error ve warning ayrımı

Testler:

- 100 TL / 20 adet = doğru mikro fiyat,
- fiyat boşsa sigara başı fiyat null,
- fiyat artışı eski kayıt snapshot’ını değiştirmez,
- fiyat artışından sonraki kayıt yeni fiyatı alır,
- geçmiş tarihli kayıt o tarihin revizyonunu alır,
- revizyon bulunmayan geçmiş tarihte değer bilinmiyor kalır,
- adet/oran düzenlemek snapshot’ı değiştirmez,
- ürün değiştirmek doğru revizyonu snapshot alır,
- `2 → 3` migration mevcut kayıt sayılarını ve snapshot değerlerini korur.

Kabul kriteri:

Ürün fiyatı veya kimyasal değeri değiştirildiğinde geçmiş dönem toplamlarının byte düzeyinde aynı snapshot verilerinden üretildiği testle kanıtlanır.

### Faz 2 — Tam ürün yönetimi

Amaç: Ayarlar içindeki ürün bölümünü gerçek yönetim ekranına dönüştürmek.

Ekranlar:

- ürün listesi,
- ürün ekleme,
- ürün detayı,
- kimlik bilgilerini düzenleme,
- yeni değer/fiyat revizyonu oluşturma,
- revizyon geçmişini görüntüleme,
- kopyalama,
- arşivleme ve arşivden çıkarma,
- varsayılan ürün seçme.

Form alanları:

- marka, varyant ve görünen ad,
- nikotin, katran, CO,
- paket fiyatı,
- paket içi adet,
- para birimi,
- değer kaynağı,
- “şu tarihten itibaren geçerli” alanı.

UX kuralları:

- Fiyat düzenleme ekranında “Bu değişiklik geçmiş kayıtları etkilemez” açıkça yazılır.
- Kaydetmeden önce eski ve yeni sigara başı fiyat gösterilir.
- Yürürlük tarihi varsayılan olarak şimdidir.
- Geçmiş bir yürürlük tarihi seçilirse bu revizyonun mevcut kayıtları değiştirmediği; yalnızca bundan sonra eklenen ve olay zamanı bu revizyon aralığına düşen kayıtlar tarafından kullanılacağı açıklanır.
- Negatif değer engellenir.
- Aşırı ama teknik olarak mümkün değerler warning ile onay ister.
- Ondalık giriş cihaz locale’ini destekler.
- Son aktif ürün arşivlenemez veya önce başka ürün seçilmesi istenir.

Kabul kriterleri:

- ürünün bütün yönetim işlemleri gerçek Room verisiyle çalışır,
- hiçbir ürün işlemi geçmiş smoking record satırlarını değiştirmez,
- varsayılan ürün transaction bütünlüğü korunur.

### Faz 3 — Kayıt ekleme, detay ve düzenleme

Amaç: Kayıtlar ekranını eksiksiz günlük yönetim aracına dönüştürmek.

Özellikler:

- kayıt detay bottom sheet’i,
- tarih ve saat seçimi,
- ürün seçimi,
- adet,
- içilen oran: `%25`, `%50`, `%75`, `%100`,
- istek seviyesi `1–5`,
- tetikleyici,
- ruh hâli,
- ortam,
- not,
- kayıt düzenleme,
- çoğaltma,
- geçmiş kayıt ekleme,
- detaylı silme onayı ve geri alma.

Filtreler:

- başlangıç/bitiş tarihi,
- ürün,
- tetikleyici,
- ruh hâli,
- bilinmeyen fiyat veya kimyasal değer,
- yalnızca not içerenler.

Liste geliştirmeleri:

- grup başlığında günlük adet, maliyet ve coverage-aware emisyon toplamları,
- kartta sigara başı snapshot değerleri,
- “Ürün değerleri bilinmiyor” durumu,
- aktif filtre chip’leri,
- filtreleri temizleme,
- boş sonuç ve boş arşiv durumları.

Dayanıklılık:

- hızlı art arda tıklamada çift UUID/kayıt engeli,
- save sırasında buton disable,
- process death sonrası form için `SavedStateHandle`,
- gelecekte tarih engeli,
- silme geri alma süresi ve erişilebilir snackbar aksiyonu.

Testler:

- hızlı kayıt,
- manuel geçmiş kayıt,
- opsiyonel alanlar boş kayıt,
- düzenleme snapshot kuralları,
- çoğaltmada güncel revizyon seçimi,
- arama ve bütün filtre kombinasyonları,
- silme/geri alma,
- aynı anda çift kaydetme engeli.

### Faz 4 — Bugün ekranını tamamlama

Amaç: Ana ekranı günlük kullanımın eksiksiz merkezi yapmak.

Eklenecekler:

- ortalama iki sigara arası süre,
- ilk sigaranın saati,
- bugünkü tahmini maliyet,
- coverage-aware maruziyet kartları,
- “Tüm kayıtları gör” aksiyonu,
- hızlı kayıt sonrası detay ekleme çağrısı,
- varsayılan ürün yok durumu,
- ürün seçerek hızlı kayıt,
- maliyet/maruziyet kartlarının ayara göre görünürlüğü.

Gerçek zaman:

- “son kayıttan beri” yalnızca ekran görünürken düşük frekanslı ticker ile güncellenir,
- saniyelik polling yapılmaz,
- uygulama arka plana geçtiğinde ticker durur.

Kabul kriterleri:

- gün başlangıcı tercihi bütün Bugün metriklerinde aynı şekilde uygulanır,
- bilinmeyen değerler toplamda sıfır gösterilmez,
- hızlı kayıt çevrimdışı ve tek dokunuşla tamamlanır.

### Faz 5 — Maliyet ve gelişmiş analiz

Amaç: Dönemsel davranış ve maliyet görünürlüğünü tamamlamak.

Dönemler:

- 7 gün,
- 30 gün,
- 90 gün,
- tüm zamanlar,
- özel tarih aralığı.

Metrikler:

- toplam ve günlük ortalama,
- en yüksek ve en düşük tüketimli gün,
- ilk sigaraya kadar geçen süre için opsiyonel uyanma saati ayarı,
- ortalama kayıt aralığı,
- en uzun sigarasız süre,
- en sık ürün, tetikleyici ve ruh hâli,
- saat ve gün dağılımları,
- nikotin, katran ve CO toplamları,
- maliyet toplamları,
- önceki eş uzunluktaki dönemle karşılaştırma.

Maliyet kartları:

- bugün,
- bu hafta,
- bu ay,
- seçilen dönem,
- mevcut hızla yıllık tahmin.

Yıllık tahmin:

- en az 7 farklı kayıtlı gün gerektirir,
- tercihen son 30 günün günlük ortalamasını kullanır,
- veri azsa tahmin üretilmez,
- “mevcut kayıt hızına göre tahmin” etiketi taşır,
- farklı para birimleri ayrı gösterilir.

Insight engine:

- önceki döneme göre en az `%10` azalma,
- ortalama aralıkta en az 10 dakika artış,
- en az `%30` tetikleyici yoğunluğu,
- belirli saat aralığında en az `%30` yoğunluk,
- gece 22:00 sonrası değişim,
- yüksek bilinmeyen değer oranı,
- yeterli veri eşikleri.

Kurallar saf domain sınıflarında ve yargılayıcı olmayan metin anahtarlarıyla uygulanır.

Grafikler:

- yalnızca renge dayanmaz,
- TalkBack özeti bulunur,
- altında aynı verinin metinsel özeti yer alır,
- “renk sağlık açısından güvenli sınır göstermez” açıklaması korunur.

### Faz 6 — Sağlık günlüğünü tamamlama

Durum: tamamlandı.

Şema:

- `systolicBloodPressure`
- `diastolicBloodPressure`
- `weightGrams` veya hassasiyet kaybı olmayan eşdeğer sabit nokta alanı

Özellikler:

- bugünün kaydını düzenleme,
- geçmiş sağlık gününü seçme,
- son 7/14/30 gün özeti,
- kişisel not geçmişi,
- sigara sayısıyla enerji, stres, uyku ve belirti karşılaştırmaları,
- yeterli veri yok durumu.

İletişim:

- en az 7, tercihen 14 ortak gün olmadan ilişki metni üretme,
- “birlikte görülüyor” dili kullan,
- hiçbir zaman neden-sonuç veya teşhis iddiası kurma,
- nefes darlığı/göğüs rahatsızlığı için tanı koymadan uygun sağlık desteği uyarısı göster.

Testler:

- fizyolojik alan sınırları,
- nullable semptomlar,
- güncellemede `createdAt` korunması,
- ilişki üretmek için veri eşiği,
- korelasyon metninin nedensellik iddiası içermemesi.

### Faz 7 — Ayarlar ve kişiselleştirme

Durum: tamamlandı.

Eklenecek tercihler:

- dinamik renk,
- para birimi,
- gün başlangıç saati,
- haftanın ilk günü,
- maruziyet kartlarını göster/gizle,
- maliyet kartını göster/gizle,
- sağlık sekmesini göster/gizle,
- bildirim tercihleri,
- biyometrik kilit,
- sağlık bilgilendirmesi,
- gizlilik açıklaması.

Kurallar:

- tercih değişiklikleri DataStore’dan reaktif akar,
- para birimi değişikliği eski snapshot para birimlerini dönüştürmez,
- gün başlangıcı değişikliği olay timestamp’lerini değiştirmez,
- tema ve dinamik renk bütün ekranlarda anında uygulanır.

### Faz 8 — Yerel yedekleme, dışa ve içe aktarma

Durum: tamamlandı.

JSON:

- versioned manifest,
- ürünler,
- ürün revizyonları,
- sigara kayıtları,
- sağlık günlükleri,
- taşınabilir tercihler,
- export zamanı ve uygulama sürümü.

CSV:

- ayrı ürün, ürün revizyonu, sigara kaydı ve sağlık dosyaları,
- UTF-8,
- locale’den bağımsız makine formatlı sayılar,
- kullanıcı metinlerinde `=`, `+`, `-`, `@` öneklerine karşı spreadsheet formula injection koruması.

Android:

- Storage Access Framework,
- geniş depolama izni yok,
- export için `ACTION_CREATE_DOCUMENT`,
- import için `ACTION_OPEN_DOCUMENT`.

Import:

1. dosyayı geçici olarak parse et,
2. format ve şema sürümünü doğrula,
3. bütün referansları ve sayısal sınırları doğrula,
4. kullanıcıya özet göster,
5. “birleştir” veya “mevcut verinin yerine koy” seçtir,
6. tek transaction içinde uygula,
7. hata olursa transaction’ı tamamen geri al.

Birleştirme:

- UUID aynı ve içerik aynıysa duplicate atlanır,
- UUID aynı ama içerik farklıysa otomatik ezilmez; conflict sayısı gösterilir,
- ürün revizyon bağlantıları doğrulanır,
- mevcut snapshot kayıtları ürünün güncel değeriyle yeniden hesaplanmaz.

Testler:

- JSON round-trip,
- CSV escaping,
- bozuk JSON,
- desteklenmeyen sürüm,
- yarım referans,
- duplicate,
- conflict,
- transaction rollback.

### Faz 9 — Bildirimler ve biyometrik kilit

Durum: tamamlandı.

Bildirimler:

- varsayılan kapalı,
- Android 13+ izni yalnızca kullanıcı özelliği açtığında istenir,
- akşam özeti,
- haftalık özet,
- uzun süre kayıt yok hatırlatması,
- hedef altyapısı eklenirse kişisel hedef özeti,
- WorkManager unique work adları,
- saat/tercih değişince işi yeniden planlama,
- yargılayıcı olmayan metin.

Biyometrik:

- cihaz desteğini kontrol et,
- kullanıcı açtığında doğrulama iste,
- uygulama belirlenen arka plan süresinden sonra açıldığında kilitle,
- destek yoksa ayarı aktif etme,
- uygulama verisini şifrelediği izlenimini verme; bu özellik uygulama erişim kapısıdır.

### Faz 10 — Test, erişilebilirlik ve release sertleştirme

Durum: kişisel kullanım profili tamamlandı. Aşağıdaki mağaza yayını ve geniş
erişilebilirlik matrisi maddeleri gelecekte genel dağıtım kararı verilirse
yeniden ele alınacaktır.

Unit test:

- bütün maliyet ve maruziyet hesapları,
- nullable coverage,
- tarih/gün sınırı ve DST,
- dönem karşılaştırmaları,
- insight kuralları,
- ürün revizyon çözümü,
- import validation.

Instrumented test:

- DAO sorguları,
- `1 → 2 → 3` ve sonraki tüm migration zinciri,
- arşivleme ve geçmiş kayıt bütünlüğü,
- import transaction rollback.

ViewModel test:

- loading/content/empty/error,
- hızlı kayıt ve çift tıklama,
- düzenleme,
- filtreler,
- ayar değişimleri,
- notification permission reddi.

Compose UI test:

- onboarding,
- hızlı kayıt,
- ürün formu,
- kayıt düzenleme/silme,
- sağlık formu,
- tema değişimi,
- empty/error durumları,
- font scale `1.0`, `1.3`, `2.0`.

Erişilebilirlik:

- minimum 48 dp dokunma alanı,
- bütün anlamlı ikonlarda açıklama,
- grafik semantiği ve metinsel eşdeğer,
- yalnızca renk üzerinden durum anlatmama,
- kontrast kontrolü,
- TalkBack sırası,
- klavye ve switch access kontrolü.

Release:

- release signing yapılandırması gizli tutulur,
- versionCode/versionName politikası,
- R8/minify release build’i,
- release APK/AAB smoke testi,
- privacy policy ve mağaza metni,
- uygulama ikonları ve ekran görüntüleri,
- internet izni bulunmadığının doğrulanması,
- otomatik Android bulut yedeklemesinin kapalı olduğunun doğrulanması,
- üçüncü taraf analitik/izleme bulunmadığının doğrulanması,
- crash ve ANR için yerel hata dayanıklılığı kontrolü.

## 5. Migration sırası

Önerilen şema evrimi:

| Sürüm | İçerik |
|---|---|
| 1 | Ürün ve sigara kayıtları |
| 2 | Günlük sağlık kaydı |
| 3 | Ürün revizyonları, record revision referansı, sorgu indeksleri |
| 4 | Tansiyon ve kilo alanları; gerekiyorsa metadata |
| 5 | Backup/import için gerekli sürüm metadata’sı veya conflict audit alanları |

Her sürüm için:

- schema JSON Git’e eklenir,
- destructive migration kullanılmaz,
- önceki bütün desteklenen sürümlerden en son sürüme migration testi çalışır,
- migration öncesi ve sonrası row count ile kritik snapshot değerleri karşılaştırılır.

## 6. Öncelik ve bağımlılık sırası

```text
Faz 0
  ↓
Faz 1 — ürün revizyonu ve snapshot omurgası
  ↓
Faz 2 — ürün yönetimi
  ↓
Faz 3 — kayıt detay/düzenleme/geçmiş giriş
  ↓
Faz 4 — Bugün ekranı
  ↓
Faz 5 — maliyet, analiz ve insight
  ↓
Faz 6 — sağlık genişletmesi
  ↓
Faz 7 — ayarlar
  ↓
Faz 8 — yedekleme/import
  ↓
Faz 9 — bildirim ve biyometrik
  ↓
Faz 10 — release sertleştirme
```

Faz 6 ve Faz 7, Faz 3 tamamlandıktan sonra kısmen paralel ilerleyebilir. Faz 8, veri modeli sabitlenmeden başlanmamalıdır. Analiz ve insight motoru, kayıt snapshot ve zaman semantiği kesinleşmeden genişletilmemelidir.

## 7. Her faz için ortak Definition of Done

Bir faz ancak aşağıdakilerin tamamı sağlanınca bitmiş sayılır:

- kullanıcı akışı placeholder olmadan çalışıyor,
- iş kuralı UI’dan bağımsız ve test edilebilir,
- nullable ve hata durumları ele alınmış,
- metinler yargılayıcı veya tıbben yanıltıcı değil,
- TalkBack ve büyük font kontrolü yapılmış,
- yeni Room şeması varsa migration ve migration testi var,
- `assembleDebug` başarılı,
- `testDebugUnitTest` başarılı,
- `lintDebug` başarılı,
- ilgili instrumented testler emülatörde başarılı,
- emülatörde açık ve koyu tema smoke testi yapılmış,
- dokümantasyon güncellenmiş,
- değişiklik anlamlı Git commit’lerine ayrılmış.

## 8. MVP, release adayı ve sonraki sürüm sınırı

### MVP için zorunlu

- Faz 0–8,
- temel bildirimler isteniyorsa Faz 9’un bildirim kısmı,
- Faz 10’un bütün kalite kapıları.

### Release adayı için kuvvetle önerilen

- biyometrik uygulama kilidi,
- bütün migration zinciri testleri,
- backup round-trip testleri,
- font scale ve TalkBack doğrulaması,
- release AAB smoke testi.

### İlk sürüm kapsamı dışında

- Health Connect,
- bulut senkronizasyonu,
- kullanıcı hesabı,
- sosyal özellikler,
- çevrimiçi marka/veri tabanı,
- yapay zekâ servisi,
- tıbbi teşhis veya tedavi önerisi,
- canlı döviz kuru ile eski maliyetleri tek para birimine dönüştürme.

## 9. İlk başlanacak uygulama paketi

Bir sonraki implementasyon paketi aşağıdaki sırada olmalıdır:

1. Faz 0 dokümantasyon ve test temizliği.
2. `CigaretteProductRevisionEntity` tasarımı.
3. Room `2 → 3` migration ve migration test altyapısı.
4. Yürürlük tarihine göre revizyon çözen domain use-case.
5. Paket fiyatı/paket adedi formu ve güvenli para hesaplama.
6. Ürün fiyat revizyonu ekranı.
7. Geçmiş kaydı değiştirmeme ve fiyatın yürürlük anından sonraki yeni kaydı güncel fiyatlandırma testleri.
8. Ardından kayıt detay/düzenleme ekranına geçiş.

Bu sıra, maliyet ve analiz ekranlarına geçmeden önce geçmiş verinin güvenilirliğini garanti eder.
