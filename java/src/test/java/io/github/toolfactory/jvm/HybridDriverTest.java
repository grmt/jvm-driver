package io.github.toolfactory.jvm;


import org.junit.jupiter.api.Test;


public class HybridDriverTest extends BaseTest {
	private static Driver driver;
	
	public static void main(String[] args) {
		System.out.println("Starting");
		new HybridDriverTest().getAndSetDirectVolatileTestOne();
	}
	
	Driver getDriver() {
		if (driver == null) {
			try {
				driver = new HybridDriver();
			} catch (Throwable exc) {
				exc.printStackTrace();
				return Throwables.getInstance().throwException(exc);
			}
		}
		return driver;
	}
	
	@Test
	public void getAndSetDirectVolatileTestOne() {
		super.getAndSetDirectVolatileTestOne();
	}
	
}
