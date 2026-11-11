"""Пример использования PyBridge.

Запуск из корня репозитория:
    python examples/example.py ТОКЕН
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "python"))

from pybridge import Minecraft  # noqa: E402

token = sys.argv[1] if len(sys.argv) > 1 else input("Токен: ").strip()

with Minecraft(token) as mc:
    print(mc.players())
    mc.say("Привет из Python!")
    mc.time_set("day")
    mc.weather("clear")
    # mc.give("Steve", "diamond", 64)
    # mc.tp("Steve", 0, 100, 0)
    # print(mc.cmd("any command here"))
