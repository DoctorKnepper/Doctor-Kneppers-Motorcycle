#!/usr/bin/env python3
"""
Generates the motorcycle's entity model (Java) and all textures (PNG) from one spec,
so the model's UV layout and the painted texture always match.

Run from the repo root:  python3 tools/generate_assets.py
Requires Pillow.
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/dkmotorcycles")
MODEL_JAVA = os.path.join(
    ROOT, "src/client/java/com/doctorknepper/motorcycles/client/render/MotorcycleModelLayer.java"
)

rng = random.Random(1907)

# ---------------------------------------------------------------------------
# Geometry spec. Units are pixels (16 = 1 block). "World" orientation:
# x = bike's left(+)/right(-), y = up from the ground, z = forward(+)/back(-).
# Group decides how the part is tinted at render time:
#   frame  - drawn as-is
#   paint  - tinted with the dye colour
#   engine - tinted with the engine tier colour
#   light  - drawn full-bright while the headlight is on
# ---------------------------------------------------------------------------
PARTS = [
    # group,   name,              x0, x1, y0, y1, z0, z1,  style
    ("frame", "swingarm_left",    1.5, 2.5, 4, 6, -11, -3, "metal"),
    ("frame", "swingarm_right",  -2.5, -1.5, 4, 6, -11, -3, "metal"),
    ("frame", "spine",            -1, 1, 11, 13, -6, 9, "metal"),
    ("frame", "fork_left",        1.5, 2.5, 5, 17, 10, 12, "chrome"),
    ("frame", "fork_right",      -2.5, -1.5, 5, 17, 10, 12, "chrome"),
    ("frame", "handlebar",        -5, 5, 17, 18, 9, 11, "black"),
    ("frame", "grip_left",         5, 7, 16, 18, 9, 11, "rubber"),
    ("frame", "grip_right",       -7, -5, 16, 18, 9, 11, "rubber"),
    ("frame", "headlight_housing", -2, 2, 13, 16, 12, 14, "chrome"),
    ("frame", "exhaust",          -5, -3, 3, 5, -13, 1, "chrome"),
    ("frame", "seat",             -3, 3, 13, 15, -10, -1, "seat"),
    ("frame", "footpeg_left",      2, 5, 5, 6, -1, 1, "black"),
    ("frame", "footpeg_right",    -5, -2, 5, 6, -1, 1, "black"),
    # storage (shown only when installed)
    ("frame", "saddlebag_1",       3, 7, 6, 12, -15, -7, "leather"),
    ("frame", "saddlebag_2",      -7, -3, 6, 12, -15, -7, "leather"),
    ("frame", "saddlebag_3",      -2, 2, 16, 18, 2, 7, "leather"),
    ("frame", "trunk",            -4, 4, 11, 17, -20, -12, "trunk"),
    # paint
    ("paint", "tank",             -3, 3, 12, 16, 0, 9, "paint"),
    ("paint", "rear_fender",      -2, 2, 10, 11, -16, -6, "paint"),
    ("paint", "front_fender",     -2, 2, 10, 11, 7, 15, "paint"),
    ("paint", "side_panel",       -3, 3, 8, 13, -10, -4, "paint"),
    # engine
    ("engine", "engine_block",    -3, 3, 4, 11, -4, 5, "engine"),
    ("engine", "engine_fin_low",  -4, 4, 6, 7, -3, 4, "engine_fin"),
    ("engine", "engine_fin_high", -4, 4, 8, 9, -3, 4, "engine_fin"),
    # lights
    ("light", "headlight_lens",   -1, 1, 14, 16, 14, 15, "lens"),
    ("light", "tail_light",       -1, 1, 11, 12, -17, -16, "tail"),
]

WHEELS = [
    # name,          parent,  centre x, centre y, centre z, radius, width
    ("front_wheel", "frame", 0, 5, 11, 5, 3),
    ("rear_wheel", "frame", 0, 5, -11, 5, 3),
]

# Sidecars, described for the LEFT side (x+); the right one is mirrored automatically.
# Each side gets its own parent parts ("sidecar_left" in frame, "sidecar_left_paint" in paint)
# so the model can show or hide a whole sidecar at once.
SIDECAR_PARTS = [
    # group,  name,       x0, x1, y0, y1, z0, z1,  style
    ("paint", "tub",       8, 19, 3, 10, -10, 9, "paint"),
    ("paint", "nose",      9, 18, 4, 9, 9, 12, "paint"),
    ("paint", "cowl",      8, 19, 10, 13, 3, 9, "paint"),
    ("frame", "cushion",   9, 18, 10, 11, -7, 3, "seat"),
    ("frame", "backrest",  8, 19, 10, 16, -10, -7, "seat"),
    ("frame", "strut_front", 2.5, 8, 5, 6, 5, 6, "metal"),
    ("frame", "strut_rear",  2.5, 8, 5, 6, -8, -7, "metal"),
    ("frame", "axle",      19, 20, 3.5, 4.5, -2.5, -1.5, "metal"),
]
SIDECAR_WHEEL = (20.5, 4, -2, 4, 2)  # centre x, centre y, centre z, radius, width

# ---------------------------------------------------------------------------
# Texture packing
# ---------------------------------------------------------------------------
TEX_W, TEX_H = 128, 256


def footprint(w, h, d):
    return 2 * d + 2 * w, d + h


class Packer:
    def __init__(self, width):
        self.width = width
        self.x = 0
        self.y = 0
        self.row_h = 0

    def place(self, fw, fh):
        if self.x + fw > self.width:
            self.x = 0
            self.y += self.row_h
            self.row_h = 0
        pos = (self.x, self.y)
        self.x += fw
        self.row_h = max(self.row_h, fh)
        return pos


def uv_size(v):
    return int(math.ceil(v - 1e-6))


def clamp(c):
    return max(0, min(255, int(c)))


def shade(rgb, f):
    return tuple(clamp(c * f) for c in rgb)


def jitter(rgb, amount=6):
    j = rng.randint(-amount, amount)
    return tuple(clamp(c + j) for c in rgb)


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


STYLE_BASE = {
    "metal": hexrgb("#4a4c53"),
    "chrome": hexrgb("#b8bcc6"),
    "black": hexrgb("#26262a"),
    "rubber": hexrgb("#1a1a1a"),
    "seat": hexrgb("#2e2622"),
    "leather": hexrgb("#7a4a26"),
    "trunk": hexrgb("#9c6a2c"),
    "paint": hexrgb("#e2e2e2"),
    "engine": hexrgb("#d0d0d0"),
    "engine_fin": hexrgb("#e6e6e6"),
    "lens": hexrgb("#fff6c8"),
    "tail": hexrgb("#d42a20"),
}

# face tone multipliers. Model-space DOWN is world-up because the renderer flips y.
FACE_TONE = {"world_top": 1.12, "world_bottom": 0.62, "side_x": 0.86, "side_z": 0.96}


def face_rects(u, v, w, h, d):
    """Cube UV layout used by ModelPart.Cube."""
    return {
        "world_top": (u + d, v, w, d),
        "world_bottom": (u + d + w, v, w, d),
        "x_a": (u, v + d, d, h),
        "z_a": (u + d, v + d, w, h),
        "x_b": (u + d + w, v + d, d, h),
        "z_b": (u + 2 * d + w, v + d, w, h),
    }


def paint_face(img, rect, style, face_kind, tone):
    fx, fy, fw, fh = rect
    base = STYLE_BASE[style]
    for py in range(fh):
        for px in range(fw):
            c = shade(base, tone)
            # style details
            if style == "chrome":
                if py == 0 or px == 0:
                    c = shade(c, 1.18)
                elif (px + py) % 5 == 0:
                    c = shade(c, 1.1)
                c = jitter(c, 3)
            elif style == "seat":
                if face_kind == "world_top" and (px == 0 or px == fw - 1):
                    c = shade(c, 1.35)  # stitching
                c = jitter(c, 4)
            elif style == "leather":
                if face_kind in ("x_a", "x_b") and px == fw // 2:
                    c = hexrgb("#c9a227")  # buckle strap
                elif py == 0:
                    c = shade(c, 1.2)
                c = jitter(c, 5)
            elif style == "trunk":
                if py == 0 or py == fh - 1 or px == 0 or px == fw - 1:
                    c = shade(base, tone * 0.62)  # dark banding
                elif face_kind in ("z_a", "z_b") and px in (fw // 2 - 1, fw // 2) and py in (1, 2):
                    c = hexrgb("#c9c9c9")  # latch
                elif py % 3 == 0:
                    c = shade(c, 0.85)  # plank lines
                c = jitter(c, 5)
            elif style == "paint":
                if py == 0 and face_kind not in ("world_top", "world_bottom"):
                    c = shade(c, 1.08)
                c = jitter(c, 3)
            elif style in ("engine", "engine_fin"):
                if style == "engine" and face_kind not in ("world_top", "world_bottom") and py % 2 == 1:
                    c = shade(c, 0.82)
                c = jitter(c, 4)
            elif style == "lens":
                c = jitter(c, 2)
            elif style == "metal":
                c = jitter(c, 4)
            else:
                c = jitter(c, 3)
            img.putpixel((fx + px, fy + py), c + (255,))


def paint_box(img, u, v, w, h, d, style):
    rects = face_rects(u, v, w, h, d)
    for kind, rect in rects.items():
        tone_key = kind if kind in FACE_TONE else ("side_x" if kind.startswith("x") else "side_z")
        paint_face(img, rect, style, kind, FACE_TONE[tone_key])


def paint_wheel(img, u, v, w, h, d):
    rects = face_rects(u, v, w, h, d)
    tire = hexrgb("#1c1c1e")
    for kind, (fx, fy, fw, fh) in rects.items():
        if kind in ("x_a", "x_b"):
            cx, cy = (fw - 1) / 2.0, (fh - 1) / 2.0
            for py in range(fh):
                for px in range(fw):
                    r = max(abs(px - cx), abs(py - cy))
                    if r > cx - 1.6:
                        c = jitter(tire, 3)
                    elif r > cx - 2.6:
                        c = jitter(hexrgb("#8f939b"), 3)  # rim
                    elif r < 1.2:
                        c = hexrgb("#d8d8dc")  # hub
                    elif abs(px - cx) < 0.6 or abs(py - cy) < 0.6:
                        c = hexrgb("#b3b6bd")  # spokes
                    else:
                        c = jitter(hexrgb("#2a2a2e"), 2)
                    img.putpixel((fx + px, fy + py), c + (255,))
        else:
            for py in range(fh):
                for px in range(fw):
                    c = shade(tire, 1.25 if (py // 2) % 2 == 0 else 0.9)  # tread
                    img.putpixel((fx + px, fy + py), jitter(c, 2) + (255,))


def to_model_box(x0, x1, y0, y1, z0, z1):
    return x0, -y1, -z1, x1 - x0, y1 - y0, z1 - z0


def fmt(f):
    s = ("%.4f" % f).rstrip("0").rstrip(".")
    if s in ("-0", ""):
        s = "0"
    return s + "F"


def generate():
    img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    packer = Packer(TEX_W)
    groups = {"frame": [], "paint": [], "engine": [], "light": []}

    def add_part(parent, group, name, x0, x1, y0, y1, z0, z1, style):
        mx, my, mz, w, h, d = to_model_box(x0, x1, y0, y1, z0, z1)
        uw, uh, ud = uv_size(w), uv_size(h), uv_size(d)
        u, v = packer.place(*footprint(uw, uh, ud))
        paint_box(img, u, v, uw, uh, ud, style)
        groups[group].append(
            f'\t\t{parent}.addOrReplaceChild("{name}", CubeListBuilder.create().texOffs({u}, {v})'
            f".addBox({fmt(mx)}, {fmt(my)}, {fmt(mz)}, {fmt(w)}, {fmt(h)}, {fmt(d)}), PartPose.ZERO);"
        )

    for group, name, x0, x1, y0, y1, z0, z1, style in PARTS:
        add_part(group, group, name, x0, x1, y0, y1, z0, z1, style)

    wheel_lines = []

    def add_wheel(parent, name, cx, cy, cz, r, width):
        u1, v1 = packer.place(*footprint(width, 2 * r, 2 * r))
        paint_wheel(img, u1, v1, width, 2 * r, 2 * r)
        u2, v2 = packer.place(*footprint(width, 2 * r, 2 * r))
        paint_wheel(img, u2, v2, width, 2 * r, 2 * r)
        box = f"{fmt(-width / 2)}, {fmt(-r)}, {fmt(-r)}, {fmt(width)}, {fmt(2 * r)}, {fmt(2 * r)}"
        wheel_lines.append(
            f'\t\tPartDefinition {name} = {parent}.addOrReplaceChild("{name}", CubeListBuilder.create().texOffs({u1}, {v1})'
            f".addBox({box}), PartPose.offset({fmt(cx)}, {fmt(-cy)}, {fmt(-cz)}));"
        )
        wheel_lines.append(
            f'\t\t{name}.addOrReplaceChild("{name}_diagonal", CubeListBuilder.create().texOffs({u2}, {v2})'
            f".addBox({box}, new CubeDeformation(-0.12F)), PartPose.rotation((float) (Math.PI / 4), 0.0F, 0.0F));"
        )

    for name, parent, cx, cy, cz, r, width in WHEELS:
        add_wheel(parent, name, cx, cy, cz, r, width)

    sidecar_parents = []
    for side, sign in (("left", 1), ("right", -1)):
        frame_parent = f"sidecar_{side}"
        paint_parent = f"sidecar_{side}_paint"
        sidecar_parents.append(
            f'\t\tPartDefinition {frame_parent} = frame.addOrReplaceChild("{frame_parent}", CubeListBuilder.create(), PartPose.ZERO);'
        )
        sidecar_parents.append(
            f'\t\tPartDefinition {paint_parent} = paint.addOrReplaceChild("{paint_parent}", CubeListBuilder.create(), PartPose.ZERO);'
        )
        for group, name, x0, x1, y0, y1, z0, z1, style in SIDECAR_PARTS:
            if sign < 0:
                x0, x1 = -x1, -x0
            parent = frame_parent if group == "frame" else paint_parent
            add_part(parent, group, f"sidecar_{side}_{name}", x0, x1, y0, y1, z0, z1, style)
        cx, cy, cz, r, width = SIDECAR_WHEEL
        add_wheel(frame_parent, f"sidecar_{side}_wheel", cx * sign, cy, cz, r, width)

    used_h = packer.y + packer.row_h
    assert used_h <= TEX_H, f"texture overflow: {used_h} > {TEX_H}"

    os.makedirs(os.path.join(ASSETS, "textures/entity"), exist_ok=True)
    img.save(os.path.join(ASSETS, "textures/entity/motorcycle.png"))

    java = f"""package com.doctorknepper.motorcycles.client.render;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * GENERATED by tools/generate_assets.py - edit the spec there, not this file.
 * Texture: assets/dkmotorcycles/textures/entity/motorcycle.png ({TEX_W}x{TEX_H}).
 */
