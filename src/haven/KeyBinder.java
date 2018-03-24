package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import rx.functions.Func0;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Type;
import java.util.*;
import java.util.List;

import static haven.Action.*;
import static haven.WidgetList.*;

public class KeyBinder {
    private static final String CONFIG_JSON = "keybindings.json";
    
    public static final int NONE = 0;
    public static final int ALT = 1;
    public static final int CTRL = 2;
    public static final int SHIFT = 4;
    
    private static final Gson gson;
    private static final Map<Action, KeyBind> binds;
    private static final List<Action> order;
    
    static {
	gson = (new GsonBuilder()).setPrettyPrinting().create();
	Map<Action, KeyBind> tmp = null;
	try {
	    Type type = new TypeToken<Map<Action, KeyBind>>() {
	    }.getType();
	    tmp = gson.fromJson(Config.loadFile(CONFIG_JSON), type);
	} catch (Exception ignored) {
	}
	if(tmp == null) {
	    tmp = new HashMap<>();
	}
	binds = tmp;
	binds.forEach((action, keyBind) -> keyBind.action = action);
	order = Arrays.asList(Action.values());
	defaults();
    }
    
    private static void defaults() {
        
	add(KeyEvent.VK_1, CTRL, ACT_HAND_0);
	add(KeyEvent.VK_2, CTRL, ACT_HAND_1);
	add(KeyEvent.VK_C, ALT, OPEN_QUICK_CRAFT);
	add(KeyEvent.VK_B, ALT, OPEN_QUICK_BUILD);
	add(KeyEvent.VK_A, ALT, OPEN_QUICK_ACTION);
	add(KeyEvent.VK_X, ALT, OPEN_CRAFT_DB);
	add(KeyEvent.VK_H, ALT, TOGGLE_CURSOR);
	add(KeyEvent.VK_S, ALT, TOGGLE_STUDY);
	add(KeyEvent.VK_F, ALT, FILTER);
	add(KeyEvent.VK_I, ALT, TOGGLE_GOB_INFO);
	add(KeyEvent.VK_H, CTRL, TOGGLE_GOB_HITBOX);
	add(KeyEvent.VK_R, ALT, TOGGLE_GOB_RADIUS);
	add(KeyEvent.VK_G, CTRL, TOGGLE_TILE_GRID);
	add(KeyEvent.VK_Z, CTRL, TOGGLE_TILE_CENTERING);
	add(KeyEvent.VK_S, CTRL, TAKE_SCREENSHOT);
    }
    
    private static synchronized void store() {
	Config.saveFile(CONFIG_JSON, gson.toJson(binds));
    }
    
    public static boolean handle(UI ui, KeyEvent e) {
	int code = e.getKeyCode();
	int modflags = getModFlags(e.getModifiersEx());
    
	for (KeyBind bind : binds.values()) {
	    if (bind.match(ui, code, modflags)) {
		return true;
	    }
	}
	
	return false;
    }
    
    public static int getModFlags(int modflags) {
	modflags = ((modflags & InputEvent.ALT_DOWN_MASK) != 0 ? ALT : 0)
	    | ((modflags & InputEvent.META_DOWN_MASK) != 0 ? ALT : 0)
	    | ((modflags & InputEvent.CTRL_DOWN_MASK) != 0 ? CTRL : 0)
	    | ((modflags & InputEvent.SHIFT_DOWN_MASK) != 0 ? SHIFT : 0);
	return modflags;
    }
    
    public static void add(int code, int mods, Action action) {
        if(!binds.containsKey(action)) {
	    binds.put(action, new KeyBind(code, mods, action));
	}
    }
    
    public static KeyBind get(Action action) {
	return binds.get(action);
    }
    
    public static KeyBind make(KeyEvent e, Action action) {
	return new KeyBind(e.getKeyCode(), getModFlags(e.getModifiersEx()), action);
    }
    
    private static boolean change(KeyBind to) {
	boolean conflicts = false;
	if(!to.isEmpty()) {
	    for(Map.Entry<Action, KeyBind> entry : binds.entrySet()) {
		KeyBind bind = entry.getValue();
		Action action = entry.getKey();
		if(to.action != action && to.code == bind.code && to.mods == bind.mods) {
		    binds.put(action, new KeyBind(0, 0, action));
		    conflicts = true;
		}
	    }
	}
	binds.put(to.action, to);
        store();
	return conflicts;
    }
    
    public static List<ShortcutWidget> makeWidgets(Func0 invalidate) {
	List<ShortcutWidget> list = new ArrayList<>(binds.size());
	for (Action action : order) {
	    if(binds.containsKey(action)) {
		list.add(new ShortcutWidget(binds.get(action), invalidate));
	    }
	}
	return list;
    }
    
