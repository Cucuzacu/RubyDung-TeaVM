package com.mojang.rubydung.level;

import com.mojang.rubydung.Textures;
import com.mojang.rubydung.phys.AABB;
import com.mojang.rubydung.RubyDung;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLRenderingContext;

public class Chunk {
   public AABB aabb;
   public final Level level;
   public final int x0;
   public final int y0;
   public final int z0;
   public final int x1;
   public final int y1;
   public final int z1;
   private boolean dirty = true;
   
   private WebGLBuffer[] vbo = new WebGLBuffer[2];
   private WebGLBuffer[] tbo = new WebGLBuffer[2];
   private WebGLBuffer[] cbo = new WebGLBuffer[2];
   private int[] vertexCounts = new int[2];
   
   private static int texture = Textures.loadTexture("/terrain.png", WebGLRenderingContext.NEAREST);
   private static Tesselator t = new Tesselator();
   public static int rebuiltThisFrame = 0;
   public static int updates = 0;

   public Chunk(Level level, int x0, int y0, int z0, int x1, int y1, int z1) {
      this.level = level;
      this.x0 = x0;
      this.y0 = y0;
      this.z0 = z0;
      this.x1 = x1;
      this.y1 = y1;
      this.z1 = z1;
      this.aabb = new AABB((float)x0, (float)y0, (float)z0, (float)x1, (float)y1, (float)z1);
   }

   private void rebuild(int layer) {
      if (rebuiltThisFrame != 2) {
         this.dirty = false;
         ++updates;
         ++rebuiltThisFrame;
         
         if (vbo[layer] == null) {
            vbo[layer] = RubyDung.gl.createBuffer();
            tbo[layer] = RubyDung.gl.createBuffer();
            cbo[layer] = RubyDung.gl.createBuffer();
         }
         
         t.init();
         int tiles = 0;

         for(int x = this.x0; x < this.x1; ++x) {
            for(int y = this.y0; y < this.y1; ++y) {
               for(int z = this.z0; z < this.z1; ++z) {
                  if (this.level.isTile(x, y, z)) {
                     int tex = y == this.level.depth * 2 / 3 ? 0 : 1;
                     ++tiles;
                     if (tex == 0) {
                        Tile.rock.render(t, this.level, layer, x, y, z);
                     } else {
                        Tile.grass.render(t, this.level, layer, x, y, z);
                     }
                  }
               }
            }
         }

         vertexCounts[layer] = t.compile(vbo[layer], tbo[layer], cbo[layer]);
      }
   }

   public void render(int layer) {
      if (this.dirty) {
         this.rebuild(0);
         this.rebuild(1);
      }

      if (vertexCounts[layer] > 0) {
         WebGLRenderingContext gl = RubyDung.gl;
         Textures.bind(texture);

         gl.uniform1i(RubyDung.uHasTexture, 1);
         gl.uniform1i(RubyDung.uHasColor, 1);
         
         gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vbo[layer]);
         gl.vertexAttribPointer(RubyDung.aPosition, 3, WebGLRenderingContext.FLOAT, false, 0, 0);
         gl.enableVertexAttribArray(RubyDung.aPosition);
         
         gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, tbo[layer]);
         gl.vertexAttribPointer(RubyDung.aTexCoord, 2, WebGLRenderingContext.FLOAT, false, 0, 0);
         gl.enableVertexAttribArray(RubyDung.aTexCoord);
         
         gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, cbo[layer]);
         gl.vertexAttribPointer(RubyDung.aColor, 3, WebGLRenderingContext.FLOAT, false, 0, 0);
         gl.enableVertexAttribArray(RubyDung.aColor);
         
         gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, RubyDung.quadEbo);
         gl.drawElements(WebGLRenderingContext.TRIANGLES, vertexCounts[layer] * 6 / 4, WebGLRenderingContext.UNSIGNED_SHORT, 0);
      }
   }

   public void setDirty() {
      this.dirty = true;
   }
}