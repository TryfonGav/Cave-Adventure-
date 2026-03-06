package com.caveadventure.world;

import java.util.*;

/**
 * Procedural cave generator using cellular automata.
 * 
 * Algorithm:
 * 1. Random fill with given wall probability
 * 2. Multiple smoothing passes (4-5 rule)
 * 3. Flood-fill to ensure connectivity
 * 4. Place features (chests, traps, stairs, water pools)
 */
public class CaveGenerator {

    private final int width;
    private final int height;
    private final float wallProbability;
    private final int smoothingPasses;
    private final Random random;

    public CaveGenerator(int width, int height, float wallProbability, int smoothingPasses) {
        this.width = width;
        this.height = height;
        this.wallProbability = wallProbability;
        this.smoothingPasses = smoothingPasses;
        this.random = new Random();
    }

    public CaveGenerator(int width, int height, float wallProbability, int smoothingPasses, long seed) {
        this(width, height, wallProbability, smoothingPasses);
        this.random.setSeed(seed);
    }

    /**
     * Generate a complete cave map with features.
     */
    public Tile[][] generate() {
        Tile[][] map = new Tile[width][height];

        // Step 1: Random fill
        randomFill(map);

        // Step 2: Smooth with cellular automata
        for (int i = 0; i < smoothingPasses; i++) {
            map = smooth(map);
        }

        // Step 3: Ensure borders are walls
        enforceBorders(map);

        // Step 4: Ensure connectivity via flood fill
        ensureConnectivity(map);

        // Step 5: Add visual wall variation
        addWallVariation(map);

        // Step 6: Place features
        placeFeatures(map);

        return map;
    }

