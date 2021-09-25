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
package io.github.toolfactory.jvm;


import java.io.Closeable;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;


@SuppressWarnings("all")
public class DefaultDriver implements Driver {

	MethodHandle getDeclaredFieldsRetriever;
	MethodHandle getDeclaredMethodsRetriever;
	MethodHandle getDeclaredConstructorsRetriever;
	MethodHandle methodInvoker;
	MethodHandle constructorInvoker;

	Object loadedClassesRetriever;
	Object loadedPackagesRetriever;
	Object allocateInstanceInvoker;
	Object fieldValueRetriever;
	Object fieldValueSetter;
	Object accessibleSetter;
	Object consulterRetriever;
	Object packageRetriever;
	Object hookClassDefiner;

	Class<?> classLoaderDelegateClass;
	Class<?> builtinClassLoaderClass;

	public DefaultDriver() {
		retrieveInitializer().start().close();
	}

	Initializer retrieveInitializer() {
		JVMInfo jVMInfo = JVMInfo.getInstance();
		return
			(jVMInfo.getVersion() > 8 ?
				jVMInfo.getVersion() > 13 ?
					jVMInfo.getVersion() > 16 ?
						newInitializerForJava17():
						newInitializerForJava14():
					newInitializerForJava9():
				newInitializerForJava8());
	}

	Initializer newInitializerForJava8() {
		return new Initializer.ForJava8(this);
	}

	Initializer newInitializerForJava9() {
		return new Initializer.ForJava9(this);
	}

	Initializer newInitializerForJava14() {
		return new Initializer.ForJava14(this);
	}

	Initializer newInitializerForJava17() {
		return new Initializer.ForJava17(this);
	}

	@Override
	public void setAccessible(AccessibleObject object, boolean flag) {
		try {
			((java.util.function.BiConsumer<AccessibleObject, Boolean>)accessibleSetter).accept(object, flag);
		} catch (Throwable exc) {
			Throwables.throwException(exc);
		}
	}

	@Override
	public Class<?> defineHookClass(Class<?> clientClass, byte[] byteCode) {
		try {
			return ((java.util.function.BiFunction<Class<?>, byte[], Class<?>>)hookClassDefiner).apply(clientClass, byteCode);
		} catch (Throwable exc) {
			return Throwables.throwException(exc);
		}
	}

	@Override
	public Package getPackage(ClassLoader classLoader, String packageName) {
		return ((java.util.function.BiFunction<ClassLoader, String, Package>)packageRetriever).apply(classLoader, packageName);
	}

	@Override
	public Collection<Class<?>> retrieveLoadedClasses(ClassLoader classLoader) {
		return ((java.util.function.Function<ClassLoader, Collection<Class<?>>>)loadedClassesRetriever).apply(classLoader);
	}

	@Override
	public Map<String, ?> retrieveLoadedPackages(ClassLoader classLoader) {
		return ((java.util.function.Function<ClassLoader, Map<String, ?>>) loadedPackagesRetriever).apply(classLoader);
	}

	@Override
	public boolean isBuiltinClassLoader(ClassLoader classLoader) {
		return builtinClassLoaderClass != null && builtinClassLoaderClass.isAssignableFrom(classLoader.getClass());
	}

	@Override
	public boolean isClassLoaderDelegate(ClassLoader classLoader) {
		return classLoaderDelegateClass != null && classLoaderDelegateClass.isAssignableFrom(classLoader.getClass());
	}

	@Override
	public Class<?> getBuiltinClassLoaderClass() {
		return builtinClassLoaderClass;
	}

	@Override
	public Class getClassLoaderDelegateClass() {
		return classLoaderDelegateClass;
	}

	@Override
	public Lookup getConsulter(Class<?> cls) {
		return ((java.util.function.Function<Class<?>, MethodHandles.Lookup>) consulterRetriever).apply(cls);
	}

	@Override
	public <T> T invoke(Method method, Object target, Object[] params) {
		try {
			return (T)methodInvoker.invoke(method, target, params);
		} catch (Throwable exc) {
			return Throwables.throwException(exc);
		}
	}

