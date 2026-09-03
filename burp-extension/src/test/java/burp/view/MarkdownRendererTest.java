/*
 * Copyright Praetorian Security Inc
 *
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

package burp.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownRendererTest {
    @Test
    void rendersGuardMarkdownAndEscapesEmbeddedHtml() {
        String html = new MarkdownRenderer().render(
                "## Finding\n\n- first\n- second\n\n`code`\n\n<script>alert(1)</script>",
                CourierTheme.surface());

        assertTrue(html.contains("<h2>Finding</h2>"));
        assertTrue(html.contains("<li>first</li>"));
        assertTrue(html.contains("<code>code</code>"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void rendersGithubMarkdownTablesForSwingHtml() {
        String html = new MarkdownRenderer().render(
                "| Header | Value |\n| --- | --- |\n| Referer | example.test |",
                CourierTheme.surface());

        assertTrue(html.contains("<table border='1'"));
        assertTrue(html.contains("<th align='left'"));
        assertTrue(html.contains("<td align='left'>Referer</td>"));
        assertTrue(html.contains("<td align='left'>example.test</td>"));
    }
}
