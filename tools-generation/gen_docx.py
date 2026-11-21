# -*- coding: utf-8 -*-
"""生成 BodyCast 身体气象台软件设计说明书 .docx（紧凑配图版、A4，纯标准库；Word 实测 18 页）。

v23 相对 v22 的改动：为控制页数（Word 实测 30 页 → 18 页）做了三件事——
1) 版本改动收敛成第二章一张表，第十章不再重复；2) 配图按主题合并为 9 张并缩小宽度；
3) 行距/段距/字号/页边距整体收紧，长文段落改写为紧凑条目。
"""
import zipfile, html, os, struct

def esc(t): return html.escape(t, quote=False)

def p(text, style=None, bold=False, sz=None, color=None, center=False):
    st = f'<w:pStyle w:val="{style}"/>' if style else ''
    jc = '<w:jc w:val="center"/>' if center else ''
    b = '<w:b/>' if bold else ''
    size = f'<w:sz w:val="{sz}"/>' if sz else ''
    col = f'<w:color w:val="{color}"/>' if color else ''
    return (f'<w:p><w:pPr>{st}{jc}</w:pPr><w:r><w:rPr>{b}'
            f'<w:rFonts w:eastAsia="微软雅黑"/>{size}{col}</w:rPr>'
            f'<w:t xml:space="preserve">{esc(text)}</w:t></w:r></w:p>')

def caption(text):
    return p(text, sz=16, color='666666', center=True)

def page_break():
    return '<w:p><w:r><w:br w:type="page"/></w:r></w:p>'

def toc_field():
    # Word 打开文档后可右键「更新域」刷新页码；标题使用 Heading1/Heading2。
    return ('<w:p><w:pPr><w:jc w:val="center"/></w:pPr><w:r><w:rPr>'
            '<w:b/><w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="28"/></w:rPr>'
            '<w:fldChar w:fldCharType="begin"/><w:instrText xml:space="preserve"> TOC \\o "1-2" \\h \\z \\u </w:instrText>'
            '<w:fldChar w:fldCharType="separate"/><w:t>目录将在 Word 中更新</w:t>'
            '<w:fldChar w:fldCharType="end"/></w:r></w:p>')

def bullets(items):
    return ''.join(p('• ' + it) for it in items)

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
                f'<w:p><w:pPr><w:spacing w:after="4" w:line="210" w:lineRule="auto"/></w:pPr><w:r><w:rPr>{b}'
                f'<w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="15"/></w:rPr>'
                f'<w:t xml:space="preserve">{esc(cell)}</w:t></w:r></w:p></w:tc>')
        xml.append('</w:tr>')
    xml.append('</w:tbl>')
    return ''.join(xml) + p('')

# ---------- 图片嵌入 ----------
_images = []   # (path,)
def png_size(path):
    with open(path, 'rb') as f:
        head = f.read(26)
    w, h = struct.unpack('>II', head[16:24])
    return w, h

IMG_ID = [0]
def img_para(path, cap=None, max_width_in=5.2):
    _images.append(path)
    IMG_ID[0] += 1
    rid = f'rIdImg{IMG_ID[0]}'
    wpx, hpx = png_size(path)
    emu_w = int(wpx * 9525)
    emu_h = int(hpx * 9525)
    max_emu = int(max_width_in * 914400)
    if emu_w > max_emu:
        emu_h = int(emu_h * max_emu / emu_w)
        emu_w = max_emu
    i = IMG_ID[0]
    xml = f'''<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:before="40" w:after="20"/></w:pPr><w:r><w:drawing>
<wp:inline distT="0" distB="0" distL="0" distR="0">
<wp:extent cx="{emu_w}" cy="{emu_h}"/><wp:effectExtent l="0" t="0" r="0" b="0"/>
<wp:docPr id="{i}" name="Picture{i}"/><wp:cNvGraphicFramePr/>
<a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
<pic:pic><pic:nvPicPr><pic:cNvPr id="0" name="image{i}.png"/><pic:cNvPicPr/></pic:nvPicPr>
<pic:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>
<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{emu_w}" cy="{emu_h}"/></a:xfrm>
<a:prstGeom prst="roundRect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic>
</a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>'''
    if cap:
        xml += caption(cap)
    return xml

B = []
h1 = lambda t: B.append(p(t, 'Heading1'))
h2 = lambda t: B.append(p(t, 'Heading2'))
h3 = lambda t: B.append(p(t, 'Heading3'))
def para(t, center=False, color=None):
    B.append(p(t, center=center, color=color))
bl = lambda items: B.append(bullets(items))
tbl = lambda rows: B.append(table(rows))
img = lambda path, cap=None, w=5.2: B.append(img_para(path, cap, w))

SHOTS = 'outputs/shots'

# ============================ 封面 ============================
B.append(p('BodyCast', 'Title'))
B.append(p('软件设计说明书', 'Subtitle'))
img('app/src/main/res/drawable-nodpi/ic_launcher_full.png', None, 1.15)
B.append(p('版本 22.47.0（versionCode 74）　　2026 年 10 月', center=True, sz=24, color='555555'))
B.append(p('开发者：zhangyuhexht / __xiaocai__', center=True, sz=24, color='555555'))
B.append(p('开发辅助 AI：Kimi 3 / Kimi 2.8 Preview / DeepSeek V4.1 Flash', center=True, sz=24, color='555555'))
B.append(p(''))
B.append(page_break())

# ============================ 一分钟看懂 ============================
h1('写在最前面：一分钟看懂这个软件')
para('如果你的手环今天告诉你：昨晚睡得不太好、压力有点大、步数也不多——大多数健康应用会给你一串数字和'
     '红色的未达标提示。BodyCast 换了一种说法：「今天是多云转阴的恢复天，风有点大，先把肩膀放下来。」')
para('它把身体状态翻译成天气，给你一张几分钟就能做完的小事清单，再配一份「今天吃什么」的建议。'
     '不评分、不排名、不显示卡路里，读不到数据就老实说「暂时没读到」。它做的不是健康管理，'
     '而是每天提醒你：身体一直在说话，值得被听见。')
img(f'{SHOTS}/v23-fig1-home.png', '图 1　首页（22.35.0）：左为深色主题、右为浅色主题；同一套版式与玻璃参数，只换色板。'
    '打开先看基础状态（四个信号），再看今天的身体天气结论', 3.3)

# ============================ 一、产品概述 ============================
h1('一、产品概述')
h2('1.1 这是什么')
para('BodyCast 是一款原生 Android 应用。它读取手环等穿戴设备同步到系统健康服务（Health Connect）里的'
     '身体数据——静息心率、心率变异性、睡眠、步数、血氧、运动时长——用「天气隐喻」播报今天的身体状态：'
     '恢复天、高能天、压力天、平衡天或敏感天；然后给出一条几分钟内能做完的「微行动处方」和一份「今天吃什么」'
     '的食谱建议。它的口吻像天气预报，不像体检报告。')
