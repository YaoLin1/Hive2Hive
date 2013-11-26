package org.hive2hive.core.network.usermessages;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;

import org.apache.log4j.Logger;
import org.hive2hive.core.log.H2HLoggerFactory;
import org.hive2hive.core.network.data.NetworkContent;
import org.hive2hive.core.security.EncryptionUtil;

/**
 * The base class of all UserMessage objects, representing an encrypted and signed message that is stored on
 * the receiving user's proxy.
 * 
 * @author Christian
 * 
 */
public abstract class UserMessage extends NetworkContent implements Runnable {

	private final static Logger logger = H2HLoggerFactory.getLogger(UserMessage.class);
	
	private static final long serialVersionUID = -773794512479641000L;
	
	private byte[] objectSignState; // serialized state of this object when signed
	private byte[] signature;

	/**
	 * Sign this user message such that the receiver can verify the sender with the senders public key.
	 */
	public final void sign(PrivateKey senderPrivateKey) {

		byte[] signatureState = EncryptionUtil.serializeObject(this);
		try {
			signature = EncryptionUtil.sign(signatureState, senderPrivateKey);
		} catch (InvalidKeyException | SignatureException e) {
			logger.error("Exception while signing user message: ", e);
		}
	}

	/**
	 * Verify this user message such that the sender can be uniquely identified.
	 * @param senderPublicKey The public key of the assumed/accepted sender.
	 */
	public final boolean verify(PublicKey senderPublicKey) {
		try {
			return EncryptionUtil.verify(objectSignState, signature, senderPublicKey);
		} catch (InvalidKeyException | SignatureException e) {
			logger.error("Exception while verifying user message: ", e);
		}
		return false;
	}

	/**
	 * Start the execution of this user message.
	 */
	public final void start() {
		new Thread(this).start();
	}

	public final void run() {
		execute();
	}
	
	/**
	 * The execution part of this user message.
	 */
	protected abstract void execute();
	
	public final byte[] getSignature() {
		return signature;
	}
}
