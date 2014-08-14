package snippets;

import org.eclipse.core.runtime.IProgressMonitor;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler.ILoginInfo;
import com.ibm.team.repository.common.TeamRepositoryException;

public class RTCConnection {
	public ITeamRepository login(IProgressMonitor monitor, String repo_address,
			String userID, String password) throws TeamRepositoryException {
		ITeamRepository repository = TeamPlatform.getTeamRepositoryService()
				.getTeamRepository(repo_address);
		repository.registerLoginHandler(new LoginHandler(userID, password));
		repository.login(monitor);

		System.out.println("登录服务器: " + repo_address + "    成功");
		return repository;
	}
}

class LoginHandler implements ILoginHandler, ILoginInfo {
	private String fUserId;
	private String fPassword;

	public LoginHandler(String userId, String password) {
		fUserId = userId;
		fPassword = password;
	}

	public String getUserId() {
		return fUserId;
	}

	public String getPassword() {
		return fPassword;
	}

	public ILoginInfo challenge(ITeamRepository repository) {
		return this;
	}
}