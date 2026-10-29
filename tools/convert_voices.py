"""Turns voice lines (mp3 from ElevenLabs) into the mod's sounds: mono 44.1 kHz ogg, silence trimmed, a light
compressor and a cold echo, levelled (about -19 dB mean; the louder lines a little more), never past -1.8 dB peak.
    python tools/convert_voices.py <folder of name.mp3> <out folder>   (writes cryona_<name>.ogg)
"""
import hashlib, os, re, subprocess, sys

FF = r"C:\Users\Escobar\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-9.0-full_build\bin\ffmpeg.exe"
src = sys.argv[1]
out = sys.argv[2]
os.makedirs(out, exist_ok=True)

# mean loudness wanted per line (dB): shouts and the death a bit louder than the quiet lines
TARGET = {"lost": -21.0, "death": -17.0, "lament": -18.0, "vengeance": -17.0, "zero": -17.0, "wake": -18.0}
# a cold hall: a light echo behind her voice
ECHO = "aecho=0.8:0.55:70|150:0.22|0.10"
TRIM = ("silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.05,"
        "areverse,silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.15,areverse")


def levels(path, filters):
    r = subprocess.run([FF, "-hide_banner", "-i", path, "-af", filters + ",volumedetect", "-f", "null", "-"],
                       capture_output=True, text=True)
    mean = float(re.search(r"mean_volume: (-?[\d.]+) dB", r.stderr).group(1))
    peak = float(re.search(r"max_volume: (-?[\d.]+) dB", r.stderr).group(1))
    return mean, peak


for name in sorted(os.listdir(src)):
    if not name.endswith(".mp3"):
        continue
    line = name[:-4]
    path = os.path.join(src, name)
    print(line, hashlib.md5(open(path, "rb").read()).hexdigest()[:10], end=" ")
    base = TRIM + ",acompressor=threshold=-28dB:ratio=3:attack=5:release=150," + ECHO
    mean, peak = levels(path, base)
    gain = TARGET.get(line, -19.0) - mean
    gain = min(gain, -1.8 - peak)
    filters = base + f",volume={gain:.2f}dB"
    dest = os.path.join(out, "cryona_" + line + ".ogg")
    subprocess.run([FF, "-hide_banner", "-loglevel", "error", "-y", "-i", path, "-af", filters, "-ac", "1", "-ar", "44100",
                    "-c:a", "libvorbis", "-q:a", "4", dest], check=True)
    m2, p2 = levels(dest, "anull")
    r = subprocess.run([FF, "-hide_banner", "-i", dest, "-f", "null", "-"], capture_output=True, text=True)
    dur = re.search(r"time=(\d+):(\d+):([\d.]+)", r.stderr)
    secs = int(dur.group(1)) * 3600 + int(dur.group(2)) * 60 + float(dur.group(3)) if dur else -1
    print(f"mean {mean:.1f} peak {peak:.1f} gain {gain:+.1f} -> mean {m2:.1f} peak {p2:.1f} {secs:.2f}s {os.path.getsize(dest)}")
