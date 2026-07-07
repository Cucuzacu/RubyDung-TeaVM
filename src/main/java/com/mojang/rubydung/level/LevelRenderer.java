package com.mojang.rubydung.level;

import com.mojang.rubydung.HitResult;
import com.mojang.rubydung.Player;
import com.mojang.rubydung.RubyDung;
import com.mojang.rubydung.phys.AABB;
import org.teavm.jso.webgl.WebGLRenderingContext;

public class LevelRenderer implements LevelListener {
   private static final int CHUNK_SIZE = 16;
   private Level level;
   private Chunk[] chunks;
   private int xChunks;
   private int yChunks;
   private int zChunks;
   Tesselator t = new Tesselator();

   public LevelRenderer(Level level) {
      this.level = level;
      level.addListener(this);
      this.xChunks = level.width / 16;
      this.yChunks = level.depth / 16;
      this.zChunks = level.height / 16;
      this.chunks = new Chunk[this.xChunks * this.yChunks * this.zChunks];

      for(int x = 0; x < this.xChunks; ++x) {
         for(int y = 0; y < this.yChunks; ++y) {
            for(int z = 0; z < this.zChunks; ++z) {
               int x0 = x * 16; int y0 = y * 16; int z0 = z * 16;
               int x1 = (x + 1) * 16; int y1 = (y + 1) * 16; int z1 = (z + 1) * 16;
               if (x1 > level.width) x1 = level.width;
               if (y1 > level.depth) y1 = level.depth;
               if (z1 > level.height) z1 = level.height;
               this.chunks[(x + y * this.xChunks) * this.zChunks + z] = new Chunk(level, x0, y0, z0, x1, y1, z1);
            }
         }
      }
   }

   public void render(Player player, int layer) {
      Chunk.rebuiltThisFrame = 0;
      Frustum frustum = Frustum.getFrustum();

      for(int i = 0; i < this.chunks.length; ++i) {
         if (frustum.cubeInFrustum(this.chunks[i].aabb)) {
            this.chunks[i].render(layer);
         }
      }
   }

   public void pick(Player player) {
      float x = player.x;
      float y = player.y;
      float z = player.z;
    
      float cosX = (float) Math.cos(Math.toRadians(-player.xRot));
      float sinX = (float) Math.sin(Math.toRadians(-player.xRot));
      float cosY = (float) Math.cos(Math.toRadians(player.yRot));
      float sinY = (float) Math.sin(Math.toRadians(player.yRot));

      float dx = sinY * cosX;
      float dy = sinX;
      float dz = -cosY * cosX;

      float reach = 5.0f;

      int voxX = (int) Math.floor(x);
      int voxY = (int) Math.floor(y);
      int voxZ = (int) Math.floor(z);

      int stepX = (dx > 0) ? 1 : -1;
      int stepY = (dy > 0) ? 1 : -1;
      int stepZ = (dz > 0) ? 1 : -1;

      float deltaX = (dx != 0) ? Math.abs(1.0f / dx) : Float.MAX_VALUE;
      float deltaY = (dy != 0) ? Math.abs(1.0f / dy) : Float.MAX_VALUE;
      float deltaZ = (dz != 0) ? Math.abs(1.0f / dz) : Float.MAX_VALUE;

      float maxX = (dx > 0) ? (voxX + 1 - x) * deltaX : (x - voxX) * deltaX;
      float maxY = (dy > 0) ? (voxY + 1 - y) * deltaY : (y - voxY) * deltaY;
      float maxZ = (dz > 0) ? (voxZ + 1 - z) * deltaZ : (z - voxZ) * deltaZ;

      int face = -1;
      float distance = 0;

      while (distance < reach) {
          if (level.isSolidTile(voxX, voxY, voxZ)) {
              RubyDung.hitResult = new HitResult(voxX, voxY, voxZ, 0, face);
              return;
          }

          if (maxX < maxY) {
              if (maxX < maxZ) {
                  distance = maxX;
                  maxX += deltaX;
                  voxX += stepX;
                  face = (stepX > 0) ? 4 : 5;
              } else {
                  distance = maxZ;
                  maxZ += deltaZ;
                  voxZ += stepZ;
                  face = (stepZ > 0) ? 2 : 3;
              }
          } else {
              if (maxY < maxZ) {
                  distance = maxY;
                  maxY += deltaY;
                  voxY += stepY;
                  face = (stepY > 0) ? 0 : 1;
              } else {
                  distance = maxZ;
                  maxZ += deltaZ;
                  voxZ += stepZ;
                  face = (stepZ > 0) ? 2 : 3;
              }
          }
      }

      RubyDung.hitResult = null;
   }

   public void renderHit(HitResult h) {
      WebGLRenderingContext gl = RubyDung.gl;
      gl.enable(WebGLRenderingContext.BLEND);
      gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE);
      gl.uniform1f(RubyDung.uAlpha, (float)Math.sin((double)System.currentTimeMillis() / (double)100.0F) * 0.2F + 0.4F);
      this.t.init();
      Tile.rock.renderFace(this.t, h.x, h.y, h.z, h.f);
      this.t.flush();
      gl.disable(WebGLRenderingContext.BLEND);
      gl.uniform1f(RubyDung.uAlpha, 1.0F);
   }

   public void setDirty(int x0, int y0, int z0, int x1, int y1, int z1) {
      x0 /= 16; x1 /= 16; y0 /= 16; y1 /= 16; z0 /= 16; z1 /= 16;
      if (x0 < 0) x0 = 0;
      if (y0 < 0) y0 = 0;
      if (z0 < 0) z0 = 0;
      if (x1 >= this.xChunks) x1 = this.xChunks - 1;
      if (y1 >= this.yChunks) y1 = this.yChunks - 1;
      if (z1 >= this.zChunks) z1 = this.zChunks - 1;

      for(int x = x0; x <= x1; ++x) {
         for(int y = y0; y <= y1; ++y) {
            for(int z = z0; z <= z1; ++z) {
               this.chunks[(x + y * this.xChunks) * this.zChunks + z].setDirty();
            }
         }
      }
   }

   public void tileChanged(int x, int y, int z) {
      this.setDirty(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
   }

   public void lightColumnChanged(int x, int z, int y0, int y1) {
      this.setDirty(x - 1, y0 - 1, z - 1, x + 1, y1 + 1, z + 1);
   }

   public void allChanged() {
      this.setDirty(0, 0, 0, this.level.width, this.level.depth, this.level.height);
   }
}