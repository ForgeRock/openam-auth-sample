package org.forgerock.openam.examples;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import com.sun.identity.authentication.spi.AuthLoginException;

@RunWith(BlockJUnit4ClassRunner.class)
public class SampleAuthPrincipalTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void compare() {
		// Setup exception handling to confirm it is being thrown
		exception.expect(NullPointerException.class);
		exception.expectMessage(Constants.NULL_AUTHPRINCIPAL);
		SampleAuthPrincipal sampleAuthPrincipal = new SampleAuthPrincipal(null);
		Assert.assertNull(sampleAuthPrincipal);
	}
	
	@Test
	public void hascode() {
		SampleAuthPrincipal sampleAuthPrincipal = new SampleAuthPrincipal(Constants.CORRECT_USERNAME);
		Assert.assertNotNull(sampleAuthPrincipal);
		Assert.assertEquals(Constants.CORRECT_USERNAME, sampleAuthPrincipal.getName());
		Assert.assertEquals(3079651, sampleAuthPrincipal.hashCode());
	}
	
	@Test
	public void equals() {
		SampleAuthPrincipal sampleAuthPrincipal = new SampleAuthPrincipal(Constants.CORRECT_USERNAME);
		SampleAuthPrincipal sampleAuthPrincipal2 = new SampleAuthPrincipal(Constants.ERROR_USERNAME1);
		SampleAuthPrincipal sampleAuthPrincipal3 = new SampleAuthPrincipal(Constants.CORRECT_USERNAME);
		Assert.assertNotNull(sampleAuthPrincipal);
		Assert.assertFalse(sampleAuthPrincipal.equals(null));
		Assert.assertFalse(sampleAuthPrincipal.equals(1234));
		Assert.assertFalse(sampleAuthPrincipal.equals(sampleAuthPrincipal2));
		Assert.assertTrue(sampleAuthPrincipal.equals(sampleAuthPrincipal));
		Assert.assertTrue(sampleAuthPrincipal.equals(sampleAuthPrincipal3));
	}

}
