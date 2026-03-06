package com.caveadventure.engine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Particle system for visual effects: torch flames, footstep dust, impact
 * sparks, ambient floats.
 */
public class ParticleSystem {

    public static class Particle {
        float x, y, vx, vy;
        float life, maxLife;
        float size;
        Color color;
        boolean alive;

        Particle(float x, float y, float vx, float vy, float life, float size, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.size = size;
            this.color = new Color(color);
            this.alive = true;
        }
    }

    private final List<Particle> particles;
    private final Random random;
    private static final int MAX_PARTICLES = 200;

    public ParticleSystem() {
        this.particles = new ArrayList<>();
        this.random = new Random();
    }

    // --- Emitters ---

    public void emitTorch(float x, float y) {
        if (particles.size() >= MAX_PARTICLES)
            return;
        for (int i = 0; i < 2; i++) {
            float vx = (random.nextFloat() - 0.5f) * 15;
            float vy = 15 + random.nextFloat() * 25;
            float life = 0.3f + random.nextFloat() * 0.5f;
            float size = 1 + random.nextFloat() * 2;
            Color c = random.nextFloat() < 0.5f
                    ? new Color(1f, 0.7f, 0.2f, 0.8f)
                    : new Color(1f, 0.4f, 0.1f, 0.7f);
            particles.add(new Particle(x, y, vx, vy, life, size, c));
        }
    }

    public void emitDust(float x, float y) {
        if (particles.size() >= MAX_PARTICLES)
            return;
        for (int i = 0; i < 3; i++) {
            float vx = (random.nextFloat() - 0.5f) * 20;
            float vy = random.nextFloat() * 10;
            float life = 0.3f + random.nextFloat() * 0.3f;
            float size = 1 + random.nextFloat() * 1.5f;
            particles.add(new Particle(x, y, vx, vy, life, size, new Color(0.5f, 0.45f, 0.35f, 0.5f)));
        }
    }

    public void emitImpact(float x, float y, Color color) {
        if (particles.size() >= MAX_PARTICLES)
            return;
        for (int i = 0; i < 8; i++) {
            float angle = random.nextFloat() * 6.28f;
            float speed = 30 + random.nextFloat() * 60;
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed;
            float life = 0.2f + random.nextFloat() * 0.3f;
            float size = 1 + random.nextFloat() * 2;
            particles.add(new Particle(x, y, vx, vy, life, size, new Color(color)));
        }
    }

    public void emitPoison(float x, float y) {
        if (particles.size() >= MAX_PARTICLES)
            return;
        float vx = (random.nextFloat() - 0.5f) * 8;
        float vy = 10 + random.nextFloat() * 15;
        float life = 0.5f + random.nextFloat() * 0.5f;
        particles.add(new Particle(x, y, vx, vy, life, 2 + random.nextFloat() * 2, new Color(0.3f, 0.9f, 0.2f, 0.6f)));
    }

    public void emitAmbient(float x, float y, Color color) {
        if (particles.size() >= MAX_PARTICLES)
            return;
        float vx = (random.nextFloat() - 0.5f) * 5;
        float vy = 3 + random.nextFloat() * 8;
        float life = 1f + random.nextFloat() * 2f;
        float size = 1 + random.nextFloat() * 1.5f;
        particles.add(new Particle(x, y, vx, vy, life, size, new Color(color.r, color.g, color.b, 0.3f)));
    }

    public void emitLevelUp(float x, float y) {
        for (int i = 0; i < 15; i++) {
            float angle = random.nextFloat() * 6.28f;
            float speed = 40 + random.nextFloat() * 40;
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed;
            float life = 0.5f + random.nextFloat() * 0.5f;
            float hue = random.nextFloat();
            Color c = new Color(0.8f + hue * 0.2f, 0.7f + hue * 0.3f, 0.2f, 1f);
            particles.add(new Particle(x, y, vx, vy, life, 2 + random.nextFloat() * 2, c));
        }
    }

    // --- Update & Render ---

    public void update(float delta) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.vy *= 0.98f; // slight drag
            p.life -= delta;
            if (p.life <= 0) {
                particles.remove(i);
            }
        }
    }

    public void render(ShapeRenderer renderer) {
        for (Particle p : particles) {
            float alpha = (p.life / p.maxLife) * p.color.a;
            float sizeFade = p.size * (p.life / p.maxLife);
            renderer.setColor(p.color.r, p.color.g, p.color.b, alpha);
            renderer.rect(p.x - sizeFade / 2, p.y - sizeFade / 2, sizeFade, sizeFade);
        }
    }

    public void clear() {
        particles.clear();
    }
}
