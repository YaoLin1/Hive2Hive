package org.hive2hive.core.test.network.messages;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;

import org.hive2hive.core.network.NetworkManager;
import org.hive2hive.core.network.messages.direct.response.IResponseCallBackHandler;
import org.hive2hive.core.network.messages.direct.response.ResponseMessage;
import org.hive2hive.core.network.messages.usermessages.direct.ContactPeerUserMessage;
import org.hive2hive.core.test.H2HJUnitTest;
import org.hive2hive.core.test.H2HWaiter;
import org.hive2hive.core.test.network.NetworkTestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class UserMessageTests extends H2HJUnitTest {

	private List<NetworkManager> network;
	private final Random random = new Random();

	@BeforeClass
	public static void initTest() throws Exception {
		testClass = UserMessageTests.class;
		beforeClass();
	}

	@Override
	@Before
	public void beforeMethod() {
		super.beforeMethod();
		network = NetworkTestUtil.createNetwork(10);
	}

	@Override
	@After
	public void afterMethod() {
		super.afterMethod();
		NetworkTestUtil.shutdownNetwork(network);
	}

	@AfterClass
	public static void endTest() {
		afterClass();
	}

	private static boolean contactPeerUserMessageHandled;
	private static boolean getNextUserMessageMessageHandled;

	@Test
	public void ContactPeerUserMessageTest() {

		// select two random nodes
		NetworkManager node1 = network.get(random.nextInt(network.size()));
		NetworkManager node2 = network.get(random.nextInt(network.size()));

		// create message
		final String evidence = NetworkTestUtil.randomString();
		ContactPeerUserMessage message = new ContactPeerUserMessage(node2.getPeerAddress(), evidence);
		message.setCallBackHandler(new IResponseCallBackHandler() {
			@Override
			public void handleResponseMessage(ResponseMessage responseMessage) {
				// handle callback
				String responseEvidence = (String) responseMessage.getContent();
				assertNotNull(responseEvidence);
				assertTrue(evidence.equals(responseEvidence));

				logger.debug("ContactPeerUserMessage got handled.");
				contactPeerUserMessageHandled = true;
			}
		});

		// send message
		node1.sendDirect(message, node2.getKeyPair().getPublic(), new TestBaseMessageListener());

		// wait for callback handling
		H2HWaiter waiter = new H2HWaiter(10);
		do {
			waiter.tickASecond();
		} while (!contactPeerUserMessageHandled);
	}

	@Test
	public void GetNextFromQueueMessageTest() {
		//
		// // select random nodes
		// NetworkManager user = network.get(random.nextInt(network.size()));
		// NetworkManager putter = network.get(random.nextInt(network.size()));
		//
		// // define a random user
		// UserCredentials credentials = NetworkTestUtil.generateRandomCredentials();
		//
		// // prepare a sample UserMessageQueue
		// UserMessageQueue umq = new UserMessageQueue(credentials.getUserId());
		// for (int i = 0; i < 10; i++) {
		// BaseMessage userMessage = new TestRoutedUserMessage(credentials.getUserId());
		// umq.getMessageQueue().add(userMessage);
		// }
		//
		// // putter globally puts queue (blocking)
		// FuturePut putGlobal = putter.putGlobal(credentials.getUserId(),
		// H2HConstants.USER_MESSAGE_QUEUE_KEY,
		// umq);
		// putGlobal.awaitUninterruptibly();
		// putGlobal.getFutureRequests().awaitUninterruptibly();
		//
		// // request the UserMessageQueue
		// GetNextUserMessageMessage message = new GetNextUserMessageMessage(credentials.getUserId());
		// message.setCallBackHandler(new IResponseCallBackHandler() {
		// @Override
		// public void handleResponseMessage(ResponseMessage responseMessage) {
		// // handle callback
		// NextFromQueueResponse response = (NextFromQueueResponse) responseMessage.getContent();
		// assertNotNull(response);
		// assertNotNull(response.getUserMessage());
		// assertNotNull(response.getRemainingCount());
		// assertTrue(response.getRemainingCount() == 9);
		//
		// logger.debug("GetNextUserMessageMessage got handled.");
		// getNextUserMessageMessageHandled = true;
		// }
		// });
		//
		// user.send(message).addListener(new FutureRoutedListener(new IBaseMessageListener() {
		// @Override
		// public void onSuccess() {
		// }
		//
		// @Override
		// public void onFailure() {
		// fail("The sending of the message failed.");
		// }
		// }, message, user));
		//
		// // wait for callback handling
		// H2HWaiter waiter = new H2HWaiter(100);
		// do {
		// waiter.tickASecond();
		// } while (!getNextUserMessageMessageHandled);
	}
}