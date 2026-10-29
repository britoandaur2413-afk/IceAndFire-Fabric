"""Every message sent to clients must be registered client-bound (with a client handler), every message sent to
the server server-bound. A missing direction disconnects the player when the message is sent.

usage: python -I tools/check_network.py
"""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
sources = {p: p.read_text(encoding="utf-8") for p in (ROOT / "src").rglob("*.java")}
code = "\n".join(sources.values())
net = (ROOT / "src/main/java/com/iafabric/network/IafNetwork.java").read_text(encoding="utf-8")
client = (ROOT / "src/client/java/com/iafabric/client/ClientNetwork.java").read_text(encoding="utf-8")

registered = {}
for m in re.finditer(r'register\("[a-z_]+", (\w+)\.class,.*?, (true|false), (true|false), ', net):
    registered[m.group(1)] = (m.group(2) == "true", m.group(3) == "true")

problems = []
for cls in sorted(set(re.findall(r"sendTo(?:All|Player|Tracking)\(new (\w+)", code))):
    if cls not in registered or not registered[cls][1]:
        problems.append(cls + " is sent to clients but not registered client-bound")
    if not re.search(r"on\(" + cls + r"\.class", client):
        problems.append(cls + " is sent to clients but the client has no handler (ClientNetwork)")
for cls in sorted(set(re.findall(r"sendToServer\(new (\w+)", code))):
    if cls not in registered or not registered[cls][0]:
        problems.append(cls + " is sent to the server but not registered server-bound")
for p in problems:
    print(p)
print(len(problems), "problems")
