/**
 * Copyright (C) 2012 pm286 <peter.murray.rust@googlemail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xmlcml.pdf2svg;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.encoding.DictionaryEncoding;
import org.apache.pdfbox.encoding.Encoding;
import org.apache.pdfbox.pdfviewer.PageDrawer;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDMatrix;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.PDLineDashPattern;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorState;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.text.PDTextState;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.TextPosition;
import org.xmlcml.euclid.Angle;
import org.xmlcml.euclid.Real;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.euclid.Transform2;
import org.xmlcml.font.CodePoint;
import org.xmlcml.font.CodePointSet;
import org.xmlcml.font.NonStandardFontFamily;
import org.xmlcml.font.NonStandardFontManager;
import org.xmlcml.graphics.svg.GraphicsElement.FontStyle;
import org.xmlcml.graphics.svg.GraphicsElement.FontWeight;
import org.xmlcml.graphics.svg.SVGClipPath;
import org.xmlcml.graphics.svg.SVGDefs;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGImage;
import org.xmlcml.graphics.svg.SVGPath;
import org.xmlcml.graphics.svg.SVGRect;
import org.xmlcml.graphics.svg.SVGSVG;
import org.xmlcml.graphics.svg.SVGShape;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.graphics.svg.SVGTitle;
import org.xmlcml.graphics.svg.SVGUtil;
import org.xmlcml.graphics.svg.util.ImageIOUtil;
import org.xmlcml.pdf2svg.util.PDF2SVGUtil;

/** converts a PDPage to SVG
 * Originally used PageDrawer to capture the PDF operations.These have been
 * largely intercepted and maybe PageDrawer could be retired at some stage
 * @author pm286 and Murray Jensen
 *
 */
public class PDFPage2SVGConverter extends PageDrawer {
	
	private static final double DEFAULT_FONT_SIZE = 8.0;
	private static final int _BOLD_FONT_MIN = 410;
	private static final String SYMBOL = "Symbol";
	private static final String ENCODING = "Encoding";
	private static final String ITALIC = "italic";
	private static final String CLIP_PATH = "clipPath";

	private final static Logger LOG = Logger.getLogger(PDFPage2SVGConverter.class);

	// only use if mediaBox fails to give dimension
	private static final Dimension DEFAULT_DIMENSION = new Dimension(800, 800);
	private static final int BADCHAR = (char)0X2775;
	static {
		LOG.setLevel(Level.DEBUG);
	}

//	private static double eps = 0.001;

	private BasicStroke basicStroke;
	private SVGSVG convertedPageSVG;
	private PDGraphicsState graphicsState;
	private Matrix testMatrix;
	private PDFont pdFont;

	private String fontFamilyName;
	private String fontName;
	
	private int nPlaces = 3;
	private PDLineDashPattern dashPattern;
	private Double lineWidth;
	private Set<String> clipStringSet;
	private String clipString;
	private PDF2SVGConverter pdf2svgConverter;
	private Encoding encoding; // to distinguish from content-type encoding
	private String charname;
	private Integer charCode = null;
	private Real2 currentXY;
	private String fontSubType;
	private String textContent;
	private NonStandardFontManager amiFontManager;
	private boolean charWasLogged = false;

	private AMIFont amiFont;
	private String lastFontName;

	private HashMap<String, Integer> integerByClipStringMap;
	private SVGElement defs1;
	private boolean reportedEncodingError = false;
	private TextPosition textPosition;


	private boolean annotateText;
	private int debugCount = 0;
	private static int MAX_DEBUG = 0;

	private Set<String> weightSet = new HashSet<String>();
	
	public PDFPage2SVGConverter() throws IOException {
		super();
	}

	/** called for each page by PDF2SVGConverter
	 * 
	 * @param page
	 * @param converter
	 */
	public SVGSVG convertPageToSVG(PDPage page, PDF2SVGConverter converter) {
		debugCount = 0;
		pageSize = null;	// reset size for each page
		this.pdf2svgConverter = converter;
		this.amiFontManager = converter.getAmiFontManager();
		amiFontManager.setNullFontDescriptorReport(true);
		createSVGSVG();
		drawPage(page);
		return convertedPageSVG;
	}
	
	void drawPage(PDPage p) {
		LOG.trace("startPage");
		ensurePageSize();
		page = p;
		reportedEncodingError = false;

		try {
			if (page.getContents() != null) {
				PDResources resources = page.findResources();
				ensurePageSize();
				// can be very slow - 35 secs/page sometimes
				processStream(page, resources, page.getContents().getStream());
			}
		} catch (Exception e) {
			// PDFBox routines have a very bad feature of trapping exceptions
			// this is the best we can do to alert you at this stage
			e.printStackTrace();
			LOG.error("***FAILED " + e);
			throw new RuntimeException("drawPage", e);
		}
		createDefsForClipPaths();
		if (pdf2svgConverter.drawBoxesForClipPaths) {
			drawBoxesForClipPaths();
		}
		LOG.trace("WEIGHT "+weightSet);
	}

	private void drawBoxesForClipPaths() {
		ensureClipStringSet();
		String[] color = {"yellow", "blue", "red", "green", "magenta", "cyan"};
		LOG.trace("Clip paths: "+clipStringSet.size());
		int icol = 0;
		for (String shapeString : clipStringSet) {
			LOG.trace("Shape: "+shapeString);
			if (shapeString != null && shapeString.trim().length() > 0) {
				SVGPath path = new SVGPath(shapeString);
				Real2Range bbox = path.getBoundingBox();
				SVGShape box = null;
				box = new SVGRect(bbox);
				box.setFill("none");
				box.setStroke(color[icol]);
				box.setOpacity(1.0);
				box.setStrokeWidth(2.0);
				convertedPageSVG.appendChild(box);
				icol = (icol+1) % 6;
			}
		}
	}

