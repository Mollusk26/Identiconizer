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

/** Beetles seen from above: head, thorax, split shell with a pattern, six legs, antennae. */
public final class BeetleGenerator {
    private static final int W = 13, H = 14, C = 6; // odd width: column C is the center seam

    private static final int[][] SHELLS = {
            {4, 4, 4, 4, 3, 2},   // rounded
            {3, 4, 4, 4, 3},      // oval
            {4, 4, 4, 3, 2},      // teardrop
            {4, 4, 4, 4, 4, 2},   // boxy
            {3, 3, 4, 4, 3, 1},   // long
            {4, 4, 3, 3, 2, 1},   // tapered
    };

    private BeetleGenerator() {
    }

    public static PixelSprite generate(byte[] hash) {
        Random rnd = new Random(PixelSprite.seed(hash, 0x62656574L));
        int[][] b = new int[H][W];

        int headHw = 1 + rnd.nextInt(2);           // head is 2*hw+1 wide
        int headTop = 3, headRows = 2;
        int thoraxRows = 1 + rnd.nextInt(2);
        int thoraxHw = Math.max(headHw + 1, 2 + rnd.nextInt(2));
        int[] shell = SHELLS[rnd.nextInt(SHELLS.length)];
        int shellTop = headTop + headRows + thoraxRows;
        if (shellTop + shell.length > H) shellTop = H - shell.length;

        // Head (with eyes) and thorax
        for (int y = headTop; y < headTop + headRows; y++)
            for (int i = 0; i <= headHw; i++) PixelSprite.mirror(b, y, C - i, PixelSprite.DARK);
        int eyeX = C - headHw;
        if (headHw == 2 && rnd.nextBoolean()) eyeX = C - 1;
        PixelSprite.mirror(b, headTop + 1, eyeX, PixelSprite.EYE);
        boolean thoraxColored = rnd.nextFloat() < 0.35f;
        for (int y = headTop + headRows; y < shellTop; y++)
            for (int i = 0; i <= thoraxHw; i++)
                PixelSprite.mirror(b, y, C - i, thoraxColored ? PixelSprite.BODY : PixelSprite.DARK);

        // Shell halves and seam
        for (int r = 0; r < shell.length; r++)
            for (int i = 0; i <= shell[r]; i++) PixelSprite.mirror(b, shellTop + r, C - i, PixelSprite.BODY);

        // Shell pattern
        int pattern = rnd.nextInt(4); // 0 spots, 1 stripes, 2 bands, 3 plain metallic
        if (pattern == 0) {
            float p = 0.25f + rnd.nextFloat() * 0.2f;
            for (int r = 0; r < shell.length; r++)
                for (int x = C - shell[r] + 1; x <= C - 2; x++) {
                    int y = shellTop + r;
                    boolean near = PixelSprite.get(b, y, x - 1) == PixelSprite.SPOT
                            || PixelSprite.get(b, y, x + 1) == PixelSprite.SPOT
                            || PixelSprite.get(b, y - 1, x) == PixelSprite.SPOT
                            || PixelSprite.get(b, y - 1, x - 1) == PixelSprite.SPOT
                            || PixelSprite.get(b, y - 1, x + 1) == PixelSprite.SPOT;
                    if (!near && rnd.nextFloat() < p) PixelSprite.mirror(b, y, x, PixelSprite.SPOT);
                }
        } else if (pattern == 1) {
            int sx = C - 2 - rnd.nextInt(2);
            for (int r = 0; r < shell.length; r++)
                if (C - shell[r] < sx) PixelSprite.mirror(b, shellTop + r, sx, PixelSprite.SPOT);
        } else if (pattern == 2) {
            int start = rnd.nextInt(2);
            for (int r = start; r < shell.length - 1; r += 2)
                for (int i = 1; i < shell[r]; i++) PixelSprite.mirror(b, shellTop + r, C - i, PixelSprite.SPOT);
        }
        if (pattern != 2) PixelSprite.mirror(b, shellTop, C - 2, PixelSprite.SHINE);

        // Seam drawn last so it splits everything on the shell
        for (int r = 0; r < shell.length; r++) b[shellTop + r][C] = PixelSprite.OUTLINE;

        // Antennae (thin dark lines, rows above the head)
        int e = C - headHw;
        int[][][] ANT = {
                {{2, -1}, {1, -2}, {0, -3}},     // diagonal
                {{2, 0}, {1, -1}, {0, -1}},      // steep
                {{2, -1}, {1, -2}, {0, -2}},     // elbow
                {{2, -1}, {1, -1}, {0, -2}},     // wavy
        };
        int[][] ant = ANT[rnd.nextInt(ANT.length)];
        boolean club = rnd.nextFloat() < 0.35f;
        for (int i = 0; i < ant.length; i++) {
            int v = (club && i == ant.length - 1) ? PixelSprite.DARK : PixelSprite.OUTLINE;
            PixelSprite.mirror(b, ant[i][0], e + ant[i][1], v);
        }
        if (rnd.nextFloat() < 0.35f) { // mandibles
            PixelSprite.mirror(b, headTop - 1, C - 1, PixelSprite.DARK);
        }

        // Legs: front pair off the thorax, middle and back pairs off the shell.
        int[] legRows = {headTop + headRows, shellTop + 1, shellTop + shell.length - 2};
        int[] legHw = {thoraxHw, shell[1], shell[shell.length - 2]};
        int[] dir = {-1, rnd.nextInt(3) - 1, 1};
        for (int k = 0; k < 3; k++) {
            int x = C - legHw[k] - 2;
            PixelSprite.mirror(b, legRows[k], x, PixelSprite.OUTLINE);
            if (x - 1 >= 0) PixelSprite.mirror(b, legRows[k] + dir[k], x - 1, PixelSprite.OUTLINE);
        }

        int[][] g = PixelSprite.pad(b);
        PixelSprite.addOutline(g);

        int[] pal = new int[PixelSprite.CODES];
        float hue, sat, val;
        int spot;
        float style = rnd.nextFloat();
        if (pattern == 0 && style < 0.5f) {            // ladybug-ish
            hue = rnd.nextFloat() * 50f; sat = 0.8f; val = 0.88f;
            spot = 0xFF1E1A1A;
        } else if (pattern == 3) {                       // metallic jewel beetle
            hue = 100f + rnd.nextFloat() * 200f; sat = 0.8f; val = 0.75f;
            spot = PixelSprite.hsv(hue, 0.5f, 1f);
        } else {
            hue = rnd.nextFloat() * 360f; sat = 0.55f + rnd.nextFloat() * 0.3f; val = 0.78f + rnd.nextFloat() * 0.15f;
            spot = rnd.nextBoolean() ? PixelSprite.hsv(hue + 180f, 0.25f, 0.97f) : PixelSprite.hsv(hue, sat, 0.3f);
        }
        pal[PixelSprite.BODY] = PixelSprite.hsv(hue, sat, val);
        pal[PixelSprite.SPOT] = spot;
        pal[PixelSprite.OUTLINE] = PixelSprite.hsv(hue, 0.5f, 0.16f);
        pal[PixelSprite.DARK] = PixelSprite.hsv(hue, 0.35f, 0.3f);
        pal[PixelSprite.SHINE] = PixelSprite.hsv(hue, sat * 0.3f, 1f);
        pal[PixelSprite.EYE] = 0xFFFFFFFF;
        pal[PixelSprite.MOUTH] = pal[PixelSprite.OUTLINE];
        return new PixelSprite(g, pal, 0.5f + rnd.nextFloat() * 0.15f, 0.4f + rnd.nextFloat() * 0.5f);
    }
}
