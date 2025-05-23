//  Copyright 2025 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.pure.grammar.test.to;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestDomainGrammarTo extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testFunctionWithEnumValueGrammarTo()
    {
        testTo("simpleFunctionWithEnumValue.json",
                "function showcase::testEnum(): meta::pure::metamodel::type::Any[*]\n" +
                            "{\n" +
                            "  |showcase::DataDefect.all()->filter(x|$x.status == showcase::Status.'Closed by DGO')\n" +
                            "}\n");
    }
}
