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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;

/**
 * Mirrored pixel-art creature. The shape and colors come from {@link PixelCreatureGenerator};
 * this class only draws them. The creature is drawn with a 1.5-cell margin so circular
 * avatar crops don't clip it.
 */
public class PixelCreatureIdenticon extends Identicon {

    private static final float CANVAS_UNITS = PixelCreatureGenerator.GRID + 3f;
    private static final float MARGIN_UNITS = 1.5f;

    @Override
    public Bitmap generateIdenticonBitmap(byte[] hash) {
        if (hash == null || hash.length != 16)
            return null;

        PixelCreatureGenerator.Result r = PixelCreatureGenerator.generate(hash);

        Bitmap bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(BG_COLOR);

        float cell = SIZE / CANVAS_UNITS;
        float offset = cell * MARGIN_UNITS;
        Paint paint = new Paint();
        paint.setAntiAlias(false);
        paint.setStyle(Paint.Style.FILL);
        RectF rect = new RectF();

        int[][] cells = r.cells;
        for (int y = 0; y < cells.length; y++) {
            for (int x = 0; x < cells[y].length; x++) {
                int v = cells[y][x];
                if (v == PixelCreatureGenerator.EMPTY) continue;
                paint.setColor(colorFor(v, r));
                float left = offset + x * cell;
                float top = offset + y * cell;
                // Round edges to whole pixels so neighbouring cells never leave hairline gaps.
                rect.set(Math.round(left), Math.round(top),
                        Math.round(left + cell), Math.round(top + cell));
                canvas.drawRect(rect, paint);
                if (v == PixelCreatureGenerator.EYE) {
                    float p = cell * r.pupilSize;
                    float pl = left + (cell - p) / 2f;
                    float pt = top + (cell - p) * r.pupilDrop;
                    paint.setColor(r.pupilColor);
                    rect.set(Math.round(pl), Math.round(pt), Math.round(pl + p), Math.round(pt + p));
                    canvas.drawRect(rect, paint);
                }
            }
        }
        return bmp;
    }

    private static int colorFor(int v, PixelCreatureGenerator.Result r) {
        switch (v) {
            case PixelCreatureGenerator.BODY:
                return r.bodyColor;
            case PixelCreatureGenerator.ACCENT:
                return r.accentColor;
            case PixelCreatureGenerator.EYE:
                return r.eyeColor;
            case PixelCreatureGenerator.OUTLINE:
            case PixelCreatureGenerator.MOUTH:
            default:
                return r.outlineColor;
        }
    }

    @Override
    public byte[] generateIdenticonByteArray(byte[] hash) {
        return bitmapToByteArray(generateIdenticonBitmap(hash));
    }

    @Override
    public Bitmap generateIdenticonBitmap(String key) {
        if (TextUtils.isEmpty(key)) return null;
        return generateIdenticonBitmap(generateHash(saltedKey(key)));
    }

    @Override
    public byte[] generateIdenticonByteArray(String key) {
        if (TextUtils.isEmpty(key)) return null;
        return generateIdenticonByteArray(generateHash(saltedKey(key)));
    }
}