	private void createDefsForClipPaths() {
//   <clipPath clipPathUnits="userSpaceOnUse" id="clipPath14">
//	    <path stroke="black" stroke-width="0.5" fill="none" d="M0 0 L89.814 0 L89.814 113.7113 L0 113.7113 L0 0 Z"/>
//	  </clipPath>
		ensureIntegerByClipStringMap();
		ensureDefs1();
		for (String pathString : integerByClipStringMap.keySet()) {
			Integer serial = integerByClipStringMap.get(pathString);
			SVGClipPath clipPath = new SVGClipPath();
			clipPath.setId(CLIP_PATH+serial);
			defs1.appendChild(clipPath);
			SVGPath path = new SVGPath();
			path.setDString(pathString);
			clipPath.appendChild(path);
		}
	}

    /**
     * DUPLICATE OF SUPER SO WE CAN DEBUG
     * This will draw the page to the requested context.
     *
     * @param g The graphics context to draw onto.
     * @param p The page to draw.
     * @param pageDimension The size of the page to draw.
     *
     * @throws IOException If there is an IO error while drawing the page.
     */
    public void drawPage( Graphics g, PDPage p, Dimension pageDimension ) throws IOException {
    	super.drawPage(g, p, pageDimension);
    	// cannot use this because private
    	// graphics = (Graphics2D)g;
        Graphics2D g2d = (Graphics2D)g;
//	        g2d = (Graphics2D)g;
        page = p;
        pageSize = pageDimension;
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g2d.setRenderingHint( RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON );
        // Only if there is some content, we have to process it. 
        // Otherwise we are done here and we will produce an empty page
        if ( page.getContents() != null) {
            PDResources resources = page.findResources();
            processStream( page, resources, page.getContents().getStream() );
        }
        List annotations = page.getAnnotations();
        if (annotations.size() > 0) {
        	throw new RuntimeException("ANNOTATIONS");
        }
        for( int i=0; i<annotations.size(); i++ ) {
            PDAnnotation annot = (PDAnnotation)annotations.get( i );
            PDRectangle rect = annot.getRectangle();
            String appearanceName = annot.getAppearanceStream();
            PDAppearanceDictionary appearDictionary = annot.getAppearance();
            if( appearDictionary != null ) {
                if( appearanceName == null ) {
                    appearanceName = "default";
                }
                Map appearanceMap = appearDictionary.getNormalAppearance();
                if (appearanceMap != null) { 
                    PDAppearanceStream appearance = 
                        (PDAppearanceStream)appearanceMap.get( appearanceName ); 
                    if( appearance != null ) { 
                        g.translate( (int)rect.getLowerLeftX(), (int)-rect.getLowerLeftY() ); 
                        processSubStream( page, appearance.getResources(), appearance.getStream() ); 
                        g.translate( (int)-rect.getLowerLeftX(), (int)+rect.getLowerLeftY() ); 
                    }
                }
            }
        }
    }


	private void ensureDefs1() {
/*
<svg fill-opacity="1" 
xmlns="http://www.w3.org/2000/svg">
  <defs id="defs1">
   <clipPath clipPathUnits="userSpaceOnUse" id="clipPath1">
    <path stroke="black" stroke-width="0.5" fill="none" d="M0 0 L595 0 L595 793 L0 793 L0 0 Z"/>
   </clipPath>
   </defs>
 */
		List<SVGElement> defList = SVGUtil.getQuerySVGElements(convertedPageSVG, "/svg:g/svg:defs[@id='defs1']");
		defs1 = (defList.size() > 0) ? defList.get(0) : null;
		if (defs1 == null) {
			defs1 = new SVGDefs();
			defs1.setId("defs1");
			convertedPageSVG.insertChild(defs1, 0);
		}
	}

	/** adds a default pagesize if not given
	 * 
	 */
	private void ensurePageSize() {
		if (pageSize == null) {
			if (page != null) {
				PDRectangle mediaBox = page.findMediaBox();
				pageSize = mediaBox == null ? null : mediaBox.createDimension();
				pageSize = pageSize == null ? DEFAULT_DIMENSION : pageSize;
				LOG.trace("set dimension: "+pageSize);
			}
		}
	}
	

	@Override
	protected void processTextPosition(TextPosition textPosition) {
		this.textPosition = textPosition;
		charname = null;
		charWasLogged = false;

		pdFont = textPosition.getFont();
		try {
			amiFont = amiFontManager.getAmiFontByFont(pdFont);
		} catch (Exception e) {
			LOG.error("bad font: "+e);
			return;
		}
		setAndProcessFontNameAndFamilyName();
		debugFont();
		getCharCodeAndSetEncodingAndCharname();

		SVGText svgText = new SVGText();
		
		createGraphicsStateAndPaintAndComposite(svgText);
		getAndFormatClipPath();

		if (pdf2svgConverter.useXMLLogger) {
			pdf2svgConverter.xmlLogger.newFont(amiFont);
			if (pdf2svgConverter.xmlLoggerLogGlyphs) {
				captureAndIndexGlyphVector();
			}
		}

		createAndReOrientateTextPosition(svgText);


		if (amiFont.isSymbolic() || amiFont.getDictionaryEncoding() != null ||
				(amiFont.getNonStandardFontFamily() != null && amiFont.getNonStandardFontFamily().getCodePointSet() != null)) {
//			convertNonUnicodeCharacterEncodings();
//			annotateContent(svgText, textContent, charCode, charname, charCode, encoding);
		}
		if (SYMBOL.equalsIgnoreCase(fontFamilyName)) {
			LOG.trace("symbol >> "+charname+"/"+charCode+"/"+Integer.toHexString(charCode)+" w "+pdFont.getFontWidth(charCode));
		}
		debugChar();

		LOG.trace("Fn: "+fontName+"; Ff: "+fontFamilyName+"; "+textContent+"; "+charCode+"; "+charname+" w "+pdFont.getFontWidth(charCode));

		addContentAndAttributesToSVGText(svgText);
		changeFontStyles(svgText);
		convertedPageSVG.appendChild(svgText);
	}

