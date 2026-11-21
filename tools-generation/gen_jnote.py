# -*- coding: utf-8 -*-
"""把 工程笔记.md 渲染成 Word（与《软件设计说明书》同一套版式，纯标准库）。

支持的 Markdown 子集：#/##/### 标题、> 引用块、表格、``` 代码块、
- 列表（含两级）、普通段落；行内 **粗体** 与 `等宽`。
"""
import zipfile, html, os, re, sys

def esc(t): return html.escape(t, quote=False)

def runs(text, base_bold=False):
    """行内解析：**粗体** 与 `等宽`。"""
    out = []
    pat = re.compile(r'\*\*(.+?)\*\*|`(.+?)`')
    pos = 0
    for m in pat.finditer(text):
        if m.start() > pos:
            out.append(_run(text[pos:m.start()], bold=base_bold))
        if m.group(1) is not None:
            out.append(_run(m.group(1), bold=True))
        else:
            out.append(_run(m.group(2), mono=True, bold=base_bold))
        pos = m.end()
    if pos < len(text):
        out.append(_run(text[pos:], bold=base_bold))
    return ''.join(out)

def _run(t, bold=False, mono=False):
    b = '<w:b/>' if bold else ''
    fonts = '<w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/>' if mono else ''
    return (f'<w:r><w:rPr>{b}{fonts}<w:rFonts w:eastAsia="微软雅黑"/></w:rPr>'
            f'<w:t xml:space="preserve">{esc(t)}</w:t></w:r>')

def para(text, style=None, center=False, sz=None, color=None, mono=False):
    st = f'<w:pStyle w:val="{style}"/>' if style else ''
    jc = '<w:jc w:val="center"/>' if center else ''
    size = f'<w:sz w:val="{sz}"/>' if sz else ''
    col = f'<w:color w:val="{color}"/>' if color else ''
    pre = f'<w:rPr>{size}{col}</w:rPr>'
    body = runs(text)
    if sz or col or mono:
        # 尺寸/颜色作为整段默认，粗体/等宽 run 保持
        body = body.replace('<w:rPr>', f'<w:rPr>{size}{col}')
    return f'<w:p><w:pPr>{st}{jc}</w:pPr>{body}</w:p>'

def toc_field():
    return ('<w:p><w:pPr><w:jc w:val="center"/></w:pPr><w:r><w:rPr>'
            '<w:b/><w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="28"/></w:rPr>'
            '<w:fldChar w:fldCharType="begin"/><w:instrText xml:space="preserve"> TOC \\o "1-2" \\h \\z \\u </w:instrText>'
            '<w:fldChar w:fldCharType="separate"/><w:t>目录（在 Word 中右键此处 → 更新域）</w:t>'
            '<w:fldChar w:fldCharType="end"/></w:r></w:p>')

def mono_lines(lines):
    out = []
    for ln in lines:
        out.append(
            '<w:p><w:pPr><w:spacing w:after="0" w:line="220" w:lineRule="auto"/></w:pPr>'
            '<w:r><w:rPr><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas" w:eastAsia="微软雅黑"/>'
            f'<w:sz w:val="16"/></w:rPr><w:t xml:space="preserve">{esc(ln)}</w:t></w:r></w:p>')
    return ''.join(out)

def table(rows):
    xml = ['<w:tbl><w:tblPr><w:tblW w:w="0" w:type="auto"/>'
           '<w:tblBorders><w:top w:val="single" w:sz="4"/>'
           '<w:left w:val="single" w:sz="4"/><w:bottom w:val="single" w:sz="4"/>'
           '<w:right w:val="single" w:sz="4"/><w:insideH w:val="single" w:sz="4"/>'
           '<w:insideV w:val="single" w:sz="4"/></w:tblBorders></w:tblPr>']
    for i, row in enumerate(rows):
        xml.append('<w:tr>')
        for cell in row:
            b = '<w:b/>' if i == 0 else ''
            xml.append(
                '<w:tc><w:tcPr><w:tcW w:w="2400" w:type="dxa"/></w:tcPr>'
                f'<w:p><w:pPr><w:spacing w:after="10" w:line="220" w:lineRule="auto"/></w:pPr><w:r><w:rPr>{b}'
                f'<w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="16"/></w:rPr>'
                f'<w:t xml:space="preserve">{esc(cell)}</w:t></w:r></w:p></w:tc>')
        xml.append('</w:tr>')
    xml.append('</w:tbl>')
    return ''.join(xml) + para('')

# ============================ 读入并渲染 ============================
_md = sys.argv[1] if len(sys.argv) > 1 else '工程笔记.md'
_out = sys.argv[2] if len(sys.argv) > 2 else '工程笔记.docx'
src = open(_md, encoding='utf-8').read().splitlines()
B = []
i = 0
quote_buf = []
list_buf = []

