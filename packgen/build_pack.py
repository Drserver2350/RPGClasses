#!/usr/bin/env python3
"""
Generates the RPGClasses resource pack (class icons as custom item models) and zips it
into src/main/resources/pack.zip so it gets bundled inside the plugin jar.
"""
import json, math, os, shutil, zipfile
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(ROOT, "pack")
NS = "rpgclasses"
S = 256          # draw resolution
FINAL = 64       # texture resolution shipped

CLASSES = {
    "warrior":     (0xC0392B, "sword"),
    "paladin":     (0xF1C40F, "holysword"),
    "berserker":   (0x8E2B2B, "axe"),
    "guardian":    (0x7F8C8D, "shield"),
    "ranger":      (0x27AE60, "bow"),
    "assassin":    (0x2C3E50, "dagger"),
    "samurai":     (0xE74C3C, "katana"),
    "mage":        (0x3498DB, "staff"),
    "pyromancer":  (0xE67E22, "flame"),
    "cryomancer":  (0x5DADE2, "snowflake"),
    "stormcaller": (0xF4D03F, "bolt"),
    "necromancer": (0x6C3483, "skull"),
    "warlock":     (0x9B59B6, "eye"),
    "druid":       (0x196F3D, "leaf"),
    "cleric":      (0xECF0F1, "cross"),
    "shaman":      (0x1ABC9C, "totem"),
    "bard":        (0xF39C12, "note"),
    "monk":        (0xD4AC0D, "fist"),
    "alchemist":   (0x58D68D, "flask"),
    "artificer":   (0xAAB7B8, "gear"),
}

def rgb(h): return ((h >> 16) & 255, (h >> 8) & 255, h & 255)
def mix(c, t, f):  return tuple(int(a + (b - a) * f) for a, b in zip(c, t))
def lighten(c, f): return mix(c, (255, 255, 255), f)
def darken(c, f):  return mix(c, (0, 0, 0), f)

def rot(points, cx, cy, deg):
    a = math.radians(deg); ca, sa = math.cos(a), math.sin(a)
    return [(cx + (x - cx) * ca - (y - cy) * sa, cy + (x - cx) * sa + (y - cy) * ca) for x, y in points]

# --------------------------------------------------------------------------- background
def background(color):
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    # radial gradient inside a rounded shield/diamond shape
    grad = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gd = ImageDraw.Draw(grad)
    for i in range(60, 0, -1):
        f = i / 60
        gd.ellipse([S/2 - f*S*0.62, S/2 - f*S*0.62 - 20, S/2 + f*S*0.62, S/2 + f*S*0.62 - 20],
                   fill=mix(lighten(color, 0.35), darken(color, 0.55), f) + (255,))
    mask = Image.new("L", (S, S), 0)
    md = ImageDraw.Draw(mask)
    # rounded square rotated 45° => gem shape
    pts = rot([(38, 38), (S-38, 38), (S-38, S-38), (38, S-38)], S/2, S/2, 45)
    md.polygon(pts, fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(1.2))
    img.paste(grad, (0, 0), mask)
    d = ImageDraw.Draw(img)
    # outline (dark + light inner rim)
    d.polygon(pts, outline=darken(color, 0.75) + (255,), width=9)
    inner = rot([(52, 52), (S-52, 52), (S-52, S-52), (52, S-52)], S/2, S/2, 45)
    d.polygon(inner, outline=lighten(color, 0.45) + (140,), width=3)
    # glossy highlight
    gloss = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gdr = ImageDraw.Draw(gloss)
    gdr.ellipse([S*0.28, S*0.12, S*0.72, S*0.42], fill=(255, 255, 255, 60))
    gloss = gloss.filter(ImageFilter.GaussianBlur(10))
    img.alpha_composite(gloss)
    return img

# --------------------------------------------------------------------------- symbols
W  = (250, 250, 250, 255)   # symbol main
W2 = (200, 205, 215, 255)   # symbol shade
K  = (25, 20, 30, 255)      # symbol outline

