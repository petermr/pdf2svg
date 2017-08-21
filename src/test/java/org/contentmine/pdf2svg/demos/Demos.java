package org.contentmine.pdf2svg.demos;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.contentmine.pdf2svg.AMIGraphicsStreamEngine;
import org.contentmine.pdf2svg.Fixtures;
import org.contentmine.pdf2svg.PDF2SVGConverter;
import org.contentmine.pdf2svg.PDF2SVGTransformer;
import org.contentmine.pdf2svg.PDFPage2SVGConverter;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.ranges.RangeException;

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
        File file = new File(Fixtures.EXAMPLES_DIR, "rendering/custom-render-demo.pdf");

        PDDocument doc = PDDocument.load(file);
        PDFRenderer renderer = new PDFRenderer(doc);
        PDPage page = doc.getPage(0);
        AMIGraphicsStreamEngine pdfPage2SVGConverter = new PDFPage2SVGConverter(new PDF2SVGTransformer(),renderer, page);
        try {
        	pdfPage2SVGConverter.processPage();
        } catch (RuntimeException e) {
        	LOG.error("******************MEND THIS***************");
        }
        doc.close();
    }
    
	@Test
    public void testCustom() throws Exception {
        File file = new File(Fixtures.EXAMPLES_DIR, "rendering/custom-render-demo.pdf");

        PDF2SVGTransformer pdf2svgTransformer = new PDF2SVGTransformer();
        // capture 
        pdf2svgTransformer.convert(file);
        
        
    }
    
	@Test
    public void testBasic() throws Exception {
		String[] fileRoots = {
//				"basic.pdf",
//				"blank.pdf",
//				"circle.pdf",
//				"dash.pdf",
				"line.pdf",
//				"text.pdf",
//				"white.pdf",
					};
		for (String fileRoot : fileRoots) {
			createSVG(new File(Fixtures.EXAMPLES_DIR, "rendering"), fileRoot);
		}
        
    }
    
	@Test
	@Ignore // too long
    public void testMDPI() throws Exception {
        File file = new File(Fixtures.MDPI_DIR, "materials-05-00027.pdf");

        PDF2SVGTransformer pdf2svgTransformer = new PDF2SVGTransformer();
        pdf2svgTransformer.convert(file);
        
    }
    
	@Test
	@Ignore // too long
	// bugs to mend
    public void testMDPI1() throws Exception {
        File file = new File(Fixtures.MDPI_DIR, "metabolites-02-00100.pdf");

        PDF2SVGTransformer pdf2svgTransformer = new PDF2SVGTransformer();
        pdf2svgTransformer.convert(file);
        
    }
    
	@Test
	@Ignore // too long
    public void testLancet() throws Exception {
        File file = new File(Fixtures.MISC_DIR, "Lancet.pdf");

        PDF2SVGTransformer pdf2svgTransformer = new PDF2SVGTransformer();
        pdf2svgTransformer.convert(file);
        
    }
    
	@Test
    public void testHindawi() throws Exception {
        File file = new File(Fixtures.MISC_DIR, "9872540.pdf");
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
	
// =============================================

	private void createSVG(File dir, String fileRoot) throws IOException {
		File file = new File(dir, fileRoot);
        PDF2SVGTransformer pdf2svgTransformer = new PDF2SVGTransformer();
        pdf2svgTransformer.convert(file);
	}

	
}
