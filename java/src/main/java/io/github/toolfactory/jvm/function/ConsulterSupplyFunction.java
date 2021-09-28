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
package io.github.toolfactory.jvm.function;


import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import io.github.toolfactory.jvm.function.template.Function;
import io.github.toolfactory.jvm.function.util.FunctionAdapter;
import io.github.toolfactory.jvm.function.util.Resources;
import io.github.toolfactory.jvm.function.util.Streams;


@SuppressWarnings("unchecked")
public abstract class ConsulterSupplyFunction<F> extends FunctionAdapter<F, Class<?>, MethodHandles.Lookup> {

	
	public static class ForJava7 extends ConsulterSupplyFunction<Function<Class<?>, MethodHandles.Lookup>> {
		public ForJava7(Map<Object, Object> context) {
			Provider functionProvider = Provider.get(context);
			final MethodHandles.Lookup consulter = functionProvider.getFunctionAdapter(ConsulterSupplier.class, context).get();
			final MethodHandle privateLookupInMethodHandle = functionProvider.getFunctionAdapter(PrivateLookupInMethodHandleSupplier.class, context).get();
			final ThrowExceptionFunction throwExceptionFunction =
				functionProvider.getFunctionAdapter(ThrowExceptionFunction.class, context); 
			setFunction(
				new Function<Class<?>, MethodHandles.Lookup>() { 
					@Override
					public MethodHandles.Lookup apply(Class<?> cls) {
						try {
							return (MethodHandles.Lookup) privateLookupInMethodHandle.invoke(consulter, cls);
						} catch (Throwable exc) {
							return throwExceptionFunction.apply(exc);
						}
					}
				}
			);
		}
		
		@Override
		public MethodHandles.Lookup apply(Class<?> input) {
			return function.apply(input);
		}
		
	}
	
	public static class ForJava9 extends ConsulterSupplyFunction<java.util.function.Function<Class<?>, MethodHandles.Lookup>> {
		
		public ForJava9(Map<Object, Object> context) throws IOException, NoSuchFieldException, SecurityException {
			Provider functionProvider = Provider.get(context);
			try (
				InputStream inputStream =
					Resources.getAsInputStream(this.getClass().getClassLoader(), this.getClass().getPackage().getName().replace(".", "/") + "/ConsulterRetrieverForJDK9.bwc"
				);
			) {
				MethodHandle privateLookupInMethodHandle = functionProvider.getFunctionAdapter(PrivateLookupInMethodHandleSupplier.class, context).get();
				Class<?> methodHandleWrapperClass = functionProvider.getFunctionAdapter(
					DefineHookClassFunction.class, context
				).apply(Class.class, Streams.toByteArray(inputStream));
				functionProvider.getFunctionAdapter(SetFieldValueFunction.class, context).accept(
					methodHandleWrapperClass, methodHandleWrapperClass.getDeclaredField("consulterRetriever"),
					privateLookupInMethodHandle
				);
				setFunction((java.util.function.Function<Class<?>, MethodHandles.Lookup>)
					functionProvider.getFunctionAdapter(AllocateInstanceFunction.class, context).apply(methodHandleWrapperClass));
			}
		}

		
		@Override
		public MethodHandles.Lookup apply(Class<?> input) {
			return function.apply(input);
		}
		
	}
}
