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

/** Round, front-facing birds: chicks, songbirds and owls, with crests, beaks, bellies and feet. */
public final class BirdGenerator {
    private static final int W = 12, H = 14, C0 = 5;

    // Body half-widths per row, top to bottom.
    private static final int[][] BODIES = {
            {2, 4, 5, 5, 5, 5, 5, 4, 3},    // round
            {3, 4, 4, 5, 5, 5, 5, 4, 3},    // egg
            {3, 5, 5, 5, 5, 5, 4, 3},       // chubby
            {2, 3, 4, 4, 5, 5, 5, 4, 2},    // pear
            {4, 5, 5, 5, 5, 5, 5, 4},       // owl block
    };

    private BirdGenerator() {
    }

    public static PixelSprite generate(byte[] hash) {
        Random rnd = new Random(PixelSprite.seed(hash, 0x62697264L));
        int[][] b = new int[H][W];
        int top = 2; // leave room for crests / tufts
        int[] body = BODIES[rnd.nextInt(BODIES.length)];
        boolean owl = body == BODIES[4] || rnd.nextFloat() < 0.15f;

        for (int r = 0; r < body.length; r++)
            for (int i = 0; i < body[r]; i++) PixelSprite.mirror(b, top + r, C0 - i, PixelSprite.BODY);
        int bottom = top + body.length; // first row below the body

        // Head decoration
        int deco = owl ? 1 : rnd.nextInt(4); // 0 none, 1 ear tufts, 2 center tuft, 3 crest
        if (deco == 1) {
            int x = C0 - body[0] + 1;
            PixelSprite.mirror(b, top - 1, x, PixelSprite.BODY);
            PixelSprite.mirror(b, top - 1, x - 1, PixelSprite.BODY);
            PixelSprite.mirror(b, top - 2, x - 1, PixelSprite.BODY);
        } else if (deco == 2) {
            PixelSprite.mirror(b, top - 1, C0, PixelSprite.BODY);
            b[top - 2][C0 + 1] = PixelSprite.BODY; // a single off-center curl
        } else if (deco == 3) {
            PixelSprite.mirror(b, top - 1, C0, PixelSprite.ACCENT);
            PixelSprite.mirror(b, top - 2, C0, PixelSprite.ACCENT);
            PixelSprite.mirror(b, top - 1, C0 - 1, PixelSprite.ACCENT);
        }

        // Wings: darker outer columns on the lower body, sometimes flared out
        int wingStart = top + 3 + rnd.nextInt(2);
        boolean flared = rnd.nextFloat() < 0.35f;
        for (int y = wingStart; y < bottom - 1; y++) {
            int hw = body[y - top];
            PixelSprite.mirror(b, y, C0 - hw + 1, PixelSprite.WING);
            if (hw >= 5) PixelSprite.mirror(b, y, C0 - hw + 2, PixelSprite.WING);
        }
        if (flared) PixelSprite.mirror(b, wingStart + 1, C0 - body[wingStart + 1 - top], PixelSprite.WING);

        // Belly
        if (rnd.nextFloat() < 0.85f) {
            int bellyTop = top + 4 + (owl ? 0 : rnd.nextInt(2));
            int[] belly = {2, 3, 3, 3, 2};
            for (int r = 0; r < belly.length && bellyTop + r < bottom - 1; r++)
                for (int i = 0; i < belly[r]; i++)
                    if (b[bellyTop + r][C0 - i] == PixelSprite.BODY)
                        PixelSprite.mirror(b, bellyTop + r, C0 - i, PixelSprite.SPOT);
            if (owl || rnd.nextFloat() < 0.3f) { // chevron feather marks
                for (int r = 1; r < belly.length - 1; r += 2)
                    if (bellyTop + r < bottom - 1) PixelSprite.mirror(b, bellyTop + r, C0 - 1, PixelSprite.DARK);
            }
        }

        // Eyes: owls get a pale face disc around big eyes
        int eyeRow = top + 2;
        int eyeX = C0 - (owl ? 2 : 1 + rnd.nextInt(2));
        if (owl) {
            for (int dy = -1; dy <= 1; dy++)
                for (int dx = -1; dx <= 1; dx++) {
                    int x = eyeX + dx;
                    if (x <= C0 && PixelSprite.get(b, eyeRow + dy, x) != PixelSprite.EMPTY)
                        PixelSprite.mirror(b, eyeRow + dy, x, PixelSprite.SHINE);
                }
        }
        PixelSprite.mirror(b, eyeRow, eyeX, PixelSprite.EYE);

        // Beak
        int beak = rnd.nextInt(3);
        PixelSprite.mirror(b, eyeRow + 1, C0, PixelSprite.BEAK);
        if (beak == 1) {
            b[eyeRow + 2][C0] = PixelSprite.BEAK; // hooked: one cell down, slightly off-center
        } else if (beak == 2 && !owl) {
            PixelSprite.mirror(b, eyeRow + 1, C0 - 1, PixelSprite.BEAK); // wide duck-ish bill
        }

        if (!owl && rnd.nextFloat() < 0.4f && eyeX - 1 > C0 - body[eyeRow + 1 - top])
            PixelSprite.mirror(b, eyeRow + 1, eyeX - 1, PixelSprite.BLUSH);

        // Feet
        int footX = C0 - 1 - rnd.nextInt(2);
        if (bottom < H) {
            PixelSprite.mirror(b, bottom, footX, PixelSprite.BEAK);
            if (rnd.nextBoolean()) PixelSprite.mirror(b, bottom, footX - 1, PixelSprite.BEAK);
        }

        int[][] g = PixelSprite.pad(b);
        PixelSprite.addOutline(g);

        int[] pal = new int[PixelSprite.CODES];
        float hue, sat, val;
        float kind = rnd.nextFloat();
        if (owl) {                                // browns and greys
            hue = 20f + rnd.nextFloat() * 25f; sat = rnd.nextFloat() < 0.3f ? 0.08f : 0.35f + rnd.nextFloat() * 0.25f;
            val = 0.55f + rnd.nextFloat() * 0.25f;
        } else if (kind < 0.2f) {                 // chick yellow
            hue = 45f + rnd.nextFloat() * 10f; sat = 0.75f; val = 0.97f;
        } else {
            hue = rnd.nextFloat() * 360f; sat = 0.5f + rnd.nextFloat() * 0.35f; val = 0.72f + rnd.nextFloat() * 0.2f;
        }
        pal[PixelSprite.BODY] = PixelSprite.hsv(hue, sat, val);
        pal[PixelSprite.WING] = PixelSprite.hsv(hue + 8f, sat + 0.1f, val - 0.18f);
        pal[PixelSprite.ACCENT] = PixelSprite.hsv(hue + (rnd.nextBoolean() ? 40f : -40f), sat + 0.1f, val);
        pal[PixelSprite.SPOT] = rnd.nextFloat() < 0.35f && !owl
                ? PixelSprite.hsv(20f + rnd.nextFloat() * 20f, 0.7f, 0.95f)   // robin-style orange breast
                : PixelSprite.hsv(hue, sat * 0.25f, 0.97f);
        pal[PixelSprite.DARK] = PixelSprite.hsv(hue, sat, val - 0.3f);
        pal[PixelSprite.SHINE] = PixelSprite.hsv(hue, sat * 0.2f, 0.98f);
        pal[PixelSprite.BEAK] = PixelSprite.hsv(32f + rnd.nextFloat() * 18f, 0.85f, 0.97f);
        pal[PixelSprite.OUTLINE] = PixelSprite.hsv(hue, Math.max(0.3f, sat), 0.2f);
        pal[PixelSprite.MOUTH] = pal[PixelSprite.OUTLINE];
        pal[PixelSprite.BLUSH] = 0xFFF2A0A8;
        pal[PixelSprite.EYE] = 0xFFFFFFFF;
        float pupil = owl ? 0.7f : 0.55f + rnd.nextFloat() * 0.15f;
        return new PixelSprite(g, pal, pupil, 0.5f + rnd.nextFloat() * 0.5f);
    }
}
