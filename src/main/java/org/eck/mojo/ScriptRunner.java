package org.eck.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eck.mojo.internal.Console;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

@Mojo( name = "run", requiresDependencyResolution=ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ScriptRunner extends AbstractMojo {

    @Component
    private MavenProject project;

    @Parameter( property = "run.task" )
    private String task;

    @Parameter( property = "run.tasksFolder" )
    private String tasksFolder;

    @Parameter( defaultValue = "${project.basedir}", readonly = true )
    private File basePath;

    public void execute() throws MojoExecutionException {
        if(task == null || tasksFolder == null) {
            throw new RuntimeException("Both task and taskFolder are required parameters");
        }

        String taskPath = basePath.getAbsolutePath() + "/" + tasksFolder + "/" + task + ".js";
        File taskFile = new File(taskPath);
        if(!taskFile.exists()) {
            throw new RuntimeException("Task does not exists");
        }

        String taskContent = readFile(taskFile);
        resolveClasspath();
        runTask(taskContent, taskPath);
    }

    public void runTask(String taskContent, String taskPath) {
        Context cx = Context.enter();
        ScriptableObject global = new ImporterTopLevel(cx);

        // Add the console object
        global.put("console", global, new Console(getLog()));

        ScriptableObject module = (ScriptableObject) cx.newObject(global);
        global.put("module", global, module);

        Script taskScript = cx.compileString(taskContent, taskPath, 1, null);
        taskScript.exec(cx, global);

        Function function = (Function) module.get("exports");

        ScriptableObject scriptableObject = (ScriptableObject) cx.newObject(global);
        scriptableObject.setParentScope(global);

        function.call(cx, global, scriptableObject, new Object[0]);
    }

    @SuppressWarnings("rawtypes")
    private void resolveClasspath() {
        try {
            List runtimeClasspathElements = project.getRuntimeClasspathElements();
            URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
            for (int i = 0; i < runtimeClasspathElements.size(); i++) {
              String element = (String) runtimeClasspathElements.get(i);
              runtimeUrls[i] = new File(element).toURI().toURL();
            }
            URLClassLoader newLoader = new URLClassLoader(runtimeUrls,
            Thread.currentThread().getContextClassLoader());
            Thread.currentThread().setContextClassLoader(newLoader);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

     protected static String readFile(File file) {
        try {
            return readStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private static String readStream(InputStream resourceAsStream) {
        if (resourceAsStream == null) {
            throw new RuntimeException("File not found to import");
        }

        BufferedReader br = null;
        StringBuilder ret = new StringBuilder();

        try {
            String line;
            br = new BufferedReader(new InputStreamReader(resourceAsStream));

            while ((line = br.readLine()) != null) {
                ret.append(line).append(System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return ret.toString();
    }
}
