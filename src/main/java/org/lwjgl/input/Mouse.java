// modified version of https://github.com/PeytonPlayz595/LWJGL-TeaVM/blob/main/src/main/java/org/lwjgl/input/Mouse.java

package org.lwjgl.input;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.lwjgl.LWJGLException;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.events.WheelEvent;
import org.teavm.jso.browser.Window;


public class Mouse {

	/** Has the mouse been created? */
	private static boolean created;

	private static int x;
	private static int y;

	private static double dx;
	private static double dy;
	private static int dwheel;

	private static String[] buttonName;

	private static final Map<String, Integer> buttonMap = new HashMap<String, Integer>(16);

	private static boolean initialized;
	
	private static int grab_x;
	private static int grab_y;

	private static boolean isGrabbed;
	
	private static LinkedList<MouseEvent> mouseEvents = new LinkedList();
	private static boolean[] buttonStates = new boolean[8];
	private static boolean isInsideWindow = true;
	
	//Make sure it cannot be constructed
	private Mouse() {
	}
	
	public static void setNativeCursor(Object obj) {
	}
	
	public static void setCursorPosition(int new_x, int new_y) {
		//Not possible due to security restrictions
		//Unless you create a custom cursor
		//But I'm not doing that
	}
	
	private static void initialize() {
		buttonName = new String[16];
		for (int i = 0; i < 16; i++) {
			buttonName[i] = "BUTTON" + i;
			buttonMap.put(buttonName[i], i);
		}

		initialized = true;
	}
	
	private static void resetMouse() {
		dx = dy = dwheel = 0;
	}
	
	public static void create() throws LWJGLException {
		try {
			if(created) {
				return;
			}
		
			if(!initialized) {
				initialize();
			}
		
			Window.current().addEventListener("contextmenu", contextmenu = new EventListener<MouseEvent>() {
				@Override
				public void handleEvent(MouseEvent evt) {
					evt.preventDefault();
					evt.stopPropagation();
				}
			});
			Window.current().addEventListener("mousedown", mousedown = new EventListener<MouseEvent>() {
				@Override
				public void handleEvent(MouseEvent evt) {
					int b = evt.getButton();
					buttonStates[b == 1 ? 2 : (b == 2 ? 1 : b)] = true;
					mouseEvents.add(evt);
					evt.preventDefault();
					evt.stopPropagation();
				}
			});
			Window.current().addEventListener("mouseup", mouseup = new EventListener<MouseEvent>() {
				@Override
				public void handleEvent(MouseEvent evt) {
					int b = evt.getButton();
					buttonStates[b == 1 ? 2 : (b == 2 ? 1 : b)] = false;
					mouseEvents.add(evt);
					evt.preventDefault();
					evt.stopPropagation();
				}
			});
			Window.current().addEventListener("mousemove", mousemove = new EventListener<MouseEvent>() {
				@Override
				public void handleEvent(MouseEvent evt) {
					evt.preventDefault();
					evt.stopPropagation();
					double ratio = Window.current().getDevicePixelRatio();
					x = (int)(evt.getClientX() * ratio);
					y = (int)((Window.current().getInnerHeight() - evt.getClientY()) * ratio);
					dx += evt.getMovementX();
					dy += -evt.getMovementY();
					//if(Display.isActive()) {
						mouseEvents.add(evt);
					//}
				}
			});
			Window.current().addEventListener("wheel", wheel = new EventListener<WheelEvent>() {
				@Override
				public void handleEvent(WheelEvent evt) {
					mouseEvents.add(evt);
					evt.preventDefault();
					evt.stopPropagation();
					int rotation = evt.getDeltaY() > 0 ? 1 : -1;
					dwheel += rotation;
				}
			});
			Window.current().getDocument().addEventListener("pointerlockchange", pointerLockChange = new EventListener<Event>() {
				@Override
				public void handleEvent(Event evt) {
					if(isPointerLocked()) {
						isGrabbed = true;
					} else {
						isGrabbed = false;
					}
				}
			});
			Window.current().addEventListener("mouseleave", mouseLeave = new EventListener<Event>() {
				@Override
				public void handleEvent(Event evt) {
					evt.preventDefault();
					evt.stopPropagation();
					isInsideWindow = false;
				}
			});
			Window.current().addEventListener("mouseenter", mouseEnter = new EventListener<Event>() {
				@Override
				public void handleEvent(Event evt) {
					evt.preventDefault();
					evt.stopPropagation();
					isInsideWindow = true;
				}
			});
			
			created = true;
			
			if(mouseEvents != null) {
				mouseEvents.clear();
			}
		
			setGrabbed(isGrabbed);
		} catch(Throwable t) {
			throw new LWJGLException(t);
		}
	}
	
	public static boolean isCreated() {
		return created;
	}
	
