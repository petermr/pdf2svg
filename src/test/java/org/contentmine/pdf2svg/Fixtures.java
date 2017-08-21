package org.contentmine.pdf2svg;

import java.io.File;

import org.contentmine.pdf2svg.util.PConstants;

public class Fixtures {
	public static final File PDF2SVG_DIR = new File("src/test/resources" + "/" + PConstants.PDF2SVG_ROOT);
	public static final File CODEPOINTS_DIR = new File(Fixtures.PDF2SVG_DIR, "codepoints/");
	public static final File EXAMPLES_DIR = new File(Fixtures.PDF2SVG_DIR, "examples/");
	public static final File MISC_DIR = new File(Fixtures.PDF2SVG_DIR, "misc/");
	public static final File MDPI_DIR = new File(Fixtures.PDF2SVG_DIR, "mdpi/"); 

}
