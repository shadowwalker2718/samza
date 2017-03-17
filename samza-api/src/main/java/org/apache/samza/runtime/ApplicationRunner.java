/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.samza.runtime;

import java.lang.reflect.Constructor;
import org.apache.samza.annotation.InterfaceStability;
import org.apache.samza.config.ConfigException;
import org.apache.samza.operators.StreamGraphBuilder;
import org.apache.samza.config.Config;
import org.apache.samza.system.StreamSpec;


/**
 * Interface to be implemented by physical execution engine to deploy the config and jobs to run the {@link org.apache.samza.operators.StreamGraph}
 *
 * Implementations of this interface must define a constructor with a single {@link Config} as the argument in order
 * to support the {@link ApplicationRunner#fromConfig(Config)} static constructor.
 */
@InterfaceStability.Unstable
public interface ApplicationRunner {

  String RUNNER_CONFIG = "app.runner.class";
  String DEFAULT_RUNNER_CLASS = "org.apache.samza.runtime.RemoteApplicationRunner";

  /**
   * Static method to create the local {@link ApplicationRunner}.
   *
   * @param config  configuration passed in to initialize the Samza local process
   * @return  the local {@link ApplicationRunner} to run the user-defined stream applications
   */
  static ApplicationRunner getLocalRunner(Config config) {
    return null;
  }

  /**
   * Static method to load the {@link ApplicationRunner}
   *
   * Requires the implementation class to define a constructor with a single {@link Config} as the argument.
   *
   * @param config  configuration passed in to initialize the Samza processes
   * @return  the configure-driven {@link ApplicationRunner} to run the user-defined stream applications
   */
  static ApplicationRunner fromConfig(Config config) {
    try {
      Class<?> runnerClass = Class.forName(config.get(RUNNER_CONFIG, DEFAULT_RUNNER_CLASS));
      if (ApplicationRunner.class.isAssignableFrom(runnerClass)) {
        Constructor<?> constructor = runnerClass.getConstructor(Config.class); // *sigh*
        return (ApplicationRunner) constructor.newInstance(config);
      }
    } catch (Exception e) {
      throw new ConfigException(String.format("Problem in loading ApplicationRunner class %s", config.get(
          RUNNER_CONFIG)), e);
    }
    throw new ConfigException(String.format(
        "Class %s does not implement interface ApplicationRunner properly",
        config.get(RUNNER_CONFIG)));
  }

  /**
   * Method to be invoked to deploy and run the actual Samza jobs to execute {@link org.apache.samza.operators.StreamGraph}
   *
   * @param graphBuilder  the user-defined {@link StreamGraphBuilder} object
   */
  void run(StreamGraphBuilder graphBuilder);

  /**
   * Constructs a {@link StreamSpec} from the configuration for the specified streamId.
   *
   * The stream configurations are read from the following properties in the config:
   * {@code streams.{$streamId}.*}
   * <br>
   * All properties matching this pattern are assumed to be system-specific with two exceptions. The following two
   * properties are Samza properties which are used to bind the stream to a system and a physical resource on that system.
   *
   * <ul>
   *   <li>samza.system -         The name of the System on which this stream will be used. If this property isn't defined
   *                              the stream will be associated with the System defined in {@code job.default.system}</li>
   *   <li>samza.physical.name -  The system-specific name for this stream. It could be a file URN, topic name, or other identifer.
   *                              If this property isn't defined the physical.name will be set to the streamId</li>
   * </ul>
   *
   * @param streamId  The logical identifier for the stream in Samza.
   * @return          The {@link StreamSpec} instance.
   */
  StreamSpec streamFromConfig(String streamId);
}