h2('1.2 设计初衷与产品铁律')
bl([
    '身体状态没有好坏，只有状态：恢复天和高能天一样正当，都值得被如实播报；',
    '指标「只出词，不出数字」：读不到就显示「暂时没读到」，绝不用 0 冒充真实读数（null 不等于 0）；',
    '建议必须「现在就能做完」：微行动都在 10 分钟以内、不需要器械，因为「做不完」会摧毁信任；',
    '用户的话永远算数：手动校准的优先级高于一切设备读数，反馈只往更保守的方向调整，永不做减法；',
    '界面上没有红色警告、没有「您今日未达标」，也不接任何大模型 API——全部由本地规则引擎加预设模板生成。',
])
h2('1.3 规模一览')
tbl([
    ['项目', '规模'],
    ['代码', '51 个 Kotlin 文件，约 1.9 万行（零 Java、零 XML 布局）'],
    ['微行动库', '28 条微行动，按天气、时长、可用场景归类，都在 10 分钟以内'],
    ['食谱库', '61 道菜，标注菜系、营养素、做法与适合的天气'],
    ['营养素', '12 类（含铁、锌、维生素 A / D、膳食纤维等）'],
    ['校准标签', '9 个（累、紧、胀、困、烦、轻、空、饿、说不清）'],
    ['身体天气', '5 种（恢复天 / 高能天 / 压力天 / 平衡天 / 敏感天）'],
    ['支持版本', 'Android 8.0 – 17；Android 13 以下自动降级为磨砂玻璃'],
])
h2('1.4 身体天气是怎么定出来的（规则口径）')
para('四项指标各自先把读数折成内部档位（3 最好、0 最差、读不到就是读不到），再用一套刻意保守的顺序定档；'
     '阈值只在引擎内部使用，界面上永远不会暴露成进度条或完成度。')
tbl([
    ['指标', '由哪些读数合成', '档位口径（内部）'],
    ['恢复云量', '心率变异性 + 静息心率，取平均', 'HRV ≥ 55 / 45 / 35；静息心率 ≤ 58 / 66 / 74'],
    ['压力风速', '设备的压力读数', '≤ 30 / ≤ 50 / ≤ 70 / 其余'],
    ['能量湿度', '活动时长 + 步数，取平均', '活动 ≥ 40 / 20 / 5 分钟；步数 ≥ 8000 / 4000 / 1000'],
    ['睡眠云层', '睡眠时长 + 深睡占比，取平均', '时长 ≥ 420 / 360 / 300 分钟；深睡 ≥ 22% / 16% / 10%'],
])
para('定档顺序（先看恢复、再看压力，最后才轮到高能）：恢复档为 0 → 恢复天；睡眠档为 0 且恢复档 ≤ 1 → 恢复天；'
     '压力档为 0 → 压力天；能量档为 3 且压力档 ≥ 2 → 高能天；四项读数全为空 → 敏感天（引擎兜底）；其余 → 平衡天。'
     '只要用户勾了任意一个校准标签，一律判为敏感天（读数不再参与定档，但四项指标照常显示）；'
     '设置里的「默认天气类型」只在读数与校准都为空时才生效。')

# ============================ 二、改动全记录 ============================
h1('二、从 10.0.0 到 22.47.0 的改动全记录')
para('这一章把 10.0.0 之后每一版的改动浓缩成一张表（22 系列共三十四个版本）；底栏（液态玻璃）的迭代最多，'
     '单独用一小节说结论与教训。完整叙述与失败方案留档在 README.md 里。')

h2('2.1 版本改动一览（10.0.0 – 22.47.0）')
tbl([
    ['版本', '主要改动'],
    ['10.0.0', '主卡片圆角 32dp 与底栏对齐；设置页改二级菜单；新增「关于」'],
    ['11.0.0', '多语言基础设施；食谱菜系与加权随机；换新图标；适配 Android 17'],
    ['12.0.0', '底栏背板改 55% 透明（恢复透亮与折射）；卡片圆角统一；返回键分层'],
    ['13.0.0', '语言收敛为简中 + 英语；修正菜品池；健康数据授权链路重做'],
    ['14.0.0', '搜索框对比度；作者居中；语言切换无感化；换页卡顿修复；图标改 BC'],
    ['15.0.0', '换页改为「立刻跳转 + 短暂落定」，去掉等待感'],
    ['16.0.0', '应用名统一为 BodyCast；图标微调；底栏一次过度修改（已回退）'],
    ['17.0.0', '底栏回修：底板不再整块消失、滑块恢复折射、消除叠字'],
    ['18.0.0', '底板在滑块处挖洞；删除冷启动开屏等待页'],
    ['19.0.0', '底栏透明度微调（拖动比静止略淡）'],
    ['20.0.0', '滑块改官方结构：图标文字常驻 + 染色副本供滑块采样'],
    ['21.0.0', '修掉底栏拖动细白线；回声页删除匿名天气墙'],
    ['21.1.0', '底栏真正反射身后页面；厂商接入补全（小米 / 华为 / 荣耀）'],
    ['22.0.0', '底座按官方结构重做；六项新功能（见 2.3）'],
    ['22.1.0', '试 depthEffect（后回退）；开箱引导四页；设置一级拆六组'],
    ['22.2.0', '底座回退到 22.0.0 写法（depthEffect 会把底板扭成放大镜）'],
    ['22.3.0', '底栏改为官方源码逐字版（只留三处应用适配）'],
    ['22.4.0', '底座折射可见（lens 前置到 blur 之前）；消除拖动两条线'],
    ['22.5.0', '浮层跟手下拉关闭；设置行高统一；末行圆角并入 32dp'],
    ['22.6.0', '底座两端补取样色块；滑块内框改对称；静止时底座比滑块淡'],
    ['22.7.0', '滑块去掉模糊与自加内框环；底座拉伸 34/32dp；行容器统一 32dp'],
    ['22.8.0', '滑块模糊改为恒定一致；底板更通透（1.5dp / 0.20）'],
    ['22.9.0', '合成层修正：折射区只留处理后的一层（后回退）'],
    ['22.10.0', '回退采样结构；滑块模糊随按压淡出；染色提到 0.26'],
    ['22.11.0', '主页层级重组；食谱页重心改「元素 ↔ 食物」；回声页加统计与对照'],
    ['22.12.0', '主页按「一屏一幕」重排；开发者门控改红色描边块'],
    ['22.13.0', 'i18n 全量补齐（120 词条 + 14 模板）；开发者门控落地'],
    ['22.14.0', '删掉底座折射的 66dp padding（拉伸跑到胶囊外的来源）'],
    ['22.15.x', '「平涂副本」架构落地后因性能（中位 77ms / 81% 掉帧）回退'],
    ['22.16.0', '根因修复：环境层录制补上底色，玻璃不再透出未折射内容'],
    ['22.17.0', '滑块静止也有液态玻璃；修掉降级开关打开后底板变黑'],
    ['22.18.0', '滑块重影根因修复；副本层恢复容器表面；与官方源码逐行对齐'],
    ['22.19.0', '边缘拉伸回落到官方默认：底座 34/32 → 24/24，滑块 12/17 → 10/14'],
    ['22.20.0', '新增「谷歌皮肤」（Material 3，可切换）；i18n 扩到四种语言（新增日语、拉丁文）'],
    ['22.21.0', '开箱引导第一页挑界面风格与主题色（即时生效）；外观与体验新增两个入口'],
    ['22.22.0', '健康数据接入修通自动通道（国行包名 / Android 14 别名 / 心率权限 / 步数按来源取最大）；数据源页新增四步自检卡与桥接引导'],
    ['22.23.0', '数据源页一键化：一句结论 + 一个主按钮，重复的三块并进自检卡（页面 17 块精简到 8 块）'],
    ['22.24.0', '加两条零权限数据通道：手动填写读数、文件导入（分享面板 + 选文件），都带 24 小时有效期'],
    ['22.25.0', '新增「小米账号（云同步）」可选通道（登录走小米官方页、令牌 Keystore 加密），默认关闭'],
    ['22.26.0', '谷歌皮肤 M3 精修：状态栏随主题、触摸区 56dp、头部 M3 化、tonal 色板、底栏指示器动效'],
    ['22.27.0', '修复小米登录黑屏 / 授权后数据断 / 进页卡顿；谷歌底栏圆角与图标对齐；半屏栏手势分区'],
    ['22.28.0', '数据获取优化：部分信号也出结论（按 N/4 算）、自检纳入小米云、自动刷新、单测 60 个全绿'],
    ['22.29.0', '半成品清尾（隐私页崩溃、跳过按钮、进度条、死选项）+ 动画节奏统一（换页更快）'],
    ['22.30.0', '产品定位落地（天气→元素→食谱闭环、回声正向计数、关于页定位语）+ 授权向导（四步分步指南、厂商写入能力探测）+ 谷歌皮肤圆角对齐 + 开发者名单调整'],
    ['22.31.0', '删除谷歌 Material 3 皮肤（只留液态玻璃一套）+ 开箱引导重做（七页、上一步、仅绑定步可跳）'],
    ['22.32.0', '五项好用性改进：食谱闭环（就吃这个/想吃的清单）、首页 6 秒首屏、小组件直达食谱、回声模式洞察、天气变化提醒'],
    ['22.33.0', '四项修缮：小组件美化（天气色强调条）、浮层灰字改白、工具箱区圆角/折叠修复、开箱第一页语言选择'],
    ['22.34.0', '数据同步一条龙（HC 链路 E2E、配对向导第⓪步、登录页黑框根因修复）+ 小组件「载入错误」三真凶修复 + 繁體中文 + 开箱重排 + 工程笔记'],
    ['22.35.0', '小组件扩类（今日天气条 3×1、四信号 2×2，五语资源）+ 数据层加固（载荷兼容/时间单位归一/单测 80 个）+ 开箱主题实时预览卡'],
    ['22.36.0', '开发者模式「同步数据明细」表（七类×三通道）+ 设置返回逻辑改栈式 + 转场提速'],
    ['22.36.1', '热修：折叠栏打不开（展开组件回退 AnimatedVisibility，撤强切显示模式）'],
    ['22.37.0', '小米云扩面（压力/HRV）+ HC 内容透视诊断 + 华为导出明路'],
    ['22.38.0', '天气明信片（Canvas 手绘成图+系统分享）+ 开箱可玩化（气象球点选）'],
    ['22.39.0', '修复底栏滑块静止态无液态玻璃（折射恢复官方档 10/14、静止恢复边缘反光）'],
    ['22.40.0', '底栏滑块重画两态：静止=同玻璃淡色区（零自身畸变），拖动=液态透镜（拉伸+色散）'],
    ['22.47.0', '底栏滑块动画修复（remember key 误改）+ 拖动方形框修复（Capsule 裁切）+ 开箱「框拉伸、字后现」切换动画 + 末页欢迎使用/完成 + 开箱过渡 ×2 + 页1 文案精简'],
    ['22.46.0', '入场动画加长做完整（3.2s 五段）+ 开箱语言页两列大格 + 底栏切换改瞬时（按 demo 写法，去掉整页过渡）'],
    ['22.45.1', '热修：开场动画「B␣␣␣C」裂开 → BC↔BodyCast 整词交叉淡化（同轴交叠、字距收紧）'],
    ['22.45.0', '开箱语言区文案缩短 + 飞线动画放大一档（横 1/2、纵 1/5、叠缩放）+ 小组件全部带预览（previewLayout）'],
    ['22.44.0', '小米登录页安全键盘修复（JS 焦点探针 + 显式唤起输入法）+ 底栏字号跟随字体设置 + 开箱内容垂直居中 + BC→BodyCast 首启入场动画'],
    ['22.43.0', '换页卡顿 + 「方框」根治（落定动画与录制层分离、由大到小落定）+ 设置行块回原大小（两行内容居中于三行块）+ 语言名永远用原文（endonym）'],
    ['22.42.0', 'Health Connect 写入桥（聚合读数写入系统健康服务）+ i18n 西/德语 1675 条全覆盖 + 小组件再 +2 并适配深浅色 + 开箱收成五页（过渡再调校）+ 列表行文字垂直居中'],
    ['22.41.0', '开箱飞线形过渡 + 小米登录页加高/键盘修复 + 导出另存为/分享 + 食谱卡烹饪方法 + 玻璃开关黑底修复'],
])