	@Override
	public <T> T newInstance(Constructor<T> ctor, Object[] params) {
		try {
			return (T)constructorInvoker.invoke(ctor, params);
		} catch (Throwable exc) {
			return Throwables.throwException(exc);
		}
	}

	@Override
	public Field getDeclaredField(Class<?> cls, String name) {
		for (Field field : getDeclaredFields(cls)) {
			if (field.getName().equals(name)) {
				return field;
			}
		}
		return null;
	}

	@Override
	public Field[] getDeclaredFields(Class<?> cls)  {
		try {
			return (Field[])getDeclaredFieldsRetriever.invoke(cls, false);
		} catch (Throwable exc) {
			return Throwables.throwException(exc);
		}
	}

	@Override
	public <T> Constructor<T>[] getDeclaredConstructors(Class<T> cls) {
		try {
			return (Constructor<T>[])getDeclaredConstructorsRetriever.invoke(cls, false);
		} catch (Throwable exc) {
			return Throwables.throwException(exc);
		}
	}

	@Override
	public Method[] getDeclaredMethods(Class<?> cls) {
		try {
			return (Method[])getDeclaredMethodsRetriever.invoke(cls, false);
		} catch (Throwable exc) {
			return Throwables.throwException(exc);
		}
	}

	@Override
	public <T> T getFieldValue(Object target, Field field) {
		return (T)((java.util.function.BiFunction<Object, Field, Object>) fieldValueRetriever).apply(target, field);
	}

	@Override
	public void setFieldValue(Object target, Field field, Object value) {
		((java.util.function.Function<Object, java.util.function.BiConsumer<Field, Object>>)fieldValueSetter).apply(target).accept(field, value);
	}

	@Override
	public <T> T allocateInstance(Class<?> cls) {
		return (T)((java.util.function.Function<Class<?>, Object>) allocateInstanceInvoker).apply(cls);
	}

	@Override
	public void close() {
		getDeclaredFieldsRetriever = null;
		getDeclaredMethodsRetriever = null;
		getDeclaredConstructorsRetriever = null;
		packageRetriever = null;
		methodInvoker = null;
		constructorInvoker = null;
		accessibleSetter = null;
		consulterRetriever = null;
		classLoaderDelegateClass = null;
		builtinClassLoaderClass = null;
		hookClassDefiner = null;
	}

	abstract static class Initializer implements Closeable {
		DefaultDriver driver;
		DriverFunctionSupplier driverFunctionSupplier;

		Initializer(DefaultDriver driver) {
			this.driver = driver;
			initNativeFunctionSupplier();
		}

		abstract void initNativeFunctionSupplier();

		Initializer start() {
			driver.allocateInstanceInvoker = driverFunctionSupplier.getAllocateInstanceFunction();
			driver.fieldValueRetriever = driverFunctionSupplier.getFieldValueFunction();
			driver.fieldValueSetter = driverFunctionSupplier.getSetFieldValueFunction();
			initDefineHookClassFunction();
			initConsulterRetriever();
			initMembersRetrievers();
			initAccessibleSetter();
			initConstructorInvoker();
			initMethodInvoker();
			initSpecificElements();
			driver.loadedClassesRetriever = driverFunctionSupplier.getRetrieveLoadedClassesFunction();
			driver.loadedPackagesRetriever = driverFunctionSupplier.getRetrieveLoadedPackagesFunction();
			return this;
		}

		abstract void initDefineHookClassFunction();

		abstract void initConsulterRetriever();

		abstract void initAccessibleSetter();

		abstract void initSpecificElements();

		abstract void initConstructorInvoker();

		abstract void initMethodInvoker();

