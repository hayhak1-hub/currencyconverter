from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os

root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
out_dir = os.path.dirname(__file__)

# Play Console high-res icon: exactly 512×512, 32-bit PNG, fully opaque.
fg_src = os.path.join(root, "app", "src", "main", "res", "drawable", "ic_launcher_foreground.png")
bg_color = (0, 19, 75)  # #00134B
fg = Image.open(fg_src).convert("RGBA").resize((512, 512), Image.Resampling.LANCZOS)
icon512 = Image.new("RGBA", (512, 512), bg_color + (255,))
icon512 = Image.alpha_composite(icon512, fg).convert("RGB").convert("RGBA")
icon512_path = os.path.join(out_dir, "app-icon-512.png")
icon512.save(icon512_path, "PNG")

W, H = 1024, 500
bg = Image.new("RGBA", (W, H), "#00134B")

grad = Image.new("RGBA", (W, H))
draw_g = ImageDraw.Draw(grad)
for y in range(H):
    t = y / H
    r = int(0 + 21 * t)
    g = int(19 + 82 * t)
    b = int(75 + 117 * t)
    draw_g.line([(0, y), (W, y)], fill=(r, g, b, 255))
bg = Image.alpha_composite(bg, grad)

for cx, cy, rad, alpha in [
    (820, 120, 180, 35),
    (900, 380, 140, 28),
    (150, 400, 120, 22),
    (680, 420, 90, 18),
]:
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    od.ellipse((cx - rad, cy - rad, cx + rad, cy + rad), fill=(255, 255, 255, alpha))
    bg = Image.alpha_composite(bg, overlay)

icon_fg = icon512.resize((300, 300), Image.Resampling.LANCZOS)
shadow = Image.new("RGBA", icon_fg.size, (0, 0, 0, 0))
sd = ImageDraw.Draw(shadow)
sd.ellipse((20, 240, 280, 310), fill=(0, 0, 0, 90))
shadow = shadow.filter(ImageFilter.GaussianBlur(12))
bg.paste(shadow, (690, 95), shadow)
bg.paste(icon_fg, (700, 100), icon_fg)

draw = ImageDraw.Draw(bg)

font_paths = [
    r"C:\Windows\Fonts\segoeuib.ttf",
    r"C:\Windows\Fonts\arialbd.ttf",
]
sub_paths = [
    r"C:\Windows\Fonts\segoeui.ttf",
    r"C:\Windows\Fonts\arial.ttf",
]


def load_font(paths, size):
    for path in paths:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


title_font = load_font(font_paths, 56)
sub_font = load_font(sub_paths, 28)

draw.text((64, 150), "Currency Converter", fill=(255, 255, 255, 255), font=title_font)
draw.text((64, 230), "Live rates - Charts - Alerts - Widget", fill=(200, 220, 255, 255), font=sub_font)
draw.rounded_rectangle((64, 310, 420, 316), radius=3, fill=(76, 175, 80, 255))

feature_path = os.path.join(out_dir, "feature-graphic-1024x500.png")
bg.convert("RGB").save(feature_path, "PNG", optimize=True)

print(icon512_path, icon512.size)
print(feature_path, bg.size)
