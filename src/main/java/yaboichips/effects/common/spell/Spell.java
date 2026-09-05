package yaboichips.effects.common.spell;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public abstract class Spell {

    private final Identifier id;
    private final Identifier icon;
    private final int maxLevel;

    protected Spell(Identifier id, Identifier icon, int maxLevel) {
        this.id = id;
        this.icon = icon;
        this.maxLevel = maxLevel;
    }

    public Identifier id() {
        return id;
    }

    public Identifier icon() {
        return icon;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public Component name() {
        return Component.translatable("spell." + id.getNamespace() + "." + id.getPath());
    }

    /**
     * Brief explanation of what the spell does, shown when hovering its equipped icon.
     */
    public Component description() {
        return Component.translatable("spell." + id.getNamespace() + "." + id.getPath() + ".description");
    }

    /**
     * Explanation of what a specific upgrade level changes, shown when hovering that level's upgrade icon.
     */
    public Component upgradeDescription(int level) {
        return Component.translatable("spell." + id.getNamespace() + "." + id.getPath() + ".upgrade." + level);
    }

    public abstract SpellType type();
}
