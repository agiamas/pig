/*
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
package org.apache.pig.test;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import org.apache.pig.PigServer;
import org.apache.pig.builtin.PigStorage;
import org.apache.pig.data.*;
import static org.apache.pig.PigServer.ExecType.MAPREDUCE;

import junit.framework.TestCase;

public class TestStreaming extends TestCase {

    MiniCluster cluster = MiniCluster.buildCluster();

	private static final String simpleEchoStreamingCommand = 
		"perl -ne 'chomp $_; print \"$_\n\"'";

	private Tuple[] setupExpectedResults(String[] firstField, int[] secondField) {
		Assert.assertEquals(firstField.length, secondField.length);
		
		Tuple[] expectedResults = new Tuple[firstField.length];
		for (int i=0; i < expectedResults.length; ++i) {
			expectedResults[i] = new Tuple(2);
			expectedResults[i].setField(0, firstField[i]);
			expectedResults[i].setField(1, secondField[i]);
		}
		
		return expectedResults;
	}
	
	@Test
	public void testSimpleMapSideStreaming() throws Exception {
		PigServer pigServer = new PigServer(MAPREDUCE);

		File input = Util.createInputFile("tmp", "", 
				                          new String[] {"A,1", "B,2", "C,3", "D,2",
				                                        "A,5", "B,5", "C,8", "A,8",
				                                        "D,8", "A,9"});

		// Expected results
		String[] expectedFirstFields = new String[] {"A", "B", "C", "A", "D", "A"};
		int[] expectedSecondFields = new int[] {5, 5, 8, 8, 8, 9};
		Tuple[] expectedResults = 
			setupExpectedResults(expectedFirstFields, expectedSecondFields);

		// Pig query to run
		pigServer.registerQuery("INPUT = load 'file:" + input + "' using " + 
				                PigStorage.class.getName() + "(',');");
		pigServer.registerQuery("FILTERED_DATA = filter INPUT by $1 > '3';");
		pigServer.registerQuery("OUTPUT = stream FILTERED_DATA through `" +
				                simpleEchoStreamingCommand + "`;");
		
		// Run the query and check the results
		Util.checkQueryOutputs(pigServer.openIterator("OUTPUT"), expectedResults);
	}

	@Test
	public void testSimpleMapSideStreamingWithOutputSchema() throws Exception {
		PigServer pigServer = new PigServer(MAPREDUCE);

		File input = Util.createInputFile("tmp", "", 
				                          new String[] {"A,1", "B,2", "C,3", "D,2",
				                                        "A,5", "B,5", "C,8", "A,8",
				                                        "D,8", "A,9"});

		// Expected results
		String[] expectedFirstFields = new String[] {"C", "A", "D", "A"};
		int[] expectedSecondFields = new int[] {8, 8, 8, 9};
		Tuple[] expectedResults = 
			setupExpectedResults(expectedFirstFields, expectedSecondFields);

		// Pig query to run
		pigServer.registerQuery("INPUT = load 'file:" + input + "' using " + 
				                PigStorage.class.getName() + "(',');");
		pigServer.registerQuery("FILTERED_DATA = filter INPUT by $1 > '3';");
		pigServer.registerQuery("STREAMED_DATA = stream FILTERED_DATA through `" +
				                simpleEchoStreamingCommand + "` as (f0, f1);");
		pigServer.registerQuery("OUTPUT = filter STREAMED_DATA by f1 > '6';");
		
		// Run the query and check the results
		Util.checkQueryOutputs(pigServer.openIterator("OUTPUT"), expectedResults);
	}

	@Test
	public void testSimpleReduceSideStreamingAfterFlatten() throws Exception {
		PigServer pigServer = new PigServer(MAPREDUCE);

		File input = Util.createInputFile("tmp", "", 
				                          new String[] {"A,1", "B,2", "C,3", "D,2",
				                                        "A,5", "B,5", "C,8", "A,8",
				                                        "D,8", "A,9"});

		// Expected results
		String[] expectedFirstFields = new String[] {"A", "A", "A", "B", "C", "D"};
		int[] expectedSecondFields = new int[] {5, 8, 9, 5, 8, 8};
		Tuple[] expectedResults = 
			setupExpectedResults(expectedFirstFields, expectedSecondFields);

		// Pig query to run
		pigServer.registerQuery("INPUT = load 'file:" + input + "' using " + 
				                PigStorage.class.getName() + "(',');");
		pigServer.registerQuery("FILTERED_DATA = filter INPUT by $1 > '3';");
		pigServer.registerQuery("GROUPED_DATA = group FILTERED_DATA by $0;");
		pigServer.registerQuery("FLATTENED_GROUPED_DATA = foreach GROUPED_DATA " +
				                "generate flatten($1);");
		pigServer.registerQuery("OUTPUT = stream FLATTENED_GROUPED_DATA through `" +
				                simpleEchoStreamingCommand + "`;");
		
		// Run the query and check the results
		Util.checkQueryOutputs(pigServer.openIterator("OUTPUT"), expectedResults);
	}

	@Test
	public void testSimpleOrderedReduceSideStreamingAfterFlatten() throws Exception {
		PigServer pigServer = new PigServer(MAPREDUCE);

		File input = Util.createInputFile("tmp", "", 
				                          new String[] {"A,1,2,3", "B,2,4,5",
				                                        "C,3,1,2", "D,2,5,2",
				                                        "A,5,5,1", "B,5,7,4",
				                                        "C,8,9,2", "A,8,4,5",
				                                        "D,8,8,3", "A,9,2,5"}
		                                 );

		// Expected results
		String[] expectedFirstFields = 
			new String[] {"A", "A", "A", "A", "B", "B", "C", "C", "D", "D"};
		int[] expectedSecondFields = new int[] {1, 9, 8, 5, 2, 5, 3, 8, 2, 8};
		int[] expectedThirdFields = new int[] {2, 2, 4, 5, 4, 7, 1, 9, 5, 8};
		int[] expectedFourthFields = new int[] {3, 5, 5, 1, 5, 4, 2, 2, 2, 3};
		Tuple[] expectedResults = new Tuple[10];
		for (int i = 0; i < expectedResults.length; ++i) {
			expectedResults[i] = new Tuple(4);
			expectedResults[i].setField(0, expectedFirstFields[i]);
			expectedResults[i].setField(1, expectedSecondFields[i]);
			expectedResults[i].setField(2, expectedThirdFields[i]);
			expectedResults[i].setField(3, expectedFourthFields[i]);
		}
			setupExpectedResults(expectedFirstFields, expectedSecondFields);

		// Pig query to run
		pigServer.registerQuery("INPUT = load 'file:" + input + "' using " + 
				                PigStorage.class.getName() + "(',');");
		pigServer.registerQuery("FILTERED_DATA = filter INPUT by $1 > '3';");
		pigServer.registerQuery("GROUPED_DATA = group INPUT by $0;");
		pigServer.registerQuery("ORDERED_DATA = foreach GROUPED_DATA { " +
				                "  D = order INPUT BY $2, $3;" +
                                "  generate flatten(D);" +
                                "};");
		pigServer.registerQuery("OUTPUT = stream ORDERED_DATA through `" +
				                simpleEchoStreamingCommand + "`;");
		
		// Run the query and check the results
		Util.checkQueryOutputs(pigServer.openIterator("OUTPUT"), expectedResults);
	}
	
}