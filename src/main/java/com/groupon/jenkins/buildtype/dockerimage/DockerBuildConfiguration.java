/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014, Groupon, Inc.
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

package com.groupon.jenkins.buildtype.dockerimage;

import com.groupon.jenkins.buildtype.install_packages.buildconfiguration.configvalue.ListOrMapOrString;
import com.groupon.jenkins.buildtype.install_packages.buildconfiguration.configvalue.MapValue;
import com.groupon.jenkins.buildtype.install_packages.buildconfiguration.configvalue.StringValue;
import com.groupon.jenkins.buildtype.util.config.Config;
import com.groupon.jenkins.buildtype.util.shell.ShellCommands;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.Combination;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DockerBuildConfiguration {
    private Config config;
    private ShellCommands checkoutCommands;

    public DockerBuildConfiguration(Map config, ShellCommands checkoutCommands) {
        this.checkoutCommands = checkoutCommands;
        this.config = new Config(config, "image", StringValue.class, "env", MapValue.class, "script", ListOrMapOrString.class);
    }

    public ShellCommands toShellCommands(Combination combination) {
        ShellCommands shellCommands = new ShellCommands();

        DockerCommandBuilder dockerRunCommand = DockerCommandBuilder.dockerCommand("run")
                .flag("rm")
                .flag("sig-proxy=true")
                .args(getImageName(), "sh -c \"" +  getRunCommand(combination) + "\"");

        exportEnvVars(dockerRunCommand);
        shellCommands.add(dockerRunCommand.get());

        return shellCommands;
    }

    private String getRunCommand(Combination combination) {
        List commands;
        if(isParallized()){
            Map script = config.get("script", Map.class);
            Object scriptCommands= script.get(combination.get("script"));
            commands = scriptCommands instanceof List? (List)scriptCommands: Arrays.asList(scriptCommands);
        }else{
            commands = config.get("script", List.class);
        }

        return checkoutCommands.add(new ShellCommands(commands)).toSingleShellCommand();
    }

    private String getImageName() {
      return config.get("image",String.class);
    }


    private void exportEnvVars(DockerCommandBuilder runCommand) {
        if(config.containsKey("env")){
            Map<String, String> envVars = config.get("env",Map.class);
            for (Map.Entry<String, String> var : envVars.entrySet()) {
                runCommand.flag("e", String.format("\"%s=%s\"", var.getKey(), var.getValue()));
            }
        }
    }

    public AxisList getAxisList() {
        AxisList  axisList = new AxisList(new Axis("script", "main"));
        if (isParallized()) {
            Set scriptKeys =  ((Map) config.get("script")).keySet();
            axisList = new AxisList(new Axis("script", new ArrayList<String>(scriptKeys)));
        }
        return axisList;
    }

    public boolean isParallized() {
        return config.get("script") instanceof Map;
    }
}
