# Bulgular

Tüm bulgular 2026-11-10 tarihinde cihaz üzerinde ADB ile doğrulandı.

## Güvenlik ve gizlilik

- **SELinux permissive** gelir (`ro.boot.selinux=permissive`). Bu yüzden `shell` kullanıcısı normalde yazamayacağı
  `persist.*` özelliklerini yazabiliyor.
- **ADB ağ üzerinden kutudan açık** gelir (port 5555); bağlantı yine de ekranda onay ister.
- Güvenlik yaması **2022-02-05**. Bootloader kilitli, ancak `ro.oem_unlock_supported=1`.
- Kendini **Google ADT-3** olarak tanıtır (`ro.build.fingerprint`), muhtemelen sertifika kontrollerini geçmek için.
- `com.htc.*` paketlerinin HTC ile ilgisi yok: üretici **Hotack** (ayarlar APK'sı `HotackSettingsT08_QZ`).
- Stok launcher bir reklam SDK'sı içerir: `com.zeasn.ad.connector`.
- `com.android.toofifi` kendini **`com.huawei.connection`** adıyla çalıştırır; sistem uid'si (1000), internet, hassas
  konum ve telefon durumu izinleri vardır. Kaldırıldı.
- `com.htc.eventuploadservice` her açılışta başlayan bir olay yükleyicidir. Kaldırıldı.

## Keystone

Ayarlar uygulaması (`com.htc.htcsettings`) JADX ile incelendi.

- Dört köşe `persist.display.keystone_{lb,lt,rt,rb}{x,y}` özelliklerinde tamsayı olarak tutulur (değer / 1000 = oran).
- Ayarlar uygulaması değerleri SurfaceFlinger'a `transact(1050)` ile, 8 float olarak gönderir
  (sıra: lb, lt, rt, rb; her biri x, y).
- **`setprop` ile yazmak çalışıyor** ve SurfaceFlinger değerleri **açılışta** uygular
  (logcat: `mKeystoneTran[i][j]`). Test: `lbx` 222 → 260 → 222.
- **Anında uygulamak** için iki yol var, ikisi de `shell` için kapalı:
  - SurfaceFlinger `1050` → `Operation not permitted`
  - Korumalı yayın `android.intent.hotack_keystone` (system_server dinliyor) → `Permission Denial`
- Orijinal değerler: [snapshots/2026-11-10/before/keystone.txt](../snapshots/2026-11-10/before/keystone.txt)
- Projeksiyon ayarları ekranı doğrudan açılabilir:
  `am start -n com.htc.htcsettings/com.htc.activity.ProjectionActivity3`

![Projeksiyon ayarları](img/projection-settings.png)

## Güç ve uyku

- Uykuda (`KEYCODE_SLEEP`) işlemci derin uykuya geçer ve Wi-Fi kopar; ağdan uyandırılamaz. Wake-on-WLAN izi yok.
- Kumandanın Menü tuşu sistem genelinde Hotack ayarlarını açar.

## Performans ölçümleri

| Değişiklik | Sonuç |
| --- | --- |
| Uygulama kaldırma | Veri bölümü kullanımı 1.4 GB → 835 MB |
| Stok launcher → FLauncher | Aynı oturumda MemAvailable 398 → 480 MB (+81 MB) |
| Google servislerini kapatma | Google süreçleri ~83 MB → 0, ama açılıştan 2 dk sonra MemAvailable ölçülebilir şekilde artmadı (408 → 394 MB); boşalan bellek önbelleğe gidiyor |
| Ek 15 paket kapatma | Used RAM 689 → 669 MB |

- FLauncher (Flutter) tek başına ~93 MB kullanır; daha hafif bir native launcher ileride kazanç sağlayabilir.
- App freezer desteklenmiyor: `Freezer.killable not present, disabling freezer`.
- `shell` başka bir uygulamanın tek bir bileşenini kapatamaz (`pm disable <component>` → `SecurityException`).

## Bilinen tuzaklar

- `pm uninstall` / `disable-user` sonrası hemen yeniden başlatılırsa değişiklik kaybolur; ~30 sn bekleyin.
- `adb shell`, bir `while read` döngüsünün girdisini yutar; scriptlerde stdin kapatılır.
- FLauncher uygulama listesini önbellekte tutar; paketler değişince `pm clear me.efesser.flauncher` gerekebilir
  (bu varsayılan ana ekran ayarını da siler, `apply.sh` yeniden ayarlar).
- SmartTube'un Kasım 2025'teki imza anahtarı sızıntısı nedeniyle sadece resmi GitHub'daki 30.56+ sürümleri kullanın.
