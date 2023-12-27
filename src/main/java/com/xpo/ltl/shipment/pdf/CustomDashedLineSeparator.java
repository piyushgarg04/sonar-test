package com.xpo.ltl.shipment.pdf;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;

public class CustomDashedLineSeparator extends SolidLine {
    protected float dash = 5;
    protected float phase = 2.5f;
    protected float pad = 2.5f;
    private float lineWidth = 1;
    private Color color = ColorConstants.BLACK;

    public float getDash() {
        return dash;
    }
 
    public float getPhase() {
        return phase;
    }
    
    public float getPad() {
        return pad;
    }
 
    public void setDash(float dash) {
        this.dash = dash;
    }
 
    public void setPhase(float phase) {
        this.phase = phase;
    }
    
    public void setPad(float pad) {
        this.pad = pad;
    }
 
    public void draw(PdfCanvas canvas, Rectangle drawArea) {
    	canvas.saveState()
        .setStrokeColor(color)
        .setLineWidth(lineWidth)
        .setLineDash(dash, pad, phase)
        .moveTo(drawArea.getX(), PageSize.LETTER.getHeight() / 2)
        .lineTo(drawArea.getX() + drawArea.getWidth(), PageSize.LETTER.getHeight() / 2)
        .stroke()
        .restoreState();
    }
}