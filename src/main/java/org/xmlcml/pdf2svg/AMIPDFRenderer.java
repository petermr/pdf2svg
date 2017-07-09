package org.xmlcml.pdf2svg;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;

public class AMIPDFRenderer extends PDFRenderer {

	public AMIPDFRenderer(PDDocument doc) {
		super(doc);
	}

    @Override
    protected PageDrawer createPageDrawer(PageDrawerParameters parameters) throws IOException {
        return new AMIPageDrawer(parameters);
    }

}
