package org.xmlcml.pdf2svg;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.xmlcml.euclid.Real2;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGLine;
import org.xmlcml.graphics.svg.SVGPath;
import org.xmlcml.graphics.svg.SVGPathPrimitive;
import org.xmlcml.graphics.svg.SVGSVG;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.graphics.svg.path.PathPrimitiveList;
import org.xmlcml.xml.XMLUtil;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import nu.xom.Element;

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
	private StringBuilder debugBuilder;
	private SVGSVG svgBuilder;
	private PathPrimitiveList pathPrimitiveList;
	private Real2 currentPoint = new Real2(0.1, 0.1); // to avoid null pointer
	private Multiset<String> glyphSet;
	private Multiset<String> codePointSet;

	public PDF2SVGTransformer() {
        this.debugBuilder = new StringBuilder();
        this.svgBuilder = new SVGSVG();
        this.pathPrimitiveList = new PathPrimitiveList();
        this.glyphSet = HashMultiset.create();
        this.codePointSet = HashMultiset.create();
	}

	private void ensureCodePointSet() {
		if (codePointSet == null) {
	    	codePointSet = HashMultiset.create();
    	}
	}

	public void convert(File file) throws IOException {
		String fileRoot = FilenameUtils.getBaseName(file.toString());
        document = PDDocument.load(file);
        renderer = new PDFRenderer(document);
        pageCount = document.getNumberOfPages();
        LOG.debug("Page count: "+pageCount);
        for (int ipage = 0; ipage < pageCount; ipage++) {
	        PDPage page = document.getPage(ipage);
	        LOG.debug("page: "+ipage);
	        PDFPage2SVGConverter pdf2svgConverter = new PDFPage2SVGConverter(this, renderer, page);
	        pdf2svgConverter.run();
	        this.writePage(new File("target/debug/"+fileRoot+"/page_"+ipage+".txt"));
	        this.writeSVG(new File("target/debug/"+fileRoot+"/page_"+ipage+".svg"));
        }
        LOG.debug("CodePoints "+codePointSet);
        document.close();
	}
	
	private void writeSVG(File file) throws IOException {
		LOG.debug("wrote: "+file.getAbsolutePath());
		XMLUtil.debug(svgBuilder, file, 1);
		svgBuilder = new SVGSVG();
	}

	public void writePage(File file) throws IOException {
		LOG.debug("wrote: "+file.getAbsolutePath());
		FileUtils.write(file, debugBuilder.toString());
		debugBuilder = new StringBuilder();
	}

	public void debug(String string) {
		debugBuilder.append(string);
	}

	public void append(SVGElement element) {
		svgBuilder.appendChild(element);
	}

	public void addPrimitive(SVGPathPrimitive primitive) {
		pathPrimitiveList.add(primitive);
	}

	public void setCurrentPoint(Real2 point) {
//		System.out.println("** point "+point);
		this.currentPoint = point;
	}

	public void clearPath() {
		SVGPath path = new SVGPath(pathPrimitiveList);
		this.append(path);
		pathPrimitiveList =  new PathPrimitiveList();
	}

	public void moveTo(Real2 point) {
		this.currentPoint = point;
	}

	public void lineTo(Real2 point) {
		SVGLine line = new SVGLine(currentPoint, point);
		svgBuilder.appendChild(line);
		this.currentPoint = point;
	}

	public void addText(String txt) {
		txt = XMLUtil.removeNonXML(txt);
		SVGText text = new SVGText(currentPoint, txt);
		text.setFontSize(10.0);
		svgBuilder.appendChild(text);
	}

	public void addCodePoint(String codePointS) {
		ensureCodePointSet();
		codePointSet.add(codePointS);
	}


}
