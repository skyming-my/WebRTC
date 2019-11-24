/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.integtests;

import org.gradle.integtests.fixtures.AbstractIntegrationTest;
import org.gradle.integtests.fixtures.executer.ExecutionFailure;
import org.gradle.test.fixtures.file.TestFile;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.Matchers.startsWith;

public class ProjectLoadingIntegrationTest extends AbstractIntegrationTest {
    @Test
    public void handlesSimilarlyNamedBuildFilesInSameDirectory() {
        TestFile buildFile1 = testFile("similarly-named build.gradle").write("task build");
        TestFile buildFile2 = testFile("similarly_named_build_gradle").write("task 'other-build'");

        usingBuildFile(buildFile1).withTasks("build").run();

        usingBuildFile(buildFile2).withTasks("other-build").run();

        usingBuildFile(buildFile1).withTasks("build").run();
    }

    @Test
    public void handlesWhitespaceOnlySettingsAndBuildFiles() {
        testFile("settings.gradle").write("   \n  ");
        testFile("build.gradle").write("   ");
        inTestDirectory().withTaskList().run();
    }

    @Test
    public void canDetermineRootProjectAndDefaultProjectBasedOnCurrentDirectory() {
        File rootDir = getTestDirectory();
        File childDir = new File(rootDir, "child");

        testFile("settings.gradle").write("include('child')");
        testFile("build.gradle").write("task('do-stuff')");
        testFile("child/build.gradle").write("task('do-stuff')");

        inDirectory(rootDir).withSearchUpwards().withTasks("do-stuff").run().assertTasksExecuted(":do-stuff", ":child:do-stuff");
        inDirectory(rootDir).withSearchUpwards().withTasks(":do-stuff").run().assertTasksExecuted(":do-stuff");

        inDirectory(childDir).withSearchUpwards().withTasks("do-stuff").run().assertTasksExecuted(":child:do-stuff");
        inDirectory(childDir).withSearchUpwards().withTasks(":do-stuff").run().assertTasksExecuted(":do-stuff");
    }

    @Test
    public void canDetermineRootProjectAndDefaultProjectBasedOnProjectDirectory() {
        File rootDir = getTestDirectory();
        File childDir = new File(rootDir, "child");

        testFile("settings.gradle").write("include('child')");
        testFile("build.gradle").write("task('do-stuff')");
        testFile("child/build.gradle").write("task('do-stuff')");

        usingProjectDir(rootDir).withSearchUpwards().withTasks("do-stuff").run().assertTasksExecuted(":do-stuff", ":child:do-stuff");
        usingProjectDir(rootDir).withSearchUpwards().withTasks(":do-stuff").run().assertTasksExecuted(":do-stuff");

        usingProjectDir(childDir).withSearchUpwards().withTasks("do-stuff").run().assertTasksExecuted(":child:do-stuff");
        usingProjectDir(childDir).withSearchUpwards().withTasks(":do-stuff").run().assertTasksExecuted(":do-stuff");
    }

    @Test
    public void canDetermineRootProjectAndDefaultProjectBasedOnBuildFile() {
        testFile("settings.gradle").write("include('child')");

        TestFile rootBuildFile = testFile("build.gradle");
        rootBuildFile.write("task('do-stuff')");

        TestFile childBuildFile = testFile("child/build.gradle");
        childBuildFile.write("task('do-stuff')");

        usingBuildFile(rootBuildFile).withSearchUpwards().withTasks("do-stuff").run().assertTasksExecuted(":do-stuff", ":child:do-stuff");
        usingBuildFile(rootBuildFile).withSearchUpwards().withTasks(":do-stuff").run().assertTasksExecuted(":do-stuff");

        usingBuildFile(childBuildFile).withSearchUpwards().withTasks("do-stuff").run().assertTasksExecuted(":child:do-stuff");
        usingBuildFile(childBuildFile).withSearchUpwards().withTasks(":do-stuff").run().assertTasksExecuted(":do-stuff");
    }

    @Test
    public void buildFailsWhenMultipleProjectsMeetDefaultProjectCriteria() {
        testFile("settings.gradle").writelns(
            "include 'child'",
            "project(':child').projectDir = rootProject.projectDir");
        testFile("build.gradle").write("// empty");

        ExecutionFailure result = inTestDirectory().withTasks("test").runWithFailure();
        result.assertThatDescription(startsWith("Multiple projects in this build have project directory"));

        result = usingProjectDir(getTestDirectory()).withTasks("test").runWithFailure();
        result.assertThatDescription(startsWith("Multiple projects in this build have project directory"));

        result = usingBuildFile(testFile("build.gradle")).withTasks("test").runWithFailure();
        result.assertThatDescription(startsWith("Multiple projects in this build have build file"));
    }

