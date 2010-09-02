package org.ensembl.healthcheck.configurationmanager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ensembl.healthcheck.configuration.ConfigurationUserParameters;

import uk.co.flamingpenguin.jewel.cli.OptionNotPresentException;

/**
 * 
 * A configuration object that gets its information from System properties.
 * 
 * Use the newInstance method for instantiation.
 *
 */
public class ConfigurationBySystemEnvironment<T> extends AbstractAliasAwareWithStanardInvocationHanderConfigurationBacking<T> {

	// Awesome for debugging
	private final String type = "My type is ConfigurationBySystemEnvironment.";
	
	/**
	 * <p>
	 * Use this method to create instances of this configuration object.
	 * </p>
	 * 
	 * <pre>
	 * 	ConfigurationUserParameters configuration = (ConfigurationUserParameters) 
	 * 		ConfigurationBySystemEnvironment.newInstance(ConfigurationUserParameters.class);
	 * </pre>
	 * 
	 * @param configurationInterfaceToProxy: The class object of the 
	 *     configuration interface that this configuration object will
	 *     proxy
	 *      
	 * @return an instance of the configuration class passed. Should be casted
	 *     to the type that was requested as shown in the code example above.
	 *     
	 */
	public static Object newInstance(Class configurationInterfaceToProxy) {

		InvocationHandler handler;

		handler = new ConfigurationBySystemEnvironment(configurationInterfaceToProxy);
		
		Object configuration = 
			java.lang.reflect.Proxy.newProxyInstance(
					configurationInterfaceToProxy.getClassLoader(),
				new Class[] { configurationInterfaceToProxy },
				handler
		);

		return configuration;		
	}
	
	public ConfigurationBySystemEnvironment(Class<T> configurationClass) {
		super(configurationClass);
	}

	protected String mockDirectGetMethod(String varRequested) {
		
		String result = System.getProperty(varRequested);
		
		if (result==null) {
			throw new OptionNotPresentException("varRequested does not exist!");
		}
		
		return result;
	}
	
	protected boolean mockDirectIsMethod(String varRequested) {

		boolean result = true;
		
		try {
			mockDirectGetMethod(varRequested);
		} catch(OptionNotPresentException e) {
			result = false;
		}
		
		return result;
	}

	public String toString() {
		return type;
	}
	
//	public Object invoke(Object proxy, Method m, Object[] args) {
//		
//		String varRequested = getVariableRequested(m);
//		
//		if (isGetMethod(m)) {
//			return mockGetMethod(varRequested);
//		}
//		if (isIsMethod(m)) {
//			return mockIsMethod(varRequested);
//		}
//
//		// Calling toString() on this object will make it print type and properties.
//		if (m.getName().equals("toString")) {
//			return type;
//		}
//		
//		throw new IllegalArgumentException("This only supports get* and is* methods! Method tried was: " + m.getName());
//	}

}