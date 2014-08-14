package snippets;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.filesystem.client.FileSystemCore;
import com.ibm.team.filesystem.client.IFileContentManager;
import com.ibm.team.filesystem.client.workitems.IFileSystemWorkItemManager;
import com.ibm.team.filesystem.common.FileLineDelimiter;
import com.ibm.team.filesystem.common.IFileContent;
import com.ibm.team.filesystem.common.IFileItem;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.ItemNotFoundException;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.UUID;
import com.ibm.team.scm.client.IFlowNodeConnection.IComponentOp;
import com.ibm.team.scm.client.IFlowNodeConnection.IComponentOpFactory;
import com.ibm.team.scm.client.IWorkspaceConnection;
import com.ibm.team.scm.client.IWorkspaceManager;
import com.ibm.team.scm.client.SCMPlatform;
import com.ibm.team.scm.client.content.util.VersionedContentManagerByteArrayInputStreamPovider;
import com.ibm.team.scm.common.ComponentNotInWorkspaceException;
import com.ibm.team.scm.common.IChangeSetHandle;
import com.ibm.team.scm.common.IComponent;
import com.ibm.team.scm.common.IComponentHandle;
import com.ibm.team.scm.common.IFolder;
import com.ibm.team.scm.common.IVersionableHandle;
import com.ibm.team.scm.common.IWorkspace;
import com.ibm.team.scm.common.IWorkspaceHandle;
import com.ibm.team.scm.common.WorkspaceComparisonFlags;
import com.ibm.team.scm.common.dto.IChangeHistorySyncReport;
import com.ibm.team.scm.common.dto.IPermissionContextProvider;
import com.ibm.team.scm.common.dto.IWorkspaceSearchCriteria;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResolvedResult;

public class Snippet2 {

	public static void main(String[] args) {
		TeamPlatform.startup();
		try {
			IProgressMonitor monitor = new SysoutProgressMonitor();
			// 登录服务器
			ITeamRepository repo = new RTCConnection().login(monitor,
					"https://www.rtc.com:9443/ccm", "rtcadmin", "rtcadmin");
			addFilesToStream(repo, monitor);
		} catch (TeamRepositoryException e) {
			System.out.println("Unable to login: " + e.getMessage());
		} finally {
			TeamPlatform.shutdown();
		}
	}

	public static IWorkspaceConnection addFilesToStream(ITeamRepository repo,
			IProgressMonitor monitor) throws TeamRepositoryException,
			ItemNotFoundException, ComponentNotInWorkspaceException {
		// 指定操作的项目区
		IProjectArea prjarea = RTCProject
				.getProjectArea(repo, monitor, "测试项目区");

		IWorkspaceManager wm = SCMPlatform.getWorkspaceManager(repo);
		IFileSystemWorkItemManager fswim = (IFileSystemWorkItemManager) repo
				.getClientLibrary(IFileSystemWorkItemManager.class);

		// 检查存储库工作区是否存在，如果不存在则创建
		IWorkspaceConnection workspace = findWorkspaceORStream(repo,
				"Example Workspace", monitor,
				IWorkspaceSearchCriteria.WORKSPACES);
		if (workspace == null) {
			workspace = wm.createWorkspace(repo.loggedInContributor(),
					"Example Workspace", "Description", monitor);
		}

		// 检查组件是否存在，不存在则创建
		IComponentHandle component = findComponentInWorkspace(workspace,
				"Component", monitor);
		if (component == null) {
			component = wm.createComponent("Component",
					repo.loggedInContributor(), monitor);
			IComponentOpFactory componentOpFactory = workspace
					.componentOpFactory();
			IComponentOp addComponentOp = componentOpFactory.addComponent(
					component, false);
			workspace.applyComponentOperations(
					Collections.singletonList(addComponentOp), false, monitor);

			// create the stream seeded from the workspace
			IWorkspaceConnection stream = wm.createStream(prjarea,
					"Example Stream", "Description", monitor);
			addComponentOp = componentOpFactory.addComponent(component, false);
			stream.applyComponentOperations(
					Collections.singletonList(addComponentOp), false, monitor);
		}

		// The root folder is created when the component is created.
		// add a folder called 'project' to the workspace
		IChangeSetHandle cs1 = workspace.createChangeSet(component, monitor);
		IFolder rootFolder = (IFolder) workspace.configuration(component)
				.rootFolderHandle(monitor);

		// create source folder ("/project")
		IFolder projectFolder = (IFolder) IFolder.ITEM_TYPE.createItem();
		projectFolder.setParent(rootFolder);
		projectFolder.setName("project");
		workspace.commit(cs1, Collections.singletonList(workspace
				.configurationOpFactory().save(projectFolder)), monitor);

		// add a file called 'file.txt' under the 'project' folder.
		IFileItem file = (IFileItem) IFileItem.ITEM_TYPE.createItem();
		file.setName("file.txt");
		file.setParent(projectFolder);
		IFileContentManager contentManager = FileSystemCore
				.getContentManager(repo);
		IFileContent storedContent = contentManager.storeContent(
				IFileContent.ENCODING_US_ASCII,
				FileLineDelimiter.LINE_DELIMITER_PLATFORM,
				new VersionedContentManagerByteArrayInputStreamPovider(
						"The contents of my file.txt".getBytes()), null,
				monitor);
		file.setContentType(IFileItem.CONTENT_TYPE_TEXT);
		file.setContent(storedContent);
		file.setFileTimestamp(new Date());
		workspace.commit(cs1, Collections.singletonList(workspace
				.configurationOpFactory().save(file)), monitor);

		// 设置目录访问权限为某团队区域。 RTC内部API，RTC升级可能导致API失效
		com.ibm.team.scm.common.dto.IPermissionContextProvider pcp = IPermissionContextProvider.FACTORY
				.create(prjarea); // 参数为团队区或者项目区
		IVersionableHandle[] vh = new IVersionableHandle[2];
		vh[0] = projectFolder;
		vh[1] = file;
		wm.setPermissions(vh, component, pcp, monitor);

		// 关联工作项
		IQueryResult<IResolvedResult<IWorkItem>> result = WorkItem
				.queryWorkItembyID(prjarea, repo, 2, monitor); // 2 为目标工作项的id
		IWorkItem workItem[] = new IWorkItem[1];
		if ((result.hasNext(null)) && (result.getTotalSize(monitor) <= 1)) {
			workItem[0] = result.next(null).getItem();
			System.out.println(workItem[0].getHTMLSummary().getPlainText());
		}
		fswim.createLink(workspace.getResolvedWorkspace(), cs1, workItem,
				monitor);

		// deliver the changes to the stream
		IWorkspaceConnection targetStream = findWorkspaceORStream(repo,
				"Example Stream", monitor, IWorkspaceSearchCriteria.STREAMS);
		IChangeHistorySyncReport sync = workspace.compareTo(targetStream,
				WorkspaceComparisonFlags.CHANGE_SET_COMPARISON_ONLY,
				Collections.EMPTY_LIST, monitor);
		workspace.deliver(targetStream, sync, Collections.EMPTY_LIST,
				sync.outgoingChangeSets(component), monitor);
		monitor.subTask("Created changes and delivered to "
				+ targetStream.getName());
		return workspace;
	}