h2('2.2 底栏（Liquid Glass）这条线：当前配方与教训')
para('当前配方（22.19.0）：底座 lens(24dp, 24dp) + blur 1.5dp，lens 排在 blur **之前**（先弯折再糊，折射才看得见）；'
     '容器染色 0.26，按下时叠 0.03 白；轮廓高光随按压淡出。染色副本层（承载图标文字、本身不可见）'
     'blur 2dp×(1−按压) + lens(24dp×按压)，表面画容器染色——这层板负责盖住滑块下方的原始内容。'
     '滑块采样 [环境 + 页面] ⊕ [染色副本层]：折射静止 5dp/7dp、随按压增长到 10dp/14dp（带光谱色散）；'
     '外圈高光与投影压 0；表面白色增亮 0.14×(1−按压)；按压放大 78/56 + 速度拉拽 + 逆变换内容缩进，与官方一致。')
bl([
    '想让折射看得见，lens 必须排在 blur 之前，且预留给透镜的采样带要够宽；',
    '折射带不能盖到文字：带子的宽度决定「文字被拖出副本」的严重程度（22.18.0 的静止档就是照这个定的）；',
    '滑块里显示的内容只能有一层：染色副本层要画容器表面盖住底层的原始内容，滑块采样的是「处理后的那一层」；',
    '录制层必须等于屏幕：环境层的录制里要有不透明底色，否则屏幕原始内容会从玻璃里透出来（22.16.0）；',
    '别自己加内框、别给滑块加模糊、拖动时不要画外圈线——这三条都是用户反复确认过的定调；',
    '两个折射数值（底座 / 滑块）别凭手感漂：底座对齐官方 (24, 24)、滑块按压上限对齐 (10, 14)，'
    '只在「静止档要不要有」上做取舍（22.7.0「拉伸不够」调大、22.19.0「太大」调回来，来回各一趟）；',
    '两次「改过头」的教训：底板整块淡到消失（16.0.0）、背板挖洞挖穿（18.0.0 之前），都在代码注释里留了档。',
])

h2('2.3 22.0.0 落地的六项新功能')
bl([
    '通知提醒：到点轻轻提醒一次（每日倾听提醒 + 微行动提醒，可分别开关与设定时间）。实现用 AlarmManager 的'
    '一次性闹钟、响过之后自己排下一次；通知文案不催促、不带任何数字；',
    '桌面小组件五种：主卡 2×2（天气 + 小事 + 吃什么）、今日天气条 3×1、四信号 2×2、'
    '今日微行动 2×1、今天先补 2×1；颜色跟着天气类型走，深浅色两套配色资源自动切换，数据由一份快照驱动；',
    '首次使用引导：五页开箱引导 + 分段进度条（不能整体跳过）；关于页里有「重看新手引导」；',
    '备份与恢复：把本机记录导出成 JSON 文本（走剪贴板，不写文件、不申请存储权限），也可以粘贴回来恢复；',
    '回声月视图：回声页顶部「这一周 / 这个月」切换；',
    '特大字号与主题图标：字号多一档「特大」；应用图标带 monochrome 层，跟随系统主题图标。',
])

