"""Draws the icons the registry index points at.

A mod's icon is optional, and a launcher that finds none draws its own mark
instead, so these exist to be looked at rather than to be required. They are
drawn rather than painted: flat shapes in the launcher's own palette, at four
times the size and then scaled down, which is the cheapest antialiasing there
is and quite enough for something shown at 64 pixels.

    python tools/icons.py

Writes icon.png beside each mod and one at the root for the repository itself.
Nothing reads this at build time; run it when an icon should change.
"""
import pathlib

from PIL import Image, ImageDraw

ROOT = pathlib.Path(__file__).resolve().parent.parent

SIZE = 256
SCALE = 4

BACK = "#16130f"
GOLD = "#c8a45c"
EMBER = "#d8955e"
BLOOD = "#d0705c"
CREAM = "#e8e2d4"
FAINT = "#3f341f"


def canvas():
    image = Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle(
        (0, 0, SIZE * SCALE - 1, SIZE * SCALE - 1), radius=48 * SCALE, fill=BACK)
    return image, draw


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.resize((SIZE, SIZE), Image.LANCZOS).save(path)
    print(path.relative_to(ROOT).as_posix())


def box(draw, x, y, wide, tall, radius, **kwargs):
    """Rounded rectangle in icon units rather than in pixels.

    `wide` and `tall` rather than width and height because `width` is what
    ImageDraw calls an outline thickness, and one of these calls passes both.
    """
    draw.rounded_rectangle(
        (x * SCALE, y * SCALE, (x + wide) * SCALE, (y + tall) * SCALE),
        radius=radius * SCALE, **kwargs)


def line(draw, points, width, fill):
    draw.line([(x * SCALE, y * SCALE) for x, y in points],
              width=width * SCALE, fill=fill, joint="curve")


def self_check():
    """A checklist: three rows, the first two settled, the third still open."""
    image, draw = canvas()
    for index, (colour, done) in enumerate(
            [(GOLD, True), (EMBER, True), (FAINT, False)]):
        top = 62 + index * 50
        box(draw, 48, top, 34, 34, 10, fill=colour if done else BACK,
            outline=colour, width=5)
        box(draw, 100, top + 11, 108, 12, 6, fill=colour if done else FAINT)
        if done:
            line(draw, [(56, top + 18), (63, top + 25), (74, top + 10)], 6, BACK)
    return image


def old_huge_potions():
    """A flask, filled to the neck."""
    image, draw = canvas()
    box(draw, 110, 50, 36, 40, 6, fill=CREAM)
    draw.ellipse((78 * SCALE, 92 * SCALE, 178 * SCALE, 192 * SCALE), fill=CREAM)
    draw.ellipse((88 * SCALE, 102 * SCALE, 168 * SCALE, 182 * SCALE), fill=BLOOD)
    box(draw, 104, 40, 48, 16, 8, fill=GOLD)
    # The highlight is what stops a circle reading as a ball bearing.
    draw.ellipse((104 * SCALE, 116 * SCALE, 122 * SCALE, 138 * SCALE), fill=CREAM)
    return image


def all_my_runes():
    """A rune: one stem, two strokes, cut at angles nothing in Latin has."""
    image, draw = canvas()
    line(draw, [(128, 44), (128, 212)], 14, GOLD)
    line(draw, [(128, 96), (184, 60)], 14, GOLD)
    line(draw, [(128, 148), (72, 112)], 14, EMBER)
    line(draw, [(128, 172), (176, 204)], 14, GOLD)
    return image


def tracer():
    """A page of lines: what this one leaves behind is a file."""
    image, draw = canvas()
    box(draw, 66, 44, 124, 168, 12, fill=CREAM)
    for index in range(6):
        top = 68 + index * 24
        wide = [86, 104, 70, 96, 60, 82][index]
        box(draw, 84, top, wide, 9, 4, fill=GOLD if index % 2 == 0 else FAINT)
    return image


def repository():
    """The registry itself: a shield, because it is the thing that vouches."""
    image, draw = canvas()
    draw.polygon([(p[0] * SCALE, p[1] * SCALE) for p in
                  [(128, 40), (200, 72), (200, 140), (128, 216), (56, 140), (56, 72)]],
                 fill=GOLD)
    draw.polygon([(p[0] * SCALE, p[1] * SCALE) for p in
                  [(128, 62), (182, 86), (182, 134), (128, 192), (74, 134), (74, 86)]],
                 fill=BACK)
    line(draw, [(100, 128), (120, 150), (158, 100)], 14, CREAM)
    return image


def main():
    save(self_check(), ROOT / "self-check" / "icon.png")
    save(old_huge_potions(), ROOT / "old-huge-potions" / "icon.png")
    save(all_my_runes(), ROOT / "all-my-runes" / "icon.png")
    save(tracer(), ROOT / "tracer" / "icon.png")
    save(repository(), ROOT / "icon.png")


if __name__ == "__main__":
    main()
