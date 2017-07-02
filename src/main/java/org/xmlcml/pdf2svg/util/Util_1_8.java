package org.xmlcml.pdf2svg.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.encoding.Encoding;
import org.apache.pdfbox.text.TextPosition;

/** temporary class to gather instances of Classes and Methods in PDFBox1.8
 * These are missing in 2.0 and until we fid replacements this is a method to 
 * remove compile errors.
 * 
 * e.g. PDFont.getBaseFont() is trapped in
 *   static Util_1.8.getBaseFont(PDFont) which will trow a RuntimeException("NYI")
 *   
 *   Workarounds can then be found and inlined and Hopefully this class will disappear soon.
 *   
 * 
 * @author pm286
 *
 */
public class Util_1_8 {
	private static final Logger LOG = Logger.getLogger(Util_1_8.class);

	static {
		LOG.setLevel(Level.DEBUG);
	}

	public static String getCharacter(TextPosition textPosition) {
		throw new RuntimeException("MUST CONVERT PDFBox1.8 method TextPosition.getCharacter()");
	}

	public static String getBaseFont(PDFont pdFont) {
		throw new RuntimeException("MUST CONVERT PDFBox1.8 method PDFont.getBaseFont()");
	}

	public static Encoding getFontEncoding(PDFont pdFont) {
		throw new RuntimeException("MUST CONVERT PDFBox1.8 method PDFont.getFontEncoding()");
	}
}
