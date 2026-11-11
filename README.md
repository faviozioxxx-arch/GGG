# PyBridge

![Build](https://github.com/YOUR_NAME/PyBridge/actions/workflows/build.yml/badge.svg)
![Paper](https://img.shields.io/badge/Paper-1.21.4-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![License](https://img.shields.io/badge/license-MIT-green)

Плагин для **Paper 1.21.4**, который позволяет управлять сервером из **Python**: вы пишете команды в Python-скрипте, а они выполняются на сервере.

```python
from pybridge import Minecraft

mc = Minecraft(token="ВАШ_ТОКЕН")
mc.say("Привет из Python!")
mc.give("Steve", "diamond", 64)
print(mc.cmd("list"))   # любая команда сервера
```

## Установка

1. Скачайте `PyBridge.jar` и `pybridge.py` из раздела [Releases](../../releases).
2. Положите `PyBridge.jar` в папку `plugins` сервера и запустите сервер.
3. Откройте `plugins/PyBridge/config.yml` и скопируйте `token` (создаётся автоматически).
4. Положите `pybridge.py` рядом со своим скриптом.

Требуется Paper 1.21.4 и Java 21. Для клиента нужен Python 3.8+, сторонние библиотеки не нужны.

## Использование

Из кода:

```python
from pybridge import Minecraft

with Minecraft(token="ТОКЕН", host="127.0.0.1", port=25580) as mc:
    mc.tp("Steve", 0, 100, 0)
    mc.time_set("day")
    mc.weather("clear")
    print(mc.cmd("whitelist list"))
```

Интерактивная консоль (пишете команды без `/`):

```
python pybridge.py ТОКЕН
mc> list
mc> gamemode creative Steve
```

Готовые методы: `say`, `tell`, `give`, `tp`, `gamemode`, `time_set`, `weather`, `setblock`, `kick`, `op`, `deop`, `players`. Всё остальное — через `cmd("...")`.

## Настройка (`plugins/PyBridge/config.yml`)

| Параметр | По умолчанию | Описание |
|---|---|---|
| `bind` | `127.0.0.1` | Адрес прослушивания. `0.0.0.0` — доступ извне |
| `port` | `25580` | Порт |
| `token` | генерируется | Пароль для подключения |
| `timeout-seconds` | `10` | Ожидание выполнения одной команды |

## Безопасность

Подключившийся клиент выполняет команды **от имени консоли**, то есть имеет полный контроль над сервером.

- Оставляйте `bind: 127.0.0.1`, если Python работает на той же машине.
- Если открываете порт наружу, держите токен в секрете и ограничьте порт файрволом.
- Не публикуйте `config.yml` с токеном.

## Сборка из исходников

```
mvn package
```

Готовый файл: `target/PyBridge.jar`. Нужны JDK 21 и Maven.

## Протокол

Текстовый, UTF-8, одна строка = одно сообщение:

```
клиент -> AUTH <token>        сервер -> OK | ERR
клиент -> <команда>           сервер -> OK <base64 вывода> | ERR <base64 вывода>
```

## Релизы

GitHub Actions собирает проект при каждом push. Чтобы выпустить версию:

```
git tag v1.0.0
git push origin v1.0.0
```

В релиз автоматически попадут `PyBridge.jar` и `pybridge.py`.

## Лицензия

[MIT](LICENSE)
