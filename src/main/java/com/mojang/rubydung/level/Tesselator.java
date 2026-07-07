package com.mojang.rubydung.level;

import org.teavm.jso.JSBody;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLRenderingContext;
import com.mojang.rubydung.RubyDung;

public class Tesselator {
   private static final int MAX_VERTICES = 100000;
   private Float32Array vertexBuffer = Float32Array.create(300000);
   private Float32Array texCoordBuffer = Float32Array.create(200000);
   private Float32Array colorBuffer = Float32Array.create(300000);
   private int vertices = 0;
   private float u, v, r, g, b;
   private boolean hasColor = false;
   private boolean hasTexture = false;
   private WebGLBuffer vbo, tbo, cbo;

   public void flush() {
      if (this.vertices > 0) {
         WebGLRenderingContext gl = RubyDung.gl;
         if (this.vbo == null) {
            this.vbo = gl.createBuffer();
            this.tbo = gl.createBuffer();
            this.cbo = gl.createBuffer();
         }

         gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, this.vbo);
         gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, subarray(this.vertexBuffer, 0, this.vertices * 3), WebGLRenderingContext.DYNAMIC_DRAW);
         gl.vertexAttribPointer(RubyDung.aPosition, 3, WebGLRenderingContext.FLOAT, false, 0, 0);
         gl.enableVertexAttribArray(RubyDung.aPosition);

         if (this.hasTexture) {
            gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, this.tbo);
            gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, subarray(this.texCoordBuffer, 0, this.vertices * 2), WebGLRenderingContext.DYNAMIC_DRAW);
            gl.vertexAttribPointer(RubyDung.aTexCoord, 2, WebGLRenderingContext.FLOAT, false, 0, 0);
            gl.enableVertexAttribArray(RubyDung.aTexCoord);
            gl.uniform1i(RubyDung.uHasTexture, 1);
         } else { gl.uniform1i(RubyDung.uHasTexture, 0); }

         if (this.hasColor) {
            gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, this.cbo);
            gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, subarray(this.colorBuffer, 0, this.vertices * 3), WebGLRenderingContext.DYNAMIC_DRAW);
            gl.vertexAttribPointer(RubyDung.aColor, 3, WebGLRenderingContext.FLOAT, false, 0, 0);
            gl.enableVertexAttribArray(RubyDung.aColor);
            gl.uniform1i(RubyDung.uHasColor, 1);
         } else { gl.uniform1i(RubyDung.uHasColor, 0); }

         gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, RubyDung.quadEbo);
         gl.drawElements(WebGLRenderingContext.TRIANGLES, this.vertices * 6 / 4, WebGLRenderingContext.UNSIGNED_SHORT, 0);
      }
      this.clear();
   }

   public int compile(WebGLBuffer targetVbo, WebGLBuffer targetTbo, WebGLBuffer targetCbo) {
      WebGLRenderingContext gl = RubyDung.gl;
      gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, targetVbo);
      gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, subarray(this.vertexBuffer, 0, this.vertices * 3), WebGLRenderingContext.STATIC_DRAW);
      gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, targetTbo);
      gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, subarray(this.texCoordBuffer, 0, this.vertices * 2), WebGLRenderingContext.STATIC_DRAW);
      gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, targetCbo);
      gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, subarray(this.colorBuffer, 0, this.vertices * 3), WebGLRenderingContext.STATIC_DRAW);
      int count = this.vertices;
      this.clear();
      return count;
   }

   private void clear() {
      this.vertices = 0;
   }

   public void init() {
      this.clear();
      this.hasColor = false;
      this.hasTexture = false;
   }

   public void tex(float u, float v) {
      this.hasTexture = true;
      this.u = u;
      this.v = v;
   }

   public void color(float r, float g, float b) {
      this.hasColor = true;
      this.r = r;
      this.g = g;
      this.b = b;
   }

   public void vertex(float x, float y, float z) {
      this.vertexBuffer.set(this.vertices * 3 + 0, x);
      this.vertexBuffer.set(this.vertices * 3 + 1, y);
      this.vertexBuffer.set(this.vertices * 3 + 2, z);
      if (this.hasTexture) {
         this.texCoordBuffer.set(this.vertices * 2 + 0, this.u);
         this.texCoordBuffer.set(this.vertices * 2 + 1, this.v);
      }
      if (this.hasColor) {
         this.colorBuffer.set(this.vertices * 3 + 0, this.r);
         this.colorBuffer.set(this.vertices * 3 + 1, this.g);
         this.colorBuffer.set(this.vertices * 3 + 2, this.b);
      }
      ++this.vertices;
      if (this.vertices == 100000) {
         this.flush();
      }
   }

   @JSBody(params = {"array", "begin", "end"}, script = "return array.subarray(begin, end);")
   public static native Float32Array subarray(Float32Array array, int begin, int end);
}