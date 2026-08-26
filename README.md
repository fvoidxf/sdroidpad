# Android Sec Pad 1.0

Защищённый блокнот для Android с шифрованием записей.

## Возможности

- Создание, редактирование и удаление заметок
- Шифрование каждой записи отдельным паролем
- Три алгоритма шифрования: **AES-256**, **Кузнечик** (ГОСТ 34.12-2015), **Магма** (ГОСТ 28147)
- Безопасное удаление с многократной перезаписью данных
- Пароль запрашивается при открытии и удалении записи

## Архитектура

```
app/src/main/java/com/secnote/pad/
├── MainActivity.kt          — список записей
├── NoteEditActivity.kt      — создание/редактирование
├── PasswordDialogFragment.kt — диалог ввода пароля
├── CryptoManager.kt         — шифрование/дешифрование
├── NotesManager.kt          — хранение и управление записями
└── NoteMeta.kt              — модель метаданных
```

### Шифрование

| Алгоритм | Режим | Размер ключа | IV |
|----------|-------|-------------|-----|
| AES-256 | GCM | 256 бит | 12 байт |
| Кузнечик | CTR + HMAC-SHA256 | 256 бит | 16 байт |
| Магма | CTR + HMAC-SHA256 | 256 бит | 8 байт |

Ключи производятся из пароля через PBKDF2 (100 000 итераций, SHA-256).

Формат зашифрованных данных:
- AES: `salt(16) + iv(12) + ciphertext+tag`
- ГОСТ: `salt(16) + iv + hmac(32) + ciphertext`

### Хранение

- Метаданные: `notes_index.json` (внутреннее хранилище)
- Зашифрованные записи: `notes/<id>.enc`

## Сборка

Требования: JDK 17, Android SDK (platform 34).

```bash
# Открыть в Android Studio или собрать из командной строки:
./gradlew assembleDebug

# APK:
app/build/outputs/apk/debug/app-debug.apk
```

## Тестирование

```bash
./gradlew test
```

18 юнит-тестов покрывают:
- Roundtrip шифрования/дешифрования для всех трёх алгоритмов
- Отказ при неверном пароле
- Уникальность шифротекста (разные salt/iv)
- Безопасное удаление (обнуление данных)
- Граничные случаи (пустой контент, большой контент, неверный алгоритм, короткие данные)

## Структура проекта

```
AndroidSecPad/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/secnote/pad/
│       │   └── res/
│       │       ├── layout/
│       │       ├── values/
│       │       ├── drawable/
│       │       └── mipmap-anydpi-v26/
│       └── test/java/com/secnote/pad/
└── gradle/
```
