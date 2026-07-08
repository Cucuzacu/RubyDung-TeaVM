package com.mojang.rubydung;

import com.mojang.rubydung.level.Chunk;
import com.mojang.rubydung.level.Level;
import com.mojang.rubydung.level.LevelRenderer;
import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.typedarrays.Int16Array;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLShader;
import org.teavm.jso.webgl.WebGLUniformLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.LWJGLException;

public class RubyDung implements Runnable {
   private static final boolean FULLSCREEN_MODE = false;
   private int width;
   private int height;
   private float[] fogColor = new float[4];
   private Timer timer = new Timer(60.0F);
   private Level level;
   private LevelRenderer levelRenderer;
   private Player player;
   public static HitResult hitResult = null;

   public static WebGLRenderingContext gl;
   public static WebGLProgram program;
   public static int aPosition, aTexCoord, aColor;
   public static WebGLUniformLocation uProjMatrix, uModlMatrix, uHasTexture, uHasColor, uFogColor, uAlpha, uUseFog;
   public static float[] projMatrix = new float[16];
   public static float[] modlMatrix = new float[16];
   public static float[] activeMatrix = modlMatrix;
   public static WebGLBuffer quadEbo;
   private long lastTime;
   private int frames;

   public void init() throws Exception {
      int col = 920330;
      float fr = 0.5F;
      float fg = 0.8F;
      float fb = 1.0F;
      this.fogColor = new float[]{(float)(col >> 16 & 255) / 255.0F, (float)(col >> 8 & 255) / 255.0F, (float)(col & 255) / 255.0F, 1.0F};
      
      HTMLCanvasElement canvas = Window.current().getDocument().getElementById("canvas").cast();
      gl = getWebGLContext(canvas);
      this.resize(Window.current().getInnerWidth(), Window.current().getInnerHeight());
      Window.current().addEventListener("resize", evt -> {
          this.resize(Window.current().getInnerWidth(), Window.current().getInnerHeight());
      });

      Keyboard.create();
      Mouse.create();

      canvas.addEventListener("click", evt -> {
         canvas.requestPointerLock();
      });

      WebGLShader vs = gl.createShader(WebGLRenderingContext.VERTEX_SHADER);
      gl.shaderSource(vs, "attribute vec3 aPosition; attribute vec2 aTexCoord; attribute vec3 aColor; uniform mat4 uProj; uniform mat4 uModl; varying vec2 vTexCoord; varying vec3 vColor; varying float vFogDist; void main() { vec4 pos = uModl * vec4(aPosition, 1.0); gl_Position = uProj * pos; vTexCoord = aTexCoord; vColor = aColor; vFogDist = length(pos.xyz); }");
      gl.compileShader(vs);
      WebGLShader fs = gl.createShader(WebGLRenderingContext.FRAGMENT_SHADER);
      gl.shaderSource(fs, "precision highp float; varying vec2 vTexCoord; varying vec3 vColor; varying float vFogDist; uniform sampler2D uTex; uniform bool uHasTexture; uniform bool uHasColor; uniform vec4 uFogColor; uniform float uAlpha; uniform bool uUseFog; void main() { vec4 col = vec4(1.0); if(uHasColor) col *= vec4(vColor, 1.0); if(uHasTexture) col *= texture2D(uTex, vTexCoord); col.a *= uAlpha; if(uUseFog) { float fogFactor = exp(-0.02 * vFogDist); fogFactor = clamp(fogFactor, 0.0, 1.0); gl_FragColor = mix(uFogColor, col, fogFactor); } else { gl_FragColor = col; } }");
      gl.compileShader(fs);
      program = gl.createProgram();
      gl.attachShader(program, vs);
      gl.attachShader(program, fs);
      gl.linkProgram(program);
      gl.useProgram(program);

      aPosition = gl.getAttribLocation(program, "aPosition");
      aTexCoord = gl.getAttribLocation(program, "aTexCoord");
      aColor = gl.getAttribLocation(program, "aColor");
      uProjMatrix = gl.getUniformLocation(program, "uProj");
      uModlMatrix = gl.getUniformLocation(program, "uModl");
      uHasTexture = gl.getUniformLocation(program, "uHasTexture");
      uHasColor = gl.getUniformLocation(program, "uHasColor");
      uFogColor = gl.getUniformLocation(program, "uFogColor");
      uAlpha = gl.getUniformLocation(program, "uAlpha");
      uUseFog = gl.getUniformLocation(program, "uUseFog");

      quadEbo = gl.createBuffer();
      gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, quadEbo);
      short[] indices = new short[300000 * 6 / 4];
      for (int i = 0, j = 0; i < 300000; i += 4, j += 6) {
          indices[j] = (short)i; indices[j+1] = (short)(i+1); indices[j+2] = (short)(i+2);
          indices[j+3] = (short)i; indices[j+4] = (short)(i+2); indices[j+5] = (short)(i+3);
      }
      Int16Array arr = Int16Array.create(indices.length);
      for(int i=0; i<indices.length; i++) arr.set(i, indices[i]);
      gl.bufferData(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, arr, WebGLRenderingContext.STATIC_DRAW);