    private void randomFill(Tile[][] map) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    map[x][y] = Tile.WALL;
                } else {
                    map[x][y] = random.nextFloat() < wallProbability ? Tile.WALL : Tile.FLOOR;
                }
            }
        }
    }

    private Tile[][] smooth(Tile[][] oldMap) {
        Tile[][] newMap = new Tile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int wallCount = countWallNeighbors(oldMap, x, y);
                if (wallCount > 4) {
                    newMap[x][y] = Tile.WALL;
                } else if (wallCount < 4) {
                    newMap[x][y] = Tile.FLOOR;
                } else {
                    newMap[x][y] = oldMap[x][y];
                }
            }
        }
        return newMap;
    }

    private int countWallNeighbors(Tile[][] map, int cx, int cy) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0)
                    continue;
                int nx = cx + dx;
                int ny = cy + dy;
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    count++; // Out of bounds counts as wall
                } else if (map[nx][ny].isSolid()) {
                    count++;
                }
            }
        }
        return count;
    }

    private void enforceBorders(Tile[][] map) {
        for (int x = 0; x < width; x++) {
            map[x][0] = Tile.WALL;
            map[x][height - 1] = Tile.WALL;
        }
        for (int y = 0; y < height; y++) {
            map[0][y] = Tile.WALL;
            map[width - 1][y] = Tile.WALL;
        }
    }

    /**
     * Find the largest connected floor region and fill all others with walls.
     */
    private void ensureConnectivity(Tile[][] map) {
        boolean[][] visited = new boolean[width][height];
        List<List<int[]>> regions = new ArrayList<>();

        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                if (!visited[x][y] && map[x][y] == Tile.FLOOR) {
                    List<int[]> region = floodFill(map, visited, x, y);
                    regions.add(region);
                }
            }
        }

        if (regions.isEmpty())
            return;

        // Find the largest region
        List<int[]> largest = regions.stream()
                .max(Comparator.comparingInt(List::size))
                .orElse(regions.get(0));

        // Fill all other regions with walls
        Set<String> largestSet = new HashSet<>();
        for (int[] pos : largest) {
            largestSet.add(pos[0] + "," + pos[1]);
        }

        for (List<int[]> region : regions) {
            if (region != largest) {
                // Try to connect small regions via tunnels if they're big enough
                if (region.size() > 15) {
                    connectRegions(map, region, largest);
                } else {
                    for (int[] pos : region) {
                        map[pos[0]][pos[1]] = Tile.WALL;
                    }
                }
            }
        }
    }

    private List<int[]> floodFill(Tile[][] map, boolean[][] visited, int startX, int startY) {
        List<int[]> region = new ArrayList<>();
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[] { startX, startY });
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            region.add(pos);

            int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
            for (int[] d : dirs) {
                int nx = pos[0] + d[0];
                int ny = pos[1] + d[1];
                if (nx > 0 && nx < width - 1 && ny > 0 && ny < height - 1
                        && !visited[nx][ny] && map[nx][ny] == Tile.FLOOR) {
                    visited[nx][ny] = true;
                    queue.add(new int[] { nx, ny });
                }
            }
        }
        return region;
    }

    /**
     * Dig a tunnel from one region to the closest point in another.
     */
    private void connectRegions(Tile[][] map, List<int[]> regionA, List<int[]> regionB) {
        int[] bestA = null, bestB = null;
        int bestDist = Integer.MAX_VALUE;

        // Sample points to find closest pair
        int sampleA = Math.min(regionA.size(), 20);
        int sampleB = Math.min(regionB.size(), 20);

        for (int i = 0; i < sampleA; i++) {
            int[] a = regionA.get(random.nextInt(regionA.size()));
            for (int j = 0; j < sampleB; j++) {
                int[] b = regionB.get(random.nextInt(regionB.size()));
                int dist = Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestA = a;
                    bestB = b;
                }
            }
        }

        if (bestA != null && bestB != null) {
            digTunnel(map, bestA[0], bestA[1], bestB[0], bestB[1]);
        }
    }

    private void digTunnel(Tile[][] map, int x1, int y1, int x2, int y2) {
        int x = x1, y = y1;
        while (x != x2 || y != y2) {
            if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                map[x][y] = Tile.FLOOR;
                // Make tunnel 2 wide for better navigation
                if (x + 1 < width - 1)
                    map[x + 1][y] = Tile.FLOOR;
            }
            if (random.nextBoolean()) {
                x += Integer.compare(x2, x);
            } else {
                y += Integer.compare(y2, y);
            }
        }
    }

    private void addWallVariation(Tile[][] map) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (map[x][y] == Tile.WALL && random.nextFloat() < 0.3f) {
                    map[x][y] = Tile.WALL_DARK;
                }
            }
        }
    }

    /**
     * Place interactive features: chests, traps, water pools, stairs.
     */
    private void placeFeatures(Tile[][] map) {
        List<int[]> floorTiles = new ArrayList<>();
        for (int x = 2; x < width - 2; x++) {
            for (int y = 2; y < height - 2; y++) {
                if (map[x][y] == Tile.FLOOR) {
                    floorTiles.add(new int[] { x, y });
                }
            }
        }

        if (floorTiles.isEmpty())
            return;
        Collections.shuffle(floorTiles, random);

        int featureIndex = 0;

        // Place 3-5 chests
        int chestCount = 3 + random.nextInt(3);
        for (int i = 0; i < chestCount && featureIndex < floorTiles.size(); i++) {
            int[] pos = floorTiles.get(featureIndex++);
            if (isDeadEnd(map, pos[0], pos[1]) || random.nextFloat() < 0.5f) {
                map[pos[0]][pos[1]] = Tile.CHEST;
            }
        }

        // Place 4-8 spike traps
        int trapCount = 4 + random.nextInt(5);
        for (int i = 0; i < trapCount && featureIndex < floorTiles.size(); i++) {
            int[] pos = floorTiles.get(featureIndex++);
            // Some traps are visible, some are hidden
            map[pos[0]][pos[1]] = random.nextFloat() < 0.4f ? Tile.TRAP_SPIKE : Tile.TRAP_HIDDEN;
        }

        // Place 1-2 water pools (clusters)
        int poolCount = 1 + random.nextInt(2);
        for (int p = 0; p < poolCount && featureIndex < floorTiles.size(); p++) {
            int[] center = floorTiles.get(featureIndex++);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    int wx = center[0] + dx;
                    int wy = center[1] + dy;
                    if (wx > 0 && wx < width - 1 && wy > 0 && wy < height - 1
                            && map[wx][wy] == Tile.FLOOR
                            && Math.abs(dx) + Math.abs(dy) <= 3
                            && random.nextFloat() < 0.7f) {
                        map[wx][wy] = Tile.WATER;
                    }
                }
            }
        }

        // Place 1 lava pool
        if (featureIndex < floorTiles.size()) {
            int[] center = floorTiles.get(featureIndex++);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int lx = center[0] + dx;
                    int ly = center[1] + dy;
                    if (lx > 0 && lx < width - 1 && ly > 0 && ly < height - 1
                            && map[lx][ly] == Tile.FLOOR && random.nextFloat() < 0.6f) {
                        map[lx][ly] = Tile.LAVA;
                    }
                }
            }
        }

        // Place stairs at a distant floor tile
        if (featureIndex < floorTiles.size()) {
            int[] pos = floorTiles.get(floorTiles.size() - 1); // furthest from start features
            map[pos[0]][pos[1]] = Tile.STAIRS_DOWN;
        }
    }

    private boolean isDeadEnd(Tile[][] map, int x, int y) {
        int openNeighbors = 0;
        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        for (int[] d : dirs) {
            if (map[x + d[0]][y + d[1]] == Tile.FLOOR)
                openNeighbors++;
        }
        return openNeighbors <= 1;
    }
}