    public static class KeyBind {
	private final int code;
	private final int mods;
	transient private Action action;
	
	public KeyBind(int code, int mods, Action action) {
	    this.code = code;
	    this.mods = mods;
	    this.action = action;
	}
	
	public boolean match(UI ui, int code, int mods) {
	    boolean match = code == this.code && ((mods & this.mods) == this.mods);
	    if (match && ui.gui != null) {
		action.run(ui.gui);
	    }
	    return match;
	}
	
	public String shortcut() {
	    if(isEmpty()) {return "<UNBOUND>";}
	    String key = KeyEvent.getKeyText(code);
	    if ((mods & SHIFT) != 0) {
		key = "SHIT+" + key;
	    }
	    if ((mods & ALT) != 0) {
		key = "ALT+" + key;
	    }
	    if ((mods & CTRL) != 0) {
		key = "CTRL+" + key;
	    }
	    return key;
	}
    
	public boolean isEmpty() {
	    return code == 0 && mods == 0;
	}
    }
    
    public static class ShortcutWidget extends Widget implements ShortcutSelectorWdg.Result {
    
	private final Button btn;
	private KeyBind keyBind;
	private final Func0 invalidate;
    
	public ShortcutWidget(KeyBind bind, Func0 invalidate) {
	    btn = add(new Button(75, bind.shortcut()) {
		  @Override
		  public void click() {
		      ui.root.add(new ShortcutSelectorWdg(keyBind, ShortcutWidget.this), ui.mc.sub(50, 20));
		  }
    
		  @Override
		  public boolean mouseup(Coord c, int button) {
		      //FIXME:a little hack, because WidgetList does not pass correct click coordinates if scrolled
		      return super.mouseup(Coord.z, button);
		  }
	      },
	    225, 0);
	    this.keyBind = bind;
	    this.invalidate = invalidate;
	    if(bind.action.description != null) {
		tooltip = RichText.render(bind.action.description, 200);
	    }
	    btn.autosize(true);
	    btn.c.x = 300 - btn.sz.x;
	    add(new Label(bind.action.name), 5, 5);
	}
    
	@Override
	public void keyBindChanged(KeyBind from, KeyBind to) {
	    if(change(to)) {
		invalidate.call();
	    } else {
		update();
	    }
	}
    
	public void update() {
	    keyBind = KeyBinder.get(keyBind.action);
	    btn.change(keyBind.shortcut());
	    btn.c.x = 300 - btn.sz.x;
	}
    }
    
    private static class ShortcutSelectorWdg extends Widget {
	private static final Color BGCOLOR = new Color(32, 64, 32, 196);
	private static final Coord PAD = new Coord(5, 5);
	private final KeyBind bind;
	private final Result listener;
	private final Tex label;
    
	private UI.Grab keygrab;
	private UI.Grab mousegrab;
	
	public ShortcutSelectorWdg(KeyBind bind, Result listener) {
	    this.bind = bind;
	    this.listener = listener;
	    label = RichText.render("Press any key...\nOr DELETE to unbind", 0).tex();
	    sz = label.sz().add(PAD.mul(2));
	}
    
	@Override
	public boolean keydown(KeyEvent ev) {
	    int code = ev.getKeyCode();
	    if(    code != 0
		&& code != KeyEvent.VK_CONTROL
		&& code != KeyEvent.VK_SHIFT
		&& code != KeyEvent.VK_ALT
		&& code != KeyEvent.VK_META) {
		if(code == KeyEvent.VK_DELETE) {
		    listener.keyBindChanged(bind, new KeyBind(0, 0, bind.action));
		} else if(code != KeyEvent.VK_ESCAPE) {
		    listener.keyBindChanged(bind, make(ev, bind.action));
		}
		remove();
	    }
	    return true;
	}
    
	@Override
	public boolean type(char key, KeyEvent ev) {
	    
	    return true;
	}
	
	@Override
	protected void attach(UI ui) {
	    super.attach(ui);
	    keygrab = ui.grabkeys(this);
	    mousegrab = ui.grabmouse(this);
	}
	
	@Override
	public boolean mousedown(Coord c, int button) {
	    remove();
	    return true;
	}
	
	public void remove() {
	    mousegrab.remove();
	    keygrab.remove();
	    reqdestroy();
	}
	
	@Override
	public void draw(GOut g) {
	    g.chcolor(BGCOLOR);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    BOX.draw(g, Coord.z, sz);
	    g.image(label, PAD);
	    super.draw(g);
	}
	
	public interface Result {
	    void keyBindChanged(KeyBind from, KeyBind to);
	}
    }
}
