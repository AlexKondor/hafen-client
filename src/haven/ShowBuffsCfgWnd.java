package haven;

import haven.rx.BuffToggles;

public class ShowBuffsCfgWnd extends Window {
    private static Window instance;

    public static void toggle(Widget parent) {
	if(instance == null) {
	    instance = parent.add(new ShowBuffsCfgWnd());
	} else {
	    remove();
	}
    }

    public static void remove() {
	if(instance != null) {
	    instance.reqdestroy();
	    instance = null;
	}
    }

    public ShowBuffsCfgWnd() {
	super(Coord.z, "Show as buffs");
	justclose = true;

	int y = 0;
	for (BuffToggles.Toggle toggle : BuffToggles.toggles) {
	    add(new OptWnd.CFGBox(toggle.name, toggle.show), 0, y);
	    y += 25;
	}

	pack();
	if(asz.x < 120) {
	    resize(new Coord(200, asz.y));
	}
    }

}
