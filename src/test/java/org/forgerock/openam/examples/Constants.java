package org.forgerock.openam.examples;

public class Constants {

	public static final String UI_LOGIN_HEADER = "SampleAuth";
	public static final String UI_USERNAME_PROMPT = "User Name:";
	public static final String UI_PASSWORD_PROMPT = "Password:";
	public static final String CORRECT_USERNAME = "demo";
	public static final String CORRECT_PASSWORD = "changeit";
	public static final String INCORRECT_PASSWORD = "incorrect";
	public static final String ERROR_USERNAME1 = "test1";
	public static final String ERROR_USERNAME2 = "test2";
	public static final String PASSWORD_IS_WRONG_EXCEPTION = "password is wrong";
	public static final String UNKOWN_USERNAME = "unknown";
	public static final String INVALID_STATE_EXCEPTION = "invalid state";
	public static final String NULL_AUTHPRINCIPAL = "illegal null input";
	public static final Object UI_USERNAME_PROMPT_NULL = "#USERNAME#";
	public static final Object UI_PASSWORD_PROMPT_NULL = "#PASSWORD#";
	public static final Object UI_STATE3 = "#THE DUMMY WILL NEVER BE SHOWN#";
	public static final Object UI_PASSWORD_PROMPT_STAGE3 = "Passsword Prompt";
	public static final Object HEADER_LOGIN = new StringBuilder().append(UI_LOGIN_HEADER).append(" Login").toString();
	public static final Object HEADER_ERROR1 = "Error 1 occurred during the authentication";
	public static final Object HEADER_ERROR2 = "Error 2 occurred during the authentication";
	public static final String INIT_HEADER_ATTRIBUTE = "specificAttribute";

}
