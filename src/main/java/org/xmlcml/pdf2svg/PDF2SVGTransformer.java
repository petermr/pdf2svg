package org.xmlcml.pdf2svg;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * new version of PDF2SVGConverter for PDFBox 2.0
 * 
 * @author pm286
 *
 */
public class PDF2SVGTransformer {
	private static final Logger LOG = Logger.getLogger(PDF2SVGTransformer.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

	private PDDocument document;
	private PDFRenderer renderer;
	private int pageCount;

	public PDF2SVGTransformer() {
	}

	public void convert(File file) throws IOException {
        document = PDDocument.load(file);
        renderer = new PDFRenderer(document);
        pageCount = document.getNumberOfPages();
        LOG.debug("Page count: "+pageCount);
        for (int ipage = 0; ipage < pageCount; ipage++) {
	        PDPage page = document.getPage(ipage);
	        AMIGraphicsStreamEngine engine = new PDFPage2SVGConverter(renderer, page);
	        engine.run();
        }
        document.close();
	}
}
