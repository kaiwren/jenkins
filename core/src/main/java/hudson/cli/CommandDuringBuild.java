/*
 * The MIT License
 *
 * Copyright (c) 2010, InfraDNA, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.cli;

import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Run;
import hudson.remoting.Callable;
import org.kohsuke.args4j.CmdLineException;

import java.io.IOException;

/**
 * Base class for those commands that are valid only during a build.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class CommandDuringBuild extends CLICommand {
    /**
     * This method makes sense only when called from within the build kicked by Hudson.
     * We use the environment variables that Hudson sets to determine the build that is being run.
     */
    protected Run getCurrentlyBuilding() throws CmdLineException {
        try {
            CLICommand c = CLICommand.getCurrent();
            if (c==null)    throw new IllegalStateException("Not executing a CLI command");
            String[] envs = c.channel.call(new GetCharacteristicEnvironmentVariables());

            if (envs[0]==null || envs[1]==null)
                throw new CmdLineException("This CLI command works only when invoked from inside a build");

            Job j = Hudson.getInstance().getItemByFullName(envs[0],Job.class);
            if (j==null)    throw new CmdLineException("No such job: "+envs[0]);

            try {
                Run r = j.getBuildByNumber(Integer.parseInt(envs[1]));
                if (r==null)    throw new CmdLineException("No such build #"+envs[1]+" in "+envs[0]);
                return r;
            } catch (NumberFormatException e) {
                throw new CmdLineException("Invalid build number: "+envs[1]);
            }
        } catch (IOException e) {
            throw new CmdLineException("Failed to identify the build being executed",e);
        } catch (InterruptedException e) {
            throw new CmdLineException("Failed to identify the build being executed",e);
        }
    }

    /**
     * Gets the environment variables that points to the build being executed.
     */
    private static final class GetCharacteristicEnvironmentVariables implements Callable<String[],IOException> {
        public String[] call() throws IOException {
            return new String[] {
                System.getenv("JOB_NAME"),
                System.getenv("BUILD_NUMBER")
            };
        }
    }
}
