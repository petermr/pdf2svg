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
package org.contentmine.pdf2svg;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/** this is a mess and needs refactoring
 * 
 * @author pm286
 *
 */
public class PDF2SVGConverterIT {

	public final static Logger LOG = Logger.getLogger(PDF2SVGConverterIT.class);

	@Test
	@Ignore
	public void testUsage() {
		PDF2SVGConverter converter = new PDF2SVGConverter();
		converter.run();
	}

	@Test
	@Ignore // FIXME needs to read and process files
	public void testBasenameOutdir() {
		File dir = new File("target", "page6");
		File file = new File("target/page6", "page6-page1.svg");

		dir.delete();
		file.delete();

		PDF2SVGConverter converter = new PDF2SVGConverter();
		converter.run("-outdir", "target", "-mkdir", "src/test/resources/page6.pdf");
		Assert.assertTrue("exists: ", dir.exists());
		Assert.assertTrue(dir.exists() && dir.isDirectory());
		Assert.assertTrue(file.exists() && file.isFile());
	}

	@Test
	public void testSimpleRun() {
		PDF2SVGConverter converter = new PDF2SVGConverter();
		converter.run("-outdir", "target/ajc", "src/test/resources/page6.pdf");
	}


	@Test
	//@Ignore
	public void testPage6() {
		File page6File = new File("target/ajc/page6-page1.svg"); // yes, this serial number is what is is output as
		if (page6File.exists()) {
			page6File.delete();
		}
		PDF2SVGConverter converter = new PDF2SVGConverter();
		converter.run("-outdir", "target/ajc", "-pages", "1", "-storesvg", "src/test/resources/page6.pdf");
	}

	@Test
	public void testWord() {
		PDF2SVGConverter converter = new PDF2SVGConverter();
		converter.run("-outdir", "target/word/", "src/test/resources/word/test.pdf");
	}

	@Test
	public void testWordMath() {
		PDF2SVGConverter converter = new PDF2SVGConverter();
		converter.run("-outdir", "target/word/", "src/test/resources/word/testmath.pdf");
	}

	@Test
	public void testWordMath1() {
		PDF2SVGConverter converter = new PDF2SVGConverter();
		converter.run("-outdir", "target/word/", "src/test/resources/word/testmath1.pdf");
	}


}
