"""UI smoke checks against an explicitly selected Android emulator.
Uses ADB only; does not grant root, dangerous permissions or alter user files.
Example: python tools/smoke_test.py --serial emulator-5554 --label api37
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument('--serial', required=True)
parser.add_argument('--label', required=True)
parser.add_argument('--adb', default=str(Path(os.environ['LOCALAPPDATA']) / 'Android/Sdk/platform-tools/adb.exe'))
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
out = root / 'outputs'
checks = []

def adb(*cmd, binary=False):
    p = subprocess.run([args.adb, '-s', args.serial, *cmd], capture_output=True, timeout=60)
    if p.returncode:
        raise RuntimeError(p.stderr.decode('utf-8', 'replace'))
    return p.stdout if binary else p.stdout.decode('utf-8', 'replace')

def dump(attempts=6):
    # uiautomator synchronizes with the UI idle state; no arbitrary sleeps needed.
    last = ''
    for _ in range(attempts):
        result = adb('shell', 'uiautomator', 'dump', '/sdcard/pocket_ui_test.xml')
        if 'dumped' in result.lower():
            dest = out / (args.label + '-ui.xml')
            adb('pull', '/sdcard/pocket_ui_test.xml', str(dest))
            return ET.parse(dest).getroot()
        last = result
    raise RuntimeError('uiautomator dump failed: ' + last)

def ensure_app():
    visible = text(dump())
    if '让手机，更得心应手' in visible or '分享设备报告' in visible or '工具详情' in visible or '系统信息' in visible:
        return
    adb('shell', 'am', 'start', '--activity-single-top', '-n', 'cn.pocket.toolbox/.MainActivity', '-f', '0x10008000')
    for _ in range(8):
        time.sleep(0.5)
        visible = text(dump())
        if '让手机' in visible:
            return

def text(tree):
    return '\n'.join(n.attrib.get('text', '') for n in tree.iter('node'))

def tap_node(node):
    x1,y1,x2,y2 = map(int, re.findall(r'\d+', node.attrib['bounds']))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

def tap_text(label):
    # Tool cards expose their title through content-desc, so both are matched.
    # Prefer tapping the content-desc node (the card itself) so the click
    # is delivered to the card's OnClickListener rather than to a child TextView.
    ensure_app()
    tree = dump()
    for node in tree.iter('node'):
        if label in node.attrib.get('content-desc', '') and node.attrib.get('clickable') == 'true':
            tap_node(node)
            return
    for node in tree.iter('node'):
        if node.attrib.get('text') == label:
            tap_node(node)
            return
    for node in tree.iter('node'):
        if label in node.attrib.get('content-desc', ''):
            tap_node(node)
            return
    raise AssertionError('Missing UI text: ' + label + '\n' + text(tree))

def dismiss_dialogs(rounds=4):
    # Emulator builds occasionally raise system ANR dialogs that cover the app.
    for _ in range(rounds):
        tree = dump()
        visible = text(tree)
        if 'isn\'t responding' not in visible and 'has stopped' not in visible:
            return
        target = None
        for node in tree.iter('node'):
            if node.attrib.get('text') in ('Wait', 'Close app', 'OK'):
                target = node
        if target is None:
            return
        x1,y1,x2,y2 = map(int, re.findall(r'\d+', target.attrib['bounds']))
        adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

def check(label, expected):
    dismiss_dialogs()
    ensure_app()
    visible = ''
    for _ in range(8):
        visible = text(dump())
        if expected in visible:
            break
        time.sleep(0.6)
    assert expected in visible, label + ': missing ' + expected + '\n' + visible
    checks.append({'check': label, 'passed': True})

def screenshot(name):
    (out / (args.label + '-' + name + '.png')).write_bytes(adb('exec-out', 'screencap', '-p', binary=True))

def back():
    adb('shell', 'input', 'keyevent', '4')

def boot_wait(timeout=600):
    for _ in range(timeout // 5):
        if adb('shell', 'getprop', 'sys.boot_completed').strip() == '1':
            # Wait for the launcher/home idle state rather than a fixed delay.
            adb('shell', 'am', 'wait-for-broadcast-idle')
            return
        time.sleep(5)
    raise AssertionError('Emulator did not finish booting')

def home():
    adb('shell', 'am', 'force-stop', 'cn.pocket.toolbox')
    adb('shell', 'am', 'start', '-W', '-n', 'cn.pocket.toolbox/.MainActivity', '-f', '0x10008000')
    for _ in range(12):
        dismiss_dialogs()
        visible = text(dump())
        if '让手机，更得心应手' in visible:
            checks.append({'check': 'Home launch', 'passed': True})
            return
    raise AssertionError('Home launch: missing 让手机，更得心应手\n' + text(dump()))

def search_tool(query):
    home()
    tree = dump()
    for node in tree.iter('node'):
        if node.attrib.get('class') == 'android.widget.EditText':
            x1,y1,x2,y2 = map(int,re.findall(r'\d+',node.attrib['bounds']))
            adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
            adb('shell','input','text',query)
            back()
            return
    raise AssertionError('Search field not present')

def fresh_home():
    # Re-launch the exported launcher activity so the task is clean and
    # the next tap into a non-exported sub-activity works reliably.
    adb('shell','am','start','-n','cn.pocket.toolbox/.MainActivity')
    for _ in range(10):
        if '让手机，更得心应手' in text(dump()):
            return
        time.sleep(0.5)

try:
    version = adb('shell','getprop','ro.build.version.sdk').strip()
    boot_wait()
    adb('shell','am','force-stop','cn.pocket.toolbox')
    home()
    screenshot('home')
    tap_text('系统信息')
    check('System information', '设备')
    screenshot('system')
    back()
    # The debug build (src/debug/AndroidManifest.xml) exposes internal
    # activities so am start from adb can drive them without run-as.
    def launch_internal(component, extras=''):
        cmd='am start -n cn.pocket.toolbox/'+component
        if extras: cmd+=' '+extras
        adb('shell', *cmd.split())
    fresh_home()
    launch_internal('ManageActivity', '--es mode apps')
    check('App manager opens', '仅展示系统允许查询的可启动应用。')
    screenshot('apps')
    launch_internal('ManageActivity', '--es mode files')
    check('File manager permission boundary', '只访问你亲自授权的目录。')
    tap_text('授权目录')
    tree=dump()
    assert 'com.android.documentsui' in ET.tostring(tree,encoding='unicode'), 'SAF picker not opened'
    checks.append({'check':'System SAF picker launch','passed':True})
    screenshot('saf-picker')
    back()
    fresh_home()
    search_tool('hash')
    tap_text('SHA-256')
    tap_text('计算 SHA-256')
    check('SHA256 empty-string known vector', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855')
    screenshot('hash')
    fresh_home()
    search_tool('dns')
    tap_text('DNS 查询')
    check('DNS form renders', 'example.com')
    screenshot('dns')
    fresh_home()
    search_tool('screen')
    tap_text('屏幕检测')
    check('Screen test opens','多点触控检测')
    tap_text('切换纯色')
    screenshot('screen')
    fresh_home()
    search_tool('password')
    tap_text('密码生成')
    tap_text('生成安全密码')
    tree=dump()
    values=[n.attrib.get('text','') for n in tree.iter('node')]
    assert any(len(s)==20 and re.fullmatch(r'[A-Za-z2-9!@#$%&*+_?\-]+',s) for s in values),'Password output absent'
    checks.append({'check':'Offline secure password generation','passed':True})
    home()
    crash=adb('logcat','-d','-b','crash','-t','150')
    assert 'Process: cn.pocket.toolbox' not in crash, 'Application crash found in crash buffer'
    checks.append({'check':'No application crash in captured buffer','passed':True})
    report={'serial':args.serial,'sdk':version,'checks':checks,'status':'passed','scope':'Emulator smoke checks; not physical-device/OEM certification'}
except Exception as exc:
    logcat = adb('logcat','-d','-t','400')
    (out / (args.label + '-logcat.txt')).write_text(logcat, encoding='utf-8')
    crash = adb('logcat','-d','-b','crash','-t','200')
    (out / (args.label + '-crash.txt')).write_text(crash, encoding='utf-8')
    report={'serial':args.serial,'checks':checks,'status':'failed','error':str(exc),
            'crash_lines':[l for l in crash.splitlines() if 'cn.pocket.toolbox' in l][:20],
            'logcat_hint':[l for l in logcat.splitlines() if 'pocket' in l.lower() or 'FATAL' in l][:25]}
    (out/(args.label+'-smoke.json')).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False,indent=2))
    raise
else:
    (out/(args.label+'-smoke.json')).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False,indent=2))
