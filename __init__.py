"""
pybridge.py — клиент для плагина PyBridge (Paper 1.21.4).

Пример:
    from pybridge import Minecraft

    mc = Minecraft(token="ТВОЙ_ТОКЕН_ИЗ_config.yml")
    mc.say("Привет из Python!")
    mc.give("Steve", "diamond", 64)
    print(mc.cmd("list"))

Интерактивный режим (вводишь команды прямо в консоли):
    python pybridge.py ТВОЙ_ТОКЕН [host] [port]
"""
import base64
import socket
import sys


class MinecraftError(Exception):
    pass


class Minecraft:
    def __init__(self, token, host="127.0.0.1", port=25580, timeout=15):
        self.sock = socket.create_connection((host, port), timeout=timeout)
        self.file = self.sock.makefile("rw", encoding="utf-8", newline="\n")
        self._send("AUTH " + token)
        if self._recv() != "OK":
            self.close()
            raise MinecraftError("Неверный токен (смотри plugins/PyBridge/config.yml)")

    # ---------- низкий уровень ----------
    def _send(self, line):
        self.file.write(line + "\n")
        self.file.flush()

    def _recv(self):
        line = self.file.readline()
        if not line:
            raise MinecraftError("Соединение с сервером закрыто")
        return line.rstrip("\n")

    def cmd(self, command, check=False):
        """Выполнить любую команду сервера (со слэшем или без). Возвращает вывод команды."""
        command = command.replace("\n", " ").replace("\r", " ")
        self._send(command)
        reply = self._recv()
        status, _, payload = reply.partition(" ")
        text = base64.b64decode(payload).decode("utf-8") if payload else ""
        if check and status != "OK":
            raise MinecraftError(text or "Команда не выполнена")
        return text

    def close(self):
        try:
            self.file.close()
            self.sock.close()
        except OSError:
            pass

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()

    # ---------- удобные обёртки ----------
    def say(self, text):
        return self.cmd(f"say {text}")

    def tell(self, player, text):
        return self.cmd(f"tell {player} {text}")

    def give(self, player, item, count=1):
        return self.cmd(f"give {player} {item} {count}")

    def tp(self, player, target_or_x, y=None, z=None):
        if y is None:
            return self.cmd(f"tp {player} {target_or_x}")
        return self.cmd(f"tp {player} {target_or_x} {y} {z}")

    def gamemode(self, mode, player="@s"):
        return self.cmd(f"gamemode {mode} {player}")

    def time_set(self, value):
        return self.cmd(f"time set {value}")

    def weather(self, kind):
        return self.cmd(f"weather {kind}")

    def setblock(self, x, y, z, block):
        return self.cmd(f"setblock {x} {y} {z} {block}")

    def kick(self, player, reason=""):
        return self.cmd(f"kick {player} {reason}".strip())

    def op(self, player):
        return self.cmd(f"op {player}")

    def deop(self, player):
        return self.cmd(f"deop {player}")

    def players(self):
        return self.cmd("list")


def _repl(token, host, port):
    with Minecraft(token, host, port) as mc:
        print("Подключено. Вводи команды (без /). Выход: exit")
        while True:
            try:
                line = input("mc> ").strip()
            except (EOFError, KeyboardInterrupt):
                print()
                break
            if line.lower() in ("exit", "quit"):
                break
            if line:
                print(mc.cmd(line))


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Использование: python pybridge.py ТОКЕН [host] [port]")
        sys.exit(1)
    _repl(
        sys.argv[1],
        sys.argv[2] if len(sys.argv) > 2 else "127.0.0.1",
        int(sys.argv[3]) if len(sys.argv) > 3 else 25580,
    )