def poly(d, pts, fill=W, outline=K, width=7):
    d.polygon(pts, fill=fill, outline=outline, width=width)

def thick_line(d, a, b, width, fill=W, outline=K):
    d.line([a, b], fill=outline, width=width + 8)
    d.line([a, b], fill=fill, width=width)

def sym_sword(d, c=S/2, gold=(230, 190, 60, 255)):
    pts = rot([(c-14, 60), (c+14, 60), (c+10, 160), (c-10, 160)], c, c, 0)
    poly(d, [(c, 40)] + pts)  # blade
    poly(d, [(c-48, 156), (c+48, 156), (c+48, 176), (c-48, 176)], fill=gold)     # guard
    poly(d, [(c-10, 176), (c+10, 176), (c+10, 218), (c-10, 218)], fill=(120, 75, 40, 255))  # grip
    d.ellipse([c-16, 210, c+16, 242], fill=gold, outline=K, width=6)

def sym_holysword(d):
    c = S/2
    d.ellipse([c-70, c-70, c+70, c+70], fill=(255, 245, 190, 80))
    sym_sword(d, gold=(255, 215, 70, 255))
    # rays
    for ang in range(0, 360, 45):
        a = math.radians(ang)
        d.line([(c + 82*math.cos(a), c + 82*math.sin(a)), (c + 100*math.cos(a), c + 100*math.sin(a))], fill=(255, 240, 160, 255), width=8)

def sym_axe(d):
    c = S/2
    thick_line(d, (c-40, 210), (c+50, 60), 16, fill=(130, 85, 45, 255))
    head = [(c+20, 40), (c+95, 70), (c+105, 130), (c+50, 120), (c+35, 100)]
    poly(d, head, fill=W2)
    poly(d, [(c+28, 55), (c+85, 78), (c+90, 118), (c+50, 108)], fill=W, outline=None, width=0)

def sym_shield(d):
    c = S/2
    pts = [(c-72, 60), (c+72, 60), (c+72, 130), (c, 210), (c-72, 130)]
    poly(d, pts, fill=W2, width=8)
    poly(d, [(c-52, 78), (c+52, 78), (c+52, 124), (c, 186), (c-52, 124)], fill=(110, 130, 150, 255), outline=None, width=0)
    poly(d, [(c-10, 95), (c+10, 95), (c+10, 165), (c-10, 165)], fill=W, outline=None, width=0)
    poly(d, [(c-38, 115), (c+38, 115), (c+38, 133), (c-38, 133)], fill=W, outline=None, width=0)

def sym_bow(d):
    c = S/2
    d.arc([c-70, 40, c+90, 216], 110, 250, fill=K, width=20)
    d.arc([c-70, 40, c+90, 216], 110, 250, fill=(140, 90, 45, 255), width=10)
    thick_line(d, (c-40, 56), (c-40, 200), 3, fill=W2)   # string
    thick_line(d, (c-95, 128), (c+70, 128), 6, fill=W)    # arrow
    poly(d, [(c+70, 116), (c+98, 128), (c+70, 140)], fill=W2)
    poly(d, [(c-95, 118), (c-75, 128), (c-95, 138), (c-110, 128)], fill=(220, 80, 80, 255))

def sym_dagger(d):
    c = S/2
    pts = rot([(c, 36), (c+16, 70), (c+9, 150), (c-9, 150), (c-16, 70)], c, c, -30)
    poly(d, pts, fill=W2)
    poly(d, rot([(c-34, 148), (c+34, 148), (c+34, 164), (c-34, 164)], c, c, -30), fill=(80, 80, 95, 255))
    poly(d, rot([(c-9, 164), (c+9, 164), (c+9, 210), (c-9, 210)], c, c, -30), fill=(40, 40, 50, 255))
    # blood drop
    d.ellipse([c+40, 60, c+62, 82], fill=(200, 30, 40, 255), outline=K, width=4)