h2('2.4 关于页与开发者模式')
bl([
    '22.0.0—22.12.0 试过「连续点击版本号五次 → 弹出密码盘」；22.13.0 起改为**红色描边的独立块**「打开开发者模式」'
    '（副标题写明「测试未完善、可能有 bug，正常用户请不要打开」）；',
    '点这个块 → 屏幕中间弹出居中的确认框（Liquid Glass 风格：大圆角、强折射、有纵深），下面并排「取消」与「确认打开」；',
    '确认之后，红块变成可进入的「开发者模式」菜单，点进去是第三级页「实验性功能」；状态持久化；',
    '实验性功能页：强调色（绿 / 蓝 / 紫 / 橙）、圆角风格、玻璃强度、底层开关「强制磨砂降级」、'
    '运行环境（版本 / SDK / 液态玻璃是否可用）与测试数据（一键填入演示读数）；改动即时生效。',
])

h2('2.5 一次失败的重构：平涂副本（性能教训）')
para('22.15.0 曾按批准的方向做过一次架构实验：让页面多组合一份不可见的「平涂副本」（副本里所有玻璃只画平涂色、'
     '不走折射），底栏改采样这份干净输入。原理上成立，代价是每帧要把整页画两遍、外加两份录制：')
bl([
    '实测帧耗时中位数 77ms、81% 掉帧——「卡死」的真身不是死循环，而是双份绘制；',
    '两个坑：副本画在可见页面之上会把滑动手势全抢走；两份组合各自 remember 滚动状态会导致栏里的滚动位置不对；',
    '22.15.2 整体回退到「页面自己录一层、底栏采样 [环境层 + 页面层]」（中位 46ms、掉帧 44.6%，'
    '模拟器软渲染基线）；失败方案与原因留档在 README 与代码注释。',
])

# ============================ 三、设计体系 ============================
h1('三、设计体系')
h2('3.1 视觉语言：Liquid Glass 液态玻璃')
para('整体采用「液态玻璃」视觉风格，基于开源库 Backdrop（Apache 2.0）。玻璃的折射是真实着色器实现的：'
     '边缘用 SDF 法线偏移采样产生拉伸畸变，配合光谱色散与模糊。和贴一张半透明 PNG 不同，'
     '这套玻璃背后「真的有东西在弯」。')
img(f'{SHOTS}/v23-fig2-bar.png', '图 2　底部导航（22.35.0，边缘拉伸已对齐官方默认）：上为拖动中的滑块（滑块里是被放大、弯折的图标文字，边缘能看到折射与光谱色散），下为同一帧的 2× 局部放大——外面那份图标文字照常显示，不会两份叠字', 5.0)
h2('3.2 老人友好设计规范')
tbl([
    ['项目', '规格'],
    ['字号阶梯（八档）', 'display 44 / pageTitle 28 / sectionTitle 21 / cardTitle 20 / body 17 / caption 15 / tabLabel 13 sp'],
    ['最小触摸目标', '56 dp（所有可点元素）；主按钮 58 dp、次按钮 56 dp'],
    ['强调色 / 默认主题', '#3ED598（绿），深浅两套色板；默认深色（玻璃折射在深色底上最清晰）'],
])
h2('3.3 圆角体系')
tbl([
    ['层级', '圆角半径'],
    ['底部导航栏（胶囊）', '32 dp'],
    ['主容器（各页卡片）', '32 dp'],
    ['次容器（列表行、指标块）', '20 dp'],
    ['行首图标色块', '12 dp'],
    ['浮层面板顶部', '34 dp'],
])
h2('3.4 五种身体天气与配色')
para('天气不是评价，只是状态。五种天气各有自己的颜色，首页气象球、周日历格、分享卡片都跟着它走：')
tbl([
    ['天气', '色值', '一句话'],
    ['恢复天', '#7FA8C9 雾蓝', '云在慢慢散，不用急着赶路'],
    ['高能天', '#E8A33D 橙黄', '阳光正好，可以往外走一点'],
    ['压力天', '#8C97A8 灰蓝', '风有点大，先找个避风的地方'],
    ['平衡天', '#3ED598 薄荷绿', '今天的身体，是稳的'],
    ['敏感天', '#9B8CFF 淡紫', '身体在说话，先听它说完'],
])
h2('3.5 玻璃的运行时分级')
bl([
    'Android 13 及以上：走完整液态玻璃——边缘用 SDF 法线偏移采样产生拉伸畸变，配合光谱色散与模糊，玻璃背后真的有东西在弯；',
    'Android 8.0 – 12：自动降级为磨砂实底（没有折射着色器），布局、功能、文案完全一致，只是玻璃变成毛玻璃；',
    '判定集中在 glass/GlassSupport.kt，两条路径共用同一套组件与尺寸，不存在「两个版本」；',
    '实验室开关：开发者模式里的「强制磨砂降级」可以在高版本上强制关掉折射着色器，用来对照检查两条路径（切换后离开当前页生效）。',
])
img(f'{SHOTS}/v22-bar-fallback.png', '图 3　同一条底栏的两种渲染路径：上为 Android 13+ 的液态玻璃（边缘有折射与色散、背后内容被弯折），下为磨砂降级（同一套尺寸与布局，玻璃变成毛玻璃实底、不做折射）', 5.0)
para('关于采样层的两条硬约束（踩过坑，写进代码注释）：一，玻璃元素把「背后的画面」录一份、折射后重画在自己位置上，'
     '未折射区域要能隐形，唯一条件是**录制像素 = 屏幕像素**，所以环境层的录制里必须包含应用的不透明底色'
     '（22.16.0 的根因修复）；二，录制层不能把玻璃自己录进去，否则渲染节点成环、启动即崩溃——'
     '因此页面里的玻璃卡片只采样环境层，页面内容单独录一层交给底栏（底栏是页面层的兄弟节点，不成环）。')
para('浮层面板在任何主题下都自带近实底，保证盖住下层内容。启动图标为自适应图标：'
     '黑底（带极弱斜向洗色与中心提亮）+ 微软雅黑「BC」，图形收在安全区内，圆形 / 方形 / squircle 遮罩都不会切到（见封面图）；'
     '另外带一层 monochrome，跟随系统主题图标。')

h2('3.6 玻璃参数表（如实记录，便于复现）')
para('这套玻璃全部是运行期着色器 + 图层参数，不是贴图。下表是当前版本（22.19.0）实际使用的数值：')
tbl([
    ['部件', '参数'],
    ['底座玻璃（底栏）', 'lens(24, 24)（官方默认）+ blur 1.5dp（lens 排在 blur 之前，先弯折再糊）；容器染色 0.26，按下叠 0.03 白；'
                          '两端各补一个取样色块（否则最边上看起来没有拉伸）；轮廓高光随按压从 1 淡到 0'],
    ['染色副本层', 'blur 2dp×(1−按压) + lens(24×按压)；表面画容器染色（盖住滑块下方的原始内容，本身不可见）'],
    ['选中滑块', 'lens 静止 5,7 → 按压增长到 10,14（官方默认，带光谱色散）；外高光与投影压 0；表面白色增亮 0.14×(1−按压)'],
    ['滑块的位移', '按压放大 78/56 + 速度拉拽（快速拖动时横向拉长）+ 逆变换内容缩进，与官方一致'],
    ['页面卡片 / 列表行', '卡片 blur 10dp + lens(16,24) + 投影 30%；列表行 blur 8dp + lens(12,20)；指标块 lens(11,18)；'
                            '提示条只做 8,14 的浅折射'],
    ['浮层面板', 'blur 18dp + lens(20,34) + 投影 45%，并垫一层近实底保证挡住下层正文'],
    ['降级实底', '低版本 / 强制磨砂时：实底 alpha 0.82（0.95 会喂黑底栏玻璃）'],
])
para('① lens(纵向折射高度, 横向折射量)：数值越大，边缘把背景「拉」进玻璃的幅度越大。'
     '折射高度决定「带子」有多宽——带子盖到文字上，文字就会被拖出一份副本，静止档的 5dp 就是照这个来定的（见 2.2）。')