	private void changeFontStyles(SVGText svgText) {
		useStandardFonts(svgText);
		computeBold(svgText);
		computeItalic(svgText);
		computeFixedPitch(svgText);
	}

	private void computeBold(SVGText svgText) {
/**	     * The weight of the font.  According to the PDF spec "possible values are
	     * 100, 200, 300, 400, 500, 600, 700, 800 or 900"  Where a higher number is
	     * more weight and appears to be more bold. */

		FontWeight fontWeight = FontWeight.NORMAL;
		Float weight = amiFont.getFontWeightFloat();

		if (weight > 0.0) {
			String ww = weight.toString();
			if (!weightSet.contains(ww)) {
				LOG.trace("WEIGHT "+weight);
				weightSet .add(ww);
			}
		}
		if (weight > _BOLD_FONT_MIN) {
			fontWeight = FontWeight.BOLD;
		}
//		amiFont.getOrCreateAMIFontFamily(amiFontManager);
//		Boolean forceBold = amiFont.isForceBold();
		Boolean forceBold = (amiFont.getNonStandardFontFamily() == null) ? null : amiFont.getNonStandardFontFamily().isForceBold();
		if (forceBold != null && forceBold) {
			fontWeight = FontWeight.BOLD;
		}
		if (amiFont.isHeuristicBold()) {
			fontWeight = FontWeight.BOLD;
		}
		svgText.setFontWeight(fontWeight);
	}

	private void computeFixedPitch(SVGText svgText) {

		Boolean fixedPitch = amiFont.isFixedPitch();
		if (!fixedPitch) {
			fixedPitch = amiFont.isHeuristicFixedPitch();
			LOG.trace("FIXED PITCH: "+fixedPitch);
		}
		if (fixedPitch) {
			LOG.trace("FIXED PITCH!!!!!!!!!: "+fixedPitch);
			svgText.setFontFamily(NonStandardFontFamily.DEFAULT_MONOSPACED_FONT);
//			svgText.debug("FIXED");
		}
	}

	private void computeItalic(SVGText svgText) {
		Boolean italic = amiFont.isItalic();
		if (!italic) {
			Float angle = amiFont.getItalicAngle();
			italic = angle > 0.001;
		}
		if (!italic) {
			italic = amiFont.isHeuristicItalic();
		}
		if (italic) {
			svgText.setFontStyle(FontStyle.ITALIC);
		}
	}

	private void useStandardFonts(SVGText svgText) {
		if (amiFont.isFixedPitch() || amiFont.isHeuristicFixedPitch()) {
			svgText.setFontFamily(NonStandardFontFamily.DEFAULT_MONOSPACED_FONT);
		} else if (amiFont.isSerif()) {
			svgText.setFontFamily(NonStandardFontFamily.DEFAULT_SERIF_FONT);
		} else if (!amiFont.isSerif()) {
			svgText.setFontFamily(NonStandardFontFamily.DEFAULT_SANS_SERIF_FONT);
		} else {
			
		}
	}

	private void debugChar() {
		if (pdf2svgConverter.debugCharname != null && pdf2svgConverter.debugCharname.equals(charname)) {
			LOG.debug("Logging charname: "+charname);
		}
		if (pdf2svgConverter.debugCharCode != null && pdf2svgConverter.debugCharCode.equals(charCode)) {
			LOG.debug("Logging charCode: "+charCode);
		}
	}
	private void debugFont() {
		if (pdf2svgConverter.debugFontName != null && pdf2svgConverter.debugFontName.equals(fontName)) {
			LOG.debug("Requested debug fontName: "+fontName+ " / "+amiFont +" / "+amiFont.isHeuristicFixedPitch());
		}
	}

	private void setAndProcessFontNameAndFamilyName() {
		fontName = amiFont.getFontName();
		if (fontName == null) {
//			throw new RuntimeException("Null font name: "+amiFont);
			LOG.error("Null font name: "+amiFont);
			return;
		} else if (!fontName.equals(lastFontName)) {
			LOG.trace("font from "+lastFontName+" -> "+fontName);
			lastFontName = fontName;
		}
		fontFamilyName = amiFont.getFontFamilyName();
		NonStandardFontFamily amiFontFamily = amiFontManager.getFontFamilyByFamilyName(fontFamilyName);
		amiFont.setNonStandardFontFamily(amiFontFamily);
	}

