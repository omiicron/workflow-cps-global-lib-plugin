package org.jenkinsci.plugins.workflow.cps.global;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Run;
import hudson.util.CopyOnWriteList;
import org.apache.commons.io.FilenameUtils;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.jenkinsci.plugins.workflow.cps.GlobalVariableSet;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Keeps {@link UserDefinedGlobalVariable}s in {@link ExtensionList} up-to-date
 * from {@code $JENKINS_HOME/workflow-libs/vars/*.groovy}.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class UserDefinedGlobalVariableList extends GlobalVariableSet {
    private @Inject
    WorkflowLibRepository repo;

    private volatile CopyOnWriteList<GlobalVariable> ours;

    /**
     * Rebuilds the list of {@link UserDefinedGlobalVariable}s and update {@link ExtensionList} accordingly.
     */
    public synchronized void rebuild() {
        File varFile = new File(repo.workspace, UserDefinedGlobalVariable.PREFIX);

        List<GlobalVariable> list = new ArrayList<GlobalVariable>(findLibraries(varFile));

        // first time, build the initial list
        if (ours == null)
            ours = new CopyOnWriteList<GlobalVariable>();
        ours.replaceBy(list);
    }

    public List<GlobalVariable> findLibraries(File file) {
        File[] children = file.listFiles();
        if (children == null) children = new File[0];

        List<GlobalVariable> list = new ArrayList<GlobalVariable>();

        for (File child : children) {
            if (child.isDirectory()) {
                list.addAll(findLibraries(child));
            } else if (child.getName().endsWith(".groovy")) {
                UserDefinedGlobalVariable uv = new UserDefinedGlobalVariable(repo, FilenameUtils.getBaseName(child.getName()));
                list.add(uv);
            }
        }
        return list;
    }

    @Override
    public Collection<GlobalVariable> forRun(Run<?, ?> run) {
        if (ours == null) {
            rebuild();
        }
        return ours.getView();
    }
}