def flush_quote():
    global quote_buf
    if quote_buf:
        text = ' '.join(quote_buf)
        B.append(
            '<w:p><w:pPr><w:ind w:left="480"/><w:spacing w:after="60"/></w:pPr>'
            + runs(text) + '</w:p>')
        quote_buf = []

def flush_list():
    global list_buf
    if list_buf:
        for lvl, t in list_buf:
            prefix = '• ' if lvl == 0 else '    ◦ '
            B.append(para(prefix + t))
        list_buf = []

while i < len(src):
    line = src[i]
    s = line.strip()

    # 代码块
    if s.startswith('```'):
        flush_quote(); flush_list()
        i += 1
        buf = []
        while i < len(src) and not src[i].strip().startswith('```'):
            buf.append(src[i])
            i += 1
        B.append(mono_lines(buf))
        i += 1
        continue

    # 表格
    if s.startswith('|') and s.endswith('|'):
        flush_quote(); flush_list()
        rows = []
        while i < len(src) and src[i].strip().startswith('|'):
            cells = [c.strip() for c in src[i].strip().strip('|').split('|')]
            if not all(re.fullmatch(r':?-{2,}:?', c) for c in cells if c):
                rows.append(cells)
            i += 1
        if rows:
            B.append(table(rows))
        continue

    # 空行
    if not s:
        flush_quote(); flush_list()
        i += 1
        continue

    # 目录标记
    if s == '[[TOC]]':
        flush_quote(); flush_list()
        B.append(toc_field())
        i += 1; continue

    # 标题
    if s.startswith('### '):
        flush_quote(); flush_list()
        B.append(para(s[4:], style='Heading2'))
        i += 1; continue
    if s.startswith('## '):
        flush_quote(); flush_list()
        B.append(para(s[3:], style='Heading1'))
        i += 1; continue
    if s.startswith('# '):
        flush_quote(); flush_list()
        B.append(para(s[2:], style='Title'))
        i += 1; continue

    # 分隔线
    if re.fullmatch(r'-{3,}', s):
        flush_quote(); flush_list()
        i += 1; continue

    # 引用块
    if s.startswith('>'):
        flush_list()
        quote_buf.append(s.lstrip('> ').strip())
        i += 1; continue

    # 列表
    m = re.match(r'^(\s*)[-*]\s+(.*)$', line)
    if m:
        flush_quote()
        list_buf.append((0 if len(m.group(1)) < 2 else 1, m.group(2)))
        i += 1; continue

    # 普通段
    flush_quote(); flush_list()
    B.append(para(s))
    i += 1

flush_quote(); flush_list()

DOC = ''.join(B)
# ============================ 打包（与说明书同款模板） ============================
document_xml = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<w:body>{DOC}
<w:sectPr><w:footerReference w:type="default" r:id="rIdFooter1"/>
<w:pgSz w:w="11906" w:h="16838"/>
<w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/>
</w:sectPr></w:body></w:document>'''

styles_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/>
<w:rPr><w:rFonts w:ascii="Calibri" w:eastAsia="微软雅黑"/><w:sz w:val="22"/></w:rPr>
<w:pPr><w:spacing w:after="40" w:line="250" w:lineRule="auto"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/>
<w:basedOn w:val="Normal"/><w:pPr><w:jc w:val="center"/><w:spacing w:before="600" w:after="200"/></w:pPr>
<w:rPr><w:b/><w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="48"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/>
<w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="200" w:after="80"/>
<w:outlineLvl w:val="0"/></w:pPr>
<w:rPr><w:b/><w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="32"/><w:color w:val="1F8A5E"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/>
<w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="150" w:after="60"/>
<w:outlineLvl w:val="1"/></w:pPr>
<w:rPr><w:b/><w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="27"/><w:color w:val="333333"/></w:rPr></w:style>
</w:styles>'''

content_types = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
<Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>
</Types>'''

rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>'''

doc_rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
<Relationship Id="rIdFooter1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/>
</Relationships>'''

_fld_rpr = ('<w:rPr><w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="20"/>'
            '<w:color w:val="808080"/></w:rPr>')
footer1_xml = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:p><w:pPr><w:jc w:val="center"/></w:pPr>
<w:r>{_fld_rpr}<w:t xml:space="preserve">BodyCast {os.path.splitext(os.path.basename(_out))[0]} · 第 </w:t></w:r>
<w:fldSimple w:instr=" PAGE "><w:r>{_fld_rpr}<w:t>1</w:t></w:r></w:fldSimple>
<w:r>{_fld_rpr}<w:t xml:space="preserve"> 页</w:t></w:r>
</w:p></w:ftr>'''

out = _out
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
    z.writestr('[Content_Types].xml', content_types)
    z.writestr('_rels/.rels', rels)
    z.writestr('word/document.xml', document_xml)
    z.writestr('word/styles.xml', styles_xml)
    z.writestr('word/footer1.xml', footer1_xml)
    z.writestr('word/_rels/document.xml.rels', doc_rels)

print('written:', out, os.path.getsize(out), 'bytes')