def sym_katana(d):
    c = S/2
    blade = rot([(c-6, 30), (c+8, 34), (c+4, 160), (c-8, 160)], c, c, -40)
    poly(d, blade, fill=W)
    poly(d, rot([(c-22, 156), (c+22, 156), (c+22, 168), (c-22, 168)], c, c, -40), fill=(230, 190, 60, 255))
    poly(d, rot([(c-8, 168), (c+8, 168), (c+8, 226), (c-8, 226)], c, c, -40), fill=(160, 30, 30, 255))
    # rising sun disc
    d.ellipse([c-96, c-10, c-56, c+30], fill=(230, 60, 60, 255), outline=K, width=5)

def sym_staff(d):
    c = S/2
    thick_line(d, (c-30, 222), (c+22, 90), 12, fill=(120, 80, 45, 255))
    d.ellipse([c-2, 36, c+62, 100], fill=(120, 190, 255, 255), outline=K, width=7)
    d.ellipse([c+12, 48, c+36, 72], fill=(230, 245, 255, 255))
    for ang in (30, 150, 270):
        a = math.radians(ang)
        d.ellipse([c+30+56*math.cos(a)-8, 68+56*math.sin(a)-8, c+30+56*math.cos(a)+8, 68+56*math.sin(a)+8], fill=(255, 255, 255, 220))

def sym_flame(d):
    c = S/2
    outer = [(c, 34), (c+40, 90), (c+62, 140), (c+50, 200), (c, 226), (c-50, 200), (c-62, 140), (c-32, 100), (c-20, 120), (c-10, 80)]
    poly(d, outer, fill=(255, 140, 30, 255))
    inner = [(c+4, 110), (c+28, 150), (c+22, 196), (c, 208), (c-22, 196), (c-26, 156), (c-6, 140)]
    poly(d, inner, fill=(255, 230, 90, 255), outline=None, width=0)

def sym_snowflake(d):
    c = S/2
    for ang in range(0, 180, 60):
        a = math.radians(ang)
        dx, dy = 84*math.cos(a), 84*math.sin(a)
        thick_line(d, (c-dx, c-dy), (c+dx, c+dy), 12)
        for sgn in (1, -1):
            for t in (0.55, 0.85):
                px, py = c + sgn*dx*t, c + sgn*dy*t
                for br in (35, -35):
                    b = a + math.radians(br)
                    thick_line(d, (px, py), (px + sgn*26*math.cos(b), py + sgn*26*math.sin(b)), 8)
    d.ellipse([c-14, c-14, c+14, c+14], fill=(200, 235, 255, 255), outline=K, width=5)

def sym_bolt(d):
    c = S/2
    pts = [(c+22, 30), (c-42, 132), (c+2, 132), (c-26, 226), (c+56, 108), (c+10, 108), (c+50, 30)]
    poly(d, pts, fill=(255, 245, 120, 255))

def sym_skull(d):
    c = S/2
    d.ellipse([c-66, 46, c+66, 170], fill=W, outline=K, width=7)
    poly(d, [(c-44, 150), (c+44, 150), (c+40, 210), (c-40, 210)], fill=W)
    d.ellipse([c-48, 92, c-10, 130], fill=K)
    d.ellipse([c+10, 92, c+48, 130], fill=K)
    poly(d, [(c, 128), (c+12, 150), (c-12, 150)], fill=K, outline=None, width=0)
    for x in (-26, -9, 8, 25):
        d.rectangle([c+x, 176, c+x+9, 204], fill=K)

def sym_eye(d):
    c = S/2
    poly(d, [(c-96, c), (c-40, c-58), (c+40, c-58), (c+96, c), (c+40, c+58), (c-40, c+58)], fill=(240, 220, 255, 255))
    d.ellipse([c-40, c-40, c+40, c+40], fill=(120, 40, 170, 255), outline=K, width=6)
    d.ellipse([c-14, c-30, c+14, c+30], fill=K)
    d.ellipse([c+10, c-26, c+24, c-12], fill=(255, 255, 255, 230))

