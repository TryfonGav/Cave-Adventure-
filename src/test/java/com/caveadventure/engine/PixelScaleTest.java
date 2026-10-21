package com.caveadventure.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PixelScaleTest {

    @Test
    void windowed1280x800UsesScale3() {
        PixelScale p = PixelScale.forWindow(1280, 800);
        assertEquals(3, p.scale);
        assertEquals(427, p.bufferWidth);
        assertEquals(267, p.bufferHeight);
        assertEquals(0, p.offsetX);
        assertEquals(0, p.offsetY);
        assertEquals(16.69f, p.visibleTilesY(), 0.01f);
        assertEquals(854f, p.worldWidth());
        assertEquals(534f, p.worldHeight());
    }

    @Test
    void fullHdUsesScale3AndFitsExactly() {
        PixelScale p = PixelScale.forWindow(1920, 1080);
        assertEquals(3, p.scale);
        assertEquals(640, p.bufferWidth);
        assertEquals(360, p.bufferHeight);
        assertEquals(22.5f, p.visibleTilesY());
        assertEquals(40f, p.visibleTilesX());
    }

    @Test
    void largerWindowsPickTheScaleClosestTo20Tiles() {
        assertEquals(5, PixelScale.forWindow(2560, 1440).scale); // 18 tiles beats 22.5
        assertEquals(7, PixelScale.forWindow(3840, 2160).scale); // 19.3 tiles beats 22.5
        assertEquals(2, PixelScale.forWindow(1366, 768).scale); // 24 vs 16 tiles: a tie keeps the smaller scale
    }

    @Test
    void remainderIsCroppedEvenly() {
        PixelScale p = PixelScale.forWindow(3840, 2160);
        assertEquals(549, p.bufferWidth); // 549 * 7 = 3843
        assertEquals(309, p.bufferHeight); // 309 * 7 = 2163
        assertEquals(-1, p.offsetX);
        assertEquals(-1, p.offsetY);
    }

    @Test
    void bufferAlwaysCoversTheWindowWithSquarePixels() {
        for (int w = 320; w <= 4000; w += 37) {
            for (int h = 200; h <= 2400; h += 41) {
                PixelScale p = PixelScale.forWindow(w, h);
                assertTrue(p.scale >= 1);
                int coveredW = p.bufferWidth * p.scale;
                int coveredH = p.bufferHeight * p.scale;
                assertTrue(coveredW >= w && coveredW - w < p.scale, "width " + w + "x" + h);
                assertTrue(coveredH >= h && coveredH - h < p.scale, "height " + w + "x" + h);
                assertTrue(p.offsetX <= 0 && p.offsetX + coveredW >= w);
                assertTrue(p.offsetY <= 0 && p.offsetY + coveredH >= h);
            }
        }
    }

    @Test
    void tinyOrEmptyWindowsStillGiveAValidBuffer() {
        PixelScale tiny = PixelScale.forWindow(300, 200);
        assertEquals(1, tiny.scale);
        assertEquals(300, tiny.bufferWidth);
        PixelScale empty = PixelScale.forWindow(0, 0);
        assertEquals(1, empty.scale);
        assertEquals(1, empty.bufferWidth);
        assertEquals(1, empty.bufferHeight);
    }

    @Test
    void uiScaleStepsInWholeNumbersAndNeverDropsBelow2() {
        assertEquals(2, PixelScale.uiScale(600));
        assertEquals(2, PixelScale.uiScale(800));
        assertEquals(2, PixelScale.uiScale(1080));
        assertEquals(2, PixelScale.uiScale(1440));
        assertEquals(3, PixelScale.uiScale(1620));
        assertEquals(4, PixelScale.uiScale(2160));
    }

    @Test
    void snapRoundsToWholeArtPixels() {
        assertEquals(2f, PixelScale.WORLD_PER_ART_PIXEL);
        assertEquals(4f, PixelScale.snap(3.0f));
        assertEquals(2f, PixelScale.snap(2.9f));
        assertEquals(6f, PixelScale.snap(5.2f));
        assertEquals(-2f, PixelScale.snap(-3.0f));
        assertEquals(64f, PixelScale.snap(64f));
    }

    @Test
    void snappedCameraPutsTheViewEdgeOnTheArtGrid() {
        float viewport = PixelScale.forWindow(1280, 800).worldWidth(); // 854: half is an odd 427
        for (float centre = 400f; centre < 410f; centre += 0.37f) {
            float snapped = PixelScale.snapCameraCentre(centre, viewport);
            float leftEdge = snapped - viewport / 2f;
            assertEquals(0f, leftEdge % PixelScale.WORLD_PER_ART_PIXEL, 1e-3f, "centre " + centre);
            assertTrue(Math.abs(snapped - centre) <= PixelScale.WORLD_PER_ART_PIXEL / 2f + 1e-3f);
        }
    }
}
