# pybridge-mc

Python-клиент для плагина [PyBridge](../README.md). Без внешних зависимостей.

```python
from pybridge import Minecraft

with Minecraft(token="ТОКЕН") as mc:
    mc.say("Привет!")
    print(mc.cmd("list"))
```
