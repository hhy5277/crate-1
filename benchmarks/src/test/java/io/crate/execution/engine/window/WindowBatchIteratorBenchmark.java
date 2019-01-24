/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.execution.engine.window;

import io.crate.analyze.WindowDefinition;
import io.crate.breaker.RamAccountingContext;
import io.crate.breaker.RowAccounting;
import io.crate.breaker.RowAccountingWithEstimators;
import io.crate.data.BatchIterator;
import io.crate.data.InMemoryBatchIterator;
import io.crate.data.Input;
import io.crate.data.Row;
import io.crate.data.Row1;
import io.crate.data.RowN;
import io.crate.execution.engine.collect.InputCollectExpression;
import io.crate.metadata.FunctionIdent;
import io.crate.metadata.Functions;
import io.crate.module.EnterpriseFunctionsModule;
import io.crate.testing.RowGenerator;
import io.crate.types.DataTypes;
import io.crate.window.NthValueFunctions;
import org.elasticsearch.common.breaker.NoopCircuitBreaker;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static io.crate.data.SentinelRow.SENTINEL;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class WindowBatchIteratorBenchmark {

    private static NoopCircuitBreaker NOOP_CIRCUIT_BREAKER = new NoopCircuitBreaker("dummy");

    private final RamAccountingContext RAM_ACCOUNTING_CONTEXT = new RamAccountingContext("test", NOOP_CIRCUIT_BREAKER);
    private final RowAccounting rowAccounting = new RowAccountingWithEstimators(Collections.singleton(DataTypes.INTEGER), RAM_ACCOUNTING_CONTEXT);

    // use materialize to not have shared row instances
    // this is done in the startup, otherwise the allocation costs will make up the majority of the benchmark.
    private List<Row> rows = StreamSupport.stream(RowGenerator.range(0, 10_000_000).spliterator(), false)
        .map(Row::materialize)
        .map(RowN::new)
        .collect(Collectors.toList());

    // used with  RowsBatchIterator without any state/error handling to establish a performance baseline.
    private final List<Row1> oneThousandRows = IntStream.range(0, 1000).mapToObj(Row1::new).collect(Collectors.toList());
    private final List<Row1> tenThousandRows = IntStream.range(0, 10000).mapToObj(Row1::new).collect(Collectors.toList());


    private WindowBatchIterator iterator;

    @Setup
    public void setup() {
        Functions functions = new ModulesBuilder().add(new EnterpriseFunctionsModule())
            .createInjector().getInstance(Functions.class);
        WindowFunction lastValueIntFunction = (WindowFunction) functions.getQualified(
            new FunctionIdent(NthValueFunctions.LAST_VALUE_NAME, Collections.singletonList(DataTypes.INTEGER)));

        BatchIterator<Row> sourceIterator = new InMemoryBatchIterator<>(rows, SENTINEL);
        InputCollectExpression input = new InputCollectExpression(0);
        this.iterator = new WindowBatchIterator(
            emptyWindow(),
            Collections.emptyList(),
            Collections.emptyList(),
            sourceIterator,
            Collections.singletonList(lastValueIntFunction),
            Collections.singletonList(input),
            Collections.singletonList(DataTypes.INTEGER),
            RAM_ACCOUNTING_CONTEXT,
            null,
            new Input[] { input }
        );

    }


    @Benchmark
    public void measureConsumeWindowBatchIterator(Blackhole blackhole) {
        while (iterator.moveNext()) {
            blackhole.consume(iterator.currentElement().get(0));
        }
    }


    private static WindowDefinition emptyWindow() {
        return new WindowDefinition(Collections.emptyList(), null, null);
    }
}
