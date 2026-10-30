"""Generates the UK fittings' models, textures and icons into a port's resources.

usage: python3 port-1.20.1/tools/gen_uk_fittings.py <port resources dir>, once for each port:
       port-1.20.1/src/main/resources and port-1.21.1/src/main/resources

Eln six-node models: X points out of the wall (0 = wall surface), Y is up and -Z is to the right of a player facing
the fitting. Parts are triangles; the loader computes normals from the winding, which quad()/tri() fix up.
"""
import math
import os
import sys
from PIL import Image, ImageDraw

out_root = sys.argv[1]


class Obj:
    def __init__(self, name, tex):
        self.name = name
        self.tex = tex
        self.lines = [f"mtllib {name}.mtl"]
        self.nv = 0
        self.mtl = "Body"

    def part(self, name, mtl="Body"):
        self.mtl = mtl
        self.lines += [f"o {name}", f"usemtl {mtl}"]

    def _emit(self, pts, uvs, normal):
        a, b, c = pts[0], pts[1], pts[2]
        e1 = [b[i] - a[i] for i in range(3)]
        e2 = [c[i] - a[i] for i in range(3)]
        cross = [e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]]
        if sum(cross[i] * normal[i] for i in range(3)) < 0:
            pts, uvs = pts[::-1], uvs[::-1]
        for p in pts:
            self.lines.append("v %.5f %.5f %.5f" % tuple(p))
        for u, v in uvs:
            # Pixel coordinates (v down) to OBJ texture coordinates (v up); the loader flips v back.
            self.lines.append("vt %.5f %.5f" % (u / self.tex, 1 - v / self.tex))
        n = self.nv
        if len(pts) == 3:
            self.lines.append(f"f {n+1}/{n+1} {n+2}/{n+2} {n+3}/{n+3}")
        else:
            self.lines.append(f"f {n+1}/{n+1} {n+2}/{n+2} {n+3}/{n+3}")
            self.lines.append(f"f {n+1}/{n+1} {n+3}/{n+3} {n+4}/{n+4}")
        self.nv += len(pts)

    def quad(self, pts, normal, uvs):
        self._emit(pts, uvs, normal)

    def tri(self, pts, normal, uvs):
        self._emit(pts, uvs, normal)

    def face_quad(self, x, y0, y1, z0, z1, rect):
        """A quad facing +X; rect (u0, v0, u1, v1) maps its left edge (+Z) to u0 and its top (+Y) to v0."""
        u0, v0, u1, v1 = rect
        self.quad([(x, y0, z1), (x, y0, z0), (x, y1, z0), (x, y1, z1)], (1, 0, 0),
                  [(u0, v1), (u1, v1), (u1, v0), (u0, v0)])

    def frustum(self, x0, x1, back, front, centre=(0.0, 0.0), front_rect=None, side_rect=None, back_face=False):
        """A box from x0 to x1 whose cross-section shrinks from back=(hy, hz) to front=(hy, hz): bevelled edges."""
        cy, cz = centre
        (by, bz), (fy, fz) = back, front
        if front_rect:
            self.face_quad(x1, cy - fy, cy + fy, cz - fz, cz + fz, front_rect)
        su0, sv0, su1, sv1 = side_rect
        side_uv = [(su0, sv1), (su1, sv1), (su1, sv0), (su0, sv0)]
        corners_b = [(cy - by, cz - bz), (cy - by, cz + bz), (cy + by, cz + bz), (cy + by, cz - bz)]
        corners_f = [(cy - fy, cz - fz), (cy - fy, cz + fz), (cy + fy, cz + fz), (cy + fy, cz - fz)]
        for i in range(4):
            j = (i + 1) % 4
            pb0, pb1 = corners_b[i], corners_b[j]
            pf0, pf1 = corners_f[i], corners_f[j]
            mid = ((pb0[0] + pb1[0]) / 2 - cy, (pb0[1] + pb1[1]) / 2 - cz)
            normal = (0.0, mid[0], mid[1])
            self.quad([(x0, *pb0), (x0, *pb1), (x1, *pf1), (x1, *pf0)], normal, side_uv)
        if back_face:
            self.quad([(x0, cy - by, cz - bz), (x0, cy + by, cz - bz), (x0, cy + by, cz + bz), (x0, cy - by, cz + bz)],
                      (-1, 0, 0), side_uv)

    def prism(self, x0, x1, r0, r1, centre=(0.0, 0.0), sides=12, cap_rect=None, side_rect=None, axis_dir=1,
              back_cap=False, back_rect=None):
        """A round-ish body along X: radius r0 at x0 to r1 at x1, capped at x1 with cap_rect (a disc texture)."""
        cy, cz = centre
        su0, sv0, su1, sv1 = side_rect
        ring = [(math.cos(2 * math.pi * k / sides), math.sin(2 * math.pi * k / sides)) for k in range(sides)]
        for k in range(sides):
            a, b = ring[k], ring[(k + 1) % sides]
            u_a = su0 + (su1 - su0) * k / sides
            u_b = su0 + (su1 - su0) * (k + 1) / sides
            mid = ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2)
            self.quad([(x0, cy + r0 * a[0], cz + r0 * a[1]), (x0, cy + r0 * b[0], cz + r0 * b[1]),
                       (x1, cy + r1 * b[0], cz + r1 * b[1]), (x1, cy + r1 * a[0], cz + r1 * a[1])],
                      (0.0, mid[0], mid[1]), [(u_a, sv1), (u_b, sv1), (u_b, sv0), (u_a, sv0)])
        for x, r, rect, nx in ((x1, r1, cap_rect, axis_dir), (x0, r0, back_rect if back_cap else None, -axis_dir)):
            if not rect or r <= 0:
                continue
            cu, cv = (rect[0] + rect[2]) / 2, (rect[1] + rect[3]) / 2
            ru, rv = (rect[2] - rect[0]) / 2, (rect[3] - rect[1]) / 2
            for k in range(sides):
                a, b = ring[k], ring[(k + 1) % sides]
                # +Y is up (v down), +Z is left (u down)
                self.tri([(x, cy, cz), (x, cy + r * a[0], cz + r * a[1]), (x, cy + r * b[0], cz + r * b[1])],
                         (nx, 0, 0), [(cu, cv), (cu - ru * a[1], cv - rv * a[0]), (cu - ru * b[1], cv - rv * b[0])])

    def write(self, folder, materials, txt=None):
        os.makedirs(folder, exist_ok=True)
        with open(os.path.join(folder, self.name + ".obj"), "w") as f:
            f.write("\n".join(self.lines) + "\n")
        with open(os.path.join(folder, self.name + ".mtl"), "w") as f:
            for mtl, png in materials.items():
                f.write(f"newmtl {mtl}\nKd 0.8 0.8 0.8\nmap_Kd {png}\n")
        if txt:
            with open(os.path.join(folder, self.name + ".txt"), "w") as f:
                f.write(txt)


