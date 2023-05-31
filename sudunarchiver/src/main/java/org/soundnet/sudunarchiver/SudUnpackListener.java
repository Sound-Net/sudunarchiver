package org.soundnet.sudunarchiver;

/**
 * A listener for receiving updates in sud file processing. 
 * @author Jamie Macaulay
 *
 */
public interface SudUnpackListener {
	
	public enum Sud_Message {
		UNPACK_START, UNPACK_FINISH, NEW_SUD_FILE
	}
	
	/**
	 * Called whenever there is in a update in processing files. 
	 * @param message - the update message
	 * @param data - any data associated with the message. 
	 */
	public void sudUnpackUpdate(Sud_Message message, Object data); 

}