h2('3.7 主题与图标')
para('支持浅色 / 深色 / 跟随系统，默认深色（玻璃折射在深色底上最清晰）。两套色板结构相同、只是取值不同，'
     '切换即时生效、不需要重启；浮层面板在任何主题下都自带近实底，保证盖住下层内容。'
     '启动图标为自适应图标：黑底（带极弱斜向洗色与中心提亮）+ 微软雅黑「BC」，图形收在安全区内，'
     '圆形 / 方形 / squircle 遮罩都不会切到。')
para('浅色主题下的首页见图 1 右侧：同一套版式与玻璃参数，只是换色板。')

h2('3.8 界面皮肤（22.31.0 起只有一套）')
para('界面只有一套：液态玻璃（即 3.1–3.7 那一套，是应用的身份）。22.31.0 之前还有第二套'
     '「谷歌风格（Material 3）」皮肤可在开箱引导与设置里切换，现已整套删除——两套并存带来的'
     '维护面（每个控件都要双分支、每改一处都要两套回归）远大于新鲜感的收益，'
     '而产品语言「把身体状态翻译成天气」本来就靠玻璃的通透感表达。')
para('实现上页面全部读一个「取值门面」（ui/skin/SkinTokens.kt），它逐字转发原设计规范——'
     '删皮肤时页面层的读点一个都没动，观感与 22.30.0 的玻璃皮肤完全一致。'
     'Material 3 的组件（Button / Switch / Card / TextField / Dialog / NavigationBar）'
     '不允许出现在页面层：玻璃观感是逐处调参调出来的，混进一个 M3 组件就会破功。')

# ============================ 四、页面详解 ============================
h1('四、页面详解（22.19.0 现状）')
para('本章配图是 22.19.0 release 包在 Pixel 10 Pro（Android 17，1280×2856）上的实机截图；'
     '为显示完整界面打开了「模拟数据模式」，界面上会挂出「当前是演示数据」的提示，不代表真实读数。')
h2('4.1 首页：先看状态，再看结论，最后才是建议')
bl([
    '问候语与日期，右上角「重新扫描」手动读取一次健康数据；',
    '「现在 · 四个信号」——恢复云量、压力风速、能量湿度、睡眠云层 2×2 排布，只给词不给数字，读不到显示「暂时没读到」；',
    '身体 hero 卡：气象球（颜色跟着天气类型走、缓慢呼吸，球内文字按长度自动选字号）+ 一句结论（如「今天的身体天气 · 平衡天」）；'
    '卡尾是产品主轴入口（22.30.0）：「今天先补：X」一句结论 + 主按钮「今天吃什么 · 去找含 X 的食物」，'
    '一键切到食谱页并按该元素筛好（压力/敏感天→镁、恢复天→蛋白质、高能天→复合碳水、平衡天→膳食纤维）；',
    '「今日微行动」默认折叠：标题 + 「今天可以先做这一件：X」+ 展开箭头，点开才是完整处方'
    '（早 / 午 / 晚时段胶囊可点选或拖拽、「开始 N 分钟微行动」进入倒计时引导、做完只回一句「身体收到。」）；',
    '「我现在的感觉是——」九个快速校准标签，点一下立刻生效、不用保存；',
    '页尾工具箱卡（天气诗、「为什么这样播报？」、语音播报）与身体回声入口卡。',
])
img(f'{SHOTS}/v23-fig4-rx.png', '图 4　左：首页的「今天可以做什么」（展开后）——处方步骤、时段胶囊与主操作按钮；'
    '右：身体天气校准——默认天气、校准标签自定义与敏感度（读不到数据时就靠这一页）', 3.3)
h2('4.2 食谱页：元素与食物的对应关系')
bl([
    '「元素 ↔ 食物」置顶：12 类元素单选（含铁、锌、维生素 A / D），选中后提示「正在看：XX」，列表只留这一类；',
    '直接搜：食物名、营养素名和口语词（「补铁」「助眠」「护眼」）都能搜到；',
    '食物列表（页面主体）：每张卡只出食材与吃法、零热量数字，点开看详情（为什么值得吃 / 怎么做 / 含哪些营养素）；',
    '菜系筛选与三餐建议：全部 / 中餐 / 西餐 / 日料 / 家常，绝不跨菜系；三餐按身体天气配、按权重抽取且当天不重复；'
    '过敏原与饮食偏好温和过滤；页面最下方是「如有特殊健康状况请咨询医生」。',
])
img(f'{SHOTS}/v23-fig5-recipe.png', '图 5　食谱页：左为默认状态（元素选择置顶 + 搜索 +「正在看：全部食物」），'
    '右为选中「铁」之后（元素高亮，列表切成含铁的食物，筛选同时作用于三餐建议）', 3.3)
h2('4.3 回声页：这一周 / 这个月身体经历过什么')
bl([
    '顶部「这一周 / 这个月」切换：周视图是七天气象格 + 颜色图例（没记的日子显示破折号，不补不猜）；'
    '「本周你照顾自己 N 次」正向计数卡（22.30.0，排在统计之前定调）：每做完一件微行动 +1，按 7 天求和；'
    '没有目标值、没有连击、漏做不扣——只是让你看见；',
    '回声统计：记录天数 / 连续记录 / 本月最多的天气 / 最常说的感觉（少于 3 天时给温和引导而不是空卡）；',
    '本周对照：最近 7 天 vs 上一个 7 天，只列变化最明显的两类天气，用朴素箭头句式，没有百分比、没有评分；',
    '这周的身体来信、天气诗、回声图表（四条定性曲线，无数值坐标轴）、分享卡片（截图即分享，不含任何数值）。',
])
img(f'{SHOTS}/v23-fig6-echo.png', '图 6　回声页：左为周天气日历与颜色说明（顶部可切这一周 / 这个月），'
    '右为回声统计与本周对照（记录天数、连续记录、本月最多天气，以及两周对比的朴素句式）', 3.3)
h2('4.4 设置页：六组一级菜单 + 九个二级页')
para('22.1.0 把设置页按「此刻想改什么」分成六组：数据（数据与同步）/ 身体（身体天气校准、微行动与处方）/'
     '内容（食谱指南）/ 提醒（通知与提醒）/ 数据与隐私（我的数据、权限与说明）/ 外观与关于（外观与体验、关于）。'
     '每组是一个分组卡片、行间用分割线；行首图标为平涂色块（外框 32dp 时图标 18dp，同心圆角）；'
     '副标题固定占两行，同组行高一致；一级 / 二级切换带短滑入动画，返回键与侧滑按层级回退；'
     '底部浮层面板都支持按住抓手跟手下拉关闭。')
img(f'{SHOTS}/v23-fig7-settings.png', '图 7　左：设置一级菜单（六组分组卡片，截图上方是演示数据提示）；'
    '右：数据源管理——顶部是「数据流水线自检」卡（四步三态），下面是厂商 App 引导、Health Connect 授权与数据来源', 3.3)