def sym_leaf(d):
    c = S/2
    pts = rot([(c, 36), (c+66, 90), (c+56, 170), (c, 222), (c-56, 170), (c-66, 90)], c, c, 25)
    poly(d, pts, fill=(120, 220, 110, 255))
    a, b = rot([(c, 40), (c, 218)], c, c, 25)
    thick_line(d, a, b, 6, fill=(40, 110, 50, 255), outline=(40, 110, 50, 255))
    for t in (0.3, 0.5, 0.7):
        p = (a[0] + (b[0]-a[0])*t, a[1] + (b[1]-a[1])*t)
        for s in (1, -1):
            q = rot([(p[0] + s*40, p[1] - 20)], p[0], p[1], 25)[0]
            d.line([p, q], fill=(40, 110, 50, 255), width=5)

def sym_cross(d):
    c = S/2
    d.ellipse([c-80, c-80, c+80, c+80], fill=(255, 250, 200, 90))
    poly(d, [(c-18, 40), (c+18, 40), (c+18, 216), (c-18, 216)], fill=(255, 225, 120, 255))
    poly(d, [(c-64, 92), (c+64, 92), (c+64, 128), (c-64, 128)], fill=(255, 225, 120, 255))
    d.rectangle([c-8, 100, c+8, 205], fill=(255, 250, 220, 255))

def sym_totem(d):
    c = S/2
    poly(d, [(c-58, 50), (c+58, 50), (c+66, 190), (c, 222), (c-66, 190)], fill=(200, 130, 70, 255))
    poly(d, [(c-42, 88), (c-8, 88), (c-8, 112), (c-42, 112)], fill=(60, 240, 200, 255))
    poly(d, [(c+8, 88), (c+42, 88), (c+42, 112), (c+8, 112)], fill=(60, 240, 200, 255))
    poly(d, [(c-30, 150), (c+30, 150), (c+30, 172), (c-30, 172)], fill=K, outline=None, width=0)
    for x in (-20, 0, 20):
        d.rectangle([c+x-4, 150, c+x+4, 172], fill=W)
    for x in (-50, 0, 50):
        poly(d, [(c+x-12, 50), (c+x+12, 50), (c+x, 22)], fill=(60, 240, 200, 255))

def sym_note(d):
    c = S/2
    d.ellipse([c-70, 150, c-14, 200], fill=W, outline=K, width=7)
    d.ellipse([c+14, 130, c+70, 180], fill=W, outline=K, width=7)
    thick_line(d, (c-20, 175), (c-20, 50), 12)
    thick_line(d, (c+64, 155), (c+64, 30), 12)
    poly(d, [(c-26, 44), (c+70, 24), (c+70, 58), (c-26, 78)], fill=W)

def sym_fist(d):
    c = S/2
    poly(d, [(c-60, 100), (c+50, 100), (c+50, 200), (c-60, 200)], fill=(245, 205, 165, 255))
    for i, y in enumerate((100, 128, 156)):
        d.rounded_rectangle([c-66, y, c+56, y+30], radius=14, fill=(245, 205, 165, 255), outline=K, width=6)
    d.rounded_rectangle([c+30, 96, c+80, 190], radius=18, fill=(245, 205, 165, 255), outline=K, width=6)
    # impact lines
    for (a, b) in (((c-88, 70), (c-70, 92)), ((c-40, 50), (c-38, 82)), ((c+10, 50), (c+4, 82))):
        thick_line(d, a, b, 6, fill=(255, 245, 160, 255))

def sym_flask(d):
    c = S/2
    body = [(c-22, 44), (c+22, 44), (c+22, 100), (c+70, 196), (c+60, 218), (c-60, 218), (c-70, 196), (c-22, 100)]
    poly(d, body, fill=(225, 240, 255, 200))
    liquid = [(c-38, 136), (c+38, 136), (c+64, 196), (c+54, 210), (c-54, 210), (c-64, 196)]
    poly(d, liquid, fill=(90, 240, 120, 255), outline=None, width=0)
    d.rectangle([c-30, 34, c+30, 52], fill=(140, 100, 60, 255), outline=K, width=5)
    for (x, y, r) in ((c-10, 160, 8), (c+16, 180, 6), (c-26, 190, 5)):
        d.ellipse([x-r, y-r, x+r, y+r], fill=(200, 255, 210, 255))

