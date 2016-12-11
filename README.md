# sales
販売システム



package jp.co.web.service;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.axis.MessageContext;
import org.apache.axis.transport.http.HTTPConstants;

public class HelloWorldTest {

	/**
	 * @param args
	 * @throws ServiceException
	 * @throws RemoteException
	 */
	public static void main(String[] args) throws ServiceException,
		// TODO 自動生成されたメソッド・スタブ
		HelloWorldServiceLocator helloWorldServiceLocator = new HelloWorldServiceLocator();
		helloWorldServiceLocator.setMaintainSession(true);
		helloWorldServiceLocator
				.setHelloWorldEndpointAddress("http://localhost:8080/hello/services/HelloWorld");
		HelloWorld helloWorld = helloWorldServiceLocator.getHelloWorld();

		HelloWorldServiceLocator helloWorldServiceLocator1 = new HelloWorldServiceLocator();
		helloWorldServiceLocator1.setMaintainSession(true);
		helloWorldServiceLocator1
				.setHelloWorldEndpointAddress("http://localhost:8080/hello/services/HelloWorld");
		HelloWorld helloWorld1 = helloWorldServiceLocator1.getHelloWorld();

		helloWorld.getHello("YangZongmao");

		HelloWorldSoapBindingStub helloWorldSoapBindingStub = (HelloWorldSoapBindingStub) helloWorld;
		MessageContext context = helloWorldSoapBindingStub._getCall().getMessageContext();

		String sessionID = context.getProperty(HTTPConstants.HEADER_COOKIE).toString();

		((javax.xml.rpc.Stub) helloWorld1)._setProperty(HTTPConstants.HEADER_COOKIE, sessionID);

		helloWorld1.test();
	}
}
