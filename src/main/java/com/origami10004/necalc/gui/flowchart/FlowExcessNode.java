package com.origami10004.necalc.gui.flowchart;

import com.origami10004.necalc.data.CalculatorState;
import com.origami10004.necalc.data.ingredient.Ingredients;
import com.origami10004.necalc.gui.GuiFlowChart;

public class FlowExcessNode extends FlowItemNode {
    public FlowExcessNode(Ingredients ingredient, int x, int y) {
        super(ingredient, x, y);
    }

    @Override
	public void draw(GuiFlowChart gui) {
		gui.drawItemSlot((int) this.canvasX, (int) this.canvasY, 3);
		super.ingredient.renderValue(gui, (int) this.canvasX + 1, (int) this.canvasY + 1, super.ingredient.getValue() / CalculatorState.getMultiplier());
	}
    
}
