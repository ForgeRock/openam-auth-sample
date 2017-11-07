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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.mock.web.MockServletContext;

import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.authentication.service.LoginStateCallback;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(BlockJUnit4ClassRunner.class)
@SuppressStaticInitializationFor("com.sun.identity.authentication.service.AuthD")
@PrepareForTest(value = { SampleAuth.class, Subject.class, AuthD.class })
public class SampleAuthTest {

	SampleAuth sampleAuthSpy;

	@Mock
	Subject subjectMock;

	@Mock
	CallbackHandler callbackHandlerMock;

	LoginState loginStateSpy;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Before
	public void setupMocks() throws AuthLoginException {
		// Need to supress the init of AMLoginModule, LoginState
		// This is not needed as we are just mocking the basic calls.
		PowerMockito.suppress(MemberMatcher.constructor(AMLoginModule.class));
		PowerMockito.suppress(MemberMatcher.constructor(LoginState.class));
		PowerMockito.suppress(MemberMatcher.defaultConstructorIn(AuthD.class));

		// Need to spy on the Authentication Module
		// This is so that when the call to #SampleAuth.getCallBackHandler is made we
		// can supply a
		// mock of our choosing, useful for when no callbacks has been supplied
		sampleAuthSpy = Mockito.spy(new SampleAuth());
		Mockito.when(sampleAuthSpy.getCallbackHandler()).thenReturn(callbackHandlerMock);

		// This is to supress an issue with #LoginState.getFilename
		loginStateSpy = PowerMockito.spy(new LoginState());
		Mockito.doReturn("config/auth/default/SampleAuth.xml").when(loginStateSpy).getFileName(Matchers.anyString());

		// Enable debug
		Debug debug = Debug.getInstance("amAuthSampleAuth");
		debug.setDebug(Debug.STR_MESSAGE);
		debug.setDebug(Debug.ON);

		/**
		 * Configure mocks for AuthD // This is a hack to get around some issues loading
		 * callbacks 1. Use PowerMock to mock all methods of #AuthD 2. Create a Mock of
		 * #AuthD 3. Create a Mock Servlet Context using spring-mock 4. Return the
		 * mocked AuthD and ServletContext when called
		 */
		PowerMockito.mockStatic(AuthD.class);
		AuthD authDMock = Mockito.mock(AuthD.class);
		MockServletContext servletContextMock = new MockServletContext();
		Mockito.when(AuthD.getAuth()).thenReturn(authDMock);
		Mockito.when(authDMock.getServletContext()).thenReturn(servletContextMock);
	}

	@Test
	public void initState() throws LoginException, IOException, UnsupportedCallbackException {

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

		// Init the shared state of the Authentication Module
		Map<Object, Object> sharedState = new HashMap<Object, Object>();
		// Init the options that would be passed though the AM GUI.
		Map<String, String> options = new HashMap<String, String>();

		// Init the initial state to enter in as
		int state = 1;

		// Create the callbacks that would come from the request to authenticate via
		// this Auth Module
		List<Callback> callBackList = new ArrayList<Callback>();

		// Convert the ArrayList<Callback> to Callback[]
		Callback[] callbacks = new Callback[callBackList.size()];
		callbacks = callBackList.toArray(callbacks);

		// Initialise the Authentication Module like it would be in AM
		sampleAuthSpy.initialize(subjectMock, callbackHandlerMock, sharedState, options);

		// Dirty hack to get around an issue getting callbacks
		// Needs more investigation
		Callback[] c = sampleAuthSpy.getCallback(1);

		// Process the callbacks and get a response of next state
		int nextState = sampleAuthSpy.process(callbacks, state);

		Assert.assertNotNull(sampleAuthSpy);
		Assert.assertEquals(ISAuthConstants.LOGIN_CHALLENGE, nextState);
		Principal principal = sampleAuthSpy.getPrincipal();
		Assert.assertNotNull(principal);
		Assert.assertEquals(Constants.CORRECT_USERNAME, principal.getName());
		Assert.assertEquals(new StringBuilder().append(SampleAuthPrincipal.class.getName()).append(" : ")
				.append(Constants.CORRECT_USERNAME).toString(), principal.toString());
		
		// Callback 1 should be empty
		Callback[] callback1 = sampleAuthSpy.getCallback(1);
		Assert.assertEquals(0, callback1.length);
		
		// Callback 2 should have 2 callbacks
		Callback[] callback2 = sampleAuthSpy.getCallback(2);
		Assert.assertEquals(2, callback2.length);
		Callback nameCallback = callback2[0];
		Callback passwordCallback = callback2[1];
		Assert.assertEquals(NameCallback.class, nameCallback.getClass());
		Assert.assertEquals(Constants.UI_USERNAME_PROMPT, ((NameCallback)nameCallback).getPrompt());
		Assert.assertEquals(PasswordCallback.class, passwordCallback.getClass());
		Assert.assertEquals(Constants.UI_PASSWORD_PROMPT, ((PasswordCallback)passwordCallback).getPrompt());
		
		// Callback 3 should have 1 callback
		Callback[] callback3 = sampleAuthSpy.getCallback(3);
		Assert.assertEquals(1, callback3.length);
		nameCallback = callback3[0];
		Assert.assertEquals(NameCallback.class, nameCallback.getClass());
		Assert.assertEquals(Constants.UI_STATE3, ((NameCallback)nameCallback).getPrompt());

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

		// Init the shared state of the Authentication Module
		Map<Object, Object> sharedState = new HashMap<Object, Object>();
		// Init the options that would be passed though the AM GUI.
		Map<String, String> options = new HashMap<String, String>();

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
		Assert.assertEquals(ISAuthConstants.LOGIN_SUCCEED, nextState);
		Principal principal = sampleAuthSpy.getPrincipal();
		Assert.assertNotNull(principal);
		Assert.assertEquals(Constants.CORRECT_USERNAME, principal.getName());
		Assert.assertEquals(new StringBuilder().append(SampleAuthPrincipal.class.getName()).append(" : ")
				.append(Constants.CORRECT_USERNAME).toString(), principal.toString());

	}

