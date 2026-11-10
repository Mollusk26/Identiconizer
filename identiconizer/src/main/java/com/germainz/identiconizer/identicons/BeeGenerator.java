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

/** Chubby front-facing bees: striped round body, see-through wings, antennae, stinger. */
public final class BeeGenerator {
    private static final int W = 14, H = 15, C0 = 6;

    private static final int[][] BODIES = {
            {3, 4, 4, 4, 4, 4, 3, 2},    // round
            {2, 3, 4, 4, 4, 4, 4, 3, 2}, // tall
            {3, 4, 5, 5, 5, 4, 3},       // bumble (wide)
            {2, 3, 4, 4, 4, 3, 3, 2},    // honeybee
    };

    private BeeGenerator() {
    }

    public static PixelSprite generate(byte[] hash) {
        Random rnd = new Random(PixelSprite.seed(hash, 0x62656521L));
        int[][] b = new int[H][W];
        int top = 4; // antennae above (row top-1 becomes the head outline)
        int[] body = BODIES[rnd.nextInt(BODIES.length)];
        for (int r = 0; r < body.length; r++)
            for (int i = 0; i < body[r]; i++) PixelSprite.mirror(b, top + r, C0 - i, PixelSprite.BODY);
        int bottom = top + body.length;

        // Face on the upper body
        int eyeRow = top + 1 + rnd.nextInt(2);
        int eyeX = C0 - 1 - (body[eyeRow - top] >= 4 && rnd.nextBoolean() ? 1 : 0);
        PixelSprite.mirror(b, eyeRow, eyeX, PixelSprite.EYE);
        if (rnd.nextFloat() < 0.6f) PixelSprite.mirror(b, eyeRow + 1, C0, PixelSprite.MOUTH);
        if (rnd.nextFloat() < 0.5f && eyeX - 1 > C0 - body[eyeRow + 1 - top])
            PixelSprite.mirror(b, eyeRow + 1, eyeX - 1, PixelSprite.BLUSH);

        // Stripes below the face, alternating dark bands
        int stripeStart = eyeRow + 2 + (rnd.nextFloat() < 0.4f ? 1 : 0);
        int gap = rnd.nextFloat() < 0.7f ? 2 : 3;
        boolean tailAccent = rnd.nextFloat() < 0.25f; // bumblebee with a colored tail
        for (int y = stripeStart; y < bottom; y += gap) {
            for (int i = 0; i < body[y - top]; i++) PixelSprite.mirror(b, y, C0 - i, PixelSprite.DARK);
        }
        if (tailAccent)
            for (int i = 0; i < body[body.length - 1]; i++)
                PixelSprite.mirror(b, bottom - 1, C0 - i, PixelSprite.ACCENT);

        // Stinger
        if (rnd.nextFloat() < 0.7f && bottom < H) PixelSprite.mirror(b, bottom, C0, PixelSprite.DARK);

        // Wings: pale ovals off the upper sides
        int wingRow = top + rnd.nextInt(2);
        int edge = C0 - body[Math.min(body.length - 1, wingRow - top + 1)]; // first empty col outside body
        int wingStyle = rnd.nextInt(3);
        int[][] wing;
        if (wingStyle == 0) wing = new int[][]{{-1, 0}, {-1, -1}, {0, 0}, {0, -1}, {0, -2}, {1, -1}};
        else if (wingStyle == 1) wing = new int[][]{{-2, 0}, {-1, 0}, {-1, -1}, {0, -1}, {0, -2}, {-2, -1}};
        else wing = new int[][]{{-1, 0}, {0, 0}, {0, -1}, {-1, -1}, {1, 0}};
        for (int[] w : wing) {
            int y = wingRow + w[0], x = edge + w[1];
            if (PixelSprite.get(b, y, x) == PixelSprite.EMPTY) PixelSprite.mirror(b, y, x, PixelSprite.WING);
        }

        // Antennae: thin dark lines (outline-colored, so they don't get a halo of their own)
        // Rooted on the head outline (row top-1) so they always connect to the head.
        int ax = Math.max(C0 - body[0] + 1, C0 - 1 - rnd.nextInt(2));
        int[][][] ANT = {
                {{-2, 0}, {-3, -1}, {-4, -2}},            // diagonal
                {{-2, 0}, {-3, 0}, {-4, -1}},             // tall, tips out
                {{-2, 0}, {-3, -1}, {-4, -1}, {-4, -2}},  // curled
                {{-2, 0}, {-3, 0}, {-3, -1}},             // short and bent
        };
        for (int[] a : ANT[rnd.nextInt(ANT.length)])
            PixelSprite.mirror(b, top + a[0], ax + a[1], PixelSprite.OUTLINE);

        int[][] g = PixelSprite.pad(b);
        PixelSprite.addOutline(g);

        int[] pal = new int[PixelSprite.CODES];
        float hue = rnd.nextFloat() < 0.8f ? 38f + rnd.nextFloat() * 16f : rnd.nextFloat() * 360f;
        float sat = 0.75f + rnd.nextFloat() * 0.2f, val = 0.93f + rnd.nextFloat() * 0.06f;
        pal[PixelSprite.BODY] = PixelSprite.hsv(hue, sat, val);
        pal[PixelSprite.DARK] = PixelSprite.hsv(hue, 0.45f, 0.17f);
        pal[PixelSprite.ACCENT] = PixelSprite.hsv(hue - 30f, 0.85f, 0.95f);
        pal[PixelSprite.WING] = PixelSprite.hsv(195f + rnd.nextFloat() * 30f, 0.12f + rnd.nextFloat() * 0.1f, 1f);
        pal[PixelSprite.OUTLINE] = PixelSprite.hsv(hue, 0.5f, 0.14f);
        pal[PixelSprite.MOUTH] = pal[PixelSprite.OUTLINE];
        pal[PixelSprite.BLUSH] = 0xFFF29080;
        pal[PixelSprite.EYE] = 0xFFFFFFFF;
        return new PixelSprite(g, pal, 0.55f + rnd.nextFloat() * 0.15f, 0.5f + rnd.nextFloat() * 0.5f);
    }
}
