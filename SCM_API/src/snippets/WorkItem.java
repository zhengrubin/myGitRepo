package snippets;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IDetailedStatus;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.expression.AttributeExpression;
import com.ibm.team.workitem.common.expression.IQueryableAttribute;
import com.ibm.team.workitem.common.expression.IQueryableAttributeFactory;
import com.ibm.team.workitem.common.expression.QueryableAttributes;
import com.ibm.team.workitem.common.expression.Term;
import com.ibm.team.workitem.common.model.AttributeOperation;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResolvedResult;
public class WorkItem {


    public static void main(String[] args) {
        // TODO Auto-generated method stub
        TeamPlatform.startup();
        try {          
            IProgressMonitor monitor = new SysoutProgressMonitor();    
            ITeamRepository rep = new RTCConnection().login(monitor,"https://www.rtc.com:9443/ccm","rtcadmin","rtcadmin");
            
//            cleanupWorkItem(rep, monitor);
        }catch (TeamRepositoryException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            TeamPlatform.shutdown();
        }

    }

    public static void cleanupWorkItem(ITeamRepository repo,IProgressMonitor monitor) throws TeamRepositoryException {
        IWorkItemClient service1 = (IWorkItemClient) repo.getClientLibrary(IWorkItemClient.class);
        try {
            IProcessItemService service = (IProcessItemService) repo.getClientLibrary(IProcessItemService.class);
            List<IProjectArea> projectAreas = service.findAllProjectAreas(null, monitor);
            if (null != projectAreas && projectAreas.size() > 0) {
                for (IProjectArea area : projectAreas) {
                    if (area.getName().indexOf("[\u8be5\u9879\u76ee\u5df2\u5e9f\u5f03]") >= 0) {
                        IQueryResult<IResolvedResult<IWorkItem>> result = queryWorkItemForProjctArea(area,repo,monitor);
                        while (result.hasNext(null)) {
                            IWorkItem workItem = result.next(null).getItem();
                            IDetailedStatus status = service1.deleteWorkItem(workItem,monitor);
                            System.out.println(status.getMessage());
                        }
                        //归档项目区
                        service.archiveProcessItem(area, monitor);
                    }
                }
            }
        } catch (TeamRepositoryException e) {
            e.printStackTrace();
        }


    }


    public static IQueryResult<IResolvedResult<IWorkItem>> queryWorkItemForProjctArea(IProjectArea projectArea, ITeamRepository repo,IProgressMonitor monitor) throws TeamRepositoryException{

        IAuditableClient auditableClient = (IAuditableClient) repo.getClientLibrary(IAuditableClient.class);
        IQueryableAttributeFactory factory = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE);

        // try {
        IQueryClient queryClient = (IQueryClient) repo.getClientLibrary(IQueryClient.class);

        // Project Area
        IQueryableAttribute projectAreaAttribute = factory.findAttribute(projectArea,
                IWorkItem.PROJECT_AREA_PROPERTY, auditableClient, monitor);
        AttributeExpression projectAreaExpression = new AttributeExpression(projectAreaAttribute,
                AttributeOperation.EQUALS, projectArea);
        
         // ProjectArea & requirementId
        Term queryTerm = new Term(Term.Operator.AND);
        queryTerm.add(projectAreaExpression);
 
        IQueryResult<IResolvedResult<IWorkItem>> result = queryClient.getResolvedExpressionResults(projectArea,
                queryTerm, IWorkItem.FULL_PROFILE);
        return result;
    }  
    
    public static IQueryResult<IResolvedResult<IWorkItem>> queryWorkItembyID(IProjectArea projectArea, ITeamRepository repo,int workitemID,IProgressMonitor monitor) throws TeamRepositoryException{

        IAuditableClient auditableClient = (IAuditableClient) repo.getClientLibrary(IAuditableClient.class);
        IQueryableAttributeFactory factory = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE);

        // try {
        IQueryClient queryClient = (IQueryClient) repo.getClientLibrary(IQueryClient.class);

//        // Project Area
//        IQueryableAttribute projectAreaAttribute = factory.findAttribute(projectArea,
//                IWorkItem.PROJECT_AREA_PROPERTY, auditableClient, monitor);
//        AttributeExpression projectAreaExpression = new AttributeExpression(projectAreaAttribute,
//                AttributeOperation.EQUALS, projectArea);
//        
        // Workitem ID
        IQueryableAttribute idAttribute = factory.findAttribute(projectArea, IWorkItem.ID_PROPERTY, auditableClient, monitor);
        AttributeExpression idExpression = new AttributeExpression(idAttribute,AttributeOperation.EQUALS,workitemID);

        // ProjectArea & requirementId
        Term queryTerm = new Term(Term.Operator.AND);
        queryTerm.add(idExpression);

        IQueryResult<IResolvedResult<IWorkItem>> result = queryClient.getResolvedExpressionResults(projectArea,
                queryTerm, IWorkItem.FULL_PROFILE);
        return result;
    } 
}
