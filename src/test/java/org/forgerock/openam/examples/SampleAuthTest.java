package org.forgerock.openam.examples;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.authentication.service.LoginStateCallback;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

@RunWith(PowerMockRunner.class)
// @PowerMockRunnerDelegate(BlockJUnit4ClassRunner.class)
@PrepareForTest(value = { SampleAuth.class, Subject.class })
public class SampleAuthTest {

	SampleAuth sampleAuthSpy;

	@Mock
	Subject subjectMock;

	@Mock
	CallbackHandler callbackHandlerMock;

	LoginState loginStateSpy;

	@Before
	public void setupMocks() {
		// Need to supress the init of AMLoginModule, LoginState
		// This is not needed as we are just mocking the basic calls.
		PowerMockito.suppress(MemberMatcher.constructor(AMLoginModule.class));
		PowerMockito.suppress(MemberMatcher.constructor(LoginState.class));

		// Need to spy on the Authentication Module
		// This is so that when the call to #SampleAuth.getCallBackHandler is made we
		// can supply a
		// mock of our choosing, useful for when no callbacks has been supplied
		sampleAuthSpy = Mockito.spy(new SampleAuth());
		Mockito.when(sampleAuthSpy.getCallbackHandler()).thenReturn(callbackHandlerMock);

		// This is to supress an issue with #LoginState.getFilename
		loginStateSpy = PowerMockito.spy(new LoginState());
		Mockito.doReturn("LoginStateFileName").when(loginStateSpy).getFileName(Matchers.anyString());

		// Enable debug
		Debug debug = Debug.getInstance("amAuthSampleAuth");
		debug.setDebug(Debug.STR_MESSAGE);
		debug.setDebug(Debug.ON);
	}

	@Test
	public void loginSuccess() throws LoginException, IOException, UnsupportedCallbackException {

		// This is how we update the callbackHandlerMock to ensurew that we can ibject
		// our own callbacks. First we have to ensure that we have a valid #LoginState,
		// so we check to see if we have a request for one and inert our mock for
		// controlling. This is a hack to get around some init issues.
		Mockito.doAnswer(new Answer() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				// get the argumnets passed to the call
				Object[] arguments = invocation.getArguments();
				// Get the correct argument for the callback
				Object callback = ((Object[]) arguments[0])[0];
				// Check to what callback hass been request and use the spy if required
				if (callback instanceof LoginStateCallback) {
					((LoginStateCallback) callback).setLoginState(loginStateSpy);
				}
				// Return null, does not seem to break anything
				return null;
			}

		}).when(callbackHandlerMock).handle((Callback[]) Matchers.any());

		// Our Mock Username
		String nameCallBackUser = "SampleAuth";

		// Init the shared state of the Authentication Module
		Map<Object, Object> sharedState = new HashMap<Object, Object>();
		// Init the options that would be passed though the AM GUI.
		Map<String, String> options = new HashMap<String, String>();
		options.put("sampleauth-ui-login-header", Constants.UI_LOGIN_HEADER);
		options.put("sampleauth-ui-username-prompt", Constants.UI_USERNAME_PROMPT);
		options.put("sampleauth-ui-password-prompt", Constants.UI_PASSWORD_PROMPT);

		// Init the initial state to enter in as
		int state = 2;

		// Create the callbacks that would come from the request to authenticate via
		// this Auth Module
		List<Callback> callBackList = new ArrayList<Callback>();
		// Creating a simple #NameCallback object
		NameCallback nameCallback = new NameCallback("Passsword Prompt");
		// Set the name of the callback to nameCallBackUser
		nameCallback.setName(Constants.CORRECT_USERNAME);
		// Add it to the list
		callBackList.add(nameCallback);
		PasswordCallback passwordCallback = new PasswordCallback("PasswordCallback", true);
		passwordCallback.setPassword(Constants.CORRECT_PASSWORD.toCharArray());
		callBackList.add(passwordCallback);

		// Convert the ArrayList<Callback> to Callback[]
		Callback[] callbacks = new Callback[callBackList.size()];
		callbacks = callBackList.toArray(callbacks);

		// Initialise the Authentication Module like it would be in AM
		sampleAuthSpy.initialize(subjectMock, callbackHandlerMock, sharedState, options);
		// Process the callbacks and get a response of next state
		int nextState = sampleAuthSpy.process(callbacks, state);

		Assert.assertNotNull(sampleAuthSpy);
		Assert.assertEquals(nextState, ISAuthConstants.LOGIN_SUCCEED);
		Principal principal = sampleAuthSpy.getPrincipal();
		Assert.assertNotNull(principal);
		Assert.assertEquals(Constants.CORRECT_USERNAME, principal.getName());
		Assert.assertEquals(new StringBuilder().append(SampleAuthPrincipal.class.getName()).append(" : ")
				.append(Constants.CORRECT_USERNAME).toString(), principal.toString());

	}

}
