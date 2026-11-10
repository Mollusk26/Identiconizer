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

import java.util.ArrayDeque;
import java.util.Random;

/**
 * Generates a mirrored "pixel creature" from a 16-byte hash.
 * <p>
 * Pure Java (no Android imports) so the exact same logic can be run on a desktop JVM to
 * render previews. java.util.Random's algorithm is fixed by the Java spec, so a given hash
 * produces the same creature everywhere.
 * <p>
 * Approach: a half-width template marks which cells are always body, never body, or
 * randomly body. The half is mirrored, everything not connected to the creature's core is
 * dropped, eyes and an optional mouth are placed, and empty cells touching the body become
 * a dark outline.
 */
public final class PixelCreatureGenerator {

    public static final int EMPTY = 0;
    public static final int OUTLINE = 1;
    public static final int BODY = 2;
    public static final int ACCENT = 3;
    public static final int EYE = 4;
    public static final int MOUTH = 5;

    /** Creature body is 8x8; one cell of padding on each side leaves room for the outline. */
    public static final int GRID = 10;
    private static final int BODY_W = 8;
    private static final int BODY_H = 8;
    private static final int HALF_W = BODY_W / 2;

    // Template for the left half (column 3 is next to the mirror axis).
    // 0 = never body, 1 = extremity (50%, drawn in accent color), 2 = body (60%),
    // 3 = always body.
    private static final int[][] TEMPLATE = {
            {0, 1, 0, 1},
            {0, 1, 2, 2},
            {1, 2, 2, 3},
            {1, 2, 3, 3},
            {1, 2, 3, 3},
            {0, 2, 2, 3},
            {0, 1, 2, 2},
            {1, 1, 0, 1},
    };

    private static final int MIN_BODY_CELLS = 22;

    public static final class Result {
        /** cells[y][x], GRID x GRID, values are EMPTY/OUTLINE/BODY/ACCENT/EYE/MOUTH. */
        public final int[][] cells;
        public final int bodyColor;
        public final int accentColor;
        public final int outlineColor;
        public final int eyeColor;
        public final int pupilColor;
        /** Pupil size as a fraction of a cell, and its vertical offset (0 = top, 1 = bottom). */
        public final float pupilSize;
        public final float pupilDrop;
        /** Center of the creature's bounding box, in cell units (grid coordinates). */
        public final float centerX;
        public final float centerY;
        /**
         * Distance, in cell units, from the center to the farthest corner of any filled cell.
         * Scaling so this equals the avatar's radius makes the creature as large as possible
         * while staying inside a circular crop.
         */
        public final float radiusCells;

        Result(int[][] cells, int bodyColor, int accentColor, int outlineColor,
               float pupilSize, float pupilDrop) {
            this.cells = cells;
            this.bodyColor = bodyColor;
            this.accentColor = accentColor;
            this.outlineColor = outlineColor;
            this.eyeColor = 0xFFFFFFFF;
            this.pupilColor = 0xFF1A1A1A;
            this.pupilSize = pupilSize;
            this.pupilDrop = pupilDrop;

            int minX = GRID, minY = GRID, maxX = -1, maxY = -1;
            for (int y = 0; y < GRID; y++)
                for (int x = 0; x < GRID; x++)
                    if (cells[y][x] != EMPTY) {
                        minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    }
            this.centerX = (minX + maxX + 1) / 2f;
            this.centerY = (minY + maxY + 1) / 2f;
            float r = 0;
            for (int y = 0; y < GRID; y++)
                for (int x = 0; x < GRID; x++)
                    if (cells[y][x] != EMPTY)
                        for (int cy = y; cy <= y + 1; cy++)
                            for (int cx = x; cx <= x + 1; cx++)
                                r = Math.max(r, (float) Math.hypot(cx - centerX, cy - centerY));
            this.radiusCells = r;
        }
    }

    private PixelCreatureGenerator() {
    }

    public static Result generate(byte[] hash) {
        Random rnd = new Random(seedFromHash(hash));

        int[][] body = null;
        // Retry a few times if the creature comes out too small; each attempt draws new
        // values from the same seeded stream, so the result is still deterministic.
        for (int attempt = 0; attempt < 8; attempt++) {
            body = buildBody(rnd);
            if (countFilled(body) >= MIN_BODY_CELLS) break;
        }

        placeFace(body, rnd);

        int[][] cells = new int[GRID][GRID];
        for (int y = 0; y < BODY_H; y++)
            for (int x = 0; x < BODY_W; x++)
                cells[y + 1][x + 1] = body[y][x];
        addOutline(cells);

        float hue = rnd.nextFloat() * 360f;
        float sat = 0.55f + rnd.nextFloat() * 0.25f;
        float val = 0.80f + rnd.nextFloat() * 0.15f;
        float accentShift = (rnd.nextBoolean() ? 1 : -1) * (25f + rnd.nextFloat() * 35f);
        int bodyColor = hsvToArgb(hue, sat, val);
        int accentColor = hsvToArgb((hue + accentShift + 360f) % 360f,
                Math.min(1f, sat + 0.1f), Math.max(0f, val - 0.12f));
        int outlineColor = hsvToArgb(hue, Math.min(1f, sat + 0.15f), 0.28f);

        float pupilSize = 0.45f + rnd.nextFloat() * 0.2f;
        float pupilDrop = 0.5f + rnd.nextFloat() * 0.5f;

        return new Result(cells, bodyColor, accentColor, outlineColor, pupilSize, pupilDrop);
    }

