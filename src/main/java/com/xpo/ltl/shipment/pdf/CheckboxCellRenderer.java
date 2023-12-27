package com.xpo.ltl.shipment.pdf;


import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfButtonFormField;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.renderer.CellRenderer;
import com.itextpdf.layout.renderer.DrawContext;
import com.itextpdf.layout.renderer.IRenderer;

public class CheckboxCellRenderer extends CellRenderer {

    // The name of the check box field
    protected String name;

    public CheckboxCellRenderer(Cell modelElement, String name) {
        super(modelElement);
        this.name = name;
    }

    // If renderer overflows on the next area, iText uses getNextRender() method to create a renderer for the overflow part.
    // If getNextRenderer isn't overriden, the default method will be used and thus a default rather than custom
    // renderer will be created
    @Override
    public IRenderer getNextRenderer() {
        return new CheckboxCellRenderer((Cell) modelElement, name);
    }

    @Override
    public void draw(DrawContext drawContext) {
        PdfAcroForm form = PdfAcroForm.getAcroForm(drawContext.getDocument(), true);

        // Define the coordinates of the middle
        float x = (getOccupiedAreaBBox().getLeft() + getOccupiedAreaBBox().getRight()) / 2;
        float y = (getOccupiedAreaBBox().getTop() + getOccupiedAreaBBox().getBottom()) / 2;

        // Define the position of a check box that measures 20 by 20
        Rectangle rect = new Rectangle(x - 10, y - 10, 20, 20);

        // The 4th parameter is the initial value of checkbox: 'Yes' - checked, 'Off' - unchecked
        // By default, checkbox value type is cross.
        PdfButtonFormField checkBox = PdfFormField.createCheckBox(drawContext.getDocument(), rect, name, "Yes");
        form.addField(checkBox);
    }
}