/*FreeMind - A Program for creating and viewing Mindmaps
 *Copyright (C) 2000-2012 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitri Polivaev and others.
 *
 *See COPYING for Details
 *
 *This program is free software; you can redistribute it and/or
 *modify it under the terms of the GNU General Public License
 *as published by the Free Software Foundation; either version 2
 *of the License, or (at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program; if not, write to the Free Software
 *Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package plugins.collaboration.socket;

import freemind.controller.actions.generated.instance.*;
import freemind.extensions.PermanentNodeHook;
import freemind.main.Tools;
import freemind.modes.MapAdapter;
import freemind.modes.mindmapmode.MindMapController;
import freemind.modes.mindmapmode.MindMapMapModel;
import freemind.modes.mindmapmode.MindMapNodeModel;
import plugins.collaboration.socket.SocketBasics.UnableToGetLockException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;

/**
 * @author foltin
 * @date 06.09.2012
 */
public class ClientCommunication extends CommunicationBase {

	private String mLockId;
	private final String mPassword;
	private SocketConnectionHook mSocketConnectionHook = null;
	private boolean mReceivedGoodbye = false;
	private CollaborationUserInformation mUserInfo;

	/**
	 */
	public ClientCommunication(String pName, Socket pClient, MindMapController pController, String pPassword)
			throws IOException {
		super(pName, pClient, pController, new DataOutputStream(
				pClient.getOutputStream()), new DataInputStream(
				pClient.getInputStream()));
		mPassword = pPassword;
		setCurrentState(STATE_WAIT_FOR_WHO_ARE_YOU);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * plugins.collaboration.socket.CommunicationBase#processCommand(freemind
	 * .controller.actions.generated.instance.CollaborationActionBase)
	 */
	public void processCommand(CollaborationActionBase pCommand)
			throws IOException {
		if (pCommand instanceof CollaborationGoodbye) {
			CollaborationGoodbye goodbye = (CollaborationGoodbye) pCommand;
			logger.info("Goodbye received from " + goodbye.getUserId());
			terminateSocket();
			return;
		}
		boolean commandHandled = processCommand_Extracted(pCommand);
		if (!commandHandled) {
			logger.warning("Received unknown message of type "
					+ pCommand.getClass());
		}
	}

	public boolean processCommand_Extracted(CollaborationActionBase pCommand) throws IOException {
		CollaborationHello helloCommand;
		boolean commandHandled = false;
		if (pCommand instanceof CollaborationUserInformation) {
			mUserInfo = (CollaborationUserInformation) pCommand;
			commandHandled = true;
		}
		if (pCommand instanceof CollaborationWhoAreYou) {
			if (getCurrentState() != STATE_WAIT_FOR_WHO_ARE_YOU) {
				logger.warning("Wrong state for " + pCommand.getClass() + ": "
						+ getCurrentState());
			}
			// send hello:
			helloCommand = new CollaborationHello();
			helloCommand.setUserId(Tools.getUserName());
			helloCommand.setPassword(mPassword);
			send(helloCommand);
			setCurrentState(STATE_WAIT_FOR_WELCOME);
			commandHandled = true;
		}
		if (pCommand instanceof CollaborationWelcome) {
			if (getCurrentState() != STATE_WAIT_FOR_WELCOME) {
				logger.warning("Wrong state for " + pCommand.getClass() + ": "
						+ getCurrentState());
			}
			CollaborationWelcome collWelcome = (CollaborationWelcome) pCommand;
			createNewMap(collWelcome.getMap());
			setCurrentState(STATE_IDLE);
			commandHandled = true;
		}
		if (pCommand instanceof CollaborationWrongCredentials) {
			if (getCurrentState() != STATE_WAIT_FOR_WELCOME) {
				logger.warning("Wrong state for " + pCommand.getClass() + ": "
						+ getCurrentState());
			}
			// Over and out.
			terminateSocket();
			// Display error message!
			getMindMapController().getController().errorMessage(
					getMindMapController().getText("socket_wrong_password"));
			commandHandled = true;
		}
		if (pCommand instanceof CollaborationTransaction) {
			if (getCurrentState() != STATE_IDLE) {
				logger.warning("Wrong state for " + pCommand.getClass() + ": "
						+ getCurrentState());
			}
			CollaborationTransaction trans = (CollaborationTransaction) pCommand;
			// check if it is from me!
			if (!Tools.safeEquals(mLockId, trans.getId())) {
				if (mSocketConnectionHook != null) {
					// it is not from me, so handle it:
					mSocketConnectionHook
							.executeTransaction(getActionPair(trans));
				}
			}
			commandHandled = true;
		}
		if (pCommand instanceof CollaborationReceiveLock) {
			if (getCurrentState() != STATE_WAIT_FOR_LOCK) {
				logger.warning("Wrong state for " + pCommand.getClass() + ": "
						+ getCurrentState());
			}
			CollaborationReceiveLock lockReceived = (CollaborationReceiveLock) pCommand;
			this.mLockId = lockReceived.getId();
			setCurrentState(STATE_LOCK_RECEIVED);
			commandHandled = true;
		}
		return commandHandled;
	}

