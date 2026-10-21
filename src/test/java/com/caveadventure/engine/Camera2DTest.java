package com.caveadventure.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Camera2DTest {

    @Test
    void clampKeepsViewInsideLargeMap() {
        assertEquals(480f, Camera2D.clampAxis(100f, 480f, 2560f));
        assertEquals(2080f, Camera2D.clampAxis(3000f, 480f, 2560f));
        assertEquals(1000f, Camera2D.clampAxis(1000f, 480f, 2560f));
    }

    @Test
    void mapSmallerThanViewIsCentred() {
        // Final-boss floor: 31 tiles x 32 px = 992 px, narrower than a 1024 px wide view.
        assertEquals(496f, Camera2D.clampAxis(0f, 512f, 992f));
        assertEquals(496f, Camera2D.clampAxis(900f, 512f, 992f));
    }
}
