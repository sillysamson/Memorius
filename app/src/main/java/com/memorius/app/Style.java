package com.memorius.app;

// The five visual styles. Each one is a set of tokens (palette, shapes, borders, surfaces, spacing,
// interaction) that the rest of the app reads, so every component follows the chosen style.
// Typography is not part of a style: fonts are configured separately.
//   Flat        clean 2D surfaces, no shadows, gradients, textures or glass (default)
//   Humanist    organic shapes, warm muted colours, paper and collage touches
//   Material    Material Design 3: tonal surfaces, rounded components, elevation
//   Swiss       strict grid, square geometry, black / white, rules instead of boxes
//   Corporate   generous whitespace, restrained colour, subtle translucency, understated motion
final class Style {
    private Style() {
    }

    static final int FLAT = 0, HUMANIST = 1, MATERIAL = 2, SWISS = 3, CORPORATE = 4;
    static final String[] NAMES = {"Flat", "Humanist", "Material Design 3", "Swiss Modernism", "Corporate Simplicity"};

    static int id = FLAT;

    static int clamp(int s) {
        return Math.max(0, Math.min(NAMES.length - 1, s));
    }

    // ---------- what each style allows ----------
    static boolean glass() { // blur, lens and translucency on the bar, switch, slider and buttons
        return id == CORPORATE;
    }

    static boolean organic() {
        return id == HUMANIST;
    }

    static boolean userTextures() { // paper / grain from the Theme page
        return id == HUMANIST || id == MATERIAL || id == CORPORATE;
    }

    static boolean userGradients() {
        return id == HUMANIST || id == MATERIAL || id == CORPORATE;
    }

    // ---------- palettes: background colours offered on the Theme page ----------
    private static final int[] H_LIGHT = {0xFFF4EEE3, 0xFFEFE3CF, 0xFFE8D9C2, 0xFFF3E1DA, 0xFFE3E8DA, 0xFFE9E2EC};
    private static final String[] H_LIGHT_N = {"Parchment", "Sand", "Kraft", "Blush", "Sage", "Lilac"};
    private static final int[] H_DARK = {0xFF211C17, 0xFF1B1713, 0xFF242A25, 0xFF2A1F22, 0xFF1F2428, 0xFF2B261D, 0xFF17130F, 0xFF2A211B};
    private static final String[] H_DARK_N = {"Umber", "Charcoal", "Moss", "Plum", "Slate", "Olive", "Ink", "Walnut"};
    private static final int[] M_LIGHT = {0xFFFEF7FF, 0xFFF3EDF7, 0xFFFFF8F0, 0xFFF1F8F1, 0xFFF0F6FF, 0xFFFFF1F4};
    private static final String[] M_LIGHT_N = {"Surface", "Lavender", "Peach", "Mint", "Sky", "Rose"};
    private static final int[] M_DARK = {0xFF141218, 0xFF1D1B20, 0xFF1B1A17, 0xFF121A15, 0xFF111A22, 0xFF1F1519, 0xFF0F0E13, 0xFF211F26};
    private static final String[] M_DARK_N = {"Surface", "Container", "Warm", "Green", "Blue", "Rose", "Deep", "High"};
    private static final int[] S_LIGHT = {0xFFFFFFFF, 0xFFF4F4F4, 0xFFE9E9E9, 0xFFFAFAFA, 0xFFF0EFEA, 0xFFE2E2E2};
    private static final String[] S_LIGHT_N = {"White", "Grey 1", "Grey 2", "Paper", "Warm white", "Grey 3"};
    private static final int[] S_DARK = {0xFF000000, 0xFF0B0B0B, 0xFF141414, 0xFF1C1C1C, 0xFF0F0F0F, 0xFF232323, 0xFF060606, 0xFF181818};
    private static final String[] S_DARK_N = {"Black", "Grey 1", "Grey 2", "Grey 3", "Grey 4", "Grey 5", "Near black", "Grey 6"};
    private static final int[] C_LIGHT = {0xFFF5F5F7, 0xFFFFFFFF, 0xFFFBFBFD, 0xFFF2F2F7, 0xFFF7F4EF, 0xFFEDEDF0};
    private static final String[] C_LIGHT_N = {"Silver", "White", "Snow", "Fog", "Linen", "Cloud"};
    private static final int[] C_DARK = {0xFF000000, 0xFF1C1C1E, 0xFF0A0A0C, 0xFF111113, 0xFF2C2C2E, 0xFF18181B, 0xFF0D0D0F, 0xFF242426};
    private static final String[] C_DARK_N = {"Black", "Graphite", "Ink", "Night", "Slate", "Carbon", "Deep", "Steel"};

    static int[] bgs(boolean dark) {
        return bgsOf(id, dark);
    }

    static int[] bgsOf(int sid, boolean dark) {
        switch (sid) {
            case HUMANIST: return dark ? H_DARK : H_LIGHT;
            case MATERIAL: return dark ? M_DARK : M_LIGHT;
            case SWISS: return dark ? S_DARK : S_LIGHT;
            case CORPORATE: return dark ? C_DARK : C_LIGHT;
            default: return dark ? Ui.DARK : Ui.LIGHT;
        }
    }

    static String[] bgNames(boolean dark) {
        switch (id) {
            case HUMANIST: return dark ? H_DARK_N : H_LIGHT_N;
            case MATERIAL: return dark ? M_DARK_N : M_LIGHT_N;
            case SWISS: return dark ? S_DARK_N : S_LIGHT_N;
            case CORPORATE: return dark ? C_DARK_N : C_LIGHT_N;
            default: return dark ? Ui.DARK_NAMES : Ui.LIGHT_NAMES;
        }
    }

