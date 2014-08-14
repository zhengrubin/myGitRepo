package snippets;

import org.eclipse.core.runtime.IProgressMonitor;

public class SysoutProgressMonitor implements IProgressMonitor {

    public void beginTask(String name, int totalWork) {
        print(name);
    }

    public void done() {
    }

    public void internalWorked(double work) {
    }

    public boolean isCanceled() {
        return false;
    }

    public void setCanceled(boolean value) {
    }

    public void setTaskName(String name) {
        print(name);
    }

    public void subTask(String name) {
        print(name);
    }

    public void worked(int work) {
    }
    
    private void print(String name) {
        if(name != null && ! "".equals(name))
            System.out.println(name);
    }
}