    @Test
    public void buildFailsWhenSpecifiedBuildFileIsNotAFile() {
        TestFile file = testFile("unknown");

        ExecutionFailure result = usingBuildFile(file).runWithFailure();
        result.assertHasDescription("The specified build file '" + file + "' does not exist.");

        file.createDir();

        result = usingBuildFile(file).runWithFailure();
        result.assertHasDescription("The specified build file '" + file + "' is not a file.");
    }

    @Test
    public void buildFailsWhenSpecifiedProjectDirectoryIsNotADirectory() {
        TestFile file = testFile("unknown");

        ExecutionFailure result = usingProjectDir(file).runWithFailure();
        result.assertHasDescription("The specified project directory '" + file + "' does not exist.");

        file.createFile();

        result = usingProjectDir(file).runWithFailure();
        result.assertHasDescription("The specified project directory '" + file + "' is not a directory.");
    }

    @Test
    public void buildFailsWhenSpecifiedSettingsFileIsNotAFile() {
        TestFile file = testFile("unknown");

        ExecutionFailure result = inTestDirectory().usingSettingsFile(file).runWithFailure();
        result.assertHasDescription("The specified settings file '" + file + "' does not exist.");

        file.createDir();

        result = inTestDirectory().usingSettingsFile(file).runWithFailure();
        result.assertHasDescription("The specified settings file '" + file + "' is not a file.");
    }

    @Test
    public void buildFailsWhenSpecifiedSettingsFileDoesNotContainMatchingProject() {
        TestFile settingsFile = testFile("settings.gradle");
        settingsFile.write("// empty");

        TestFile projectDir = testFile("project dir");
        TestFile buildFile = projectDir.file("build.gradle").createFile();

        ExecutionFailure result = usingProjectDir(projectDir).usingSettingsFile(settingsFile).runWithFailure();
        result.assertHasDescription(String.format("No projects in this build have project directory '%s'.", projectDir));

        result = usingBuildFile(buildFile).usingSettingsFile(settingsFile).runWithFailure();
        result.assertHasDescription(String.format("No projects in this build have build file '%s'.", buildFile));
    }

    @Test
    public void settingsFileTakesPrecedenceOverBuildFileInSameDirectory() {
        testFile("settings.gradle").write("rootProject.buildFileName = 'root.gradle'");
        testFile("root.gradle").write("task('do-stuff')");

        TestFile buildFile = testFile("build.gradle");
        buildFile.write("throw new RuntimeException()");

        inTestDirectory().withTasks("do-stuff").run();
        usingProjectDir(getTestDirectory()).withTasks("do-stuff").run();
    }

    @Test
    public void settingsFileInParentDirectoryTakesPrecedenceOverBuildFile() {
        testFile("settings.gradle").writelns(
            "include 'child'",
            "project(':child').buildFileName = 'child.gradle'"
        );

        TestFile subDirectory = getTestDirectory().file("child");
        subDirectory.file("build.gradle").write("throw new RuntimeException()");
        subDirectory.file("child.gradle").write("task('do-stuff')");

        inDirectory(subDirectory).withSearchUpwards().withTasks("do-stuff").run();
        usingProjectDir(subDirectory).withSearchUpwards().withTasks("do-stuff").run();
    }

    @Test
    public void explicitBuildFileTakesPrecedenceOverSettingsFileInSameDirectory() {
        testFile("settings.gradle").write("rootProject.buildFileName = 'root.gradle'");
        testFile("root.gradle").write("throw new RuntimeException()");

        TestFile buildFile = testFile("build.gradle");
        buildFile.write("task('do-stuff')");

        usingBuildFile(buildFile).withTasks("do-stuff").run();
    }

    @Test
    public void ignoresMultiProjectBuildInParentDirectoryWhichDoesNotMeetDefaultProjectCriteria() {
        testFile("settings.gradle").write("include 'another'");
        testFile("gradle.properties").writelns("prop=value2", "otherProp=value");

        TestFile subDirectory = getTestDirectory().file("subdirectory");
        TestFile buildFile = subDirectory.file("build.gradle");
        buildFile.writelns("task('do-stuff') {",
                "doLast {",
                "assert prop == 'value'",
                "assert !project.hasProperty('otherProp')",
                "}",
                "}");
        testFile("subdirectory/gradle.properties").write("prop=value");

        inDirectory(subDirectory).withSearchUpwards().withTasks("do-stuff").run();
        usingProjectDir(subDirectory).withSearchUpwards().withTasks("do-stuff").run();
        usingBuildFile(buildFile).withSearchUpwards().withTasks("do-stuff").run();
    }

