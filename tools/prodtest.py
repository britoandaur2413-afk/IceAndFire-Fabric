"""Production check: the remapped mod jar in a real Fabric 1.21.1 client (offline, its own game folder), with the
remapped test jar running some client tests (as gradlew runClienttest does in the dev world).

usage: python -I tools/prodtest.py [timeout_seconds] [only=menu,scenes] [with=other_mod.jar;...]
Build first: gradlew build remapGametestJar. Uses the CurseForge launcher's 1.21.1 (libraries, assets, client jar)
read only, Fabric Loader and Fabric API from the Gradle cache. Runs in build/prodtest; the log is build/prodtest/run.log,
the screenshots build/prodtest/screenshots.
"""
import json
import os
import pathlib
import shutil
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
INSTALL = pathlib.Path(r"C:/Users/Escobar/curseforge/minecraft/Install")
CACHE = pathlib.Path.home() / ".gradle/caches/modules-2/files-2.1"
RUN = ROOT / "build" / "prodtest"
JAVA = pathlib.Path(r"C:/Program Files/Java/jdk-21.0.12/bin/java.exe")
MC = "1.21.1"
LOADER = "0.16.10"
FABRIC_API = "0.116.17+1.21.1"
# Fabric Loader's own libraries (its launcher profile)
LOADER_LIBS = ["net.fabricmc:fabric-loader:" + LOADER, "net.fabricmc:sponge-mixin:0.15.4+mixin.0.8.7", "net.fabricmc:intermediary:" + MC + ":v2",
               "org.ow2.asm:asm:9.7.1", "org.ow2.asm:asm-analysis:9.7.1", "org.ow2.asm:asm-commons:9.7.1", "org.ow2.asm:asm-tree:9.7.1",
               "org.ow2.asm:asm-util:9.7.1"]


def cached(name):
    parts = name.split(":")
    group, artifact, version = parts[0], parts[1], parts[2]
    classifier = ("-" + parts[3]) if len(parts) > 3 else ""
    want = f"{artifact}-{version}{classifier}.jar"
    found = list((CACHE / group / artifact / version).glob("*/" + want))
    if not found:
        raise SystemExit("not in the Gradle cache: " + name)
    return found[0]


def allowed(lib):
    rules = lib.get("rules")
    if not rules:
        return True
    ok = False
    for r in rules:
        osn = r.get("os", {}).get("name")
        if osn is None or osn == "windows":
            ok = r["action"] == "allow"
    return ok


def main():
    nums = [a for a in sys.argv[1:] if a.isdigit()]
    timeout = int(nums[0]) if nums else 900
    only = next((a.split("=", 1)[1] for a in sys.argv[1:] if a.startswith("only=")), "menu,scenes")
    vanilla = json.loads((INSTALL / "versions" / MC / (MC + ".json")).read_text())
    cp = [cached(n) for n in LOADER_LIBS]
    for lib in vanilla["libraries"]:
        if allowed(lib):
            art = lib.get("downloads", {}).get("artifact")
            if art:
                cp.append(INSTALL / "libraries" / art["path"])
    cp.append(INSTALL / "versions" / MC / (MC + ".jar"))
    missing = [str(p) for p in cp if not p.exists()]
    if missing:
        print("missing libraries:", missing[:10])
        return 2

    for d in ("mods", "screenshots", "saves", "crash-reports"):
        shutil.rmtree(RUN / d, ignore_errors=True)
    (RUN / "mods").mkdir(parents=True, exist_ok=True)
    libs = ROOT / "build" / "libs"
    for f in libs.glob("IceAndFire-Fabric-*.jar"):
        if f.name.endswith("-sources.jar") or f.name.endswith("-gametest-dev.jar"):
            continue
        shutil.copy(f, RUN / "mods" / f.name)
    shutil.copy(cached("net.fabricmc.fabric-api:fabric-api:" + FABRIC_API), RUN / "mods")
    # other mods beside it (with=a.jar;b.jar), to see that they get on together
    for extra in next((a.split("=", 1)[1] for a in sys.argv[1:] if a.startswith("with=")), "").split(";"):
        if extra:
            shutil.copy(extra, RUN / "mods")
    print("mods:", sorted(p.name for p in (RUN / "mods").iterdir()))

    args = [str(JAVA), "-Xmx3G", "-Diceandfire_fabric.clienttest=true", "-Diceandfire_fabric.test.only=" + only,
            "-Dfabric.gameVersion=" + MC,
            "-cp", os.pathsep.join(str(p) for p in cp), "net.fabricmc.loader.impl.launch.knot.KnotClient",
            "--username", "IafTest", "--version", "fabric-loader-" + LOADER + "-" + MC, "--gameDir", str(RUN),
            "--assetsDir", str(INSTALL / "assets"), "--assetIndex", vanilla["assetIndex"]["id"],
            "--uuid", "00000000000000000000000000000001", "--accessToken", "0", "--userType", "legacy",
            "--width", "854", "--height", "480"]
    with open(RUN / "run.log", "w", encoding="utf-8", errors="replace") as log:
        try:
            p = subprocess.run(args, cwd=RUN, stdout=log, stderr=subprocess.STDOUT, timeout=timeout)
            print("exit", p.returncode)
            return p.returncode
        except subprocess.TimeoutExpired:
            print("timeout")
            return 3


sys.exit(main())