# ---------------------------------------------------------------- textures
WHITE = (244, 243, 238)
WHITE_HI = (252, 252, 249)
EDGE = (214, 213, 206)
SHADE = (228, 227, 220)
DEEP = (190, 189, 182)
SLOT = (22, 22, 24)
SLOT_IN = (60, 58, 56)
RED = (205, 28, 24)
RED_HI = (240, 70, 60)
BRASS = (196, 160, 80)
CORD = (236, 236, 232)


def gradient_rect(d, box, top, bottom):
    x0, y0, x1, y1 = box
    for y in range(y0, y1 + 1):
        t = (y - y0) / max(1, y1 - y0)
        d.line([x0, y, x1, y], fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))


def plate_face(d, box):
    """The flat front of a moulded plate: a soft light-to-shade gradient with a faint highlight rim."""
    gradient_rect(d, box, WHITE_HI, SHADE)
    x0, y0, x1, y1 = box
    d.rectangle(box, outline=WHITE_HI)
    d.line([x0, y1, x1, y1], fill=EDGE)
    d.line([x1, y0, x1, y1], fill=EDGE)


def screw_cap(d, box):
    """A white screw cap with a slotted screw showing through."""
    d.ellipse(box, fill=WHITE_HI, outline=EDGE)
    x0, y0, x1, y1 = box
    cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
    r = (x1 - x0) // 4
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(200, 200, 196))
    d.line([cx - r + 1, cy, cx + r - 1, cy], fill=(120, 120, 118))


def recess(d, box):
    """A recess in the plate: a darker floor with a shadowed top-left and a lit bottom-right lip."""
    x0, y0, x1, y1 = box
    d.rectangle(box, fill=DEEP)
    d.line([x0, y0, x1, y0], fill=(160, 160, 154))
    d.line([x0, y0, x0, y1], fill=(160, 160, 154))
    d.line([x0, y1, x1, y1], fill=WHITE_HI)
    d.line([x1, y0, x1, y1], fill=WHITE_HI)