public final class MotorcycleModelLayer {{
\tprivate MotorcycleModelLayer() {{
\t}}

\tpublic static LayerDefinition create() {{
\t\tMeshDefinition mesh = new MeshDefinition();
\t\tPartDefinition root = mesh.getRoot();
\t\tPartDefinition frame = root.addOrReplaceChild("frame", CubeListBuilder.create(), PartPose.ZERO);
\t\tPartDefinition paint = root.addOrReplaceChild("paint", CubeListBuilder.create(), PartPose.ZERO);
\t\tPartDefinition engine = root.addOrReplaceChild("engine", CubeListBuilder.create(), PartPose.ZERO);
\t\tPartDefinition light = root.addOrReplaceChild("light", CubeListBuilder.create(), PartPose.ZERO);
{chr(10).join(sidecar_parents)}
{chr(10).join(groups["frame"])}
{chr(10).join(wheel_lines)}
{chr(10).join(groups["paint"])}
{chr(10).join(groups["engine"])}
{chr(10).join(groups["light"])}
\t\treturn LayerDefinition.create(mesh, {TEX_W}, {TEX_H});
\t}}
}}
"""
    os.makedirs(os.path.dirname(MODEL_JAVA), exist_ok=True)
    with open(MODEL_JAVA, "w") as f:
        f.write(java)
    print(f"model texture rows used: {used_h}/{TEX_H}")


# ---------------------------------------------------------------------------
# 16x16 item icons, drawn from small pixel maps
# ---------------------------------------------------------------------------

def icon_from_map(rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != "." and ch in palette:
                img.putpixel((x, y), hexrgb(palette[ch]) + (255,))
    return img


MOTORCYCLE_ICON = [
    "................",
    "................",
    "..........KK....",
    "...........F....",
    "....SSS....F....",
    "...SSSSPPPPFL...",
    "..PPPPPPPPPFL...",
    "..MM.EEEEM..F...",
    "..M..EEEE...F...",
    ".TTT.XXXXX.TTT..",
    "TRRRT.....TRRRT.",
    "TRHRT.....TRHRT.",
    "TRRRT.....TRRRT.",
    ".TTT.......TTT..",
    "................",
    "................",
]
MOTORCYCLE_PAL = {
    "K": "#26262a", "H": "#d8d8dc", "F": "#b8bcc6", "L": "#fff2a0", "S": "#2e2622",
    "T": "#1c1c1e", "P": "#c8302a", "M": "#4a4c53", "E": "#d08a5a",
    "R": "#8f939b", "X": "#c4c8cf",
}

ENGINE_ICON = [
    "................",
    "................",
    "...AAAAAAAAAA...",
    "..ABBBBBBBBBBA..",
    "...AAAAAAAAAA...",
    "..ABBBBBBBBBBA..",
    "...AAAAAAAAAA...",
    "..ABBBBBBBBBBA..",
    "..ACCCCCCCCCCA..",
    "..ACCDDCCDDCCA..",
    "..ACCDDCCDDCCA..",
    "..ACCCCCCCCCCA..",
    "..AAAAAAAAAAAA..",
    "...GG......GG...",
    "................",
    "................",
]

ENGINE_TIERS = {
    "copper_engine": ("#8a4a2c", "#e3874f", "#c46a3a", "#5b2f1c"),
    "iron_engine": ("#5e6066", "#e6e6e6", "#c2c4c9", "#3c3e42"),
    "diamond_engine": ("#1b6d72", "#7af3e6", "#3ccdc0", "#0f4447"),
    "netherite_engine": ("#2a2527", "#6a6164", "#4a4346", "#151213"),
}

FUEL_TANK_ICON = [
    "................",
    ".....KK.........",
    "....K..KAAAAA...",
    ".....KKABBBBBA..",
    "......ABBBBBBBA.",
    "......ABCCCCCBA.",
    "......ABCBBBCBA.",
    "......ABCBBBCBA.",
    "......ABCBBBCBA.",
    "......ABCCCCCBA.",
    "......ABBBBBBBA.",
    "......ABBBBBBBA.",
    "......ABBBBBBBA.",
    ".......AAAAAAA..",
    "................",
    "................",
]
FUEL_TANK_PAL = {"A": "#6b1410", "B": "#d4302a", "C": "#f05a4f", "K": "#3a3a3e"}

SADDLEBAG_ICON = [
    "................",
    "................",
    "....AAAAAAAA....",
    "...ABBBBBBBBA...",
    "...ABBBBBBBBA...",
    "...ACCCCCCCCA...",
    "...ABBBDDBBBA...",
    "...ABBBDDBBBA...",
    "...ABBBBBBBBA...",
    "...ABBBBBBBBA...",
    "...ABBBBBBBBA...",
    "...ABBBBBBBBA...",
    "....AAAAAAAA....",
    "................",
    "................",
    "................",
]
SADDLEBAG_PAL = {"A": "#4a2a12", "B": "#8a5630", "C": "#6b3f1f", "D": "#d4b13a"}

TRUNK_ICON = [
    "................",
    "................",
    "..AAAAAAAAAAAA..",
    "..ABBBBBBBBBBA..",
    "..ACCCCCCCCCCA..",
    "..ABBBBBBBBBBA..",
    "..AAAAADDAAAAA..",
    "..ABBBBDDBBBBA..",
    "..ACCCCCCCCCCA..",
    "..ABBBBBBBBBBA..",
    "..ACCCCCCCCCCA..",
    "..ABBBBBBBBBBA..",
    "..AAAAAAAAAAAA..",
    "...EE......EE...",
    "................",
    "................",
]
TRUNK_PAL = {"A": "#3a2510", "B": "#a8742f", "C": "#8a5c22", "D": "#cfcfd4", "E": "#2a2a2e"}


SIDECAR_ICON = [
    "................",
    "................",
    "................",
    "...SSSS.........",
    "...SSSSAAAA.....",
    "..APPPPPPPPPA...",
    ".APPPPPPPPPPPA..",
    ".APPPPPPPPPPPPA.",
    ".APPPPPPPPPPPA..",
    "..AAAAAAAAAAA...",
    ".....MMMMM......",
    "....TTT.........",
    "...TRHRT........",
    "....TTT.........",
    "................",
    "................",
]
SIDECAR_PAL = {"S": "#2e2622", "A": "#7a1c18", "P": "#c8302a", "M": "#4a4c53", "T": "#1c1c1e", "R": "#8f939b", "H": "#d8d8dc"}


def generate_items():
    out = os.path.join(ASSETS, "textures/item")
    os.makedirs(out, exist_ok=True)
    icon_from_map(MOTORCYCLE_ICON, MOTORCYCLE_PAL).save(os.path.join(out, "motorcycle.png"))
    for name, (a, b, c, d) in ENGINE_TIERS.items():
        pal = {"A": a, "B": b, "C": c, "D": d, "G": "#2a2a2e"}
        icon_from_map(ENGINE_ICON, pal).save(os.path.join(out, f"{name}.png"))
    icon_from_map(FUEL_TANK_ICON, FUEL_TANK_PAL).save(os.path.join(out, "fuel_tank.png"))
    icon_from_map(SADDLEBAG_ICON, SADDLEBAG_PAL).save(os.path.join(out, "saddlebag.png"))
    icon_from_map(TRUNK_ICON, TRUNK_PAL).save(os.path.join(out, "trunk.png"))
    icon_from_map(SIDECAR_ICON, SIDECAR_PAL).save(os.path.join(out, "sidecar.png"))


def generate_mod_icon():
    big = icon_from_map(MOTORCYCLE_ICON, MOTORCYCLE_PAL).resize((128, 128), Image.NEAREST)
    bg = Image.new("RGBA", (128, 128), hexrgb("#2b3a4a") + (255,))
    bg.alpha_composite(big)
    bg.save(os.path.join(ASSETS, "icon.png"))


if __name__ == "__main__":
    generate()
    generate_items()
    generate_mod_icon()
    print("done")
