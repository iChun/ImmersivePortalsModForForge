package com.qouteall.immersive_portals.altius_world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.Consumer;

public class DimTermWidget extends AbstractList.AbstractListEntry<DimTermWidget> {
    
    public DimensionType dimension;
    public final DimListWidget parent;
    private Consumer<DimTermWidget> selectCallback;
    
    public DimTermWidget(
        DimensionType dimension,
        DimListWidget parent,
        Consumer<DimTermWidget> selectCallback
    ) {
        this.dimension = dimension;
        this.parent = parent;
        this.selectCallback = selectCallback;
    }
    
    @Override
    public void render(
        int index,
        int y,
        int x,
        int width,
        int height,
        int mouseX,
        int mouseY,
        boolean hovering,
        float delta
    ) {
        Minecraft.getInstance().fontRenderer.drawString(
            dimension.toString(),
            x, y, 0xFFFFFFFF
        );
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        selectCallback.accept(this);
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
