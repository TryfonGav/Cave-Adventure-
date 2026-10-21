package com.caveadventure.world;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PaletteTest {

    @Test
    void packPaletteHas66DistinctColours() {
        Set<Integer> seen = new HashSet<>();
        for (int rgb : Palette.ALL)
            assertTrue(seen.add(rgb), String.format("duplicate %06x", rgb));
        assertEquals(66, seen.size());
    }

    @Test
    void everyNamedConstantIsAPackColour() throws IllegalAccessException {
        int named = 0;
        for (Field f : Palette.class.getFields()) {
            if (f.getType() == Color.class && Modifier.isStatic(f.getModifiers())) {
                Color c = (Color) f.get(null);
                assertTrue(Palette.contains(c), f.getName() + String.format(" %06x", Palette.rgb(c)));
                assertEquals(1f, c.a, f.getName() + " should be opaque; use Palette.alpha for overlays");
                named++;
            }
        }
        assertTrue(named >= 40, "named entries: " + named);
    }

    @Test
    void everyTileColourIsAPaletteEntry() {
        for (Tile tile : Tile.values())
            assertTrue(Palette.contains(tile.getColor()), tile.name());
    }

    @Test
    void everyBiomeColourIsAPaletteEntry() {
        for (Biome b : Biome.values()) {
            assertTrue(Palette.contains(b.floorColor), b + " floor");
            assertTrue(Palette.contains(b.wallColor), b + " wall");
            assertTrue(Palette.contains(b.wallShadeColor), b + " wall shade");
            assertTrue(Palette.contains(b.accentColor), b + " accent");
            assertTrue(Palette.contains(b.ambientColor), b + " ambient");
        }
    }

    @Test
    void hiddenTrapsLookExactlyLikeFloor() {
        assertEquals(Tile.FLOOR.getColor(), Tile.TRAP_HIDDEN.getColor());
    }

    @Test
    void alphaKeepsTheEntryAndDoesNotMutateIt() {
        Color glow = Palette.alpha(Palette.SEA, 0.3f);
        assertEquals(Palette.rgb(Palette.SEA), Palette.rgb(glow));
        assertEquals(0.3f, glow.a);
        assertEquals(1f, Palette.SEA.a);
        assertEquals(0x417089, Palette.rgb(Palette.SEA));
    }
}