	private void getCharCodeAndSetEncodingAndCharname() {

		encoding = amiFont.getEncoding();
		textContent = textPosition.getCharacter();
		charCode = -1;
		if (encoding == null) {
			int[] codePoints = textPosition.getCodePoints();
			if (textContent == null && codePoints != null) {
				charCode = codePoints[0];
				LOG.trace("charCode "+charCode);
				textContent = String.valueOf((char) (int) charCode);
			}
			annotateText = false;
		} else {
			if (textContent.length() > 1) {
				// this can happen for ligatures
				LOG.trace("multi-char string: "+textContent);
			} 
			charCode = new Integer(textContent.charAt(0));
		}

		annotateText = false;
		if (encoding == null) {
			if (!reportedEncodingError ) {
//				PDFont pdFont = amiFont.getPDFont();
//				byte[] bytes = new byte[1];
//				bytes[0] = (byte) charCode;
//				String charString = null;
//				try {
//					charString = pdFont.encode(bytes, 0, 1);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				System.out.println((charString == null) ? null : (charString+" / "+(int)charString.charAt(0)));
//				PDSimpleFont pdSimpleFont = (PDSimpleFont)pdFont;
//				Encoding encoding1 = pdFont.getFontEncoding();
//				PDFontDescriptor fontDescriptor = amiFont.getFontDescriptor();
//				COSDictionary cosDict = (COSDictionary) ((pdFont == null) ? null : ((PDSimpleFont) pdFont).getToUnicode());
				LOG.trace("Null encoding for character: "+charname+" / "+charCode+" at "+currentXY+" font: "+fontName+" / "+
			       fontFamilyName+" / "+amiFont.getBaseFont()+
			       "\n                FURTHER NULL ENCODING ERRORS HIDDEN");
				reportedEncodingError = true;
			}
			
		} else {
			getCharnameThroughEncoding();
		}
		convertCharactersToUnicode();
	}

	private void convertCharactersToUnicode() {
		// must have a codePointSet
		CodePointSet codePointSet = amiFont.getNonStandardFontFamily().getCodePointSet();
		// no point if its already unicode
		if (codePointSet != null && !codePointSet.isUnicodeEncoded()) {
			// use charname first
			CodePoint codePoint = (charname != null) ? codePointSet.getByName(charname) : null;
			if (codePoint == null) {
				charCode = textContent.codePointAt(0);
				// try charCode as last resort
				codePoint = codePointSet.getByDecimal(charCode);
			}
			annotateText = true;
			if (codePoint != null) {
				charCode = codePoint.getUnicodeDecimal();
				textContent = String.valueOf((char)(int)charCode);
				annotateText = false;
			}
		}
	}

	private void getCharnameThroughEncoding() {
		try {
			// NOTE: charname is the formal name for the character such as "period", "bracket" or "a", "two"
			charname = encoding.getName(charCode);
			LOG.trace("code "+charCode+" (font: "+fontSubType+" "+fontName+") "+charname);
		} catch (IOException e1) {
			LOG.warn("cannot get char encoding "+" at "+currentXY, e1);
			annotateText = true;
		}
	}                                                                                                    

	private void addContentAndAttributesToSVGText(SVGText svgText) {
		try {
			svgText.setText(textContent);
		} catch (nu.xom.IllegalCharacterDataException e) {
			System.err.println("?"+textContent);
			// drops here if cannot encode as XML character
			annotateText = true;
		}
		
		getFontSizeAndSetNotZeroRotations(svgText);
		float width = getCharacterWidth(pdFont, textContent);
		if (width < 0.01) {
			Encoding encoding = pdFont.getFontEncoding();
			if (encoding instanceof DictionaryEncoding) {
				DictionaryEncoding dictionaryEncoding = (DictionaryEncoding) encoding;
				LOG.trace(dictionaryEncoding);
			}
		}
		ensureHighSurrogatePoints(svgText);
		addAttributesToSVGText(width, svgText);
		addTooltips(svgText);
		if (amiFont.isItalic() != null && amiFont.isItalic()) {
			svgText.setFontStyle(ITALIC);
		}
		if (SYMBOL.equals(svgText.getFontFamily())) {
			svgText.setFontFamily("Symbol-X"); // to stop browsers misbehaving
		}
		if (annotateText) {
			annotateUnusualCharacters(svgText);
		}
	}


	private void convertNonUnicodeCharacterEncodings() {
		CodePointSet codePointSet = amiFont.getNonStandardFontFamily().getCodePointSet();
		if (codePointSet != null) {
			CodePoint codePoint = null;
			if (charname != null) {	
				codePoint = codePointSet.getByName(charname);
			} else {
				codePoint = codePointSet.getByDecimal((int)textContent.charAt(0));
			}
			if (codePoint == null) {
				//or add Bad Character Glyph
				if (pdf2svgConverter.useXMLLogger && !charWasLogged) {
					pdf2svgConverter.xmlLogger.newCharacter(fontName, fontFamilyName, charname, charCode);
					charWasLogged = true;
				} else {
					LOG.error("Cannot convert character: "+textContent+" char: "+charCode+" charname: "+charname+" fn: "+fontFamilyName);
				}
				textContent = String.valueOf(NonStandardFontManager.getUnknownCharacterSymbol()+charCode);
			} else {
				Integer codepoint = codePoint.getUnicodeDecimal();
				textContent = String.valueOf((char)(int) codepoint);
				if (pdf2svgConverter.useXMLLogger && pdf2svgConverter.xmlLoggerLogMore && !charWasLogged) {
					int ch = (int) textContent.charAt(0);
					pdf2svgConverter.xmlLogger.newCharacter(fontName, fontFamilyName, charname, ch);
					charWasLogged = true;
				}
			}
		}
	}

