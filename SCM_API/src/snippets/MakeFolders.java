package snippets;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import com.ibm.team.filesystem.common.IFileItem;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.TeamRepositoryException;
import org.eclipse.core.runtime.IProgressMonitor;
import com.ibm.team.scm.client.IConfiguration;
import com.ibm.team.scm.client.IWorkspaceConnection;
import com.ibm.team.scm.common.IChangeSetHandle;
import com.ibm.team.scm.common.IFolder;
import com.ibm.team.scm.common.IFolderHandle;
import com.ibm.team.scm.common.IVersionable;
import com.ibm.team.scm.common.IVersionableHandle;

public class MakeFolders {

	private ITeamRepository fTeamRepository = null; 
	private IWorkspaceConnection fWorkspace = null;
	private IChangeSetHandle fChangeSet = null;
	private IConfiguration fConfiguration=null;
	private IProgressMonitor fMonitor = new SysoutProgressMonitor();  

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	private IFolder findOrCreateFolderWithParents(File folder)
			throws TeamRepositoryException {
		IFolder parent = null;
		String folderName = folder.getName();
		String parentName = folder.getParent();
		if (parentName == null) {
			parent = fConfiguration.completeRootFolder(fMonitor);
		} else {
			// Recursively find the parent folders
			parent = findOrCreateFolderWithParents(new File(parentName));
		}
		IFolder found = getFolder(folderName, parent);
		if (found == null) {
			found = createFolder(folderName, parent);
		}
		return found;
	}

	@SuppressWarnings("unchecked")
	private IFolder getFolder(String folderName, IFolderHandle parentFolder)
			throws TeamRepositoryException {
		IVersionable foundItem = getVersionable(folderName, parentFolder);
		if (null != foundItem) {
			if (foundItem instanceof IFolder) {
				return (IFolder) foundItem;
			}
		}
		return null;
	}

	private IVersionable getVersionable(String name, IFolderHandle parentFolder)
			throws TeamRepositoryException {
		// get all the child entries
		@SuppressWarnings("unchecked")
		Map<String, IVersionableHandle> handles = fConfiguration.childEntries(
				parentFolder, fMonitor);
		// try to find an entry with the name
		IVersionableHandle foundHandle = handles.get(name);
		if (null != foundHandle) {
			return fConfiguration.fetchCompleteItem(foundHandle, fMonitor);
		}
		return null;
	}

	private IFolder createFolder(String folderName, IFolder parent)
			throws TeamRepositoryException {
		IFolder newFolder = (IFolder) IFolder.ITEM_TYPE.createItem();
		newFolder.setParent(parent);
		newFolder.setName(folderName);
		fWorkspace.commit(fChangeSet, Collections.singletonList(fWorkspace
				.configurationOpFactory().save(newFolder)), fMonitor);
		return newFolder;
	}

	private IFileItem getFile(File file, IFolderHandle parentFolder)
			throws TeamRepositoryException {
		IVersionable foundItem = getVersionable(file.getName(), parentFolder);
		if (null != foundItem) {
			if (foundItem instanceof IFileItem) {
				return (IFileItem) foundItem;
			}
		}
		return null;
	}

}