	@Test
	public void loginFailure() throws LoginException, IOException, UnsupportedCallbackException {

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

		// Init the shared state of the Authentication Module
		Map<Object, Object> sharedState = new HashMap<Object, Object>();
		// Init the options that would be passed though the AM GUI.
		Map<String, String> options = new HashMap<String, String>();

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
		passwordCallback.setPassword(Constants.INCORRECT_PASSWORD.toCharArray());
		callBackList.add(passwordCallback);

		// Convert the ArrayList<Callback> to Callback[]
		Callback[] callbacks = new Callback[callBackList.size()];
		callbacks = callBackList.toArray(callbacks);

		// Setup exception handling to confirm it is being thrown
		exception.expect(InvalidPasswordException.class);
		exception.expectMessage(Constants.PASSWORD_IS_WRONG_EXCEPTION);

		// Initialise the Authentication Module like it would be in AM
		sampleAuthSpy.initialize(subjectMock, callbackHandlerMock, sharedState, options);
		// Process the callbacks and get a response of next state
		int nextState = sampleAuthSpy.process(callbacks, state);

		Assert.assertNotNull(sampleAuthSpy);
		Assert.assertEquals(ISAuthConstants.LOGIN_SUCCEED, nextState);
		Principal principal = sampleAuthSpy.getPrincipal();
		Assert.assertNotNull(principal);
		Assert.assertEquals(Constants.CORRECT_USERNAME, principal.getName());
		Assert.assertEquals(new StringBuilder().append(SampleAuthPrincipal.class.getName()).append(" : ")
				.append(Constants.CORRECT_USERNAME).toString(), principal.toString());

	}

