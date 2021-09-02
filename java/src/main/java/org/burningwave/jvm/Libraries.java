/*
 * This file is part of Burningwave JVM driver.
 *
 * Author: Roberto Gentili
 *
 * Hosted at: https://github.com/burningwave/jvm-driver
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019-2021 Roberto Gentili
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.burningwave.jvm;
import java.util.Locale;


public class Libraries {
	String conventionedSuffix;
	String extension;
	
	private Libraries() {
		JVMInfo jVMInfo = JVMInfo.getInstance();
		if (jVMInfo.is32Bit()) {
			conventionedSuffix = "x32";
		} else if (jVMInfo.is64Bit()) {
			conventionedSuffix = "x64";
		}
	    String operatingSystemName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
		if ((operatingSystemName.indexOf("mac") >= 0) || (operatingSystemName.indexOf("darwin") >= 0)) {
			extension = "dylib";
		} else if (operatingSystemName.indexOf("win") >= 0) {
			extension = "dll";
		} else if (operatingSystemName.indexOf("nux") >= 0) {
			extension = "so";
		} else {
			Throwables.throwException("Unable to initialize {}: unsupported operating system ('{}')", this, operatingSystemName);
		}
	}
	
    public static Libraries getInstance() {
    	return Holder.getWithinInstance();
    }
    
    public static Libraries create() {
    	return new Libraries();
    }
	
	public void load(String libName) {
		Files.extractAndExecute(
			libName + "-" + conventionedSuffix + "." + extension,
			file ->
				System.load(file.getAbsolutePath())
		);
	}
	
	public void loadFor(Class<?> clazz) {
		load(clazz.getName().replace(".", "/"));
	}
	
	private static class Holder {
		private static final Libraries INSTANCE = Libraries.create();
		
		private static Libraries getWithinInstance() {
			return INSTANCE;
		}
	}
}