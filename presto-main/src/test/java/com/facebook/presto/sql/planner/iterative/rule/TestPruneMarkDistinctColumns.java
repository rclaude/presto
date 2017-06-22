/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.sql.planner.iterative.rule;

import com.facebook.presto.sql.planner.Symbol;
import com.facebook.presto.sql.planner.iterative.rule.test.BaseRuleTest;
import com.facebook.presto.sql.planner.plan.Assignments;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import static com.facebook.presto.spi.type.BigintType.BIGINT;
import static com.facebook.presto.sql.planner.assertions.PlanMatchPattern.expression;
import static com.facebook.presto.sql.planner.assertions.PlanMatchPattern.markDistinct;
import static com.facebook.presto.sql.planner.assertions.PlanMatchPattern.strictProject;
import static com.facebook.presto.sql.planner.assertions.PlanMatchPattern.values;

public class TestPruneMarkDistinctColumns
        extends BaseRuleTest
{
    @Test
    public void testMarkerSymbolNotReferenced()
            throws Exception
    {
        tester().assertThat(new PruneMarkDistinctColumns())
                .on(p ->
                {
                    Symbol key = p.symbol("key", BIGINT);
                    Symbol key2 = p.symbol("key2", BIGINT);
                    Symbol mark = p.symbol("mark", BIGINT);
                    Symbol unused = p.symbol("unused", BIGINT);
                    return p.project(
                            Assignments.of(key2, key.toSymbolReference()),
                            p.markDistinct(mark, ImmutableList.of(key), p.values(key, unused)));
                })
                .matches(
                        strictProject(
                                ImmutableMap.of("key2", expression("key")),
                                values(ImmutableList.of("key", "unused"))));
    }

    @Test
    public void testSourceSymbolNotReferenced()
            throws Exception
    {
        tester().assertThat(new PruneMarkDistinctColumns())
                .on(p ->
                {
                    Symbol key = p.symbol("key", BIGINT);
                    Symbol mark = p.symbol("mark", BIGINT);
                    Symbol hash = p.symbol("hash", BIGINT);
                    Symbol unused = p.symbol("unused", BIGINT);
                    return p.project(
                            Assignments.identity(mark),
                            p.markDistinct(
                                    mark,
                                    ImmutableList.of(key),
                                    hash,
                                    p.values(key, hash, unused)));
                })
                .matches(
                        strictProject(
                                ImmutableMap.of("mark", expression("mark")),
                                markDistinct("mark", ImmutableList.of("key"), "hash",
                                        strictProject(
                                                ImmutableMap.of(
                                                        "key", expression("key"),
                                                        "hash", expression("hash")),
                                                values(ImmutableList.of("key", "hash", "unused"))))));
    }

    @Test
    public void testKeySymbolNotReferenced()
            throws Exception
    {
        tester().assertThat(new PruneMarkDistinctColumns())
                .on(p ->
                {
                    Symbol key = p.symbol("key", BIGINT);
                    Symbol mark = p.symbol("mark", BIGINT);
                    return p.project(
                            Assignments.identity(mark),
                            p.markDistinct(mark, ImmutableList.of(key), p.values(key)));
                })
                .doesNotFire();
    }

    @Test
    public void testAllOutputsReferenced()
            throws Exception
    {
        tester().assertThat(new PruneMarkDistinctColumns())
                .on(p ->
                {
                    Symbol key = p.symbol("key", BIGINT);
                    Symbol mark = p.symbol("mark", BIGINT);
                    return p.project(
                            Assignments.identity(key, mark),
                            p.markDistinct(mark, ImmutableList.of(key), p.values(key)));
                })
                .doesNotFire();
    }
}