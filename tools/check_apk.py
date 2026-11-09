#!/usr/bin/env python3
"""Inspect the delivered APK, including merged permissions and native alignment."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("apk",type=Path)
parser.add_argument("--build-tools",type=Path,required=True)
parser.add_argument("--output",type=Path,required=True)
args = parser.parse_args()
def command(name,*arguments):
    return subprocess.check_output([str(args.build_tools/name),*map(str,arguments)],text=True,stderr=subprocess.STDOUT)
permissions = command("aapt","dump","permissions",args.apk)
names = set(re.findall(r"uses-permission: name='([^']+)'",permissions))
expected = {"android.permission."+name for name in ("RECORD_AUDIO","MODIFY_AUDIO_SETTINGS","POST_NOTIFICATIONS","FOREGROUND_SERVICE","FOREGROUND_SERVICE_MICROPHONE")}
assert names == expected, f"Unexpected merged permissions: {names ^ expected}"
alignments = {}
with zipfile.ZipFile(args.apk) as archive:
    libs = [n for n in archive.namelist() if n.startswith("lib/") and n.endswith(".so")]
    assert libs and all(n.startswith("lib/arm64-v8a/") for n in libs), "Unexpected or missing ABI"
    for name in libs:
        data = archive.read(name)
        assert data[:6] == b"\x7fELF\x02\x01", f"Expected little-endian ELF64: {name}"
        phoff = struct.unpack_from("<Q",data,32)[0]
        size,count = struct.unpack_from("<HH",data,54)
        values = [struct.unpack_from("<Q",data,phoff+i*size+48)[0] for i in range(count)
                  if struct.unpack_from("<I",data,phoff+i*size)[0] == 1]
        assert values and all(a >= 16384 for a in values), f"ELF alignment below 16 KiB: {name}"
        alignments[name] = values
signature = command("apksigner","verify","--verbose",args.apk)
assert "Verifies" in signature
command("zipalign","-c","-P","16","4",args.apk)
report = {"apk":args.apk.name,"bytes":args.apk.stat().st_size,
          "sha256":hashlib.sha256(args.apk.read_bytes()).hexdigest(),
          "permissions":sorted(names),"internet_permission":False,
          "abi":"arm64-v8a","elf_load_segment_alignment":alignments,
          "signature_verified":True,"zip_alignment_16k_verified":True,
          "device_installation_tested":False}
args.output.write_text(json.dumps(report,indent=2)+"\n")
print(json.dumps(report,indent=2))