	@Test
	public void loginFailureUnknownUser() throws LoginException, IOException, UnsupportedCallbackException {

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

		// Init the shared state of the Authentication Module
		Map<Object, Object> sharedState = new HashMap<Object, Object>();
		// Init the options that would be passed though the AM GUI.
		Map<String, String> options = new HashMap<String, String>();

		// Init the initial state to enter in as
		int state = 2;

		// Create the callbacks that would come from the request to authenticate via
		// this Auth Module
		List<Callback> callBackList = new ArrayList<Callback>();
		// Creating a simple #NameCallback object
		NameCallback nameCallback = new NameCallback("Passsword Prompt");
		// Set the name of the callback to nameCallBackUser
		nameCallback.setName(Constants.UNKOWN_USERNAME);
		// Add it to the list
		callBackList.add(nameCallback);
		PasswordCallback passwordCallback = new PasswordCallback("PasswordCallback", true);
		passwordCallback.setPassword(Constants.INCORRECT_PASSWORD.toCharArray());
		callBackList.add(passwordCallback);

		// Convert the ArrayList<Callback> to Callback[]
		Callback[] callbacks = new Callback[callBackList.size()];
		callbacks = callBackList.toArray(callbacks);

		// Setup exception handling to confirm it is being thrown
		exception.expect(InvalidPasswordException.class);
		exception.expectMessage(Constants.PASSWORD_IS_WRONG_EXCEPTION);

		// Initialise the Authentication Module like it would be in AM
		sampleAuthSpy.initialize(subjectMock, callbackHandlerMock, sharedState, options);
		// Process the callbacks and get a response of next state
		int nextState = sampleAuthSpy.process(callbacks, state);

		Assert.assertNotNull(sampleAuthSpy);
		Assert.assertEquals(ISAuthConstants.LOGIN_SUCCEED, nextState);
		Principal principal = sampleAuthSpy.getPrincipal();
		Assert.assertNotNull(principal);
		Assert.assertEquals(Constants.CORRECT_USERNAME, principal.getName());
		Assert.assertEquals(new StringBuilder().append(SampleAuthPrincipal.class.getName()).append(" : ")
				.append(Constants.CORRECT_USERNAME).toString(), principal.toString());

	}

	@Test
	public void loginErrorUsername1() throws LoginException, IOException, UnsupportedCallbackException {

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

		// Init the shared state of the Authentication Module
		Map<Object, Object> sharedState = new HashMap<Object, Object>();
		// Init the options that would be passed though the AM GUI.
		Map<String, String> options = new HashMap<String, String>();

		// Init the initial state to enter in as
		int state = 2;

		// Create the callbacks that would come from the request to authenticate via
		// this Auth Module
		List<Callback> callBackList = new ArrayList<Callback>();
		// Creating a simple #NameCallback object
		NameCallback nameCallback = new NameCallback("Passsword Prompt");
		// Set the name of the callback to nameCallBackUser
		nameCallback.setName(Constants.ERROR_USERNAME1);
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

		// Dirty hack to get around an issue getting callbacks
		// Needs more investigation
		Callback[] c = sampleAuthSpy.getCallback(1);

		// Process the callbacks and get a response of next state
		int nextState = sampleAuthSpy.process(callbacks, state);

		Assert.assertNotNull(sampleAuthSpy);
		Assert.assertEquals(ISAuthConstants.LOGIN_NEXT_TOKEN, nextState);
		Principal principal = sampleAuthSpy.getPrincipal();
		Assert.assertNotNull(principal);
		Assert.assertEquals(Constants.CORRECT_USERNAME, principal.getName());
		Assert.assertEquals(new StringBuilder().append(SampleAuthPrincipal.class.getName()).append(" : ")
				.append(Constants.CORRECT_USERNAME).toString(), principal.toString());

		// Callback 1 should be empty
		Callback[] callback1 = sampleAuthSpy.getCallback(1);
		Assert.assertEquals(0, callback1.length);
		
		// Callback 2 should have 2 callbacks
		Callback[] callback2 = sampleAuthSpy.getCallback(2);
		Assert.assertEquals(2, callback2.length);
		Callback nameCallback_1 = callback2[0];
		Callback passwordCallback_1 = callback2[1];
		Assert.assertEquals(NameCallback.class, nameCallback_1.getClass());
		Assert.assertEquals(Constants.UI_USERNAME_PROMPT_NULL, ((NameCallback)nameCallback_1).getPrompt());
		Assert.assertEquals(PasswordCallback.class, passwordCallback_1.getClass());
		Assert.assertEquals(Constants.UI_PASSWORD_PROMPT_NULL, ((PasswordCallback)passwordCallback_1).getPrompt());
		
		// Callback 3 should have 1 callback
		Callback[] callback3 = sampleAuthSpy.getCallback(3);
		Assert.assertEquals(1, callback3.length);
		nameCallback_1 = callback3[0];
		Assert.assertEquals(NameCallback.class, nameCallback.getClass());
		Assert.assertEquals(Constants.UI_STATE3, ((NameCallback)nameCallback_1).getPrompt());
		
	}

