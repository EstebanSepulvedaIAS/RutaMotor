"""Real HTTP, 10k catalog and restart verification. Python 3.10+, standard library only."""
import argparse
import json
import os
from pathlib import Path
import platform
import socket
import statistics
import subprocess
import time
import urllib.request
import urllib.error
import uuid

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--java', default='java')
args = parser.parse_args()
run_id = str(uuid.uuid4())
run_dir = ROOT / 'database' / 'data' / ('verification-' + run_id)
run_dir.mkdir(parents=True)
with socket.socket() as s:
    s.bind(('127.0.0.1', 0))
    port = s.getsockname()[1]
base = f'http://127.0.0.1:{port}/api/v1'
jar = ROOT / 'backend/build/libs/rutamotor-backend-1.0.0.jar'
assert jar.exists(), 'Run gradlew bootJar first'
log = (run_dir / 'server.log').open('w', encoding='utf-8')
process = None

def call(path, payload=None):
    request = urllib.request.Request(base + path,
        data=None if payload is None else json.dumps(payload).encode(),
        headers={'Content-Type': 'application/json'})
    start = time.perf_counter()
    try:
        response = urllib.request.urlopen(request, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        raw = response.read()
        return response.status, json.loads(raw), len(raw), (time.perf_counter() - start) * 1000

def start_server():
    global process
    process = subprocess.Popen([args.java, '-jar', str(jar), f'--server.port={port}',
        '--rutamotor.seed-size=10000',
        f'--spring.datasource.url=jdbc:h2:file:{(run_dir / "catalog").as_posix()};DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0'],
        cwd=ROOT / 'backend', stdout=log, stderr=log,
        creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)
    deadline = time.monotonic() + 120
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError(f'Backend exited. See {run_dir / "server.log"}')
        try:
            if call('/vehicles')[1].get('total') == 10000:
                return
        except (OSError, ValueError):
            pass
        time.sleep(.3)
    raise TimeoutError('Backend did not become ready')

def stop_server():
    if process and process.poll() is None:
        process.terminate()
        process.wait(timeout=20)

try:
    start_server()
    measurements = [call('/vehicles?page=0&size=25') for _ in range(30)]
    assert all(status == 200 and len(page['items']) == 25 and page['total'] == 10000
               for status, page, _, _ in measurements)
    _, first, response_bytes, _ = measurements[0]
    _, second, _, _ = call('/vehicles?page=1&size=25')
    assert not ({v['id'] for v in first['items']} & {v['id'] for v in second['items']})
    assert first == call('/vehicles?page=0&size=25')[1]
    vehicle = next(v for v in first['items'] if v['status'] == 'AVAILABLE')
    accepted = {'reference': str(uuid.uuid4()), 'vehicleId': vehicle['id'], 'buyerAlias': 'Runtime-demo'}
    status, original, _, _ = call('/reservations', accepted)
    assert status == 200 and original['appliedPrice'] == vehicle['price']
    rejected = {**accepted, 'reference': str(uuid.uuid4()), 'buyerAlias': 'Second-demo'}
    reject_status, rejection, _, _ = call('/reservations', rejected)
    assert reject_status == 409 and rejection['result']['reason'] == 'UNAVAILABLE'
    missing = {**accepted, 'reference': str(uuid.uuid4()), 'vehicleId': str(uuid.uuid4())}
    missing_status, missing_result, _, _ = call('/reservations', missing)
    assert missing_status == 404
    stop_server()
    start_server()
    replay = call('/reservations', accepted)[:2]
    assert replay == (200, original), {'original': original, 'replay': replay}
    assert call('/reservations', rejected)[:2] == (409, rejection)
    assert call('/reservations', missing)[:2] == (404, missing_result)
    assert next(v for v in call('/vehicles')[1]['items'] if v['id'] == vehicle['id'])['status'] == 'RESERVED'
    assert call('/reservations', {**accepted, 'buyerAlias': 'Changed-buyer'})[0] == 409
    durations = [m[3] for m in measurements]
    evidence = {
        'verifiedAtUtc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
        'os': platform.platform(), 'java': subprocess.check_output([args.java, '-version'], stderr=subprocess.STDOUT, text=True).strip(),
        'volume': 10000, 'itemsPerResponse': 25, 'responseBytesUtf8': response_bytes,
        'requests': len(durations), 'warmup': 'readiness polling, JVM newly started',
        'timingScope': 'HTTP localhost + JSON response read (not SQL alone)',
        'milliseconds': {'first': durations[0], 'min': min(durations), 'median': statistics.median(durations), 'max': max(durations)},
        'checks': ['stable non-overlapping pages', 'server-owned price', 'accepted replay after process restart',
                   'unavailable rejection replay after restart', 'missing rejection replay after restart',
                   'seed preserves reserved state', 'reference conflict preserves original'],
        'isolatedDatabase': str(run_dir),
        'note': 'Process termination and second JVM startup; no data deleted. No concurrent benchmark load.'
    }
    target = ROOT / 'docs/evidence/runtime.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(evidence, ensure_ascii=False, indent=2))
finally:
    stop_server()
    log.close()