def slot(d, box):
    """A socket slot: black with the shutter's dull face and a bevelled rim."""
    x0, y0, x1, y1 = box
    d.rectangle([x0 - 1, y0 - 1, x1 + 1, y1 + 1], fill=DEEP)
    d.rectangle(box, fill=SLOT)
    d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=SLOT_IN)
    d.line([x0, y0, x1, y0], fill=(8, 8, 8))


def rocker_face(d, box, red_top=False):
    """A slightly curved rocker: lit in the middle, shaded towards the ends, with the moulded centre ridge."""
    x0, y0, x1, y1 = box
    h = y1 - y0
    for y in range(y0, y1 + 1):
        t = abs((y - y0) / h - 0.5) * 2
        c = tuple(int(WHITE_HI[i] - (WHITE_HI[i] - DEEP[i]) * t * t * 0.8) for i in range(3))
        d.line([x0, y, x1, y], fill=c)
    my = (y0 + y1) // 2
    d.line([x0 + 2, my, x1 - 2, my], fill=EDGE)
    d.line([x0 + 2, my + 1, x1 - 2, my + 1], fill=WHITE_HI)
    d.rectangle(box, outline=EDGE)
    if red_top:
        d.rectangle([x0 + 2, y0 + 2, x1 - 2, y0 + max(3, h // 5)], fill=RED)


def new_tex(size):
    im = Image.new("RGB", (size, size), WHITE)
    return im, ImageDraw.Draw(im)


T = 128
# Shared texture regions (pixels in a 128 texture).
R_PLATE = (0, 0, 63, 63)
R_EDGE = (64, 0, 79, 15)
R_CAP = (80, 0, 95, 15)
R_ROCK = (0, 64, 23, 111)
R_ROCK_SIDE = (24, 64, 31, 71)


def plate_texture(extra=None):
    im, d = new_tex(T)
    plate_face(d, R_PLATE)
    gradient_rect(d, R_EDGE, SHADE, DEEP)
    screw_cap(d, R_CAP)
    rocker_face(d, R_ROCK)
    d.rectangle(R_ROCK_SIDE, fill=EDGE)
    if extra:
        extra(d)
    return im


def plate(o, half=0.172, bevel=0.012, thick=0.028, screws=((0.0, 0.132), (0.0, -0.132))):
    o.frustum(0.0, thick, (half, half), (half - bevel, half - bevel), front_rect=R_PLATE, side_rect=R_EDGE)
    for cy, cz in screws:
        o.prism(thick, thick + 0.005, 0.013, 0.011, centre=(cy, cz), sides=12, cap_rect=R_CAP, side_rect=R_EDGE)


def rocker(o, centre, hy, hz, x0=0.028, depth=0.022):
    o.frustum(x0, x0 + depth, (hy, hz), (hy - 0.004, hz - 0.003), centre=centre, front_rect=R_ROCK,
              side_rect=R_ROCK_SIDE)


def bezel(o, centre, hy, hz, x0=0.028, h=0.004, w=0.006):
    """A thin raised frame around a rocker."""
    cy, cz = centre
    o.frustum(x0, x0 + h, (w / 2, hz + w), (w / 2, hz + w), centre=(cy + hy + w / 2, cz), front_rect=R_EDGE, side_rect=R_EDGE)
    o.frustum(x0, x0 + h, (w / 2, hz + w), (w / 2, hz + w), centre=(cy - hy - w / 2, cz), front_rect=R_EDGE, side_rect=R_EDGE)
    o.frustum(x0, x0 + h, (hy, w / 2), (hy, w / 2), centre=(cy, cz + hz + w / 2), front_rect=R_EDGE, side_rect=R_EDGE)
    o.frustum(x0, x0 + h, (hy, w / 2), (hy, w / 2), centre=(cy, cz - hz - w / 2), front_rect=R_EDGE, side_rect=R_EDGE)


def rocker_txt(part, origin_x, alpha_off, alpha_on, speed=0.05, cy=0.0, cz=0.0):
    return (f"o {part}\nf originX {origin_x}\nf originY {cy}\nf originZ {cz}\n"
            f"f axisX 0.0\nf axisY 0.0\nf axisZ 1.0\nf alphaOff {alpha_off}\nf alphaOn {alpha_on}\nf speed {speed}\n")


models = os.path.join(out_root, "assets/eln/model")

# --- UK Light Switch: plate and one rocker ("down is on": the top tilts out when on).
o = Obj("uklightswitch", T)
o.part("main")
plate(o)
bezel(o, (0.0, 0.0), 0.074, 0.038)
o.part("rot")
rocker(o, (0.0, 0.0), 0.072, 0.036)
o.write(os.path.join(models, "uklightswitch"), {"Body": "uklightswitch.png"}, "s type rocker\n" + rocker_txt("rot", 0.028, 8.0, -8.0))
plate_texture().save(os.path.join(models, "uklightswitch/uklightswitch.png"))

# --- UK Double Light Switch: two rockers, left (+Z) and right (-Z).
o = Obj("ukdoubleswitch", T)
o.part("main")
plate(o)
for cz in (0.056, -0.056):
    bezel(o, (0.0, cz), 0.074, 0.034)
o.part("rotLeft")
rocker(o, (0.0, 0.056), 0.072, 0.032)
o.part("rotRight")
rocker(o, (0.0, -0.056), 0.072, 0.032)
o.write(os.path.join(models, "ukdoubleswitch"), {"Body": "ukdoubleswitch.png"},
        rocker_txt("rotLeft", 0.028, 8.0, -8.0, cz=0.056) + rocker_txt("rotRight", 0.028, 8.0, -8.0, cz=-0.056))
plate_texture().save(os.path.join(models, "ukdoubleswitch/ukdoubleswitch.png"))

# --- 240V Type G socket: plate (SocketBase), face with the slots (SocketTypeG), its switch and the red ON flag.
R_FACE = (64, 64, 127, 127)
R_RED = (96, 16, 103, 23)


def socket_extra(d):
    plate_face(d, R_FACE)
    ox, oy = R_FACE[0], R_FACE[1]
    # Face is 64 px across 0.32 blocks: 200 px per block. Slots: earth on top, neutral left, live right.
    slot(d, (ox + 18, oy + 12, ox + 22, oy + 25))
    slot(d, (ox + 7, oy + 33, ox + 16, oy + 37))
    slot(d, (ox + 25, oy + 33, ox + 34, oy + 37))
    # The recess the socket's own switch sits in, at the right.
    recess(d, (ox + 44, oy + 17, ox + 57, oy + 46))
    gradient_rect(d, R_RED, RED_HI, RED)


o = Obj("uksocket", T)
o.part("SocketBase")
plate(o, screws=((0.0, 0.142), (0.0, -0.142)))
o.part("SocketTypeG")
o.face_quad(0.0285, -0.160, 0.160, -0.160, 0.160, R_FACE)
o.part("SocketSwitch")
rocker(o, (0.0, -0.095), 0.068, 0.028, x0=0.0285, depth=0.018)
o.part("SocketOnMark")
o.face_quad(0.0475, 0.036, 0.058, -0.118, -0.072, R_RED)
o.write(os.path.join(models, "uksocket"), {"Body": "uksocket.png"},
        rocker_txt("SocketSwitch", 0.0285, 8.0, -8.0, cz=-0.095) + "o SocketOnMark\nf originX 0.0285\nf originY 0.0\nf originZ -0.095\n")
plate_texture(socket_extra).save(os.path.join(models, "uksocket/uksocket.png"))

# --- UK Switched Fused Spur (13A FCU): plate, the fuse carrier, a red neon, and a rocker that rocks about Y.
R_FUSE = (64, 16, 95, 47)
R_NEON = (104, 16, 111, 23)
R_HROCK = (0, 112, 47, 127)


def spur_extra(d):
    d.rectangle(R_FUSE, fill=SHADE, outline=EDGE)
    fx, fy = R_FUSE[0], R_FUSE[1]
    d.rectangle([fx + 3, fy + 3, fx + 28, fy + 28], outline=DEEP)
    d.rectangle([fx + 12, fy + 13, fx + 19, fy + 18], fill=DEEP)  # the screwdriver slot to open the carrier
    # The spur sits with its model Y across the wall, so the label is drawn turned to read left to right in game.
    label = Image.new("RGBA", (20, 10), (0, 0, 0, 0))
    ImageDraw.Draw(label).text((1, -1), "13A", fill=(110, 110, 106))
    d._image.paste(label.rotate(90, expand=True), (fx + 21, fy + 6), label.rotate(90, expand=True))
    gradient_rect(d, R_NEON, RED_HI, RED)
    x0, y0, x1, y1 = R_HROCK
    for x in range(x0, x1 + 1):
        t = abs((x - x0) / (x1 - x0) - 0.5) * 2
        d.line([x, y0, x, y1], fill=tuple(int(WHITE_HI[i] - (WHITE_HI[i] - DEEP[i]) * t * t * 0.8) for i in range(3)))
    d.rectangle(R_HROCK, outline=EDGE)


o = Obj("ukfusedspur", T)
o.part("main")
plate(o, screws=((0.142, 0.0), (-0.142, 0.0)))
o.frustum(0.028, 0.040, (0.05, 0.05), (0.046, 0.046), centre=(-0.06, 0.0), front_rect=R_FUSE, side_rect=R_EDGE)
o.prism(0.028, 0.034, 0.012, 0.010, centre=(0.105, 0.0), sides=10, cap_rect=R_NEON, side_rect=R_EDGE)
o.part("lever")
o.frustum(0.028, 0.048, (0.026, 0.06), (0.022, 0.056), centre=(0.045, 0.0), front_rect=R_HROCK, side_rect=R_ROCK_SIDE)
o.write(os.path.join(models, "ukfusedspur"), {"Body": "ukfusedspur.png"},
        "s type lever\no lever\nf originX 0.028\nf originY 0.045\nf originZ 0.0\nf alphaOff 8.0\nf alphaOn -8.0\nf speed 0.1\n")
plate_texture(spur_extra).save(os.path.join(models, "ukfusedspur/ukfusedspur.png"))

# --- UK Pull Cord Switch (ceiling): a round body and nozzle, the cord and its acorn handle, pulled down when on.
R_DISC = (64, 64, 95, 95)
R_ROUND = (64, 96, 127, 103)
R_CORD = (64, 104, 127, 107)
R_ACORN = (64, 108, 127, 115)


def round_extra(d):
    d.ellipse(R_DISC, fill=WHITE_HI, outline=EDGE)
    x0, y0, x1, y1 = R_DISC
    d.ellipse([x0 + 4, y0 + 4, x1 - 4, y1 - 4], outline=SHADE)
    gradient_rect(d, R_ROUND, WHITE_HI, SHADE)
    for x in range(R_CORD[0], R_CORD[2] + 1, 3):  # the braided cord
        d.line([x, R_CORD[1], x + 2, R_CORD[3]], fill=EDGE)
    gradient_rect(d, R_ACORN, WHITE_HI, DEEP)


o = Obj("ukpullcord", T)
o.part("main")
o.prism(0.0, 0.03, 0.075, 0.068, sides=20, cap_rect=R_DISC, side_rect=R_ROUND)
o.prism(0.03, 0.045, 0.02, 0.014, sides=12, cap_rect=R_DISC, side_rect=R_ROUND)
o.part("button")
o.frustum(0.03, 0.40, (0.004, 0.004), (0.004, 0.004), front_rect=R_CORD, side_rect=R_CORD)
o.prism(0.40, 0.425, 0.006, 0.020, sides=12, side_rect=R_ACORN)
o.prism(0.425, 0.47, 0.020, 0.008, sides=12, cap_rect=R_DISC, side_rect=R_ACORN)
o.write(os.path.join(models, "ukpullcord"), {"Body": "ukpullcord.png"}, "s type button\no button\nf tx 0.03\nf speed 0.15\n")
plate_texture(round_extra).save(os.path.join(models, "ukpullcord/ukpullcord.png"))

# --- UK Ceiling Rose: rose, flex and lampholder (socket_unlightable), and a bulb (lampOff/lampOn) on its own textures.
R_GLASS = (0, 0, 31, 31)
R_GLASS_SIDE = (32, 0, 95, 15)


def rose_extra(d):
    round_extra(d)
    gradient_rect(d, (64, 116, 127, 123), (230, 230, 226), (180, 178, 170))  # lampholder skirt


o = Obj("ukceilingrose", T)
o.part("socket_unlightable")
o.prism(0.0, 0.035, 0.088, 0.078, sides=24, cap_rect=R_DISC, side_rect=R_ROUND)
o.prism(0.035, 0.045, 0.03, 0.022, sides=16, cap_rect=R_DISC, side_rect=R_ROUND)
o.frustum(0.045, 0.40, (0.005, 0.005), (0.005, 0.005), side_rect=R_CORD)
o.prism(0.40, 0.46, 0.028, 0.03, sides=16, side_rect=(64, 116, 127, 123), back_cap=True, back_rect=R_DISC)
o.prism(0.46, 0.475, 0.034, 0.034, sides=16, cap_rect=R_DISC, side_rect=(64, 116, 127, 123))
for name, mtl in (("lampOff", "BulbOff"), ("lampOn", "BulbOn")):
    o.part(name, mtl)
    o.prism(0.475, 0.50, 0.016, 0.026, sides=16, side_rect=R_GLASS_SIDE)
    o.prism(0.50, 0.535, 0.026, 0.052, sides=16, side_rect=R_GLASS_SIDE)
    o.prism(0.535, 0.575, 0.052, 0.054, sides=16, side_rect=R_GLASS_SIDE)
    o.prism(0.575, 0.61, 0.054, 0.036, sides=16, side_rect=R_GLASS_SIDE)
    o.prism(0.61, 0.625, 0.036, 0.004, sides=16, side_rect=R_GLASS_SIDE)
o.write(os.path.join(models, "ukceilingrose"),
        {"Body": "ukceilingrose.png", "BulbOff": "ukceilingrose_bulb_off.png", "BulbOn": "ukceilingrose_bulb_on.png"},
        "s tOn ukceilingrose.png\ns tOff ukceilingrose.png\n")
plate_texture(rose_extra).save(os.path.join(models, "ukceilingrose/ukceilingrose.png"))
for state, top, bottom in (("off", (236, 236, 230), (200, 200, 194)), ("on", (255, 252, 220), (255, 214, 120))):
    im, d = new_tex(T)
    gradient_rect(d, (0, 0, 127, 127), top, bottom)
    im.save(os.path.join(models, f"ukceilingrose/ukceilingrose_bulb_{state}.png"))

# --- 16x16 inventory icons.
icons = os.path.join(out_root, "assets/eln/textures/blocks")
os.makedirs(icons, exist_ok=True)


def icon(name, draw):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    draw(d)
    im.save(os.path.join(icons, name + ".png"))


def icon_plate(d):
    d.rectangle([1, 1, 14, 14], fill=WHITE, outline=EDGE)
    d.line([2, 14, 14, 14], fill=DEEP)
    d.line([14, 2, 14, 14], fill=DEEP)
    d.point([3, 7], fill=DEEP)
    d.point([12, 7], fill=DEEP)


def icon_switch(d):
    icon_plate(d)
    d.rectangle([6, 4, 9, 11], fill=WHITE_HI, outline=DEEP)
    d.line([6, 7, 9, 7], fill=EDGE)


def icon_double(d):
    icon_plate(d)
    for x in (4, 9):
        d.rectangle([x, 4, x + 2, 11], fill=WHITE_HI, outline=DEEP)


def icon_socket(d):
    icon_plate(d)
    d.rectangle([5, 3, 5, 6], fill=SLOT)
    d.rectangle([2, 9, 4, 9], fill=SLOT)
    d.rectangle([6, 9, 8, 9], fill=SLOT)
    d.rectangle([10, 4, 12, 11], fill=WHITE_HI, outline=DEEP)
    d.line([11, 5, 11, 6], fill=RED)


def icon_spur(d):
    icon_plate(d)
    d.rectangle([4, 9, 11, 12], fill=SHADE, outline=DEEP)
    d.rectangle([4, 4, 11, 6], fill=WHITE_HI, outline=DEEP)
    d.point([12, 2], fill=RED)


def icon_pullcord(d):
    d.ellipse([3, 0, 12, 4], fill=WHITE, outline=EDGE)
    d.line([7, 4, 7, 11], fill=CORD)
    d.line([8, 4, 8, 11], fill=EDGE)
    d.ellipse([5, 11, 10, 15], fill=WHITE_HI, outline=DEEP)


def icon_rose(d):
    d.ellipse([3, 0, 12, 3], fill=WHITE, outline=EDGE)
    d.line([7, 3, 7, 7], fill=EDGE)
    d.rectangle([6, 7, 9, 8], fill=EDGE)
    d.ellipse([4, 8, 11, 15], fill=(255, 236, 170), outline=(220, 190, 110))


icon("uklightswitch", icon_switch)
icon("ukdoubleswitch", icon_double)
icon("typegsocket", icon_socket)
icon("ukfusedspur", icon_spur)
icon("ukpullcord", icon_pullcord)
icon("ukceilingrose", icon_rose)
print("written to", out_root)
