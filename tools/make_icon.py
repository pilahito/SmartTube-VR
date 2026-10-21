#!/usr/bin/env python3
"""Genera los iconos de SmartTube VR (icono, icono alternativo y banner).

Todo se dibuja por código, así que el resultado es del proyecto y se puede regenerar:
    python3 tools/make_icon.py        (necesita Pillow y la fuente DejaVu Sans Bold)

Salida en patches/icon/. El parche smarttube_icon.py los copia sobre los del SmartTube original.
"""
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'patches', 'icon')
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
SS = 4  # supersampling para bordes limpios

# (fondo arriba, fondo abajo, pantalla arriba, pantalla abajo, acento del visor)
PALETTES = {
    'app_icon': ((38, 22, 96), (10, 7, 36), (255, 92, 92), (214, 31, 58), (79, 214, 255)),
    'app_icon_alt': ((12, 38, 70), (6, 14, 34), (255, 176, 64), (240, 96, 40), (120, 255, 200)),
}


def vgrad(size, top, bottom):
    """Degradado vertical."""
    w, h = size
    g = Image.new('RGBA', size)
    px = g.load()
    for y in range(h):
        t = y / max(h - 1, 1)
        c = tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3)) + (255,)
        for x in range(w):
            px[x, y] = c
    return g


def rounded_mask(size, radius):
    m = Image.new('L', size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius, fill=255)
    return m


def draw_icon(palette, px=320):
    """Icono cuadrado: pantalla de cine curva con play, y un visor VR debajo."""
    bg_top, bg_bot, sc_top, sc_bot, accent = palette
    S = px * SS
    img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    bg = vgrad((S, S), bg_top, bg_bot)
    img.paste(bg, (0, 0), rounded_mask((S, S), int(S * 0.225)))

    # resplandor detrás de la pantalla
    glow = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([S * 0.12, S * 0.14, S * 0.88, S * 0.62], fill=sc_top + (120,))
    glow = glow.filter(ImageFilter.GaussianBlur(S * 0.07))
    img = Image.alpha_composite(img, glow)

    # pantalla curva: el borde superior y el inferior se arquean hacia dentro
    sx0, sx1 = S * 0.14, S * 0.86
    sy0, sy1 = S * 0.20, S * 0.58
    bow = S * 0.045
    steps = 60
    top_edge = [(sx0 + (sx1 - sx0) * i / steps, sy0 + bow * (1 - (2 * i / steps - 1) ** 2)) for i in range(steps + 1)]
    bot_edge = [(sx1 - (sx1 - sx0) * i / steps, sy1 + bow * (1 - (2 * i / steps - 1) ** 2)) for i in range(steps + 1)]
    screen_poly = top_edge + bot_edge
    mask = Image.new('L', (S, S), 0)
    ImageDraw.Draw(mask).polygon(screen_poly, fill=255)
    fill = vgrad((S, S), sc_top, sc_bot)
    img.paste(fill, (0, 0), mask)
    # reflejo suave en la mitad de arriba de la pantalla
    shine = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(shine).polygon(
        [(sx0 + S * 0.03, sy0 + S * 0.01), (sx0 + S * 0.40, sy0 + S * 0.01), (sx0 + S * 0.18, sy1 - S * 0.02), (sx0 + S * 0.03, sy1 - S * 0.02)],
        fill=(255, 255, 255, 46))
    shine.putalpha(Image.composite(shine.getchannel('A'), Image.new('L', (S, S), 0), mask))
    img = Image.alpha_composite(img, shine)

    d = ImageDraw.Draw(img)
    # botón de play (blanco, esquinas suaves)
    cx, cy = S * 0.50, (sy0 + sy1) / 2 + S * 0.035
    r = S * 0.115
    tri = [(cx - r * 0.70, cy - r), (cx - r * 0.70, cy + r), (cx + r * 1.05, cy)]
    d.polygon(tri, fill=(255, 255, 255, 255))
    d.line(tri + [tri[0]], fill=(255, 255, 255, 255), width=int(S * 0.012), joint='curve')

    # visor VR: carcasa redondeada con una banda de acento y las dos lentes
    vx0, vx1 = S * 0.27, S * 0.73
    vy0, vy1 = S * 0.68, S * 0.84
    d.rounded_rectangle([vx0, vy0, vx1, vy1], radius=S * 0.07, fill=(18, 14, 52, 255), outline=accent + (255,), width=int(S * 0.016))
    for lx in (S * 0.395, S * 0.605):
        d.ellipse([lx - S * 0.052, (vy0 + vy1) / 2 - S * 0.052, lx + S * 0.052, (vy0 + vy1) / 2 + S * 0.052], fill=accent + (255,))
        d.ellipse([lx - S * 0.024, (vy0 + vy1) / 2 - S * 0.024, lx + S * 0.024, (vy0 + vy1) / 2 + S * 0.024], fill=(18, 14, 52, 255))
    # correa
    d.rounded_rectangle([vx0 - S * 0.045, vy0 + S * 0.035, vx0 + S * 0.01, vy1 - S * 0.035], radius=S * 0.02, fill=accent + (255,))
    d.rounded_rectangle([vx1 - S * 0.01, vy0 + S * 0.035, vx1 + S * 0.045, vy1 - S * 0.035], radius=S * 0.02, fill=accent + (255,))

    return img.resize((px, px), Image.LANCZOS)


def fit_font(text, max_width, start, font_path=FONT):
    """Mayor tamaño (hasta `start`) con el que `text` cabe en `max_width` píxeles."""
    size = start
    while size > 8:
        f = ImageFont.truetype(font_path, size)
        if f.getlength(text) <= max_width:
            return f
        size -= 2
    return ImageFont.truetype(font_path, 8)


def draw_banner(palette, w, h, font_path=FONT):
    """Banner 16:9 para el launcher: fondo oscuro, icono a la izquierda y nombre."""
    bg_top, bg_bot = palette[0], palette[1]
    W, H = w * SS, h * SS
    img = vgrad((W, H), bg_top, bg_bot)
    icon_px = int(H * 0.72)
    icon = draw_icon(palette, 320).resize((icon_px, icon_px), Image.LANCZOS)
    left = int(W * 0.06)
    img.alpha_composite(icon, (left, (H - icon_px) // 2))
    d = ImageDraw.Draw(img)
    tx = left + icon_px + int(W * 0.045)
    max_w = W - tx - int(W * 0.05)
    f1 = fit_font('SmartTube', max_w, int(H * 0.19), font_path)
    f2 = fit_font('VR', max_w, int(H * 0.26), font_path)
    f3 = fit_font('YouTube en tu cine virtual', max_w, int(H * 0.085), font_path)
    y = int(H * 0.25)
    d.text((tx, y), 'SmartTube', font=f1, fill=(255, 255, 255, 255))
    y += int(f1.size * 1.15)
    d.text((tx, y), 'VR', font=f2, fill=palette[4] + (255,))
    y += int(f2.size * 1.25)
    d.text((tx, y), 'YouTube en tu cine virtual', font=f3, fill=(255, 255, 255, 170))
    return img.resize((w, h), Image.LANCZOS).convert('RGB')


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, pal in PALETTES.items():
        draw_icon(pal, 320).save(os.path.join(OUT, name + '.png'))
        print('ok', name + '.png')
    draw_banner(PALETTES['app_icon'], 640, 360).save(os.path.join(OUT, 'app_banner_640.png'))
    draw_banner(PALETTES['app_icon'], 320, 180).save(os.path.join(OUT, 'app_banner_320.png'))
    print('ok banners')


if __name__ == '__main__':
    main()
