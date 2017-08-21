package org.contentmine.pdf2svg;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;

public class AMIPDFRenderer extends PDFRenderer {
	private static final Logger LOG = Logger.getLogger(AMIPDFRenderer.class);
	
	static {
		LOG.setLevel(Level.DEBUG);
	}

	public AMIPDFRenderer(PDDocument doc) {
		super(doc);
	}

    @Override
    protected PageDrawer createPageDrawer(PageDrawerParameters parameters) throws IOException {
        LOG.info("AMIPDFRenderer used");
        return new AMIPageDrawer(parameters);
    }

}
