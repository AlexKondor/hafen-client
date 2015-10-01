package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public enum CFG {
    VERSION("version",""),
    DISPLAY_KINNAMES("display.kinnames", Utils.getprefb("showkinnames", true)),
    DISPLAY_FLAVOR("display.flavor", Utils.getprefb("showflavor", true)),
    DISPLAY_GOB_HEALTH("display.gob_health", false),
    STORE_MAP("general.storemap", Utils.getprefb("storemap", false)),
    SHOW_CHAT_TIMESTAMP("ui.chat.timestamp", true),
    STORE_CHAT_LOGS("ui.chat.logs", false),
    LOCK_STUDY("ui.lock_study", false),
    MMAP_FLOAT("ui.mmapfloat", false),
    MENU_SINGLE_CTRL_CLICK("ui.menu_single_ctrl_click", true),

    SHOW_ITEM_DURABILITY("ui.item_durability", true),
    SHOW_ITEM_WEAR_BAR("ui.item_wear_bar", true),
    SHOW_ITEM_ARMOR("ui.item_armor", true),

    CAMERA_BRIGHT("camera.bright", 0f),

    Q_SHOW_ALL_MODS("ui.q.allmods", 7),
    Q_SHOW_SINGLE("ui.q.showsingle", true),
    Q_MAX_SINGLE("ui.q.maxsingle", false);

    private static final String CONFIG_JSON = "config.json";
    private static final Map<Object, Object> cfg;
    private static final Map<String, Object> cache = new HashMap<String, Object>();
    private static final Gson gson;
    private final String path;
    public final Object def;
    private Observer observer;

    static {
	gson = (new GsonBuilder()).setPrettyPrinting().create();
	Map<Object, Object> tmp = null;
	try {
	    Type type = new TypeToken<Map<Object, Object>>() {
	    }.getType();
	    tmp = gson.fromJson(Config.loadFile(CONFIG_JSON), type);
	} catch(Exception ignored) {
	}
	if(tmp == null){
	    tmp = new HashMap<Object, Object>();
	}
	cfg = tmp;
    }

    public static interface Observer {
	void updated(CFG cfg);
    }

    CFG(String path, Object def) {
	this.path = path;
	this.def = def;
    }

    public Object val(){
	return CFG.get(this);
    }

    public boolean valb(){
	return CFG.getb(this);
    }

    public int vali() {
	return CFG.geti(this);
    }

    public float valf() {
	return CFG.getf(this);
    }

    public void set(Object value){
	CFG.set(this, value);
	if(observer != null){
	    observer.updated(this);
	}
    }

    public void set(Object value, boolean observe){
	set(value);
	if(observe && observer != null){
	    observer.updated(this);
	}
    }

    public void setObserver(Observer observer){
	this.observer = observer;
    }

    public static synchronized Object get(CFG name) {
	if(cache.containsKey(name.path)) {
	    return cache.get(name.path);
	} else {
	    Object value = retrieve(name);
	    cache.put(name.path, value);
	    return value;
	}
    }

    public static boolean getb(CFG name) {
	return (Boolean) get(name);
    }

    public static int geti(CFG name) {
	return  ((Number)get(name)).intValue();
    }

    public static float getf(CFG name) {
	return  ((Number)get(name)).floatValue();
    }

    @SuppressWarnings("unchecked")
    public static synchronized void set(CFG name, Object value) {
	cache.put(name.path, value);
	String[] parts = name.path.split("\\.");
	int i;
	Object cur = cfg;
	for(i = 0; i < parts.length - 1; i++) {
	    String part = parts[i];
	    if(cur instanceof Map) {
		Map<Object, Object> map = (Map<Object, Object>) cur;
		if(map.containsKey(part)) {
		    cur = map.get(part);
		} else {
		    cur = new HashMap<String, Object>();
		    map.put(part, cur);
		}
	    }
	}
	if(cur instanceof Map) {
	    Map<Object, Object> map = (Map) cur;
	    map.put(parts[parts.length - 1], value);
	}
	store();
    }

    private static synchronized void store() {
	Config.saveFile(CONFIG_JSON, gson.toJson(cfg));
    }

    private static Object retrieve(CFG name) {
	String[] parts = name.path.split("\\.");
	Object cur = cfg;
	for(String part : parts) {
	    if(cur instanceof Map) {
		Map map = (Map) cur;
		if(map.containsKey(part)) {
		    cur = map.get(part);
		} else {
		    return name.def;
		}
	    } else {
		return name.def;
	    }
	}
	return cur;
    }
}