	private void captureAndIndexGlyphVector() {
		String key = charname;
		if (key == null) {
			key = "" + charCode;
		}
		String pathString = amiFont.getPathStringByCharnameMap().get(key);
		LOG.trace("charname: "+charname+" path: "+pathString);
		if (pathString == null) {
			ensurePageSize();
			PDFGraphics2D graphics = new PDFGraphics2D(amiFont);
			Matrix textPos = textPosition.getTextPos().copy();
			float x = textPos.getXPosition();
			// the 0,0-reference has to be moved from the lower left (PDF) to
			// the upper left (AWT-graphics)
			float y = pageSize.height - textPos.getYPosition();
			// Set translation to 0,0. We only need the scaling and shearing
			textPos.setValue(2, 0, 0);
			textPos.setValue(2, 1, 0);
			// because of the moved 0,0-reference, we have to shear in the
			// opposite direction
			textPos.setValue(0, 1, (-1) * textPos.getValue(0, 1));
			textPos.setValue(1, 0, (-1) * textPos.getValue(1, 0));
			AffineTransform at = textPos.createAffineTransform();
			PDMatrix fontMatrix = pdFont.getFontMatrix();
			// matrix is r00 r01 r10 r11 t0 t1
			double r00 = fontMatrix.getValue(0, 0) * 1000f; 
			double r11 = fontMatrix.getValue(1, 1) * 1000f; 
			LOG.trace("scales: "+r00+"/"+r11);
			at.scale(r00, r11);
			// TODO setClip() is a massive performance hot spot. Investigate
			// optimization possibilities
			if (graphicsState == null) {
				LOG.debug("NULL graphics state");
//				return;
			} else {
				graphics.setClip(graphicsState.getCurrentClippingPath());
			}
			// the fontSize is no longer needed as it is already part of the
			// transformation
			// we should remove it from the parameter list in the long run
			try {
				pdFont.drawString(textPosition.getCharacter(), textPosition.getCodePoints(),
						graphics, 1, at, x, y);
			} catch (IOException e) {
				throw new RuntimeException("font.drawString", e);
			}
			pathString = graphics.getCurrentPathString();
			LOG.trace(charname+": created "+pathString);
			amiFont.getPathStringByCharnameMap().put(key, pathString);
		}
		LOG.trace("pathString: "+pathString);
	}

	private void addTooltips(SVGText svgText) {
		if (pdf2svgConverter.addTooltipDebugTitles) {
			addTitleChildAsTooltip(svgText);
		}
	}

	private void addTitleChildAsTooltip(SVGText svgText) {
		String enc = (encoding == null) ? null : encoding.getClass().getSimpleName();
		enc =(enc != null && enc.endsWith(AMIFont.ENCODING)) ? enc.substring(0, enc.length()-AMIFont.ENCODING.length()) : enc;
		String title = "char: "+charCode+"; name: "+charname+"; f: "+fontFamilyName+"; fn: "+fontName+"; e: "+enc;
		SVGTitle svgTitle = new SVGTitle(title);
		svgText.appendChild(svgTitle);
	}

	private void addAttributesToSVGText(float width, SVGText svgText) {
		setClipPath(svgText, clipString, (Integer) integerByClipStringMap.get(clipString));
		svgText.setFontFamily(fontFamilyName);
		setFontName(svgText, fontName);
		setCharacterWidth(svgText, width);
		outputHighOrInconsistentCharCodeAttributes(svgText);
		svgText.format(nPlaces);
	}

	private void ensureHighSurrogatePoints(SVGText svgText) {
		if (charCode != null && charCode > 0xFFFF) {
			StringBuffer sb = new StringBuffer();
			sb.appendCodePoint((int)charCode);
			String s = sb.toString();
			svgText.setText(s);
		}
	}

	private void outputHighOrInconsistentCharCodeAttributes(SVGText svgText) {
		String value = svgText.getValue();
		Integer textChar = (value == null || value.length() == 0) ? null : (int) value.charAt(0);
		if (textChar == null || textChar > 127 || !textChar.equals(charCode)) {
			setCharName(svgText, charname);
			setCharCode(svgText, charCode);
			if (charCode == null || !charCode.equals(textChar)) {
				setTextChar(svgText, textChar);
			}
		}
	}

	private void setFontName(SVGElement svgElement, String fontName) {
		if (fontName != null) {
			PDF2SVGUtil.setSVGXAttribute(svgElement, NonStandardFontManager.FONT_NAME, fontName);
		} else {
			LOG.error("NULL font name");
		}
	}
	
	private void setCharacterWidth(SVGElement svgElement, double width) {
		PDF2SVGUtil.setSVGXAttribute(svgElement, PDF2SVGUtil.CHARACTER_WIDTH, String.valueOf(width));
	}
	
	private void setCharCode(SVGText svgText, Integer charCode) {
		if (charCode != null) {
			PDF2SVGUtil.setSVGXAttribute(svgText, PDF2SVGUtil.CHARACTER_CODE, String.valueOf(charCode));
			PDF2SVGUtil.setSVGXAttribute(svgText, PDF2SVGUtil.CHARACTER_HEX, Integer.toHexString(charCode));
		}
	}

	private void setTextChar(SVGText svgText, Integer textChar) {
		if (textChar != null) {
			PDF2SVGUtil.setSVGXAttribute(svgText, PDF2SVGUtil.TEXT_CHAR, String.valueOf(textChar));
			PDF2SVGUtil.setSVGXAttribute(svgText, PDF2SVGUtil.TEXT_HEX, Integer.toHexString(textChar));
		}
	}