def sym_gear(d):
    c = S/2
    pts = []
    for i in range(16):
        a = math.radians(i * 22.5)
        r = 92 if i % 2 == 0 else 68
        a2 = a + math.radians(7)
        pts.append((c + r*math.cos(a), c + r*math.sin(a)))
        pts.append((c + r*math.cos(a2), c + r*math.sin(a2)))
    poly(d, pts, fill=W2)
    d.ellipse([c-40, c-40, c+40, c+40], fill=(70, 80, 90, 255), outline=K, width=7)
    d.ellipse([c-16, c-16, c+16, c+16], fill=(255, 120, 60, 255), outline=K, width=5)

SYMBOLS = {k: v for k, v in globals().items() if k.startswith("sym_")}

# --------------------------------------------------------------------------- build
def make_icon(color, symbol):
    img = background(color)
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    SYMBOLS["sym_" + symbol](d)
    # drop shadow
    shadow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    shadow.paste((0, 0, 0, 150), (0, 0), layer.split()[3])
    shadow = shadow.filter(ImageFilter.GaussianBlur(5))
    img.alpha_composite(shadow, (5, 7))
    img.alpha_composite(layer)
    return img.resize((FINAL, FINAL), Image.LANCZOS)

def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w" if isinstance(data, str) else "wb") as f:
        f.write(data)

def main():
    if os.path.exists(OUT): shutil.rmtree(OUT)
    os.makedirs(OUT)

    write(os.path.join(OUT, "pack.mcmeta"), json.dumps({
        "pack": {
            "pack_format": 75,
            "min_format": [75, 0],
            "max_format": [200, 0],
            "supported_formats": [46, 200],
            "description": "§6§lRPGClasses §7- class icons & HUD art"
        }
    }, indent=2))

    tex_dir  = os.path.join(OUT, "assets", NS, "textures", "item")
    mdl_dir  = os.path.join(OUT, "assets", NS, "models", "item")
    item_dir = os.path.join(OUT, "assets", NS, "items")

    preview = Image.new("RGBA", (FINAL*5 + 24, FINAL*4 + 20), (30, 30, 40, 255))
    for i, (cid, (hexcol, symbol)) in enumerate(CLASSES.items()):
        icon = make_icon(rgb(hexcol), symbol)
        os.makedirs(tex_dir, exist_ok=True)
        icon.save(os.path.join(tex_dir, f"{cid}.png"))
        preview.alpha_composite(icon, (4 + (i % 5) * (FINAL + 4), 4 + (i // 5) * (FINAL + 4)))
        write(os.path.join(mdl_dir, f"{cid}.json"), json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{NS}:item/{cid}"}
        }, indent=2))
        # item model definition (1.21.4+): referenced by the `minecraft:item_model` component
        write(os.path.join(item_dir, f"{cid}.json"), json.dumps({
            "model": {"type": "minecraft:model", "model": f"{NS}:item/{cid}"}
        }, indent=2))

    # pack icon
    pack_icon = make_icon(rgb(0x8E44AD), "sword").resize((128, 128), Image.LANCZOS)
    pack_icon.save(os.path.join(OUT, "pack.png"))
    preview.save(os.path.join(ROOT, "icons_preview.png"))

    # zip
    zip_path = os.path.join(ROOT, "..", "src", "main", "resources", "pack.zip")
    os.makedirs(os.path.dirname(zip_path), exist_ok=True)
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as z:
        for base, _, files in os.walk(OUT):
            for fn in files:
                full = os.path.join(base, fn)
                z.write(full, os.path.relpath(full, OUT))
    print("Wrote", os.path.abspath(zip_path))

if __name__ == "__main__":
    main()