      gl.clearColor(fr, fg, fb, 1.0F);
      gl.clearDepth(1.0f);
      gl.enable(WebGLRenderingContext.DEPTH_TEST);
      gl.depthFunc(WebGLRenderingContext.LEQUAL);
      
      glMatrixMode(5889);
      glLoadIdentity();
      glMatrixMode(5888);
      
      this.level = new Level(256, 256, 64);
      this.levelRenderer = new LevelRenderer(this.level);
      this.player = new Player(this.level);
      Mouse.setGrabbed(true);
   }

   public static void glMatrixMode(int mode) {
       activeMatrix = (mode == 5889) ? projMatrix : modlMatrix;
   }
   public static void glLoadIdentity() {
       for(int i=0; i<16; i++) activeMatrix[i] = (i%5==0)?1:0;
   }
   public static void glTranslatef(float x, float y, float z) {
       activeMatrix[12] += activeMatrix[0]*x + activeMatrix[4]*y + activeMatrix[8]*z;
       activeMatrix[13] += activeMatrix[1]*x + activeMatrix[5]*y + activeMatrix[9]*z;
       activeMatrix[14] += activeMatrix[2]*x + activeMatrix[6]*y + activeMatrix[10]*z;
       activeMatrix[15] += activeMatrix[3]*x + activeMatrix[7]*y + activeMatrix[11]*z;
   }
   public static void glRotatef(float angle, float x, float y, float z) {
       float r = (float)Math.toRadians(angle);
       float c = (float)Math.cos(r), s = (float)Math.sin(r), omc = 1.0f - c;
       float m0 = x*x*omc + c, m1 = y*x*omc + z*s, m2 = x*z*omc - y*s;
       float m4 = x*y*omc - z*s, m5 = y*y*omc + c, m6 = y*z*omc + x*s;
       float m8 = x*z*omc + y*s, m9 = y*z*omc - x*s, m10 = z*z*omc + c;
       float[] temp = new float[16];
       System.arraycopy(activeMatrix, 0, temp, 0, 16);
       for(int i=0; i<4; i++) {
           activeMatrix[i]   = temp[i]*m0 + temp[i+4]*m1 + temp[i+8]*m2;
           activeMatrix[i+4] = temp[i]*m4 + temp[i+4]*m5 + temp[i+8]*m6;
           activeMatrix[i+8] = temp[i]*m8 + temp[i+4]*m9 + temp[i+8]*m10;
       }
   }
   public static void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
       float f = 1.0f / (float)Math.tan(fovy * Math.PI / 360.0);
       glLoadIdentity();
       activeMatrix[0] = f / aspect; activeMatrix[5] = f;
       activeMatrix[10] = (zFar + zNear) / (zNear - zFar);
       activeMatrix[11] = -1.0f;
       activeMatrix[14] = (2.0f * zFar * zNear) / (zNear - zFar);
       activeMatrix[15] = 0.0f;
   }
   public static void updateUniforms() {
       gl.uniformMatrix4fv(uProjMatrix, false, projMatrix);
       gl.uniformMatrix4fv(uModlMatrix, false, modlMatrix);
   }

   public void destroy() {
      this.level.save();
   }

   public void run() {
      try {
         this.init();
      } catch (Exception e) {
         e.printStackTrace();
         return;
      }
      this.lastTime = System.currentTimeMillis();
      this.frames = 0;
      this.doFrame();
   }

   private void doFrame() {
      if (Keyboard.isKeyDown(1)) { this.destroy(); return; }
      this.timer.advanceTime();
      for(int i = 0; i < this.timer.ticks; ++i) {
         this.tick();
      }
      this.render(this.timer.a);
      ++frames;
      while(System.currentTimeMillis() >= lastTime + 1000L) {
         System.out.println(frames + " fps, " + Chunk.updates);
         Chunk.updates = 0;
         lastTime += 1000L;
         frames = 0;
      }
      Window.current().requestAnimationFrame(t -> doFrame());
   }

   public void tick() {
      this.player.tick();
   }

   private void moveCameraToPlayer(float a) {
      glTranslatef(0.0F, 0.0F, -0.3F);
      glRotatef(this.player.xRot, 1.0F, 0.0F, 0.0F);
      glRotatef(this.player.yRot, 0.0F, 1.0F, 0.0F);
      float x = this.player.xo + (this.player.x - this.player.xo) * a;
      float y = this.player.yo + (this.player.y - this.player.yo) * a;
      float z = this.player.zo + (this.player.z - this.player.zo) * a;
      glTranslatef(-x, -y, -z);
   }

   private void setupCamera(float a) {
      glMatrixMode(5889);
      glLoadIdentity();
      gluPerspective(70.0F, (float)this.width / (float)this.height, 0.1F, 1000.0F);
      glMatrixMode(5888);
      glLoadIdentity();
      this.moveCameraToPlayer(a);
   }

   private void pick(float a) {
      this.levelRenderer.pick(this.player);
   }

   public void render(float a) {
      float xo = (float)Mouse.getDX();
      float yo = (float)Mouse.getDY();
      this.player.turn(xo, yo);
      this.pick(a);

      while(Mouse.next()) {
         if (Mouse.getEventButton() == 1 && Mouse.getEventButtonState() && this.hitResult != null) {
            this.level.setTile(this.hitResult.x, this.hitResult.y, this.hitResult.z, 0);
         }
         if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState() && this.hitResult != null) {
            int x = this.hitResult.x;
            int y = this.hitResult.y;
            int z = this.hitResult.z;
            if (this.hitResult.f == 0) --y;
            if (this.hitResult.f == 1) ++y;
            if (this.hitResult.f == 2) --z;
            if (this.hitResult.f == 3) ++z;
            if (this.hitResult.f == 4) --x;
            if (this.hitResult.f == 5) ++x;
            this.level.setTile(x, y, z, 1);
         }
      }

      while(Keyboard.next()) {
         if (Keyboard.getEventKey() == 28 && Keyboard.getEventKeyState()) {
            this.level.save();
         }
      }

      gl.clear(16640);
      this.setupCamera(a);
      updateUniforms();
      
      gl.uniform1i(uHasColor, 1);
      gl.uniform1i(uHasTexture, 1);

      gl.enable(WebGLRenderingContext.CULL_FACE);
      gl.uniform1i(uUseFog, 0);
      this.levelRenderer.render(this.player, 0);
   
      gl.uniform1i(uUseFog, 1);
      gl.uniform4f(uFogColor, 0.0f, 0.0f, 0.0f, 1.0f);
      this.levelRenderer.render(this.player, 1);
   
      gl.uniform1i(uHasTexture, 0);
      if (this.hitResult != null) {
         this.levelRenderer.renderHit(this.hitResult);
      }
      gl.uniform1i(uUseFog, 0);
   }

   public static void checkError() {
      int e = gl.getError();
      if (e != 0) {
         throw new IllegalStateException("WebGL Error: " + e);
      }
   }

   public void resize(int newWidth, int newHeight) {
      this.width = newWidth;
      this.height = newHeight;
    
      HTMLCanvasElement canvas = Window.current().getDocument().getElementById("canvas").cast();
      canvas.setWidth(newWidth);
      canvas.setHeight(newHeight);
    
      gl.viewport(0, 0, newWidth, newHeight);
   }

   public static void main(String[] args) throws LWJGLException {
      (new Thread(new RubyDung())).start();
   }

   @JSBody(params = {"c"}, script = "return c.getContext('webgl', {antialias: false});")
   public static native WebGLRenderingContext getWebGLContext(HTMLCanvasElement c);
}