h2('4.5 关于页与开发者模式')
para('关于页首卡是产品定位（22.30.0）：「把身体状态翻译成天气，告诉你今天该怎么用力」——答辩口径与产品口径同一句。'
     '往下是：开发者三人（整行居中）、版本号、开源许可（Compose / AndroidX、Kotlin、Backdrop、Health Connect、'
     'Material 组件）、「重看新手引导」、反馈与建议（不联网，提供「复制版本信息」）。页面底部是开发者入口：'
     '默认是不可进入的红色描边块「打开开发者模式」，点开是居中的 Liquid Glass 确认框；'
     '确认后它变成可进入的「开发者模式」菜单，点进去是实验性功能页。')
img(f'{SHOTS}/v23-fig8-about.png', '图 8　左：关于页底部的开发者入口——红色描边的「打开开发者模式」，副标题写明「正常用户请不要打开」；'
    '右：点开后的确认框（Liquid Glass 风格）——完整的风险说明 + 取消 / 确认打开', 3.3)
img(f'{SHOTS}/v23-fig9-dev.png', '图 9　左：确认之后的第三级页「实验性功能」（强调色、圆角风格、玻璃强度、强制磨砂降级、运行环境与测试数据）；'
    '右：开箱引导（顶部是分段进度条 + 上一步 / 下一步，七页见 4.6）', 3.3)
h2('4.6 开箱引导')
para('第一次打开会先走一遍七页引导（22.31.0 重排）：①把身体状态翻译成天气（含主题色点选）'
     '②它不评分，也不排名 ③每天的三个问题（首页 / 食谱 / 回声各管什么）④数据从哪来（手环绑定）'
     '⑤今天是什么天气你说了算（校准）⑥今天的这一件小事（微行动）⑦只存在这台手机上（隐私）。'
     '页面之间滑动 + 淡入淡出；导航规则是**不能跳过整个引导**——右上角的全局「跳过」已删掉，'
     '必须一页页走完，但每页都有「上一步」可以回头；只有第 ④ 页（绑定数据）右上角有一个「跳过」，'
     '跳过的是绑定这一步、不是引导本身。走完最后一页点「开始使用」收尾；「重看新手引导」随时可重看。')

# ============================ 五、多语言 ============================
h1('五、多语言与文案')
bl([
    '支持七种语言：简体中文（默认）、繁體中文、英语、Español、Deutsch、日语、拉丁文；不选则跟随系统'
    '（中文含台港澳按 Hant 走繁體转换层，系统日语显示日语，系统西/德语对应 Español / Deutsch，'
    '其它语言显示英语；拉丁文没有对应的系统语言，只能应用内显式选择）；',
    '切换是内存态完成的——不重建界面，瞬间生效，也不会把当前二级页弹回一级页；',
    '界面文案集中在 BodyCastText / HomeTextCatalog / DomainText；运行期拼接的句子用正则模板覆盖；'
    '空槽一律兜底到英语；用户自己输入的昵称与自定义标签保留原文；',
    '22.13.0 做过一次全量补齐；22.20.0 新增日语与拉丁文：日语按「句子です・ます、标签体言止め、'
    '诗句与来信用常体」的口径写完（实测 1185/1187），拉丁文按「简化技术拉丁：主格单数 + 祈使句、'
    '不做变格」写完 P0/P1（834/1166），长内容（诗句、来信、开发者页、传统菜名、做法段落）按设计回退英语；',
    '22.34.0 繁體中文走「台湾用语词表 + 字级简转繁」转换层（伺服器/数位这类词表也换了）；'
    '22.42.0 新增西班牙语与德语：i18n/EsDe.kt 用 EN→ES/DE 映射表挂进取词口，1675 条全覆盖，'
    '小组件另出 values-es / values-de 资源，模拟器扫屏零中文泄漏；',
    '开发者页有「漏翻审计」出口（逐句表缺口 / 兜底回退两类计数，可列前 30 条并清空）——'
    '日语界面本来就有汉字，字符集 grep 查不出漏翻，只有审计计数可靠。',
])

# ============================ 六、数据与隐私 ============================
h1('六、数据与隐私')
bl([
    '应用不联网、没有服务器、没有账号；不保存任何原始生理数值——读数进来只为算天气，算完即丢弃，'
    '本机只存「今天是什么天气」和「你勾过的感觉标签」；',
    '默认只读：仅申请六项读取权限（步数、睡眠、静息心率、心率变异性、血氧、运动记录）。'
    '唯一的写入例外是 22.42.0 的写入桥——用户显式打开「把读数写入系统健康服务」并授权后，'
    '才写入本机已同步的聚合读数（睡眠/运动只在有真实起止时间时写），开关默认关闭；',
    '导出与备份都走剪贴板（JSON/CSV），不写文件、不申请存储权限，内容只有日期 + 天气 + 标签；'
    '「删除所有数据」一键清空本机记录；',
    '第三条**可选通道**（22.25.0）：「小米账号（云同步）」——登录走小米官方页面（应用不碰密码），'
    '令牌用 Android Keystore 加密存本机、可随时断开并删除；取回的是用户自己账号里的六类读数，'
    '超过 24 小时视为过期。默认关闭；开启后应用才会联网，隐私说明页已如实写明；',
    '两条**零权限通道**（22.24.0）：①「手动填写读数」——打开厂商 App 看到数字照着抄进来，六类字段都注明在厂商 App 哪里看，'
    '不认识的留空（留空不是 0），24 小时后自动过期；②「文件导入」——应用出现在厂商 App 的系统分享面板里，也能从设置里选文件，'
    '解析器只按表头认列、认不出就原样列出来，确认后才写入，同样 24 小时过期；两条都只存聚合值、明细不落盘；',
    '数据链路：手环 → 厂商运动健康 App → Health Connect → BodyCast（应用不直连手环、不用蓝牙）；'
    '22.22.0 起数据源页给出一张「数据流水线自检」卡：① 系统健康服务是否就绪 ② 六类权限是否齐 '
    '③ 厂商有没有真的写进数据（每条流水线的最后写入时间 + 来源 App）④ 本次读到几类——'
    '断在哪一步就修哪一步，不再让用户盲试；卡顶先给一句结论（「断在第 ③ 步：厂商还没写数据」）与**一个主按钮**'
    '（按状态自动决定它是哪一步的修法），授权返回与手动同步后卡片会自己重新检查；',
    '华为/荣耀手环的用户：厂商只写自家健康服务、不写 Health Connect，应用会如实说明，并给出「借道第三方桥接 App」的'
    '一次性引导（桥接把华为数据写进 Health Connect，之后与其它数据源一样自动）；',
])