    @Test
    public void multiProjectBuildCanHaveMultipleProjectsWithSameProjectDir() {
        testFile("settings.gradle").writelns(
            "include 'child1', 'child2'",
            "project(':child1').projectDir = new File(settingsDir, 'shared')",
            "project(':child2').projectDir = new File(settingsDir, 'shared')"
        );
        testFile("shared/build.gradle").write("task('do-stuff')");

        inTestDirectory().withTasks("do-stuff").run().assertTasksExecuted(":child1:do-stuff", ":child2:do-stuff");
    }

    @Test
    public void multiProjectBuildCanHaveSeveralProjectsWithSameBuildFile() {
        testFile("settings.gradle").writelns(
            "include 'child1', 'child2'",
            "project(':child1').buildFileName = '../child.gradle'",
            "project(':child2').buildFileName = '../child.gradle'"
        );
        testFile("child.gradle").write("task('do-stuff')");

        inTestDirectory().withTasks("do-stuff").run().assertTasksExecuted(":child1:do-stuff", ":child2:do-stuff");
    }

    @Test
    public void multiProjectBuildCanHaveSettingsFileAndRootBuildFileInSubDir() {
        TestFile buildFilesDir = getTestDirectory().file("root");
        TestFile settingsFile = buildFilesDir.file("settings.gradle");
        settingsFile.writelns(
            "includeFlat 'child'",
            "rootProject.projectDir = new File(settingsDir, '..')",
            "rootProject.buildFileName = 'root/build.gradle'"
        );

        TestFile rootBuildFile = buildFilesDir.file("build.gradle");
        rootBuildFile.write("task('do-stuff', dependsOn: ':child:task')");

        TestFile childBuildFile = testFile("child/build.gradle");
        childBuildFile.writelns("task('do-stuff')", "task('task')");

        usingProjectDir(getTestDirectory()).usingSettingsFile(settingsFile).withTasks("do-stuff").run().assertTasksExecuted(":child:task", ":do-stuff", ":child:do-stuff").assertTaskOrder(":child:task", ":do-stuff");
        usingBuildFile(rootBuildFile).withTasks("do-stuff").run().assertTasksExecuted(":child:task", ":do-stuff", ":child:do-stuff").assertTaskOrder(":child:task", ":do-stuff");
        usingBuildFile(childBuildFile).usingSettingsFile(settingsFile).withTasks("do-stuff").run().assertTasksExecutedInOrder(":child:do-stuff");
    }

    @Test
    public void multiProjectBuildCanHaveAllProjectsAsChildrenOfSettingsDir() {
        TestFile settingsFile = testFile("settings.gradle");
        settingsFile.writelns(
            "rootProject.projectDir = new File(settingsDir, 'root')",
            "include 'sub'",
            "project(':sub').projectDir = new File(settingsDir, 'root/sub')"
        );

        getTestDirectory().createDir("root").file("build.gradle").writelns("allprojects { task thing }");

        inTestDirectory().withTasks(":thing").run().assertTasksExecuted(":thing");
        inTestDirectory().withTasks(":sub:thing").run().assertTasksExecuted(":sub:thing");
    }

    @Test
    public void usesRootProjectAsDefaultProjectWhenInSettingsDir() {
        TestFile settingsDir = testFile("gradle");
        TestFile settingsFile = settingsDir.file("settings.gradle");
        settingsFile.writelns(
            "rootProject.projectDir = new File(settingsDir, '../root')",
            "include 'sub'",
            "project(':sub').projectDir = new File(settingsDir, '../root/sub')"
        );
        getTestDirectory().createDir("root").file("build.gradle").writelns("allprojects { task thing }");

        inDirectory(settingsDir).withTasks("thing").run().assertTasksExecuted(":thing", ":sub:thing");
    }

    @Test
    public void rootProjectDirectoryAndBuildFileDoNotHaveToExistWhenInSettingsDir() {
        TestFile settingsDir = testFile("gradle");
        TestFile settingsFile = settingsDir.file("settings.gradle");
        settingsFile.writelns(
                "rootProject.projectDir = new File(settingsDir, '../root')",
                "include 'sub'",
                "project(':sub').projectDir = new File(settingsDir, '../sub')"
        );
        getTestDirectory().createDir("sub").file("build.gradle").writelns("task thing");

        inDirectory(settingsDir).withTasks("thing").run().assertTasksExecuted(":sub:thing");
    }

    @Test
    public void settingsFileGetsIgnoredWhenUsingSettingsOnlyDirectoryAsProjectDirectory() {
        TestFile settingsDir = testFile("gradle");
        TestFile settingsFile = settingsDir.file("settings.gradle");
        settingsFile.writelns(
                "rootProject.projectDir = new File(settingsDir, '../root')"
        );
        getTestDirectory().createDir("root").file("build.gradle").writelns("task thing");

        inTestDirectory().withArguments("-p", settingsDir.getAbsolutePath()).withTasks("thing").runWithFailure()
                .assertHasDescription("Task 'thing' not found in root project 'gradle'.");
    }
}