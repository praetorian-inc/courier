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

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

final class MarkdownRenderer {
    private static final java.util.List<Extension> EXTENSIONS =
            java.util.List.of(TablesExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build();

    String render(String markdown, java.awt.Color background) {
        String source = breakLongTokens(markdown == null ? "" : markdown);
        Node document = PARSER.parse(source);
        String body = styleTables(RENDERER.render(document));
        return "<html><body style='font-family:sans-serif;font-size:11px;margin:0;padding:0;color:"
                + color(CourierTheme.text()) + ";background-color:"
                + color(background) + "'>" + body + "</body></html>";
    }

    private static String styleTables(String html) {
        String headerColor = color(CourierTheme.elevatedSurface());
        return html.replace("<table>",
                        "<table border='1' cellspacing='0' cellpadding='5' width='100%'>")
                .replace("<th>", "<th align='left' bgcolor='" + headerColor + "'>")
                .replace("<td>", "<td align='left'>");
    }

    private static String breakLongTokens(String source) {
        StringBuilder wrapped = new StringBuilder(source.length() + source.length() / 48);
        int runLength = 0;
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (Character.isWhitespace(character)) {
                runLength = 0;
            } else if (++runLength > 48) {
                wrapped.append('\u200B');
                runLength = 1;
            }
            wrapped.append(character);
        }
        return wrapped.toString();
    }

    private static String color(java.awt.Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