# ============================ 七、典型使用流程 ============================
h1('七、典型使用流程')
h2('7.1 第一次打开：先走四页引导，再五步上手')
bl([
    '① 打开先看四页开箱引导（不评分不排名 / 数据从哪来 / 今天是什么天气你说了算 / 只存在这台手机上），'
    '右上角可跳过；没有开屏等待页，也没有账号；',
    '② 想读手环：设置 → 数据与同步 → 连接（小米 / 华为 / Health Connect），在系统授权框里勾选六类读取权限；'
    '不想连也没关系——手动校准和演示数据一直可用；',
    '③ 首页的九个快速校准标签（如「睡得还行」「压力有点大」）点一下立刻改写今天的天气，优先级高于设备读数；',
    '④ 看今天的身体天气（气象球 + 一句话结论），再看四项指标——只给词，不给数字（校准页见图 4 右侧）；',
    '⑤ 「今日微行动」默认折叠，点开挑一件几分钟能做完的，跟着做完只回一句「身体收到。」，'
    '再随手点一个感受（更累了 / 更稳了）——它会参与下一次处方的调整。',
])
h2('7.2 老用户的一天')
bl([
    '早上：打开看今天是什么天气，顺手挑一件微行动做完；也可以在设置里开「每日倾听提醒」，'
    '到点由系统通知轻轻提醒一次（不催促、不带数字）；',
    '中午：食谱页先选元素再看食物——想看「补铁」就点「铁」（也能直接搜「助眠」「护眼」）；',
    '晚上：回声页回放这一周或这个月——周天气日历、回声统计、本周对照、身体来信、天气诗、四条定性趋势，再截一张分享卡；',
    '随时：偏好都在设置里（六组），想录感受就点首页的校准标签；把小组件放到桌面，解锁就能看到今天的身体天气。',
])
h2('7.3 数据读不到的时候')
para('这是健康类应用最容易被做假的地方，BodyCast 的选择是：读不到就如实说。')
bl([
    '四项指标显示「暂时没读到」，并写清原因（没有活动数据 / 设备没有提供压力数据 / 没有睡眠数据）；',
    '首页出现一条可操作提示：要么去设置里连接手环，要么直接用「手动校准」；',
    '想看完整界面效果，可以在数据源管理里打开「模拟数据模式」，界面上会挂出「当前是演示数据」的提示；',
    '导出到剪贴板的数据也只有「日期 + 天气 + 你勾过的标签」，不含任何原始生理数值。',
])

# ============================ 八、验证与质量保障 ============================
h1('八、验证与质量保障')
h2('8.1 每个版本都这样验证')
bl([
    '编译：用包内 gradlew 在 JDK 21 上跑 assembleRelease，判定看 BUILD SUCCESSFUL；装机后四个页面逐个走一遍，'
    '看 logcat 有没有 FATAL EXCEPTION；',
    '像素级复核：玻璃观感的改动把前后截图逐像素比对；底栏至少要在静止 / 拖动中 / 回弹三态各截一张对照'
    '（22.18.0 的滑块重影就是这样抓到的）；',
    '与官方源码逐行对照：把官方仓库（Kyant0/AndroidLiquidGlass，kmp 分支）克隆下来，与我们的 LiquidBottomTabs.kt'
    '逐行 diff，并对着官方 shader 源码核对参数语义；偏离官方处都要在注释里写明原因；',
    'A/B 帧耗时（dumpsys gfxinfo）；从零构建验证（源码包解压到空目录重跑 assembleRelease）；'
    '模拟器矩阵：Pixel 10 Pro（Android 17）走液态玻璃，Pixel 6 Pro（Android 12）走磨砂降级。',
])
h2('8.2 版本归档与回归习惯')
bl([
    'apks/ 里逐版留档（9.0.0 起），根目录同时放最新版副本；branches/ 里保存大版本开工前的完整源码快照；',
    'README.md 记录版本变更与已知限制；凡是出现「改过头」的改动（底板整块消失、图标被删掉等），'
    '都在代码注释里留档失败方案与原因，避免下次重犯。',
])

# ============================ 九、技术架构 ============================
h1('九、技术架构与构建')
h2('9.1 技术栈')
tbl([
    ['项目', '版本 / 说明'],
    ['compileSdk / targetSdk / minSdk', '37 / 37（Android 17）/ 26（Android 8.0）'],
    ['Kotlin / AGP / Gradle', '2.4.10 / 9.3.2 / 9.5（构建 JDK 21）'],
    ['UI / 液态玻璃 / 健康数据', 'Jetpack Compose 1.12（零 XML） / Backdrop 2.0.1（真实 AGSL 着色器折射） / Health Connect 1.1.0-alpha10（读 + 可选写入桥）'],
    ['签名', '调试密钥侧载签名，release 可直接安装'],
])
h2('9.2 分层')
bl([
    '业务逻辑层（bodycast/）：规则引擎、本地仓库、数据模型与内容库；不碰界面，也不碰厂商 SDK；',
    '数据源层（data/）：HealthSource 接口 + Health Connect 实现 + 备份恢复；每项读数独立尝试，读不到就留空；',
    '玻璃组件层（glass/）：官方 Catalog 逐字照抄的组件 + 自写容器件，Android 13 以下自动降级；',
    '页面层（ui/、ui/bodycast/）：四个页面 + 共用控件 + 浮层系统 + 开箱引导；'
    '系统集成层（notify/、widget/）：提醒调度与桌面小组件。',
])
h2('9.3 运行时分级与已知取舍')
bl([
    'Android 13+ 走完整液态玻璃（有折射着色器）；Android 8–12 自动降级为磨砂实底，功能完全一致；',
    '采样层两条硬约束：环境层的录制里必须包含应用的不透明底色（录制像素 = 屏幕像素，未折射区域才隐形）；'
    '录制层不能把玻璃自己录进去（渲染节点成环会启动即崩溃）——所以页面玻璃只采样环境层，'
    '页面内容单独录一层交给底栏采样；',
    '底栏采样结构：底座采样 [环境 + 页面]；染色副本层（不可见）承载图标文字并画容器表面，'
    '盖住滑块下方的原始内容；滑块采样 [环境 + 页面] ⊕ [染色副本层]；',
    '降级路径的取舍：低版本可读性靠实底（alpha 0.82）——这个值不能太高，0.95 会让底栏玻璃把近黑实底'
    '采样进去、整条底板变黑。',
])

# ============================ 十、开发信息 ============================
h1('十、开发信息')
tbl([
    ['项目', '内容'],
    ['开发者 / 开发使用的 AI', 'zhangyuhexht / __xiaocai__　·　Kimi 3、Kimi 2.8 Preview、DeepSeek V4.1 Flash'],
    ['开源依赖', 'Jetpack Compose、AndroidX / AppCompat、Kotlin、Kyant Backdrop / Shapes、Health Connect、Material Components（仅窗口主题）'],
    ['包名 / 当前版本', 'cn.pocket.toolbox / 22.47.0（versionCode 74）'],
    ['应用名 / 支持语言', 'BodyCast（安装器与桌面显示，不带中文）　·　简体中文、英语（跟随系统自动选择）'],
    ['运行环境 / APK 归档', 'Android 8.0 及以上（13 以下自动磨砂降级）　·　apks/BodyCast-22.47.0.apk（9.0.0 起逐版留档）'],
])
h2('10.1 版本历史')
para('4.0.0 以前的版本是原型阶段（工具盒子 → 健康记录 → 现在的身体天气），只留档在 branches/ 快照里；'
     '4.0.0 之后（含 4.0.0 – 9.0.0 的要点）的逐版改动见第二章的「版本改动一览」。')
h2('10.2 已知限制')
bl([
    '通知与提醒已实现（AlarmManager 一次性闹钟），能否准时受系统省电策略影响，界面上不承诺精确到分；',
    '背景音选项会被记住，但本版本仍然没有任何音频资源文件（界面上如实标注）；',
    '个别动态长文案（个别食谱做法、处方步骤）在英语下可能仍显示中文原文，TranslationAudit 会记录；',
    '手环数据依赖厂商 App 同步进 Health Connect；部分机型需自行安装 Health Connect；',
    '性能基线在模拟器（软渲染）上量取，真机需另测；单元测试为空，靠真机 / 模拟器冒烟；',
    '拉丁文的长内容（诗句、来信、开发者页、开源许可、传统菜名、做法段落）按设计回退英语，'
    '开发者页与实验性功能页在非中文语言下是英语/日语混合；通知渠道名与桌面小组件的静态串跟系统语言（既有行为）；',
    '隐私说明页（PrivacyPolicyActivity）存在一个发版前就有的崩溃（RenderThread 栈溢出），待单独修。',
])

