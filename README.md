# projector

MUTU HY320 Mini Android projeksiyon cihazını kurcalama projesi. Root olmadan, sadece Wi-Fi üzerinden ADB ile:
reklam ve telemetri içeren üretici yazılımını temizler, cihazı hızlandırır ve donanımın nasıl çalıştığını belgeler.

| Önce | Sonra |
| --- | --- |
| ![Stok ana ekran](docs/img/home-before.png) | ![Temizlenmiş ana ekran](docs/img/home-after.png) |

## Cihaz

| | |
| --- | --- |
| SoC | Allwinner H713 (`sun50iw12p1`), 4× Cortex-A53, 32-bit userland |
| RAM / depolama | ~1 GB / 4.7 GB veri bölümü |
| Çözünürlük | 1280×720 |
| Android | 11, `user` build, root yok, SELinux **permissive** |
| Wi-Fi | AIC8800 (`aicwf_sdio`) |
| İç platform adı | `hy260`; kendini Google ADT-3 olarak tanıtıyor |

Ayrıntılar ve bulgular: [docs/findings.md](docs/findings.md)

## Kullanım

Gereken: [Android platform-tools](https://developer.android.com/tools/releases/platform-tools) (`adb`), aynı Wi-Fi ağı.
Cihazda geliştirici seçenekleri ve ağ üzerinden hata ayıklama açık olmalı; ilk bağlantıda ekrandaki izni onaylayın.

```sh
export DEV=192.168.1.112:5555        # projektörün IP:port'u
export ADB=/path/to/adb              # PATH'te değilse

scripts/apply.sh --dry-run           # ne yapılacağını göster
scripts/apply.sh                     # temizliği uygula (tekrar çalıştırmak güvenli)
scripts/restore.sh                   # her şeyi geri al
scripts/restore.sh com.android.music # tek paketi geri al
scripts/snapshot.sh                  # cihaz durumunu snapshots/<tarih>/ altına kaydet
```

Yeni launcher olarak [FLauncher](https://gitlab.com/flauncher/flauncher) kurulu olmalı; değilse stok launcher yerinde
bırakılır (aksi halde cihaz siyah ekranla açılır).

> Paket değişikliklerinden sonra yeniden başlatmadan önce ~30 saniye bekleyin; Android değişiklikleri diske geç yazar
> ve erken yeniden başlatmada kaybolurlar.

## Yapı

```
config/packages.tsv   kaldırılan / kapatılan paketler ve nedenleri (tek kaynak)
config/settings.tsv   değiştirilen ayarlar ve stok değerleri
scripts/              apply, restore, snapshot
snapshots/            cihaz dökümleri (seri numarası ve MAC maskeli)
docs/                 bulgular ve ekran görüntüleri
```
