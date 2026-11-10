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
 * Mirrored pixel-art creatures: critters, mushroom folk, beetles, birds and bees, with the
 * species picked per contact by {@link PixelSpecies}. This class only draws the sprite. Each creature is scaled to the largest size whose farthest
 * corner still fits inside the circle most dialers crop contact photos to.
 */
public class PixelCreatureIdenticon extends Identicon {

    /** Fraction of the avatar's radius the creature may reach (a hair of breathing room). */
    private static final float FILL = 0.96f;

    @Override
    public Bitmap generateIdenticonBitmap(byte[] hash) {
        if (hash == null || hash.length != 16)
            return null;

        PixelSprite r = PixelSpecies.generate(hash);

        Bitmap bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(BG_COLOR);

        float cell = (SIZE / 2f) * FILL / r.radiusCells;
        float offsetX = SIZE / 2f - r.centerX * cell;
        float offsetY = SIZE / 2f - r.centerY * cell;
        Paint paint = new Paint();
        paint.setAntiAlias(false);
        paint.setStyle(Paint.Style.FILL);
        RectF rect = new RectF();

        int[][] cells = r.cells;
        for (int y = 0; y < cells.length; y++) {
            for (int x = 0; x < cells[y].length; x++) {
                int v = cells[y][x];
                if (v == PixelSprite.EMPTY) continue;
                paint.setColor(r.palette[v]);
                float left = offsetX + x * cell;
                float top = offsetY + y * cell;
                // Round edges to whole pixels so neighbouring cells never leave hairline gaps.
                rect.set(Math.round(left), Math.round(top),
                        Math.round(left + cell), Math.round(top + cell));
                canvas.drawRect(rect, paint);
                if (v == PixelSprite.EYE) {
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