	public void terminateSocket() {
		mReceivedGoodbye = true;
		if (mSocketConnectionHook != null) {
			// first deregister, as otherwise, the toggle hook command is tried to
			// be sent over the wire.
			mSocketConnectionHook.deregisterFilter();
			// Terminates socket by shutdownHook.
			SocketBasics.togglePermanentHook(getMindMapController());
		} else {
			// Terminate socket.
			shutdown();
		}
	}

	/**
	 * Sends the lock requests, blocks until timeout or answer and returns the
	 * associated id. Exception otherwise.
	 *
	 */
	public synchronized String sendLockRequest() throws InterruptedException,
			UnableToGetLockException {
		// TODO: Global lock needed?
		mLockId = null;
		CollaborationRequireLock lockRequest = new CollaborationRequireLock();
		setCurrentState(STATE_WAIT_FOR_LOCK);
		if (!send(lockRequest)) {
			setCurrentState(STATE_IDLE);
			throw new SocketBasics.UnableToGetLockException();
		}
		final int sleepTime = ROUNDTRIP_TIMEOUT / ROUNDTRIP_ROUNDS;
		int timeout = ROUNDTRIP_ROUNDS;
		while (getCurrentState() != STATE_LOCK_RECEIVED && timeout >= 0) {
			sleep(sleepTime);
			timeout--;
		}
		setCurrentState(STATE_IDLE);
		if (timeout < 0) {
			throw new SocketBasics.UnableToGetLockException();
		}
		return mLockId;
	}

	public void createNewMap(String map) throws IOException {
		{
			logger.info("Restoring the map...");
			MindMapController newModeController = (MindMapController) getMindMapController()
					.getMode().createModeController();
			MapAdapter newModel = new MindMapMapModel(getMindMapController()
					.getFrame(), newModeController);
			HashMap IDToTarget = new HashMap();
			StringReader reader = new StringReader(map);
			MindMapNodeModel rootNode = (MindMapNodeModel) newModeController
					.createNodeTreeFromXml(reader, IDToTarget);
			reader.close();
			newModel.setRoot(rootNode);
			rootNode.setMap(newModel);
			getMindMapController().newMap(newModel);
			newModeController.invokeHooksRecursively(rootNode,
					newModel);
			setController(newModeController);
			// add new hook
			SocketBasics.togglePermanentHook(getMindMapController());
			// tell him about this thread.
			Collection activatedHooks = getMindMapController().getRootNode()
					.getActivatedHooks();
			for (Object activatedHook : activatedHooks) {
				PermanentNodeHook hook = (PermanentNodeHook) activatedHook;
				if (hook instanceof SocketConnectionHook) {
					SocketConnectionHook connHook;
					connHook = (SocketConnectionHook) hook;
					// Tell the hook about me
					connHook.setClientCommunication(this);
					/* register as listener, as I am a slave. */
					connHook.registerFilter();
					this.mSocketConnectionHook = connHook;
					break;
				}
			}
		}
	}

	/**
	 */
	public void setController(MindMapController pNewModeController) {
		mController = pNewModeController;
	}

	/**
	 */
	public MindMapController getMindMapController() {
		return mController;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.collaboration.socket.SocketBasics#shutdown()
	 */
	public void shutdown() {
		// we don't want to read anymore, as we send the goodbye anyway!
		commitSuicide();
		try {
			if (!mReceivedGoodbye) {
				// Send only, if own goodbye.
				CollaborationGoodbye goodbye = new CollaborationGoodbye();
				goodbye.setUserId(Tools.getUserName());
				send(goodbye);
				// in between, the socket has been closed.
			}
		} catch (Exception e) {
			freemind.main.Resources.getInstance().logException(e);
		}
		try {
			close();
		} catch (IOException e) {
			freemind.main.Resources.getInstance().logException(e);
		}
	}

	/**
	 */
	public int getPort() {
		return mSocket.getLocalPort();
	}

	public CollaborationUserInformation getUserInfo() {
		return mUserInfo;
	}

}