# ============================ 附录 ============================
h1('附录 A　源码结构（关键目录）')
para('源码包内 app/src/main/java/cn/pocket/toolbox/ 的分层（共 45 个 Kotlin 文件、约 1.7 万行）：')
tbl([
    ['目录', '职责', '代表文件'],
    ['ui/、ui/bodycast/', '页面层：四个 tab、全局采样层、换页手感、开箱引导', 'MainActivity.kt、HomeTab.kt、RecipeTab.kt、EchoTab.kt、SettingsTab.kt、Onboarding.kt'],
    ['bodycast/', '业务层：规则引擎、仓库、数据模型、内容库', 'Engine.kt、BodyCastStore.kt、Signals.kt、Weather.kt、Actions.kt、Recipe.kt、Echo.kt'],
    ['glass/', '玻璃层：设计规范、降级开关、玻璃控件', 'GlassTheme.kt、GlassSupport.kt、GlassCard.kt、LiquidBottomTabs.kt、InteractiveHighlight.kt、GlassIcons.kt'],
    ['i18n/', '文案层：语言、文案总表、运行期模板、漏翻审计', 'AppLanguage.kt、BodyCastText.kt、DomainText.kt、HomeTextCatalog.kt、TranslationAudit.kt'],
    ['data/、notify/、widget/', '数据源、提醒与桌面小组件', 'HealthSource.kt、HealthConnectSource.kt、Backup.kt、ReminderScheduler.kt、BodyCastWidgetProvider.kt'],
])
h1('附录 B　构建与出包')
bl([
    '环境：JDK 21、Android SDK（compileSdk / targetSdk 37）、包内 Gradle Wrapper；构建：'
    './gradlew :app:assembleRelease --console=plain，产物在 app/build/outputs/apk/release/，'
    '安装用 adb install -r（调试密钥签名，可直接侧载）；',
    '注意：gradle.properties 里必须保留 android.builtInKotlin=false（AGP 9 内置的 Kotlin 编译器读不了 Backdrop 库的 metadata）；'
    '工程路径尽量不要带中文；',
    '出包流程：改版本号 → assembleRelease → 拷一份到 apks/ 与根目录 → 更新 README 与本文档 → '
    '源码打包后解压到空目录再跑一次构建验证。',
])
h1('附录 C　术语表')
tbl([
    ['术语', '含义'],
    ['身体天气', '用天气词播报的身体状态：恢复天 / 高能天 / 压力天 / 平衡天 / 敏感天'],
    ['微行动', '10 分钟以内、不需要器械、现在就能做完的一件小事'],
    ['液态玻璃 / 磨砂降级', '带真实折射与光谱色散渲染的玻璃材质（Android 13+）／低版本自动退回的毛玻璃实底'],
    ['折射 / 色散', '边缘把背景「拉」进玻璃的形变 / 边缘拉出来的一圈光谱彩边'],
    ['手动校准', '用户自己勾的感受标签，优先级高于设备读数'],
    ['暂时没读到', '数据缺失时的如实显示——null 不等于 0'],
])

# ============================ 打包 ============================
DOC = ''.join(B)
document_xml = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
<w:body>{DOC}
<w:sectPr><w:footerReference w:type="default" r:id="rIdFooter1"/>
<w:footerReference w:type="first" r:id="rIdFooter2"/>
<w:pgSz w:w="11906" w:h="16838"/>
<w:pgMar w:top="980" w:right="980" w:bottom="980" w:left="980"/>
<w:titlePg/>
</w:sectPr></w:body></w:document>'''

styles_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/>
<w:rPr><w:rFonts w:ascii="Calibri" w:eastAsia="微软雅黑"/><w:sz w:val="22"/></w:rPr>
<w:pPr><w:spacing w:after="22" w:line="224" w:lineRule="auto"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/>
<w:basedOn w:val="Normal"/><w:pPr><w:jc w:val="center"/><w:spacing w:before="1200" w:after="200"/></w:pPr>
<w:rPr><w:b/><w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="52"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Subtitle"><w:name w:val="Subtitle"/>
<w:basedOn w:val="Normal"/><w:pPr><w:jc w:val="center"/><w:spacing w:after="400"/></w:pPr>
<w:rPr><w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="30"/><w:color w:val="555555"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/>
<w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="200" w:after="80"/>
<w:outlineLvl w:val="0"/></w:pPr>
<w:rPr><w:b/><w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="32"/><w:color w:val="1F8A5E"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/>
<w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="150" w:after="60"/>
<w:outlineLvl w:val="1"/></w:pPr>
<w:rPr><w:b/><w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="27"/><w:color w:val="333333"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading3"><w:name w:val="heading 3"/>
<w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="110" w:after="50"/>
<w:outlineLvl w:val="2"/></w:pPr>
<w:rPr><w:b/><w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="24"/></w:rPr></w:style>
</w:styles>'''

overrides = ''.join(
    f'<Override PartName="/word/media/image{i}.png" ContentType="image/png"/>'
    for i in range(1, len(_images) + 1))
footer_overrides = (
    '<Override PartName="/word/footer1.xml" '
    'ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>'
    '<Override PartName="/word/footer2.xml" '
    'ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>'
    '<Override PartName="/word/settings.xml" '
    'ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>'
)
content_types = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Default Extension="png" ContentType="image/png"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
{overrides}
{footer_overrides}</Types>'''

rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>'''

img_rels = ''.join(
    f'<Relationship Id="rIdImg{i}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image{i}.png"/>'
    for i in range(1, len(_images) + 1))
doc_rels = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
<Relationship Id="rIdFooter1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/>
<Relationship Id="rIdFooter2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer2.xml"/>
<Relationship Id="rIdSettings" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>
{img_rels}</Relationships>'''

_fld_rpr = ('<w:rPr><w:rFonts w:eastAsia="微软雅黑"/><w:sz w:val="20"/>'
            '<w:color w:val="808080"/></w:rPr>')
footer1_xml = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:p><w:pPr><w:jc w:val="center"/></w:pPr>
<w:r>{_fld_rpr}<w:t xml:space="preserve">BodyCast 软件设计说明书 · 第 </w:t></w:r>
<w:fldSimple w:instr=" PAGE "><w:r>{_fld_rpr}<w:t>1</w:t></w:r></w:fldSimple>
<w:r>{_fld_rpr}<w:t xml:space="preserve"> 页</w:t></w:r>
</w:p></w:ftr>'''
footer2_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:p><w:pPr><w:jc w:val="center"/></w:pPr></w:p></w:ftr>'''
settings_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:settings xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:updateFields w:val="true"/>
</w:settings>'''

out = '软件设计说明书.docx'   # 正式名；另存一份带版本号的副本在项目根目录
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
    z.writestr('[Content_Types].xml', content_types)
    z.writestr('_rels/.rels', rels)
    z.writestr('word/document.xml', document_xml)
    z.writestr('word/styles.xml', styles_xml)
    z.writestr('word/footer1.xml', footer1_xml)
    z.writestr('word/footer2.xml', footer2_xml)
    z.writestr('word/settings.xml', settings_xml)
    z.writestr('word/_rels/document.xml.rels', doc_rels)
    for i, path in enumerate(_images, 1):
        z.write(path, f'word/media/image{i}.png')

print('written:', out, os.path.getsize(out), 'bytes; images:', len(_images))