	private void setCharName(SVGText svgText, String charName) {
		if (charName != null) {
			PDF2SVGUtil.setSVGXAttribute(svgText, PDF2SVGUtil.CHARACTER_NAME, charName);
		}
	}


	
	private void annotateContent(SVGText svgText, String unicodeContent, int charCode, String charname, int newCode, Encoding fontEncoding) {
//		try {
//			svgText.setText(unicodeContent);
//		} catch (Exception e) {
////			if (pdf2svgConverter.useXMLLogger && !charWasLogged) {
////				pdf2svgConverter.xmlLogger.newCharacter(fontName, fontFamilyName, charname, charCode);
////				charWasLogged = true;
////			}
////			else
//				LOG.error("couldn't set unicode: "+unicodeContent+" / +font: "+fontName+" charname: "+charname+" "+charCode+" / "+e);
//			svgText.setText("?"+(int)charCode);
//		}
		if (unicodeContent.length() > 1) {
			PDF2SVGUtil.setSVGXAttribute(svgText, PDF2SVGUtil.LIGATURE, String.valueOf(unicodeContent.length()));
		}
		PDF2SVGUtil.setSVGXAttribute(svgText, PDF2SVGUtil.CHARACTER_CODE, String.valueOf(charCode));
		String fontEnc = (fontEncoding == null) ? "null" : fontEncoding.getClass().getSimpleName();
		if (fontEnc.endsWith(ENCODING)) {
			fontEnc = fontEnc.substring(0, fontEnc.length()-ENCODING.length());
		}
		PDF2SVGUtil.setSVGXAttribute(svgText, PDF2SVGUtil.FONT_ENCODING, String.valueOf(fontEnc));
		if (charname != null) {
			PDF2SVGUtil.setSVGXAttribute(svgText, PDF2SVGUtil.CHARACTER_NAME, String.valueOf(charname));
		}
		if (newCode != charCode) {
			PDF2SVGUtil.setSVGXAttribute(svgText, PDF2SVGUtil.CHARACTER_NEW_CODE, String.valueOf(newCode));
		}
		if (pdf2svgConverter.useXMLLogger && pdf2svgConverter.xmlLoggerLogMore && !charWasLogged) {
			pdf2svgConverter.xmlLogger.newCharacter(fontName, fontFamilyName, charname, charCode);
			charWasLogged = true;
		}
	}

	private String getAndFormatClipPath() {
		Shape shape = getGraphicsState().getCurrentClippingPath();
		SVGPath path = new SVGPath(shape);
		path.format(nPlaces);
		clipString = path.getDString();
		// old approach
		ensureClipStringSet();
		clipStringSet.add(clipString);
		// new approach
		ensureIntegerByClipStringMap();
		if (!integerByClipStringMap.containsKey(clipString)) {
			integerByClipStringMap.put(clipString, integerByClipStringMap.size()+1); // count from 1
		}
		return clipString;
	}

	private void ensureIntegerByClipStringMap() {
		if (integerByClipStringMap == null) {
			integerByClipStringMap = new HashMap<String, Integer>();
		}
	}

	private void ensureClipStringSet() {
		if (clipStringSet == null) {
			clipStringSet = new HashSet<String>();
		}
	}

	private float getCharacterWidth(PDFont font, String textContent) {
		float width = 0.0f;
		try {
			width = font.getStringWidth(textContent);
			if (/*Math.abs(1000 - width) < 0.1 && */textContent.equals("I")) {
//				for (int i = 32; i < 128; i++) {
//					System.out.print((char)i+": "+font.getStringWidth(String.valueOf((char)i)));
//				}
//				LOG.debug("font: "+font.getBaseFont()+" width: "+width+" ("+textContent+")"+(int)textContent.charAt(0));
			}
//			LOG.debug("font: "+font.getBaseFont()+" width: "+width+" ("+textContent+")"+(int)textContent.charAt(0));
		} catch (IOException e) {
			throw new RuntimeException("PDFBox exception ", e);
		}
		return width;
	}

	private void annotateUnusualCharacters(SVGText svgText) {
		String s = NonStandardFontManager.BADCHAR_S+(int)charCode+NonStandardFontManager.BADCHAR_E;
		if (pdf2svgConverter.useXMLLogger && !charWasLogged) {
			pdf2svgConverter.xmlLogger.newCharacter(fontName, fontFamilyName, charname, charCode);
			charWasLogged = true;
		} else {
			if (debugCount < MAX_DEBUG) {
				LOG.debug(s+" "+fontName+" ("+fontSubType+") charname: "+charname);
				debugCount++;
			}
		}
		s = String.valueOf((char)(BADCHAR+Math.min(9, charCode)));
		svgText.setText(s);
		svgText.setStroke("red");
		svgText.setFill("red");
		svgText.setFontFamily("Helvetica");
		svgText.setStrokeWidth(0.5);
		addTitleChildAsTooltip(svgText);
	}

	/** translates java color to CSS RGB
	 * 
	 * @param paint
	 * @return CCC as #rrggbb (alpha is currently discarded)
	 */
	private static String getCSSColor(Paint paint) {
		String colorS = null;
		if (paint instanceof Color) {
			int r = ((Color) paint).getRed();
			int g = ((Color) paint).getGreen();
			int b = ((Color) paint).getBlue();
			// int a = ((Color) paint).getAlpha();
			int rgb = (r<<16)+(g<<8)+b;
			colorS = String.format("#%06x", rgb);
			if (rgb != 0) {
				LOG.trace("Paint "+rgb+" "+colorS);
			}
		}
		return colorS;
	}

	private double getFontSizeAndSetNotZeroRotations(SVGText svgText) {
		AffineTransform at = testMatrix.createAffineTransform();
		PDMatrix fontMatrix = pdFont.getFontMatrix();
		at.scale(fontMatrix.getValue(0, 0) * 1000f,
				fontMatrix.getValue(1, 1) * 1000f);
		double scalex = at.getScaleX();
		double scaley = at.getScaleY();
		double scale = Math.sqrt(scalex * scaley);
		Transform2 t2 = new Transform2(at);
		
		int angleDeg =0;
		Angle angle = t2.getAngleOfRotationNew();
		if (angle != null) {
			angleDeg = Math.round((float)angle.getDegrees());
			if (!Real.isZero(angleDeg, 0.0001)) LOG.trace("ANG "+angleDeg);
		}
		Angle skew = t2.getAngleOfSkew(0.001);
		if (skew != null) {
			double skewDeg = Math.round((float)angle.getDegrees());
			if (!Real.isZero(skewDeg, 0.0001)) LOG.trace("SKEW "+skewDeg+" "+charCode);
		}
		if (angleDeg != 0) {
			LOG.trace("Transform "+t2+" "+svgText.getText()+" "+at+" "+PDF2SVGUtil.getRealArray(fontMatrix));
			// do this properly later (only if scales are anisotropic and so far no evidence)
			scale = Math.sqrt(Math.abs(t2.elementAt(0, 1)*t2.elementAt(1, 0)));
			Transform2 t2a = Transform2.getRotationAboutPoint(angle, svgText.getXY());
			svgText.setTransform(t2a);
		}
		svgText.setFontSize(scale);
		if (scale < 1) {
			LOG.trace("scale "+scale);
		}
		return scale;
	}

