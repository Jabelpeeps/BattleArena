package mc.alk.arena.plugins;

public class FactionsController {
	static boolean hasFactions = false;

	public static boolean enabled() {
		return hasFactions;
	}

	public static boolean setPlugin(boolean enable) {
		hasFactions = FactionsListener.enable();
		return hasFactions;
	}
}