    static int fg(boolean dark, boolean contrast) {
        return fgOf(id, dark, contrast);
    }

    static int fgOf(int sid, boolean dark, boolean contrast) {
        switch (sid) {
            case HUMANIST: return dark ? (contrast ? 0xFFFFF8EE : 0xFFF1E9DC) : (contrast ? 0xFF1A1511 : 0xFF2B2620);
            case MATERIAL: return dark ? (contrast ? 0xFFFFFFFF : 0xFFE6E0E9) : (contrast ? 0xFF000000 : 0xFF1D1B20);
            case SWISS: return dark ? 0xFFFFFFFF : 0xFF000000;
            case CORPORATE: return dark ? (contrast ? 0xFFFFFFFF : 0xFFF5F5F7) : (contrast ? 0xFF000000 : 0xFF1D1D1F);
            default: return dark ? (contrast ? 0xFFFFFFFF : 0xFFF1F1F1) : (contrast ? 0xFF000000 : 0xFF1A1A1A);
        }
    }

    // the colour of filled buttons, selected options, "on" switches and slider fills
    static int primary(boolean dark, int fg) {
        return primaryOf(id, dark, fg);
    }

    static int primaryOf(int sid, boolean dark, int fg) {
        switch (sid) {
            case HUMANIST: return dark ? 0xFFE58B64 : 0xFFB5573A;
            case MATERIAL: return dark ? 0xFFD0BCFF : 0xFF6750A4;
            case CORPORATE: return dark ? 0xFF2997FF : 0xFF0071E3;
            default: return fg; // Flat and Swiss stay monochrome
        }
    }

    // ---------- surfaces ----------
    static int surface(int bg, int fg, boolean contrast) {
        float t = contrast ? 0.1f : 0.06f;
        if (id == SWISS) t = contrast ? 0.08f : 0.04f;
        if (id == CORPORATE) t = contrast ? 0.08f : 0.045f;
        return Ui.mix(bg, fg, t);
    }

    static int line(int bg, int fg, boolean contrast) {
        if (contrast) return Ui.mix(bg, fg, 0.45f);
        switch (id) {
            case HUMANIST: return Ui.mix(bg, fg, 0.26f);
            case MATERIAL: return Ui.mix(bg, fg, 0.2f);
            case SWISS: return Ui.mix(bg, fg, 0.9f); // a real rule
            case CORPORATE: return Ui.mix(bg, fg, 0.1f);
            default: return Ui.mix(bg, fg, 0.14f);
        }
    }

    // card surface
    static int cardBase(int bg, int fg, boolean dark, int primary) {
        return cardBaseOf(id, bg, fg, dark, primary);
    }

    // A card must be visible against its page without any border: if the style's shade lands too close
    // to the page colour (for example a white card on a white page), it is pushed away from it.
    static int cardBaseOf(int sid, int bg, int fg, boolean dark, int primary) {
        int card = cardRaw(sid, bg, fg, dark, primary);
        if (sid != SWISS && Ui.ratio(card | 0xFF000000, bg | 0xFF000000) < 1.04) card = Ui.mix(bg, fg, 0.05f);
        return card;
    }

    private static int cardRaw(int sid, int bg, int fg, boolean dark, int primary) {
        switch (sid) {
            case HUMANIST: return dark ? Ui.mix(bg, 0xFFFFF1DC, 0.07f) : Ui.mix(bg, 0xFFFFFFFF, 0.6f);
            case MATERIAL: return Ui.mix(bg, primary, dark ? 0.1f : 0.07f); // tonal surface
            case SWISS: return Ui.mix(bg, fg, 0.04f);
            case CORPORATE: return dark ? Ui.mix(bg, 0xFFFFFFFF, 0.07f) : Ui.mix(bg, 0xFFFFFFFF, 0.8f);
            default: return dark ? Ui.mix(bg, 0xFFFFFFFF, 0.08f) : Ui.mix(bg, 0xFF000000, 0.06f);
        }
    }

    // ---------- shapes ----------
    // Maps the radius a component asks for (dp, or Surface.STADIUM) to the radius of this style.
    static float radius(float r) {
        boolean stadium = r >= Surface.STADIUM_MIN;
        switch (id) {
            case SWISS: return 0f;
            case CORPORATE: return stadium ? 12f : Math.round(r * 0.8f);
            case MATERIAL: return stadium ? Surface.STADIUM : Math.max(12f, r);
            default: return r;
        }
    }

    static float cardRadius(float baseDp) { // 24 standby, 28 preview, 32 full view
        switch (id) {
            case SWISS: return 0f;
            case MATERIAL: return 28f;
            case CORPORATE: return baseDp - 4f;
            default: return baseDp;
        }
    }

    static float sheetRadius(float r) {
        switch (id) {
            case SWISS: return 0f;
            case CORPORATE: return 24f;
            default: return r;
        }
    }

    static float strokeDp(float s) {
        if (s <= 0f) return 0f;
        switch (id) {
            case HUMANIST: return 1.5f;
            case SWISS: return 1f;
            case CORPORATE: return 0.5f;
            default: return s;
        }
    }

    static int strokeColor(int c) {
        return id == SWISS ? Ui.fg : c;
    }

    // padding multiplier: Corporate breathes, Swiss sticks to its 8 dp grid
    static float space() {
        switch (id) {
            case CORPORATE: return 1.25f;
            case HUMANIST: return 1.1f;
            default: return 1f;
        }
    }

    // elevation in dp of floating things (menu, sheet, bar)
    static float elevation() {
        switch (id) {
            case MATERIAL: return 6f;
            case CORPORATE: return 4f;
            default: return 0f;
        }
    }
}