		void initMembersRetrievers() {
			try {
				MethodHandles.Lookup consulter = driver.getConsulter(Class.class);
				driver.getDeclaredFieldsRetriever = consulter.findSpecial(
					Class.class,
					"getDeclaredFields0",
					MethodType.methodType(Field[].class, boolean.class),
					Class.class
				);

				driver.getDeclaredMethodsRetriever = consulter.findSpecial(
					Class.class,
					"getDeclaredMethods0",
					MethodType.methodType(Method[].class, boolean.class),
					Class.class
				);

				driver.getDeclaredConstructorsRetriever = consulter.findSpecial(
					Class.class,
					"getDeclaredConstructors0",
					MethodType.methodType(Constructor[].class, boolean.class),
					Class.class
				);
			} catch (Throwable exc) {
				Throwables.throwException(exc);
			}
		}

		@Override
		public void close() {
			driverFunctionSupplier.close();
			driverFunctionSupplier = null;
			driver = null;
		}

		static class ForJava8 extends Initializer {
			MethodHandles.Lookup mainConsulter;
			MethodHandle privateLookupInMethodHandle;

			ForJava8(DefaultDriver driver) {
				super(driver);
				try {
					Field modes = MethodHandles.Lookup.class.getDeclaredField("allowedModes");
					mainConsulter = MethodHandles.lookup();
					modes.setAccessible(true);
					modes.setInt(mainConsulter, -1);
					privateLookupInMethodHandle = mainConsulter.findSpecial(
						MethodHandles.Lookup.class, "in",
						MethodType.methodType(MethodHandles.Lookup.class, Class.class),
						MethodHandles.Lookup.class
					);
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize consulter retriever", exc));
				}
			}

			@Override
			void initNativeFunctionSupplier()  {
				this.driverFunctionSupplier = new DriverFunctionSupplierUnsafe.ForJava8(this.driver);
			}

			@Override
			void initConsulterRetriever() {
				try {
					driver.consulterRetriever = new java.util.function.Function<Class<?>, MethodHandles.Lookup>() { 
						@Override
						public Lookup apply(Class<?> cls) {
							try {
								return (Lookup) privateLookupInMethodHandle.invoke(mainConsulter, cls);
							} catch (Throwable exc) {
								return Throwables.throwException(exc);
							}
						}
					};
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize consulter retriever", exc));
				}
			}

			@Override
			void initDefineHookClassFunction() {
				try {
					driver.hookClassDefiner = driverFunctionSupplier.getDefineHookClassFunction(mainConsulter, privateLookupInMethodHandle);
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize consulter retriever", exc));
				}
			}

			@Override
			void initAccessibleSetter() {
				try {
					final Method accessibleSetterMethod = AccessibleObject.class.getDeclaredMethod("setAccessible0", AccessibleObject.class, boolean.class);
					final MethodHandle accessibleSetterMethodHandle = driver.getConsulter(AccessibleObject.class).unreflect(accessibleSetterMethod);
					driver.accessibleSetter = new java.util.function.BiConsumer<AccessibleObject, Boolean>() {
						@Override
						public void accept(AccessibleObject accessibleObject, Boolean flag) {
							try {
								accessibleSetterMethodHandle.invoke(accessibleObject, flag);
							} catch (Throwable exc) {
								Throwables.throwException(exc);
							}
						}
					};
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize accessible setter", exc));
				}
			}

			@Override
			void initConstructorInvoker() {
				try {
					Class<?> nativeAccessorImplClass = Class.forName("sun.reflect.NativeConstructorAccessorImpl");
					Method method = nativeAccessorImplClass.getDeclaredMethod("newInstance0", Constructor.class, Object[].class);
					MethodHandles.Lookup consulter = driver.getConsulter(nativeAccessorImplClass);
					driver.constructorInvoker = consulter.unreflect(method);
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize constructor invoker", exc));
				}
			}

			@Override
			void initMethodInvoker() {
				try {
					Class<?> nativeAccessorImplClass = Class.forName("sun.reflect.NativeMethodAccessorImpl");
					Method method = nativeAccessorImplClass.getDeclaredMethod("invoke0", Method.class, Object.class, Object[].class);
					MethodHandles.Lookup consulter = driver.getConsulter(nativeAccessorImplClass);
					driver.methodInvoker = consulter.unreflect(method);
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize method invoker", exc));
				}
			}

