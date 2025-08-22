package com.aref.cloud_assistant_mcp.views.utils;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.List;

public class MarkdownUtil {

    private static final MutableDataSet OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, List.of(TablesExtension.create()));

    private static final Parser PARSER = Parser.builder(OPTIONS).build();

    private static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

    public static String toHtml(String markdown) {
        Node document = PARSER.parse(markdown);
        return RENDERER.render(document);
    }
}