/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.ql.plan;

import junit.framework.Assert;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.exec.Task;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class TestConditionalResolverCommonJoin {

  @Test
  public void testResolvingDriverAlias() throws Exception {
    ConditionalResolverCommonJoin resolver = new ConditionalResolverCommonJoin();

    HashMap<String, ArrayList<String>> pathToAliases = new HashMap<String, ArrayList<String>>();
    pathToAliases.put("path1", new ArrayList<String>(Arrays.asList("alias1", "alias2")));
    pathToAliases.put("path2", new ArrayList<String>(Arrays.asList("alias3")));

    HashMap<String, Long> aliasToKnownSize = new HashMap<String, Long>();
    aliasToKnownSize.put("alias1", 1024l);
    aliasToKnownSize.put("alias2", 2048l);
    aliasToKnownSize.put("alias3", 4096l);

    // joins alias1, alias2, alias3 (alias1 was not eligible for big pos)
    HashMap<String, Task<? extends Serializable>> aliasToTask =
        new HashMap<String, Task<? extends Serializable>>();
    aliasToTask.put("alias2", null);
    aliasToTask.put("alias3", null);

    ConditionalResolverCommonJoin.ConditionalResolverCommonJoinCtx ctx =
        new ConditionalResolverCommonJoin.ConditionalResolverCommonJoinCtx();
    ctx.setPathToAliases(pathToAliases);
    ctx.setAliasToTask(aliasToTask);
    ctx.setAliasToKnownSize(aliasToKnownSize);

    HiveConf conf = new HiveConf();
    conf.setLongVar(HiveConf.ConfVars.HIVESMALLTABLESFILESIZE, 4096);

    // alias3 only can be selected
    String resolved = resolver.resolveMapJoinTask(ctx, conf);
    Assert.assertEquals("alias3", resolved);

    conf.setLongVar(HiveConf.ConfVars.HIVESMALLTABLESFILESIZE, 65536);

    // alias1, alias2, alias3 all can be selected but overriden by biggest one (alias3)
    resolved = resolver.resolveMapJoinTask(ctx, conf);
    Assert.assertEquals("alias3", resolved);

    conf.setLongVar(HiveConf.ConfVars.HIVESMALLTABLESFILESIZE, 2048);

    // not selected
    resolved = resolver.resolveMapJoinTask(ctx, conf);
    Assert.assertNull(resolved);
  }
}