	/** changes coordinates because AWT and SVG use top-left origin while PDF uses bottom left
	 * 
	 * @param textPosition
	 * @param svgText
	 */
	private void createAndReOrientateTextPosition(SVGText svgText) {
		ensurePageSize();
		testMatrix = textPosition.getTextPos().copy();
		float x = testMatrix.getXPosition();
		// the 0,0-reference has to be moved from the lower left (PDF) to
		// the upper left (AWT-graphics)
		float y = pageSize.height - testMatrix.getYPosition();
		// Set translation to 0,0. We only need the scaling and shearing
		testMatrix.setValue(2, 0, 0);
		testMatrix.setValue(2, 1, 0);
		// because of the moved 0,0-reference, we have to shear in the
		// opposite direction
		testMatrix.setValue(0, 1, (-1) * testMatrix.getValue(0, 1));
		testMatrix.setValue(1, 0, (-1) * testMatrix.getValue(1, 0));
		currentXY = new Real2(x, y);
		svgText.setXY(currentXY);
	}

	private void createGraphicsStateAndPaintAndComposite(SVGText svgText) {
		Paint paint = null;
		try {
			graphicsState = getGraphicsState();
			ensurePageSize();
			PDColorState colorState = null;
			switch (graphicsState.getTextState().getRenderingMode()) {
			case PDTextState.RENDERING_MODE_FILL_TEXT:
				// composite = graphicsState.getNonStrokeJavaComposite();
				colorState = graphicsState.getNonStrokingColor();
				paint = colorState.getJavaColor();
				if (paint == null) {
					paint = colorState.getPaint(pageSize.height);
				}
				svgText.setFill(getCSSColor(paint));
				break;
			case PDTextState.RENDERING_MODE_STROKE_TEXT:
				// composite = graphicsState.getStrokeJavaComposite();
				colorState = graphicsState.getStrokingColor();
				paint = colorState.getJavaColor();
				if (paint == null) {
					paint = colorState.getPaint(pageSize.height);
				}
				Double lineWidth = graphicsState.getLineWidth();
				svgText.setStroke(getCSSColor(paint));
				svgText.setStrokeWidth(lineWidth);
				break;
			case PDTextState.RENDERING_MODE_NEITHER_FILL_NOR_STROKE_TEXT:
				// basic support for text rendering mode "invisible"
				Color nsc = graphicsState.getStrokingColor().getJavaColor();
				float[] components = { Color.black.getRed(),
						Color.black.getGreen(), Color.black.getBlue() };
				paint = new Color(nsc.getColorSpace(), components, 0f);
				// composite = graphicsState.getStrokeJavaComposite();
				break;
			default:
				// TODO : need to implement....
				LOG.trace("Unsupported RenderingMode "
						+ this.getGraphicsState().getTextState()
								.getRenderingMode()
						+ " in PageDrawer.processTextPosition()."
						+ " Using RenderingMode "
						+ PDTextState.RENDERING_MODE_FILL_TEXT + " instead");
				// composite = graphicsState.getNonStrokeJavaComposite();
				paint = graphicsState.getNonStrokingColor().getJavaColor();
				svgText.setFill(getCSSColor(paint));
			}
		} catch (IOException e) {
			throw new RuntimeException("graphics state error???", e);
		}
	}

	/** traps any remaining unimplemented PageDrawer calls
	 * 
	 */
	public Graphics2D getGraphics() {
		LOG.error("getGraphics was called!!!!!!! (May mean method was not overridden) %n");
		return null;
	}

	public void fillPath(int windingRule) throws IOException {
		PDColorState colorState = getGraphicsState().getNonStrokingColor();
		Paint currentPaint = getCurrentPaint(colorState, "non-stroking");
		createAndAddSVGPath(windingRule, currentPaint);
	}

	public void strokePath() throws IOException {
		PDColorState colorState = getGraphicsState().getStrokingColor(); 
		Paint currentPaint = getCurrentPaint(colorState, "stroking");
		Integer windingRule = null;
		createAndAddSVGPath(windingRule, currentPaint);
	}

	/** processes both stroke and fill for paths
	 * 
	 * @param windingRule if not null implies fill else stroke
	 * @param currentPaint
	 */
	private void createAndAddSVGPath(Integer windingRule, Paint currentPaint) {
//		renderIntent = getGraphicsState().getRenderingIntent(); // probably ignorable at first (converts color maps)
		dashPattern = getGraphicsState().getLineDashPattern();
		lineWidth = getGraphicsState().getLineWidth();
//		PDTextState textState = getGraphicsState().getTextState();  // has things like character and word spacings // not yet used
		GeneralPath generalPath = getLinePath();
		if (windingRule != null) {
			generalPath.setWindingRule(windingRule);
		}
		SVGPath svgPath = new SVGPath(generalPath);
		clipString = getAndFormatClipPath();
		svgPath.setClipPath(clipString);
		setClipPath(svgPath, clipString, integerByClipStringMap.get(clipString));
		if (windingRule != null) {
			svgPath.setFill(getCSSColor(currentPaint));
		} else {
			svgPath.setStroke(getCSSColor(currentPaint));
		}
		if (dashPattern != null) {
			setDashArray(svgPath);
		}
		svgPath.setStrokeWidth(lineWidth);
		svgPath.format(nPlaces);
		convertedPageSVG.appendChild(svgPath);
		generalPath.reset();
	}

