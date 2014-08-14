package snippets;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProcessDefinition;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContent;
import com.ibm.team.repository.common.TeamRepositoryException;

/**
 * 创建项目区
 * 
 * 
 */
public class RTCProject {

	private final Log logger = LogFactory.getLog(this.getClass());

	/**
	 * 创建项目
	 */
	public IProjectArea createOrUpdateProject(ITeamRepository rep,
			String projectTitle, String projectState, String projectDesc,
			String processid) throws TeamRepositoryException {

		IProgressMonitor monitor = new NullProgressMonitor();

		// 获取项目区
		IProjectArea processArea = null;
		String projectAreaNo = projectTitle.substring(projectTitle.lastIndexOf("_")+1, projectTitle.length()) ;
		
		
		if (null != projectAreaNo) {
			processArea = getProjectArea(rep, monitor, projectAreaNo);
		} else {
			logger.info("项目编号为空 ....");
		}

		if (null == processArea) {
			return createProject(rep, monitor, projectTitle, projectState,
					projectDesc, processid);

		} else {
			return updateProject(rep, monitor, processArea, projectTitle,
					projectState, projectDesc);
		}

	}

	public IProjectArea createProject(ITeamRepository repo,
			IProgressMonitor monitor, String projectTitle, String projectState,
			String projectDesc, String processid)
			throws TeamRepositoryException {

		IProcessItemService service = (IProcessItemService) repo
				.getClientLibrary(IProcessItemService.class);
		// First create the project area
		IProjectArea area = service.createProjectArea();

		// 项目标题
		area.setName(projectTitle);
		
		if((projectState != null) && projectState!=""){
	        area.getDescription().setSummary(projectState);
		}
		
		IContent content = repo.contentManager().storeContent("text/plain",
				projectDesc, monitor);
		area.getDescription().setDetails(content);
		// 流程模板ID

		try {
			area.setProcessDefinition(getProcessDefinition(repo, processid));
			area = (IProjectArea) service.save(area, monitor);

			// Now initialize the project making it usable.
			// The logged-in user is added as a project administrator.
			area = (IProjectArea) service.getMutableCopy(area);
			area = service.initialize(area, monitor);
			logger.info("项目区: " + projectTitle + " 创建成功");
			return area;

		} catch (TeamRepositoryException e) {

			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("unused")
	private IProjectArea updateProject(ITeamRepository repo,
			IProgressMonitor monitor, IProjectArea area, String projectTitle,
			String projectState, String projectDesc) {

		IProcessItemService service = (IProcessItemService) repo
				.getClientLibrary(IProcessItemService.class);

		IProjectArea workingCopy = null;

		try {
			// update projectArea
			workingCopy = (IProjectArea) service.getMutableCopy(area);

			workingCopy.setName(projectTitle);
			
			if((projectState != null) && projectState!=""){
			    workingCopy.getDescription().setSummary(projectState);
	        }
			
			IContent content = repo.contentManager().storeContent("text/plain",
					projectDesc, monitor);
			workingCopy.getDescription().setDetails(content);
			workingCopy = (IProjectArea) service.save(workingCopy, monitor);

			logger.info("项目区: " + projectTitle + " 更新成功..");

		} catch (TeamRepositoryException e) {
			e.printStackTrace();
		}
		return workingCopy;

	}

	// 基于过程模板ID返回模板定义
	private IProcessDefinition getProcessDefinition(ITeamRepository repo,
			String processTemplateID) throws TeamRepositoryException {
		IProgressMonitor monitor = new NullProgressMonitor();
		IProcessItemService service = (IProcessItemService) repo
				.getClientLibrary(IProcessItemService.class);
		List<IProcessDefinition> definitions = service
				.findAllProcessDefinitions(
						IProcessClientService.ALL_PROPERTIES, monitor);

		for (IProcessDefinition definition : definitions) {
			if (definition.getProcessId().equals(processTemplateID)) {
				return definition;
			}
		}
		return null;
	}

	public static IProjectArea getProjectArea(ITeamRepository repo,
			IProgressMonitor monitor, String projectAreaName)
			throws TeamRepositoryException {

		IProcessItemService service = (IProcessItemService) repo
				.getClientLibrary(IProcessItemService.class);

		List projectAreas = service.findAllProjectAreas(null, monitor);

		if (null != projectAreas && projectAreas.size() > 0) {

			for (int i = 0; i < projectAreas.size(); i++) {
				IProjectArea projectArea = (IProjectArea) projectAreas.get(i);
				// System.out.println(projectArea.getName());
				if (projectArea.getName().indexOf(projectAreaName) >= 0) {
					IProjectArea desiredProject = projectArea;
					return desiredProject;
				}
			}
		}

		return null;
	}
}