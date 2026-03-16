package com.caveadventure.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.caveadventure.CaveAdventure;
import com.caveadventure.entity.Enemy;
import com.caveadventure.entity.Player;
import com.caveadventure.world.GameMap;
import com.caveadventure.world.Tile;

import com.caveadventure.engine.SkillTree;
import java.util.List;

/**
 * Corner minimap that reveals explored areas and shows enemies, stairs, and the
 * player.
 */
public class Minimap {

    private final CaveAdventure game;
    private final OrthographicCamera camera;
    private boolean[][] explored;
    private int mapWidth, mapHeight;
    private SkillTree skillTree;

    private static final float MAP_SIZE = 160;
    private static final float MARGIN = 10;
    private static final int REVEAL_RADIUS = 6;

    public Minimap(CaveAdventure game) {
        this.game = game;
        this.camera = new OrthographicCamera();
    }

    public void setSkillTree(SkillTree skillTree) {
        this.skillTree = skillTree;
    }

    public void init(int width, int height) {
        this.mapWidth = width;
        this.mapHeight = height;
        this.explored = new boolean[width][height];
    }

    /**
     * Reveal tiles around the player's current position.
     */
    public void updateExplored(int playerX, int playerY) {
        for (int dx = -REVEAL_RADIUS; dx <= REVEAL_RADIUS; dx++) {
            for (int dy = -REVEAL_RADIUS; dy <= REVEAL_RADIUS; dy++) {
                int x = playerX + dx;
                int y = playerY + dy;
                if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
                    if (dx * dx + dy * dy <= REVEAL_RADIUS * REVEAL_RADIUS) {
                        explored[x][y] = true;
                    }
                }
            }
        }
    }

    public void render(GameMap gameMap, Player player, List<Enemy> enemies) {
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Position: bottom-right corner
        float mapX = screenW - MAP_SIZE - MARGIN;
        float mapY = MARGIN + 30; // Above control hints

        float tileW = MAP_SIZE / mapWidth;
        float tileH = MAP_SIZE / mapHeight;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Background
        game.shapeRenderer.setColor(0.02f, 0.02f, 0.05f, 0.75f);
        game.shapeRenderer.rect(mapX - 2, mapY - 2, MAP_SIZE + 4, MAP_SIZE + 4);

        // Border
        game.shapeRenderer.setColor(0.3f, 0.3f, 0.35f, 0.6f);
        game.shapeRenderer.rect(mapX - 2, mapY - 2, MAP_SIZE + 4, 2);
        game.shapeRenderer.rect(mapX - 2, mapY + MAP_SIZE, MAP_SIZE + 4, 2);
        game.shapeRenderer.rect(mapX - 2, mapY - 2, 2, MAP_SIZE + 4);
        game.shapeRenderer.rect(mapX + MAP_SIZE, mapY - 2, 2, MAP_SIZE + 4);

        // Draw explored tiles
        Tile[][] tiles = gameMap.getTiles();
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (!explored[x][y]) {
                    // TREASURE_SENSE: always show chests even if unexplored
                    if (skillTree != null && skillTree.hasSkill(SkillTree.Skill.TREASURE_SENSE)
                            && tiles[x][y] == Tile.CHEST) {
                        float px2 = mapX + x * tileW;
                        float py2 = mapY + y * tileH;
                        game.shapeRenderer.setColor(0.8f, 0.6f, 0.1f, 0.7f);
                        game.shapeRenderer.rect(px2, py2, Math.max(1, tileW), Math.max(1, tileH));
                    }
                    continue;
                }

                float px = mapX + x * tileW;
                float py = mapY + y * tileH;
                Tile tile = tiles[x][y];

                if (tile == Tile.WALL) {
                    game.shapeRenderer.setColor(0.25f, 0.2f, 0.18f, 0.8f);
                } else if (tile == Tile.FLOOR) {
                    game.shapeRenderer.setColor(0.12f, 0.12f, 0.1f, 0.6f);
                } else if (tile == Tile.STAIRS_DOWN) {
                    game.shapeRenderer.setColor(0.9f, 0.8f, 0.2f, 1f);
                } else if (tile == Tile.CHEST) {
                    game.shapeRenderer.setColor(0.8f, 0.6f, 0.1f, 0.9f);
                } else if (tile == Tile.DOOR_LOCKED) {
                    game.shapeRenderer.setColor(0.6f, 0.3f, 0.1f, 0.9f);
                } else if (tile == Tile.LAVA) {
                    game.shapeRenderer.setColor(0.8f, 0.2f, 0.05f, 0.8f);
                } else if (tile == Tile.WATER) {
                    game.shapeRenderer.setColor(0.1f, 0.3f, 0.6f, 0.7f);
                } else if (tile == Tile.TRAP_SPIKES || tile == Tile.TRAP_ARROW) {
                    game.shapeRenderer.setColor(0.5f, 0.1f, 0.1f, 0.7f);
                } else {
                    game.shapeRenderer.setColor(0.1f, 0.1f, 0.08f, 0.5f);
                }

                game.shapeRenderer.rect(px, py, Math.max(1, tileW), Math.max(1, tileH));
            }
        }

        // Draw enemies as red dots
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive())
                continue;
            int ex = enemy.getGridX();
            int ey = enemy.getGridY();
            if (ex >= 0 && ex < mapWidth && ey >= 0 && ey < mapHeight && explored[ex][ey]) {
                float px = mapX + ex * tileW;
                float py = mapY + ey * tileH;
                float dotSize = Math.max(2, tileW * 2);
                game.shapeRenderer.setColor(1f, 0.2f, 0.15f, 0.9f);
                game.shapeRenderer.rect(px, py, dotSize, dotSize);
            }
        }

        // Draw player as bright dot
        float ppx = mapX + player.getGridX() * tileW;
        float ppy = mapY + player.getGridY() * tileH;
        float playerDotSize = Math.max(3, tileW * 3);
        game.shapeRenderer.setColor(0.2f, 0.8f, 1f, 1f);
        game.shapeRenderer.rect(ppx - 1, ppy - 1, playerDotSize, playerDotSize);

        game.shapeRenderer.end();

        // Label
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        com.badlogic.gdx.graphics.g2d.BitmapFont smallFont = game.fontSmall != null ? game.fontSmall : game.font;
        smallFont.setColor(0.5f, 0.5f, 0.45f, 0.7f);
        smallFont.draw(game.batch, "MAP", mapX, mapY + MAP_SIZE + 16);
        game.batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
