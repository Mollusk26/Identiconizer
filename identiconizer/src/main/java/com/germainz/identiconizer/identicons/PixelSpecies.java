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
 * Picks which kind of pixel creature a contact gets from its hash, so the choice is stable
 * per contact. Each species has equal odds. Every generator seeds its own random stream from
 * the whole hash plus its own salt.
 */
public final class PixelSpecies {
    public static final int CRITTER = 0, MUSHROOM = 1, BEETLE = 2, BIRD = 3, BEE = 4;
    public static final int COUNT = 5;

    private PixelSpecies() {
    }

    public static int speciesOf(byte[] hash) {
        return (hash[hash.length - 1] & 0xFF) % COUNT;
    }

    public static PixelSprite generate(byte[] hash) {
        switch (speciesOf(hash)) {
            case MUSHROOM:
                return MushroomGenerator.generate(hash);
            case BEETLE:
                return BeetleGenerator.generate(hash);
            case BIRD:
                return BirdGenerator.generate(hash);
            case BEE:
                return BeeGenerator.generate(hash);
            case CRITTER:
            default:
                return PixelCreatureGenerator.generateSprite(hash);
        }
    }
}
