/*
 * This file is part of ToolFactory JVM driver.
 *
 * Hosted at: https://github.com/toolfactory/jvm-driver
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019-2021 Luke Hutchison, Roberto Gentili
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
package io.github.toolfactory.jvm.function.catalog;


import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import io.github.toolfactory.jvm.Info;
import io.github.toolfactory.jvm.function.template.Supplier;
import io.github.toolfactory.jvm.util.ObjectProvider;


@SuppressWarnings("all")
public abstract class ConsulterSupplier implements Supplier<MethodHandles.Lookup> {
	protected MethodHandles.Lookup consulter;
	
	@Override
	public MethodHandles.Lookup get() {
		return consulter;
	}
	
	
	public static class ForJava7 extends ConsulterSupplier {
		
		public ForJava7(Map<Object, Object> context) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
			Field modes = MethodHandles.Lookup.class.getDeclaredField("allowedModes");
			consulter = MethodHandles.lookup();
			modes.setAccessible(true);
			modes.setInt(consulter, -1);
		}
		
		public static class ForSemeru extends ConsulterSupplier {
			public static final int PUBLIC = Modifier.PUBLIC;
			public static final int PRIVATE = Modifier.PRIVATE;
			public static final int PROTECTED = Modifier.PROTECTED;
			public static final int PACKAGE = 0x8;

			static final int INTERNAL_PRIVILEGED = 0x80;

			private static final int FULL_ACCESS_MASK = PUBLIC | PRIVATE | PROTECTED | PACKAGE;
			public ForSemeru(Map<Object, Object> context) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
				Field modes = MethodHandles.Lookup.class.getDeclaredField("accessMode");
				modes.setAccessible(true);
				consulter = MethodHandles.lookup();
				modes.setInt(consulter, INTERNAL_PRIVILEGED);
			}
			
		}
		
	}
	
	
	public static class ForJava9 extends ConsulterSupplier {
		
		public ForJava9(Map<Object, Object> context) {
			consulter = MethodHandles.lookup();
		}
		
	}

	
	public static class ForJava17 extends ConsulterSupplier {
		
		public ForJava17(Map<Object, Object> context) {
			sun.misc.Unsafe unsafe = ObjectProvider.get(context).getOrBuildObject(UnsafeSupplier.class, context).get();
			final long allowedModesFieldMemoryOffset = Info.Provider.getInfoInstance().is64Bit() ? 12L : 8L;
			consulter = MethodHandles.lookup();
			unsafe.putInt(consulter, allowedModesFieldMemoryOffset, -1);
		}
		
	}
	
	public static abstract class Hybrid extends	ConsulterSupplier {
		
		public static class ForJava17 extends Hybrid {
			
			public ForJava17(Map<Object, Object> context) {
				consulter = MethodHandles.lookup();
				io.github.toolfactory.narcissus.Narcissus.setAllowedModes(consulter, -1);
			}
			
		}
	}
	
}