			@Override
			void initSpecificElements() {
				driver.packageRetriever = new java.util.function.BiFunction<ClassLoader, String, Package>() {
					@Override
					public Package apply(ClassLoader t, String packageName) {
						return Package.getPackage(packageName);
					}	
				};
			}

			@Override
			public void close() {
				super.close();
				MethodHandles.Lookup mainConsulter = null;
				MethodHandle privateLookupInMethodHandle =null;
			}

		}

		static class ForJava9 extends Initializer {
			MethodHandles.Lookup mainConsulter;
			MethodHandle privateLookupInMethodHandle;

			ForJava9(DefaultDriver driver) {
				super(driver);
				mainConsulter = driverFunctionSupplier.getMethodHandlesLookupSupplyingFunction().get();
				try {
					privateLookupInMethodHandle = mainConsulter.findStatic(
						MethodHandles.class, "privateLookupIn",
						MethodType.methodType(MethodHandles.Lookup.class, Class.class, MethodHandles.Lookup.class)
					);
				} catch (Throwable exc) {
					Throwables.throwException(exc);
				}
			}

			@Override
			void initNativeFunctionSupplier() {
				this.driverFunctionSupplier = new DriverFunctionSupplierUnsafe.ForJava9(this.driver);
			}

			@Override
			void initDefineHookClassFunction() {
				driver.hookClassDefiner = driverFunctionSupplier.getDefineHookClassFunction(mainConsulter, privateLookupInMethodHandle);
			}

			@Override
			void initConsulterRetriever() {
				try (
					InputStream inputStream =
						Resources.getAsInputStream(this.getClass().getClassLoader(), this.getClass().getPackage().getName().replace(".", "/") + "/ConsulterRetrieverForJDK9.bwc"
					);
				) {
					Class<?> methodHandleWrapperClass = driver.defineHookClass(
						Class.class, Streams.toByteArray(inputStream)
					);
					driver.setFieldValue(methodHandleWrapperClass, methodHandleWrapperClass.getDeclaredField("consulterRetriever"), privateLookupInMethodHandle);
					driver.consulterRetriever = driver.allocateInstance(methodHandleWrapperClass);
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize consulter retriever", exc));
				}

			}

			@Override
			void initAccessibleSetter() {
				try (
					InputStream inputStream =
						Resources.getAsInputStream(this.getClass().getClassLoader(), this.getClass().getPackage().getName().replace(".", "/") + "/AccessibleSetterInvokerForJDK9.bwc"
					);
				) {
					Class<?> methodHandleWrapperClass = driver.defineHookClass(
						AccessibleObject.class, Streams.toByteArray(inputStream)
					);
					driver.setFieldValue(methodHandleWrapperClass, methodHandleWrapperClass.getDeclaredField("methodHandleRetriever"), driver.getConsulter(methodHandleWrapperClass));
					driver.accessibleSetter = driver.allocateInstance(methodHandleWrapperClass);
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize accessible setter", exc));
				}
			}


			@Override
			void initConstructorInvoker() {
				try {
					Class<?> nativeAccessorImplClass = Class.forName("jdk.internal.reflect.NativeConstructorAccessorImpl");
					Method method = nativeAccessorImplClass.getDeclaredMethod("newInstance0", Constructor.class, Object[].class);
					MethodHandles.Lookup consulter = driver.getConsulter(nativeAccessorImplClass);
					driver.constructorInvoker = consulter.unreflect(method);
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize constructor invoker", exc));
				}
			}

			@Override
			void initMethodInvoker() {
				try {
					Class<?> nativeMethodAccessorImplClass = Class.forName("jdk.internal.reflect.NativeMethodAccessorImpl");
					Method invoker = nativeMethodAccessorImplClass.getDeclaredMethod("invoke0", Method.class, Object.class, Object[].class);
					MethodHandles.Lookup consulter = driver.getConsulter(nativeMethodAccessorImplClass);
					driver.methodInvoker = consulter.unreflect(invoker);
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize method invoker", exc));
				}
			}


