package org.hive2hive.core.process.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.hive2hive.core.exceptions.IllegalFileLocation;
import org.hive2hive.core.exceptions.NoSessionException;
import org.hive2hive.core.log.H2HLogger;
import org.hive2hive.core.log.H2HLoggerFactory;
import org.hive2hive.core.model.FileTreeNode;
import org.hive2hive.core.network.NetworkManager;
import org.hive2hive.core.process.Process;
import org.hive2hive.core.process.delete.DeleteFileProcess;
import org.hive2hive.core.process.download.DownloadFileProcess;
import org.hive2hive.core.process.upload.newfile.NewFileProcess;
import org.hive2hive.core.process.upload.newversion.NewVersionProcess;

public class FileRecursionUtil {

	private static final H2HLogger logger = H2HLoggerFactory.getLogger(FileRecursionUtil.class);

	private FileRecursionUtil() {
		// only static methods
	}

	public enum FileProcessAction {
		NEW_FILE,
		MODIFY_FILE,
		DELETE;
	}

	/**
	 * Creates a process tree based on a list of files. Note that the list must be in
	 * preorder (root first, even if the action is DELETE)
	 * 
	 * @param files preorder list of files
	 * @param networkManager the network manager
	 * @param action which action to perform (decides over kind of process)
	 * @return the root process which can be started and holds all necessary information of its child
	 *         processes
	 */
	public static ProcessTreeNode buildProcessTree(List<File> files, NetworkManager networkManager,
			FileProcessAction action) {
		// synchronize the files that need to be uploaded into the DHT
		FileProcessTreeNode rootProcess = new FileProcessTreeNode();
		for (File file : files) {
			ProcessTreeNode parent = getParent(rootProcess, file);
			try {
				// initialize the process
				Process process = null;
				switch (action) {
					case NEW_FILE:
						process = new NewFileProcess(file, networkManager);
						break;
					case MODIFY_FILE:
						process = new NewVersionProcess(file, networkManager);
						break;
					case DELETE:
						process = new DeleteFileProcess(file, networkManager);
						break;
					default:
						logger.error("Type mismatch");
						continue;
				}

				new FileProcessTreeNode(process, parent, file);
			} catch (IllegalFileLocation e) {
				logger.error("File cannot be uploaded", e);
			} catch (NoSessionException e) {
				logger.error("File cannot be uploaded because there is no session", e);
			}
		}

		if (action == FileProcessAction.DELETE) {
			// deletion happens in reverse order
			return reverseTree(rootProcess);
		} else {
			return rootProcess;
		}
	}

	/**
	 * Finds the parent process node
	 */
	private static ProcessTreeNode getParent(FileProcessTreeNode root, File file) {
		File parent = file.getParentFile();
		for (ProcessTreeNode node : root.getAllChildren()) {
			FileProcessTreeNode fileNode = (FileProcessTreeNode) node;
			if (fileNode.getFile().equals(parent)) {
				return fileNode;
			}
		}

		return root;
	}

	public static List<File> getPreorderList(File root) {
		List<File> allFiles = new ArrayList<File>();
		listFiles(root, allFiles);
		return allFiles;
	}

	private static void listFiles(File file, List<File> preorderList) {
		preorderList.add(file);
		File[] listFiles = file.listFiles();
		if (listFiles != null)
			for (File child : listFiles) {
				listFiles(child, preorderList);
			}
	}

	/**
	 * Creates a process tree based on a list of files to download from the DHT. Note that the list must be in
	 * preorder.
	 * 
	 * @param toDownload preorder list of files
	 * @param networkManager the network manager
	 * @return the root process which can be started and holds all necessary information of its child
	 *         processes
	 */
	public static ProcessTreeNode buildProcessTreeForDownload(List<FileTreeNode> toDownload,
			NetworkManager networkManager) {
		NodeProcessTreeNode rootProcess = new NodeProcessTreeNode();
		for (FileTreeNode node : toDownload) {
			ProcessTreeNode parent = getParent(rootProcess, node);
			try {
				// initialize the process
				Process process = new DownloadFileProcess(node, networkManager);
				new NodeProcessTreeNode(process, parent, node);
			} catch (NoSessionException e) {
				logger.error("File cannot be downloaded because there is no session");
			}
		}

		return rootProcess;
	}

	/**
	 * Creates a process tree based on a list of files to delete in the DHT. Note that the list must be in
	 * preorder.
	 * 
	 * @param toDelete preorder list of files
	 * @param networkManager the network manager
	 * @return the root process which can be started and holds all necessary information of its child
	 *         processes
	 */
	public static ProcessTreeNode buildProcessTreeForDeletion(List<FileTreeNode> toDelete,
			NetworkManager networkManager) {
		// delete the files in the DHT that are deleted while offline. First create a normal tree, but the
		// order must be reverse. With the created tree, reverse it in a 2nd step.
		NodeProcessTreeNode rootProcess = new NodeProcessTreeNode();
		for (FileTreeNode node : toDelete) {
			ProcessTreeNode parent = getParent(rootProcess, node);
			try {
				// initialize the process
				DeleteFileProcess deleteProcess = new DeleteFileProcess(node, networkManager);
				new NodeProcessTreeNode(deleteProcess, parent, node);
			} catch (IllegalArgumentException e) {
				logger.error("File cannot be deleted", e);
			} catch (NoSessionException e) {
				logger.error("File cannot be deleted because there is no session", e);
			}
		}

		// files are deleted in reverse order (not pre-order)
		// get parent means get the child files --> start deletion at children,
		// thus the tree must be reversed (by using the depth)
		return reverseTree(rootProcess);
	}

	/**
	 * Reverses a tree (conserving the levels, but rearranging parent-child relations).
	 */
	private static ProcessTreeNode reverseTree(ProcessTreeNode root) {
		List<ProcessTreeNode> allNodes = root.getAllChildren();
		NodeProcessTreeNode reverseRootProcess = new NodeProcessTreeNode();
		ProcessTreeNode currentParent = reverseRootProcess;
		int currentDepth = allNodes.size();
		while (currentDepth >= 0) {
			for (ProcessTreeNode processNode : allNodes) {
				if (processNode.getDepth() == currentDepth) {
					currentParent.addChild(processNode);
				}
			}

			if (currentParent.getChildren() != null && !currentParent.getChildren().isEmpty()) {
				// children are filled --> take first child as new parent
				currentParent = currentParent.getChildren().get(0);
			}

			// decrease depth
			currentDepth--;
		}

		return reverseRootProcess;
	}

	/**
	 * Finds the parent process node
	 */
	private static ProcessTreeNode getParent(NodeProcessTreeNode root, FileTreeNode node) {
		FileTreeNode parent = node.getParent();
		for (ProcessTreeNode child : root.getAllChildren()) {
			NodeProcessTreeNode treeNode = (NodeProcessTreeNode) child;
			// skip non-directories
			if (treeNode.getNode().getFullPath().equals(parent.getFullPath())) {
				return treeNode;
			}
		}

		return root;
	}
}
