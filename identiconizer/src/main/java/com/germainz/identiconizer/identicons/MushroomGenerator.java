/*
 * Copyright (C) 2026 Identiconizer contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.germainz.identiconizer.identicons;

import java.util.Random;

/** Mushroom folk: a spotted cap on a stem with a face, mirrored left/right. */
public final class MushroomGenerator {
    private static final int W = 12, H = 13, C0 = 5; // C0 = left of the two center columns

    // Cap half-widths per row, top to bottom (each row is 2*hw cells wide).
    // Cap half-widths per row, top to bottom (each row is 2*hw cells wide).
    private static final int[][] CAPS = {
            {3, 5, 6, 6},       // round dome
            {4, 6, 6},          // flat
            {1, 2, 3, 4, 5, 6}, // pointy
            {2, 4, 5, 6, 6},    // tall dome
            {2, 4, 5, 5},       // small dome
            {3, 5, 6, 6, 5},    // puffball (curls under)
            {4, 5, 6, 6, 6},    // big bell
    };

    private MushroomGenerator() {
    }

    public static PixelSprite generate(byte[] hash) {
        Random rnd = new Random(PixelSprite.seed(hash, 0x6D75736CL));
        int[][] b = new int[H][W];
        int row = 0;

        // Cap
        int[] cap = CAPS[rnd.nextInt(CAPS.length)];
        int capTop = row;
        for (int hw : cap) {
            for (int i = 0; i < hw; i++) PixelSprite.mirror(b, row, C0 - i, PixelSprite.BODY);
            row++;
        }
        int capRows = cap.length;
        int capBottomHw = cap[cap.length - 1];

        // Spots, or a plain cap with a shine
        if (rnd.nextFloat() < 0.8f) {
            float p = 0.3f + rnd.nextFloat() * 0.25f;
            for (int y = capTop; y < capTop + capRows; y++)
                for (int x = 0; x <= C0; x++) {
                    if (b[y][x] != PixelSprite.BODY) continue;
                    boolean interior = PixelSprite.get(b, y, x - 1) != PixelSprite.EMPTY
                            && PixelSprite.get(b, y - 1, x) != PixelSprite.EMPTY
                            && PixelSprite.get(b, y + 1, x) != PixelSprite.EMPTY;
                    boolean nearSpot = PixelSprite.get(b, y, x - 1) == PixelSprite.SPOT
                            || PixelSprite.get(b, y - 1, x) == PixelSprite.SPOT
                            || PixelSprite.get(b, y - 1, x - 1) == PixelSprite.SPOT
                            || PixelSprite.get(b, y - 1, x + 1) == PixelSprite.SPOT;
                    if ((interior || y == capTop) && !nearSpot && rnd.nextFloat() < p)
                        PixelSprite.mirror(b, y, x, PixelSprite.SPOT);
                }
        } else {
            int sy = capTop + 1, sx = C0 - Math.max(1, cap[1] - 2);
            if (PixelSprite.get(b, sy, sx) == PixelSprite.BODY) PixelSprite.mirror(b, sy, sx, PixelSprite.SHINE);
        }

        // Gills under the cap
        if (rnd.nextFloat() < 0.55f) {
            for (int i = 0; i < capBottomHw - 1; i++) PixelSprite.mirror(b, row, C0 - i, PixelSprite.GILL);
            row++;
        }

        // Stem with face
        int sHalf = rnd.nextFloat() < 0.75f ? 3 : 4;     // stem is 2*sHalf wide, cap up to 12
        int stemH = 4 + rnd.nextInt(2);
        int stemTop = row;
        for (int y = 0; y < stemH; y++, row++)
            for (int i = 0; i < sHalf; i++) PixelSprite.mirror(b, row, C0 - i, PixelSprite.STEM);
        // slightly wider base on some
        if (rnd.nextBoolean()) PixelSprite.mirror(b, row - 1, C0 - sHalf, PixelSprite.STEM);

        int eyeOff = (sHalf == 4 && rnd.nextBoolean()) ? 2 : 1;
        int eyeRow = stemTop + (rnd.nextFloat() < 0.7f ? 1 : 0);
        PixelSprite.mirror(b, eyeRow, C0 - eyeOff, PixelSprite.EYE);
        float m = rnd.nextFloat();
        if (m < 0.45f) {
            PixelSprite.mirror(b, eyeRow + 1, C0, PixelSprite.MOUTH);              // small mouth
        } else if (m < 0.7f && eyeOff == 2) {
            PixelSprite.mirror(b, eyeRow + 1, C0, PixelSprite.MOUTH);              // grin
            PixelSprite.mirror(b, eyeRow + 1, C0 - 1, PixelSprite.MOUTH);
        } else if (m < 0.85f) {
            PixelSprite.mirror(b, eyeRow + 2, C0, PixelSprite.MOUTH);              // lower "o"
        }
        if (rnd.nextFloat() < 0.5f && C0 - eyeOff - 1 > C0 - sHalf)
            PixelSprite.mirror(b, eyeRow + 1, C0 - eyeOff - 1, PixelSprite.BLUSH);

        // Little arms
        if (rnd.nextFloat() < 0.4f) PixelSprite.mirror(b, stemTop + 2, C0 - sHalf, PixelSprite.STEM);

        // Shoes
        if (rnd.nextFloat() < 0.5f && row < H) {
            PixelSprite.mirror(b, row, C0 - sHalf + 1, PixelSprite.DARK);
            PixelSprite.mirror(b, row, C0 - sHalf + 2, PixelSprite.DARK);
        }

        int[][] g = PixelSprite.pad(b);
        PixelSprite.addOutline(g);

        float hue = rnd.nextFloat() * 360f;
        float sat = 0.55f + rnd.nextFloat() * 0.3f, val = 0.75f + rnd.nextFloat() * 0.17f;
        int[] pal = new int[PixelSprite.CODES];
        pal[PixelSprite.BODY] = PixelSprite.hsv(hue, sat, val);
        pal[PixelSprite.OUTLINE] = PixelSprite.hsv(hue, sat + 0.15f, 0.24f);
        pal[PixelSprite.MOUTH] = pal[PixelSprite.OUTLINE];
        pal[PixelSprite.SPOT] = rnd.nextFloat() < 0.75f ? 0xFFF7F0DC
                : PixelSprite.hsv(hue + 180f, 0.3f, 0.97f);
        float stemHue = 28f + rnd.nextFloat() * 20f, stemSat = 0.10f + rnd.nextFloat() * 0.15f;
        pal[PixelSprite.STEM] = PixelSprite.hsv(stemHue, stemSat, 0.95f);
        pal[PixelSprite.GILL] = PixelSprite.hsv(stemHue, stemSat + 0.1f, 0.78f);
        pal[PixelSprite.DARK] = PixelSprite.hsv(hue, 0.5f, 0.38f);
        pal[PixelSprite.SHINE] = PixelSprite.hsv(hue, sat * 0.35f, 1f);
        pal[PixelSprite.BLUSH] = 0xFFF2A0A8;
        pal[PixelSprite.EYE] = 0xFFFFFFFF;
        return new PixelSprite(g, pal, 0.5f + rnd.nextFloat() * 0.15f, 0.6f + rnd.nextFloat() * 0.4f);
    }
}