	@Test
	public void loginErrorUsername2() throws LoginException, IOException, UnsupportedCallbackException {

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

		// Init the shared state of the Authentication Module
		Map<Object, Object> sharedState = new HashMap<Object, Object>();
		// Init the options that would be passed though the AM GUI.
		Map<String, String> options = new HashMap<String, String>();

		// Init the initial state to enter in as
		int state = 2;

		// Create the callbacks that would come from the request to authenticate via
		// this Auth Module
		List<Callback> callBackList = new ArrayList<Callback>();
		// Creating a simple #NameCallback object
		NameCallback nameCallback = new NameCallback("Passsword Prompt");
		// Set the name of the callback to nameCallBackUser
		nameCallback.setName(Constants.ERROR_USERNAME2);
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

		// Dirty hack to get around an issue getting callbacks
		// Needs more investigation
		Callback[] c = sampleAuthSpy.getCallback(1);

		// Process the callbacks and get a response of next state
		int nextState = sampleAuthSpy.process(callbacks, state);

		Assert.assertNotNull(sampleAuthSpy);
		Assert.assertEquals(ISAuthConstants.LOGIN_NEXT_TOKEN, nextState);
		Principal principal = sampleAuthSpy.getPrincipal();
		Assert.assertNotNull(principal);
		Assert.assertEquals(Constants.CORRECT_USERNAME, principal.getName());
		Assert.assertEquals(new StringBuilder().append(SampleAuthPrincipal.class.getName()).append(" : ")
				.append(Constants.CORRECT_USERNAME).toString(), principal.toString());

	}

	@Test
	public void invalidState() throws LoginException, IOException, UnsupportedCallbackException {

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

		// Init the shared state of the Authentication Module
		Map<Object, Object> sharedState = new HashMap<Object, Object>();
		// Init the options that would be passed though the AM GUI.
		Map<String, String> options = new HashMap<String, String>();

		// Init the initial state to enter in as
		int state = 0;

		// Create the callbacks that would come from the request to authenticate via
		// this Auth Module
		List<Callback> callBackList = new ArrayList<Callback>();

		// Convert the ArrayList<Callback> to Callback[]
		Callback[] callbacks = new Callback[callBackList.size()];
		callbacks = callBackList.toArray(callbacks);

		// Setup exception handling to confirm it is being thrown
		exception.expect(AuthLoginException.class);
		exception.expectMessage(Constants.INVALID_STATE_EXCEPTION);

		// Initialise the Authentication Module like it would be in AM
		sampleAuthSpy.initialize(subjectMock, callbackHandlerMock, sharedState, options);
		// Process the callbacks and get a response of next state
		int nextState = sampleAuthSpy.process(callbacks, state);

		Assert.assertNotNull(sampleAuthSpy);
		Assert.assertEquals(ISAuthConstants.LOGIN_SUCCEED, nextState);
		Principal principal = sampleAuthSpy.getPrincipal();
		Assert.assertNotNull(principal);
		Assert.assertEquals(Constants.CORRECT_USERNAME, principal.getName());
		Assert.assertEquals(new StringBuilder().append(SampleAuthPrincipal.class.getName()).append(" : ")
				.append(Constants.CORRECT_USERNAME).toString(), principal.toString());

	}

	@Test
	public void errorState() throws LoginException, IOException, UnsupportedCallbackException {

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

		// Init the shared state of the Authentication Module
		Map<Object, Object> sharedState = new HashMap<Object, Object>();
		// Init the options that would be passed though the AM GUI.
		Map<String, String> options = new HashMap<String, String>();

		// Init the initial state to enter in as
		int state = 3;

		// Create the callbacks that would come from the request to authenticate via
		// this Auth Module
		List<Callback> callBackList = new ArrayList<Callback>();

		// Convert the ArrayList<Callback> to Callback[]
		Callback[] callbacks = new Callback[callBackList.size()];
		callbacks = callBackList.toArray(callbacks);

		// Initialise the Authentication Module like it would be in AM
		sampleAuthSpy.initialize(subjectMock, callbackHandlerMock, sharedState, options);
		// Process the callbacks and get a response of next state
		int nextState = sampleAuthSpy.process(callbacks, state);

		Assert.assertNotNull(sampleAuthSpy);
		Assert.assertEquals(3, nextState);
		Principal principal = sampleAuthSpy.getPrincipal();
		Assert.assertNotNull(principal);
		Assert.assertEquals(Constants.CORRECT_USERNAME, principal.getName());
		Assert.assertEquals(new StringBuilder().append(SampleAuthPrincipal.class.getName()).append(" : ")
				.append(Constants.CORRECT_USERNAME).toString(), principal.toString());

	}

}
