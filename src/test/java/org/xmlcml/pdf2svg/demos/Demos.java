package org.xmlcml.pdf2svg.demos;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.Test;
import org.xmlcml.pdf2svg.AMIGraphicsStreamEngine;
import org.xmlcml.pdf2svg.Fixtures;
import org.xmlcml.pdf2svg.PDF2SVGConverter;
import org.xmlcml.pdf2svg.PDF2SVGTransformer;
import org.xmlcml.pdf2svg.PDFPage2SVGConverter;

public class Demos {
	private static final Logger LOG = Logger.getLogger(Demos.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

	public static void main(String[] args) {
//			ebola1();
//			astro1();
		run("target/plot", "demos/plot/22649_Sada_2012-1.pdf");
//		run("target/gandhi", "demos/gandhi/sample.pdf");

	}

	private static void ebola1() {
		new PDF2SVGConverter().run(
				"-logger", 
				"-infofiles", 
				"-logglyphs", 
				"-outdir", "target/ebola", 
				"demos/ebola/roadmapsitrep_12Nov2014_eng.pdf"
		);
	}
	
	private static void astro1() {
		new PDF2SVGConverter().run(
				"-logger", 
				"-infofiles", 
				"-logglyphs", 
				"-outdir", "target/astro", 
				"demos/astro/0004-637X_778_1_1.pdf"
		);
	}
	
	@Test
    public void testPDFBox2_0Example() throws Exception {
        File file = new File("src/main/resources/org/apache/pdfbox/examples/rendering/",
                             "custom-render-demo.pdf");

        PDDocument doc = PDDocument.load(file);
        PDFRenderer renderer = new PDFRenderer(doc);
        PDPage page = doc.getPage(0);
        AMIGraphicsStreamEngine engine = new PDFPage2SVGConverter(renderer, page);
        engine.run();
        doc.close();
    }
    
	@Test
    public void testMDPI() throws Exception {
        File file = new File(Fixtures.MDPI_DIR, "materials-05-00027.pdf");

        PDF2SVGTransformer pdf2svgTransformer = new PDF2SVGTransformer();
        pdf2svgTransformer.convert(file);
        
    }
    

	
	private static void plot1() {
		new PDF2SVGConverter().run(
				"-logger", 
				"-infofiles", 
				"-logglyphs", 
				"-outdir", "target/plot", 
				"demos/plot/22649_Sada_2012-1.pdf"
		);
	}
	
	private static void run(String outdir, String infile) {
		new PDF2SVGConverter().run(
				"-logger", 
				"-infofiles", 
				"-logglyphs", 
				"-outdir", outdir, 
				infile
		);
	}
	
	
	
}