			@Override
			void initSpecificElements() {
				try {
					MethodHandles.Lookup classLoaderConsulter = driver.getConsulter(ClassLoader.class);
					MethodType methodType = MethodType.methodType(Package.class, String.class);
					final MethodHandle methodHandle = classLoaderConsulter.findSpecial(ClassLoader.class, "getDefinedPackage", methodType, ClassLoader.class);
					driver.packageRetriever = new java.util.function.BiFunction<ClassLoader, String, Package>() {
						@Override
						public Package apply(ClassLoader classLoader, String packageName) {
							try {
								return (Package)methodHandle.invokeExact(classLoader, packageName);
							} catch (Throwable exc) {
								return Throwables.throwException(exc);
							}
						}
					};
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize package retriever", exc));
				}
				try {
					driver.builtinClassLoaderClass = Class.forName("jdk.internal.loader.BuiltinClassLoader");
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize builtin class loader class", exc));
				}
				try (
					InputStream inputStream =
						Resources.getAsInputStream(this.getClass().getClassLoader(), this.getClass().getPackage().getName().replace(".", "/") + "/ClassLoaderDelegateForJDK9.bwc"
					);
				) {
					driver.classLoaderDelegateClass = driver.defineHookClass(
						driver.builtinClassLoaderClass, Streams.toByteArray(inputStream)
					);
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize class loader delegate class", exc));
				}
				try {
					initDeepConsulterRetriever();
				} catch (Throwable exc) {
					Throwables.throwException(new InitializationException("Could not initialize deep consulter retriever", exc));
				}
			}

			void initDeepConsulterRetriever() throws Throwable {
				Constructor<MethodHandles.Lookup> lookupCtor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
				driver.setAccessible(lookupCtor, true);
				final MethodHandle methodHandle = lookupCtor.newInstance(MethodHandles.Lookup.class, -1).findConstructor(
					MethodHandles.Lookup.class, MethodType.methodType(void.class, Class.class, int.class)
				);
				driver.consulterRetriever = new java.util.function.Function<Class<?>, MethodHandles.Lookup>() {
					@Override
					public Lookup apply(Class<?> cls) {
						try {
							return (MethodHandles.Lookup)methodHandle.invoke(cls, -1);
						} catch (Throwable exc) {
							return Throwables.throwException(exc);
						}
					}
				};
			}

			@Override
			public void close() {
				super.close();
				this.mainConsulter = null;
				this.privateLookupInMethodHandle = null;
			}

		}

		static class ForJava14 extends ForJava9 {

			ForJava14(DefaultDriver driver) {
				super(driver);
			}

			@Override
			void initDeepConsulterRetriever() throws Throwable {
				Constructor<?> lookupCtor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Class.class, int.class);
				driver.setAccessible(lookupCtor, true);
				final MethodHandle mthHandle = ((MethodHandles.Lookup)lookupCtor.newInstance(MethodHandles.Lookup.class, null, -1)).findConstructor(
					MethodHandles.Lookup.class, MethodType.methodType(void.class, Class.class, Class.class, int.class)
				);
				driver.consulterRetriever = new java.util.function.Function<Class<?>, MethodHandles.Lookup>() {
					@Override
					public Lookup apply(Class<?> cls) {
						try {
							return (MethodHandles.Lookup)mthHandle.invoke(cls, null, -1);
						} catch (Throwable exc) {
							return Throwables.throwException(exc);
						}
					}
				};
			}
		}

		static class ForJava17 extends ForJava14 {

			ForJava17(DefaultDriver driver) {
				super(driver);
			}

			@Override
			void initNativeFunctionSupplier() {
				this.driverFunctionSupplier = new DriverFunctionSupplierUnsafe.ForJava17(this.driver);
			}

		}

	}

}