	private void setClipPath(SVGElement svgElement, String clipString, Integer clipPathNumber) {
		String urlString = "url(#clipPath"+clipPathNumber+")";
		svgElement.setClipPath(urlString);
	}

	private void setDashArray(SVGPath svgPath) {
		@SuppressWarnings("unchecked")
		List<Integer> dashIntegerList = (List<Integer>) dashPattern.getDashPattern();
		StringBuilder sb = new StringBuilder("");
		LOG.trace("COS ARRAY "+dashIntegerList.size());
		if (dashIntegerList.size() > 0) {
			for (int i = 0; i < dashIntegerList.size(); i++) {
				if (i > 0) {
					sb.append(" ");
				}
				sb.append(dashIntegerList.get(i));
			}
			svgPath.setStrokeDashArray(sb.toString());
			LOG.trace("dash "+dashPattern);
		}
	}

	private Paint getCurrentPaint(PDColorState colorState, String type) throws IOException {
		Paint currentPaint = colorState.getJavaColor();
		if (currentPaint == null) {
			currentPaint = colorState.getPaint(pageSize.height);
		}
		if (currentPaint == null) {
			LOG.trace("ColorSpace "
					+ colorState.getColorSpace().getName()
					+ " doesn't provide a " + type
					+ " color, using white instead!");
			currentPaint = Color.WHITE;
		}
		return currentPaint;
	}

	/** maye be removed later
	 * @throws IOException 
	 * 
	 */
	@Override
	public void drawImage(Image awtImage, AffineTransform at) {
//		System.out
//				.printf("\tdrawImage: awtImage='%s', affineTransform='%s', composite='%s', clip='%s'%n",
//						awtImage.toString(), at.toString(), getGraphicsState()
//								.getStrokeJavaComposite().toString(),
//						getGraphicsState().getCurrentClippingPath().toString());
		if (awtImage instanceof BufferedImage) {
			Transform2 t2 = new Transform2(at);
			BufferedImage bImage = (BufferedImage) awtImage;
			LOG.trace("IMAGE: x="+bImage.getMinX()+" y="+bImage.getMinY()+" h="+bImage.getHeight()+" w="+bImage.getWidth());
			int size = bImage.getHeight() * bImage.getWidth(); 
			if (size > pdf2svgConverter.maxInlineImageSize) {
				String filename = writeImage(bImage);
				createImageRef(t2, filename, bImage);
			} else {
				createImage(t2, bImage);
			}
		} else {
			LOG.warn("Image not incorporated");
		}
	}

	private String writeImage(BufferedImage bImage) {
		pdf2svgConverter.imageNumber++;
		String filename = null;
		File imageDirectory = pdf2svgConverter.getOrCreateImageDirectory();
		if (imageDirectory != null) {
			filename = createImageFilename();
			File imageFile =  imageDirectory == null ? null : new File(imageDirectory, filename);
			try {
				LOG.trace("imageFile "+imageFile.getCanonicalPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
			ImageIOUtil.writeImageQuietly(bImage, imageFile);
		}
		return filename;
	}

	private String createImageFilename() {
		return pdf2svgConverter.inputBasename+".p"+pdf2svgConverter.pageNumber+".i"+pdf2svgConverter.imageNumber+".png";
	}

	private void createImage(Transform2 t2, BufferedImage bImage) {
		SVGImage svgImage = new SVGImage();
		svgImage.setTransform(t2);
		try {
			svgImage.readImageDataIntoSrcValue(bImage, SVGImage.IMAGE_PNG);
			convertedPageSVG.appendChild(svgImage);
		} catch (Exception e) {
			LOG.error("Cannot convert image, skipping "+e);
		}
	}

	private void createImageRef(Transform2 t2, String filename, BufferedImage bImage) {
		SVGImage svgImage = new SVGImage();
		svgImage.setTransform(t2);
		try {
			svgImage.setHref(filename);
			svgImage.setXYWidthHeight(bImage);
			convertedPageSVG.appendChild(svgImage);
		} catch (Exception e) {
			LOG.error("Cannot convert image, skipping "+e);
		}
	}

	/** used in pageDrawer - shaded type of fill
	 * 
	 */
	@Override
	public void shFill(COSName shadingName) throws IOException {
		LOG.trace("Shading Fill Not Implemented");
	}

	/** creates new <svg> and removes/sets some defaults
	 * this is partly beacuse SVGFoo may have defaults (bad idea?)
	 */
	private void createSVGSVG() {
		this.convertedPageSVG = new SVGSVG();
		convertedPageSVG.setWidth(pdf2svgConverter.pageWidth);
		convertedPageSVG.setHeight(pdf2svgConverter.pageHeight);
		convertedPageSVG.setStroke("none");
		convertedPageSVG.setStrokeWidth(0.0);
		convertedPageSVG.addNamespaceDeclaration(PDF2SVGUtil.SVGX_PREFIX, PDF2SVGUtil.SVGX_NS);
		clipStringSet = new HashSet<String>();
	}

	public SVGSVG getConvertedPageSVG() {
		return convertedPageSVG;
	}

	@Override
	public void setStroke(BasicStroke basicStroke) {
		this.basicStroke = basicStroke;
	}

	@Override
	public BasicStroke getStroke() {
		return basicStroke;
	}



	
}