    private static int[][] buildBody(Random rnd) {
        int[][] body = new int[BODY_H][BODY_W];
        for (int y = 0; y < BODY_H; y++) {
            for (int x = 0; x < HALF_W; x++) {
                int t = TEMPLATE[y][x];
                int v;
                switch (t) {
                    case 3:
                        v = BODY;
                        break;
                    case 2:
                        v = rnd.nextFloat() < 0.6f ? BODY : EMPTY;
                        break;
                    case 1:
                        v = rnd.nextBoolean() ? ACCENT : EMPTY;
                        break;
                    default:
                        v = EMPTY;
                }
                body[y][x] = v;
                body[y][BODY_W - 1 - x] = v;
            }
        }
        keepCoreComponent(body);
        return body;
    }

    /** Drops any cells not connected (4-way) to the always-filled core of the creature. */
    private static void keepCoreComponent(int[][] body) {
        boolean[][] seen = new boolean[BODY_H][BODY_W];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        int startY = 3, startX = HALF_W - 1; // a TEMPLATE "3" cell, always body
        seen[startY][startX] = true;
        queue.add(new int[]{startY, startX});
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (int[] d : dirs) {
                int ny = p[0] + d[0], nx = p[1] + d[1];
                if (ny < 0 || nx < 0 || ny >= BODY_H || nx >= BODY_W) continue;
                if (seen[ny][nx] || body[ny][nx] == EMPTY) continue;
                seen[ny][nx] = true;
                queue.add(new int[]{ny, nx});
            }
        }
        for (int y = 0; y < BODY_H; y++)
            for (int x = 0; x < BODY_W; x++)
                if (!seen[y][x]) body[y][x] = EMPTY;
    }

    private static void placeFace(int[][] body, Random rnd) {
        // Eyes: a mirrored pair on row 2-4, one or two cells in from the mirror axis.
        int eyeRow = 2 + rnd.nextInt(3);
        int eyeCol = 1 + rnd.nextInt(2); // 1 or 2 in half-coordinates
        body[eyeRow][eyeCol] = EYE;
        body[eyeRow][BODY_W - 1 - eyeCol] = EYE;
        // Make sure the eyes sit inside the body rather than floating at the edge.
        fillIfEmpty(body, eyeRow, eyeCol + 1);
        fillIfEmpty(body, eyeRow, BODY_W - 2 - eyeCol);
        fillIfEmpty(body, eyeRow + 1, eyeCol);
        fillIfEmpty(body, eyeRow + 1, BODY_W - 1 - eyeCol);

        // Mouth: two center cells, two rows under the eyes, on about 60% of creatures.
        int mouthRow = eyeRow + 2;
        if (mouthRow < BODY_H - 1 && rnd.nextFloat() < 0.6f) {
            body[mouthRow][HALF_W - 1] = MOUTH;
            body[mouthRow][HALF_W] = MOUTH;
            // A wide grin on some of them.
            if (rnd.nextFloat() < 0.35f && body[mouthRow][HALF_W - 2] != EMPTY) {
                body[mouthRow][HALF_W - 2] = MOUTH;
                body[mouthRow][HALF_W + 1] = MOUTH;
            }
        }
    }

    private static void fillIfEmpty(int[][] body, int y, int x) {
        if (y >= 0 && x >= 0 && y < BODY_H && x < BODY_W && body[y][x] == EMPTY)
            body[y][x] = BODY;
    }

    /** Empty cells that touch the creature (8-way) become outline. */
    private static void addOutline(int[][] cells) {
        int[][] out = new int[GRID][GRID];
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                out[y][x] = cells[y][x];
                if (cells[y][x] != EMPTY) continue;
                outer:
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int ny = y + dy, nx = x + dx;
                        if (ny < 0 || nx < 0 || ny >= GRID || nx >= GRID) continue;
                        int c = cells[ny][nx];
                        if (c != EMPTY && c != OUTLINE) {
                            out[y][x] = OUTLINE;
                            break outer;
                        }
                    }
                }
            }
        }
        for (int y = 0; y < GRID; y++)
            System.arraycopy(out[y], 0, cells[y], 0, GRID);
    }

    private static int countFilled(int[][] body) {
        int n = 0;
        for (int[] row : body)
            for (int c : row)
                if (c != EMPTY) n++;
        return n;
    }

    private static long seedFromHash(byte[] hash) {
        long seed = 0;
        for (int i = 0; i < hash.length; i++)
            seed = seed * 31 + (hash[i] & 0xFF);
        return seed;
    }

    /** HSV (h in degrees, s and v in 0..1) to opaque ARGB. */
    static int hsvToArgb(float h, float s, float v) {
        float c = v * s;
        float hp = (h % 360f) / 60f;
        float x = c * (1 - Math.abs(hp % 2 - 1));
        float r = 0, g = 0, b = 0;
        if (hp < 1) { r = c; g = x; }
        else if (hp < 2) { r = x; g = c; }
        else if (hp < 3) { g = c; b = x; }
        else if (hp < 4) { g = x; b = c; }
        else if (hp < 5) { r = x; b = c; }
        else { r = c; b = x; }
        float m = v - c;
        int ri = Math.round((r + m) * 255), gi = Math.round((g + m) * 255), bi = Math.round((b + m) * 255);
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }
}
