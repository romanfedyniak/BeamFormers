"""Turns the Blockbench models in models-src into the mod's 1.12 models and textures: python tools/convert.py"""
import json, colorsys, os, re
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
SRC = os.path.join(ROOT, 'models-src') + '/'
REPO = os.path.join(ROOT, 'src/main/resources/assets/beamformers') + '/'
AXES = 'xyz'


def pink(p):
    r, g, b, a = p
    if a == 0:
        return False
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return s > 0.25 and r > 150


def rnd(v):
    v = round(v, 4)
    return int(v) if v == int(v) else v


def convert(bb, png, kind, name, xform):
    d = json.load(open(SRC + bb, encoding='utf-8'))
    res = d['resolution']['width']
    assert d['resolution']['height'] == res
    tex = Image.open(SRC + png).convert('RGBA')
    assert tex.size == (res, res), (tex.size, res)

    glow = Image.new('RGBA', tex.size)
    for y in range(res):
        for x in range(res):
            if pink(tex.getpixel((x, y))):
                glow.putpixel((x, y), tex.getpixel((x, y)))

    folder = 'parts' if kind == 'part' else 'blocks'
    os.makedirs(REPO + 'textures/' + folder, exist_ok=True)
    tex.save(REPO + f'textures/{folder}/{name}.png')
    glow.save(REPO + f'textures/{folder}/{name}_glow.png')

    def has_pink(uv):
        u1, v1, u2, v2 = uv
        xs = range(int(min(u1, u2)), int(max(u1, u2) + 0.999))
        ys = range(int(min(v1, v2)), int(max(v1, v2) + 0.999))
        return any(pink(tex.getpixel((x, y))) for x in xs for y in ys if 0 <= x < res and 0 <= y < res)

    base, lit = [], []
    for e in d['elements']:
        inf = e.get('inflate', 0)
        f = xform([a - inf for a in e['from']])
        t = xform([a + inf for a in e['to']])
        out = {'from': [rnd(v) for v in f], 'to': [rnd(v) for v in t]}
        r = e.get('rotation')
        if r and any(r):
            axes = [i for i in range(3) if r[i]]
            assert len(axes) == 1 and abs(r[axes[0]]) in (22.5, 45), r
            out['rotation'] = {'angle': r[axes[0]], 'axis': AXES[axes[0]],
                               'origin': [rnd(v) for v in xform(e['origin'])]}
        faces, glows = {}, {}
        for side, fc in e['faces'].items():
            if fc.get('texture') is None:
                continue
            uv = [rnd(u * 16 / res) for u in fc['uv']]
            faces[side] = {'uv': uv, 'texture': '#body'}
            if has_pink(fc['uv']):
                glows[side] = {'uv': uv, 'texture': '#glow', 'uvlightmap': {'block': 15, 'sky': 15}}
        base.append(dict(out, faces=faces))
        if glows:
            lit.append(dict(out, faces=glows))

    model = {
        'ae2_uvl_marker': True,
        'textures': {
            'particle': f'beamformers:{folder}/{name}',
            'body': f'beamformers:{folder}/{name}',
            'glow': f'beamformers:{folder}/{name}_glow',
        },
        'elements': base + lit,
    }
    os.makedirs(REPO + 'models/' + kind, exist_ok=True)
    with open(REPO + f'models/{kind}/{name}.json', 'w', encoding='utf-8', newline='\n') as fp:
        text = json.dumps(model, indent=2)
        text = re.sub(r'\[\s*([-\d.,\s]+?)\s*\]', lambda m: '[' + ', '.join(re.split(r'[,\s]+', m.group(1).strip())) + ']', text)
        fp.write(text + '\n')
    print(name, len(base), 'elements,', len(lit), 'glowing')


def bounds(bb):
    d = json.load(open(SRC + bb, encoding='utf-8'))
    lo = [min(min(e['from'][i], e['to'][i]) - e.get('inflate', 0) for e in d['elements']) for i in range(3)]
    hi = [max(max(e['from'][i], e['to'][i]) + e.get('inflate', 0) for e in d['elements']) for i in range(3)]
    return lo, hi


# small: scaled 0.7 about its own centre, back face against the cable core (z 6), pointing north
lo, hi = bounds('small_beam_former.bbmodel')
cx, cy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2
S = 0.7
convert('small_beam_former.bbmodel', 'small_beam_former.png', 'part', 'small_beam_former',
        lambda p: [8 + (p[0] - cx) * S, 8 + (p[1] - cy) * S, 6 + (p[2] - hi[2]) * S])

for bb, png, name in [('dense_beam_former.bbmodel', 'dense_beam_former.png', 'dense_beam_former'),
                      ('beam_tower.bbmodel', 'beam_tower.png', 'beam_tower')]:
    lo, hi = bounds(bb)
    dx, dy = 8 - (lo[0] + hi[0]) / 2, 8 - (lo[1] + hi[1]) / 2
    convert(bb, png, 'block', name, lambda p, dx=dx, dy=dy: [p[0] + dx, p[1] + dy, p[2]])
