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

/**
 * A generated pixel-art avatar: a grid of cell codes plus a palette indexed by code.
 * Pure Java so generators can be previewed on a desktop JVM.
 */
public final class PixelSprite {
    public static final int EMPTY = 0, OUTLINE = 1, BODY = 2, ACCENT = 3, EYE = 4, MOUTH = 5,
            SPOT = 6, STEM = 7, DARK = 8, SHINE = 9, BLUSH = 10, GILL = 11, BEAK = 12, WING = 13;
    public static final int CODES = 14;

    public final int[][] cells;
    public final int[] palette;
    public final int pupilColor = 0xFF1A1A1A;
    public final float pupilSize, pupilDrop;
    public final float centerX, centerY, radiusCells;

    public PixelSprite(int[][] cells, int[] palette, float pupilSize, float pupilDrop) {
        this.cells = cells;
        this.palette = palette;
        this.pupilSize = pupilSize;
        this.pupilDrop = pupilDrop;
        int h = cells.length, w = cells[0].length;
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (cells[y][x] != EMPTY) {
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                }
        centerX = (minX + maxX + 1) / 2f;
        centerY = (minY + maxY + 1) / 2f;
        float r = 0;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (cells[y][x] != EMPTY)
                    for (int cy = y; cy <= y + 1; cy++)
                        for (int cx = x; cx <= x + 1; cx++)
                            r = Math.max(r, (float) Math.hypot(cx - centerX, cy - centerY));
        radiusCells = r;
    }

    /** Sets a cell and its horizontal mirror (works for odd and even widths). */
    static void mirror(int[][] g, int y, int x, int v) {
        if (y < 0 || y >= g.length || x < 0 || x >= g[0].length) return;
        g[y][x] = v;
        g[y][g[0].length - 1 - x] = v;
    }

    static int get(int[][] g, int y, int x) {
        if (y < 0 || y >= g.length || x < 0 || x >= g[0].length) return EMPTY;
        return g[y][x];
    }

    /** Copies a body into a grid with one empty cell of padding on every side. */
    static int[][] pad(int[][] body) {
        int[][] g = new int[body.length + 2][body[0].length + 2];
        for (int y = 0; y < body.length; y++)
            System.arraycopy(body[y], 0, g[y + 1], 1, body[y].length);
        return g;
    }

    /** Empty cells touching (8-way) any filled, non-outline cell become outline. */
    static void addOutline(int[][] g) {
        int h = g.length, w = g[0].length;
        boolean[][] mark = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                if (g[y][x] != EMPTY) continue;
                for (int dy = -1; dy <= 1 && !mark[y][x]; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int c = get(g, y + dy, x + dx);
                        if (c != EMPTY && c != OUTLINE) { mark[y][x] = true; break; }
                    }
            }
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (mark[y][x]) g[y][x] = OUTLINE;
    }

    static long seed(byte[] hash, long salt) {
        long s = salt;
        for (byte b : hash) s = s * 31 + (b & 0xFF);
        return s;
    }

    static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    static int hsv(float h, float s, float v) {
        h = ((h % 360f) + 360f) % 360f;
        s = clamp(s); v = clamp(v);
        float c = v * s, hp = h / 60f, x = c * (1 - Math.abs(hp % 2 - 1));
        float r = 0, g = 0, b = 0;
        if (hp < 1) { r = c; g = x; } else if (hp < 2) { r = x; g = c; }
        else if (hp < 3) { g = c; b = x; } else if (hp < 4) { g = x; b = c; }
        else if (hp < 5) { r = x; b = c; } else { r = c; b = x; }
        float m = v - c;
        return 0xFF000000 | (Math.round((r + m) * 255) << 16)
                | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }
}