	private static UUID toUUID(String id) {
		try {
			return UUID.valueOf(id);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static IWorkspaceConnection findWorkspaceORStream(
			ITeamRepository repo, String workspace, IProgressMonitor monitor,
			int streamORworkspace) throws TeamRepositoryException {
		IWorkspaceHandle wh;
		UUID workspaceId = toUUID(workspace);
		if (workspaceId != null) {
			wh = (IWorkspaceHandle) IWorkspace.ITEM_TYPE.createItemHandle(
					workspaceId, null);
		} else {
			// not a UUID, could be a name.
			IWorkspaceSearchCriteria wsc = IWorkspaceSearchCriteria.FACTORY
					.newInstance();
			wsc.setExactName(workspace);
			wsc.setKind(streamORworkspace);
			List<IWorkspaceHandle> ws = SCMPlatform.getWorkspaceManager(repo)
					.findWorkspaces(wsc, 2, monitor);
			switch (ws.size()) {
			case 0:
				wsc = IWorkspaceSearchCriteria.FACTORY.newInstance();
				wsc.setExactName(workspace);
				ws = SCMPlatform.getWorkspaceManager(repo).findWorkspaces(wsc,
						2, monitor);
				switch (ws.size()) {
				case 0:
					System.err.println("Failed to find workspace/stream named "
							+ workspace);
					return null;
				case 1:
					wh = ws.get(0);
					break;
				default:
					System.err.println("Multiple workspaces/streams named "
							+ workspace);
					return null;
				}
			case 1:
				wh = ws.get(0);
				break;
			default:
				System.err.println("Multiple workspaces/stream named "
						+ workspace + " owned by "
						+ repo.loggedInContributor().getUserId());
				return null;
			}
		}
		return SCMPlatform.getWorkspaceManager(repo).getWorkspaceConnection(wh,
				monitor);
	}

	private static IComponentHandle findComponentInWorkspace(
			IWorkspaceConnection conn, String component,
			IProgressMonitor monitor) throws TeamRepositoryException {
		UUID componentId = toUUID(component);
		if (componentId != null) {
			for (Object o : conn.getComponents()) {
				IComponentHandle cmp = (IComponentHandle) o;
				if (cmp.getItemId().equals(componentId)) {
					return cmp;
				}
			}
		} else {
			List components = conn
					.teamRepository()
					.itemManager()
					.fetchCompleteItems(conn.getComponents(),
							IItemManager.DEFAULT, monitor);
			for (Object o : components) {
				IComponent cmp = (IComponent) o;
				if (component.equals(cmp.getName())) {
					return cmp;
				}
			}
		}

		System.err.println("Failed to find component named " + component
				+ " in workspace " + conn.getName());
		return null;
	}

}