	public static void destroy() {
		if(!created) {
			return;
		}
		
		isGrabbed = false;
		setGrabbed(false);
		created = false;
		
		resetMouse();
		Window.current().removeEventListener("contextmenu", contextmenu);
		Window.current().removeEventListener("mousedown", mousedown);
		Window.current().removeEventListener("mouseup", mouseup);
		Window.current().removeEventListener("mousemove", mousemove);
		Window.current().removeEventListener("wheel", wheel);
		Window.current().getDocument().removeEventListener("pointerlockchange", pointerLockChange);
		Window.current().removeEventListener("mouseleave", mouseLeave);
		Window.current().removeEventListener("mouseenter", mouseEnter);
		mouseEvents.clear();
	}
	
	public static boolean isButtonDown(int button) {
		return buttonStates[button];
	}
	
	public static String getButtonName(int button) {
		if (button >= buttonName.length || button < 0) {
			return null;
		} else {
			return buttonName[button];
		}
	}
	
	public static int getButtonIndex(String buttonName) {
		Integer ret = buttonMap.get(buttonName);
		if (ret == null) {
			return -1;
		} else {
			return ret;
		}
	}
	
	public static boolean next() {
		currentEvent = null;
		return !mouseEvents.isEmpty() && (currentEvent = mouseEvents.remove(0)) != null;
	}
	
	public static int getEventButton() {
		if(currentEvent == null) return -1;
		int b = currentEvent.getButton();
		return b == 1 ? 2 : (b == 2 ? 1 : b);
	}
	
	public static boolean getEventButtonState() {
		return currentEvent == null ? false : currentEvent.getType().equals(MouseEvent.MOUSEDOWN);
	}
	
	public static int getEventDX() {
		return currentEvent == null ? -1 : (int)(currentEvent.getMovementX() * getPixelScaleFactor());
	}
	
	public static int getEventDY() {
		return currentEvent == null ? -1 : (int)(-currentEvent.getMovementY() * getPixelScaleFactor());
	}
	
	public static int getEventX() {
		return currentEvent == null ? -1 : (int)(currentEvent.getClientX() * Window.current().getDevicePixelRatio());
	}
	
	public static final int getEventY() {
		return currentEvent == null ? -1 : (int)((Window.current().getInnerHeight() - currentEvent.getClientY()) * Window.current().getDevicePixelRatio());
	}
	
	public static int getEventDWheel() {
		return ("wheel".equals(currentEvent.getType())) ? (((WheelEvent)currentEvent).getDeltaY() == 0.0D ? 0 : (((WheelEvent)currentEvent).getDeltaY() > 0.0D ? -1 : 1)) : 0;
	}
	
	public static int getX() {
		return x;
	}
	
	public static int getY() {
		return y;
	}
	
	public static int getDX() {
		int result = (int)dx;
		dx = 0.0D;
		return result;
	}
	
	public static int getDY() {
		int result = (int)dy;
		dy = 0.0D;
		return result;
	}
	
	public static int getDWheel() {
		int result = dwheel;
		dwheel = 0;
		return result;
	}
	
	//Left, right, wheel button and wheel axis
	//Other buttons are most likely software controlled
	//So I'm just returning the default amount
	public static int getButtonCount() {
		return 4;
	}
	
	public static boolean hasWheel() {
		return true;
	}
	
	public static boolean isGrabbed() {
		if(!isPointerLocked() && isGrabbed) {
			isGrabbed = false;
		}
		return isGrabbed;
	}
	
	public static void setGrabbed(boolean grab) {
		boolean grabbed = isGrabbed;
		isGrabbed = grab;
		if(isCreated()) {
			if(grab && !grabbed) {
				if(!isPointerLocked()) {
					grab_x = x;
					grab_y = y;
					dx = 0;
					dy = 0;
					requestPointerLockNative();
				}
			} else if (!grab && grabbed) {
				if(isPointerLocked()) {
					exitPointerLockNative();
					setCursorPosition(grab_x, grab_y);
				}
			}
			
			resetMouse();
		}
	}
	
	public static void updateCursor() {
	}
	
	public static boolean isInsideWindow() {
		return isInsideWindow;
	}
	
	private static EventListener contextmenu = null;
	private static EventListener mousedown = null;
	private static EventListener mouseup = null;
	private static EventListener mousemove = null;
	private static EventListener wheel = null;
	private static EventListener pointerLockChange = null;
	private static EventListener mouseLeave = null;
	private static EventListener mouseEnter = null;
	private static MouseEvent currentEvent = null;
	
	@JSBody(params = { }, script = "return document.pointerLockElement != null;")
	private static native boolean isPointerLocked();
	
	@JSBody(script = "document.body.requestPointerLock();")
	private static native void requestPointerLockNative();
	
	@JSBody(script = "document.exitPointerLock();")
	private static native void exitPointerLockNative();

        @JSBody(script = "return window.devicePixelRatio;")
        public static native float getPixelScaleFactor();
}