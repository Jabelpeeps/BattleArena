package mc.alk.arena.objects.arenas;

public interface ArenaFactory {

    public Arena newArena();

    public static final ArenaFactory DEFAULT = () -> new Arena();
}
