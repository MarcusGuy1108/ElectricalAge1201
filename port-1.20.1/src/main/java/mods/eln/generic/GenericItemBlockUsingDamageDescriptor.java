package mods.eln.generic;

import mods.eln.Eln;
import mods.eln.misc.RealisticEnum;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;

public class GenericItemBlockUsingDamageDescriptor {

    String iconName;
    public String name;

    public static String INVALID_NAME = "$NO_DESCRIPTOR";

    public static HashMap<String, GenericItemBlockUsingDamageDescriptor> byName = new HashMap<>();

    public static GenericItemBlockUsingDamageDescriptor getByName(String name) {
        return byName.get(name);
    }

    // In 1.20.1 every descriptor has its own item; parentItemDamage stays as the stable numeric id saves use.
    public Item parentItem;
    public int parentItemDamage;

    public GenericItemBlockUsingDamageDescriptor(String name) {
        this(name, name);
    }

    public GenericItemBlockUsingDamageDescriptor(String name, String iconName) {
        setDefaultIcon(iconName);
        this.name = name;
        byName.put(name, this);
    }

    public void setDefaultIcon(String name) {
        String iconName = name.replaceAll(" ", "").toLowerCase();
        if (Eln.config.getBooleanOrElse("ui.icons.noSymbols", false) &&
            getClass().getClassLoader().getResource("assets/eln/textures/blocks/" + iconName + "-ni.png") != null) {
            this.iconName = iconName + "-ni";
        } else {
            this.iconName = iconName;
        }
    }

    // Texture name under assets/eln/textures/blocks/, used to generate the item model.
    public String getIconName() {
        return iconName;
    }

    public CompoundTag getDefaultNBT() {
        return null;
    }

    public void addInformation(ItemStack itemStack, Player entityPlayer, List<String> list, boolean par4) {
    }

    public RealisticEnum addRealismContext(List<String> list) {
        return null;
    }

    // PORT: updateIcons/getIcon (IIcon registration) are replaced by generated item models.

    public String getName(ItemStack stack) {
        return name;
    }

    private boolean hidden = false;

    public void setParent(Item item, int damage) {
        this.parentItem = item;
        this.parentItemDamage = damage;
    }

    public ItemStack newItemStack(int size) {
        return new ItemStack(parentItem, size);
    }

    public ItemStack newItemStack() {
        return new ItemStack(parentItem, 1);
    }

    public ItemStack newCreativeTabStack() {
        ItemStack stack = new ItemStack(parentItem, 1);
        CompoundTag nbt = getDefaultNBT();
        if (nbt != null) stack.setTag(nbt);
        return stack;
    }

    public int getItemStackLimit(ItemStack stack) {
        return 64;
    }

    public boolean checkSameItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() == parentItem;
    }

    public static GenericItemBlockUsingDamageDescriptor getDescriptor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (!(stack.getItem() instanceof IDescriptorItem)) return null;
        return ((IDescriptorItem) stack.getItem()).getDescriptor();
    }

    public static GenericItemBlockUsingDamageDescriptor getDescriptor(ItemStack stack, Class<?> extendClass) {
        GenericItemBlockUsingDamageDescriptor desc = getDescriptor(stack);
        if (desc == null) return null;
        if (!extendClass.isAssignableFrom(desc.getClass())) return null;
        return desc;
    }

    public boolean onEntityItemUpdate(ItemEntity entityItem) {
        return false;
    }

    public boolean onItemUseFirst(ItemStack stack, Player player) {
        return false;
    }

    private GenericCreativeTab creativeTab;

    public GenericItemBlockUsingDamageDescriptor setCreativeTab(GenericCreativeTab creativeTab) {
        this.creativeTab = creativeTab;
        return this;
    }

    public GenericCreativeTab getCreativeTab() {
        return creativeTab;
    }

    public GenericItemBlockUsingDamageDescriptor hideFromCreative() {
        this.hidden = true;
        return this;
    }

    public boolean isHidden() {
        return hidden;
    }
}
