package com.mojang.rubydung;

import java.util.HashMap;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLTexture;

public class Textures {
   private static HashMap<String, Integer> idMap = new HashMap<>();
   private static HashMap<Integer, WebGLTexture> texMap = new HashMap<>();
   private static int nextId = 1;
   private static int lastId = -9999999;
   
   public static int loadTexture(String resourceName, int mode) {
      if (idMap.containsKey(resourceName)) {
         return idMap.get(resourceName);
      } else {
         int id = nextId++;
         WebGLRenderingContext gl = RubyDung.gl;
         WebGLTexture tex = gl.createTexture();
         texMap.put(id, tex);
         idMap.put(resourceName, id);
         
         bind(id);
         gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MIN_FILTER, mode);
         gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MAG_FILTER, mode);
         
         HTMLImageElement img = Window.current().getDocument().createElement("img").cast();
         img.setSrc(resourceName.startsWith("/") ? resourceName.substring(1) : resourceName);
         img.addEventListener("load", evt -> {
             bind(id);
             gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, 0, WebGLRenderingContext.RGBA, WebGLRenderingContext.RGBA, WebGLRenderingContext.UNSIGNED_BYTE, img);
             gl.generateMipmap(WebGLRenderingContext.TEXTURE_2D);
         });
         return id;
      }
   }

   public static void bind(int id) {
      if (id != lastId) {
         RubyDung.gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texMap.get(id));
         lastId = id;
      }
   }
}