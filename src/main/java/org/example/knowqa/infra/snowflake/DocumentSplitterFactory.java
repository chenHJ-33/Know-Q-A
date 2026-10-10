package org.example.knowqa.infra.snowflake;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.document.splitter.DocumentByWordSplitter;
import org.example.knowqa.document.constant.SplitType;
import org.example.knowqa.document.entity.DocumentSplitParam;


public class DocumentSplitterFactory {

    public static DocumentSplitter getInstance(DocumentSplitParam documentSplitParam) {
        if (SplitType.LENGTH.name().equals(documentSplitParam.getSplitType())) {
            return new DocumentByWordSplitter(documentSplitParam.getChunkSize(), documentSplitParam.getOverlap());
        }

        if (SplitType.TITLE.name().equals(documentSplitParam.getSplitType())) {
            return new MarkdownHeaderParentTextSplitter(documentSplitParam.getTitleLevel(), false, false, documentSplitParam.getChunkSize(), documentSplitParam.getOverlap());
        }

        if (SplitType.REGEX.name().equals(documentSplitParam.getSplitType())) {
            return new DocumentByRegexSplitter(documentSplitParam.getRegex(), "\\n\\n", documentSplitParam.getChunkSize(), documentSplitParam.getOverlap());
        }

        if (SplitType.SMART.name().equals(documentSplitParam.getSplitType())) {
            return new MarkdownHeaderParentTextSplitter(documentSplitParam.getChunkSize(), (int) (documentSplitParam.getChunkSize() * 0.1));
        }

        if (SplitType.SEPARATOR.name().equals(documentSplitParam.getSplitType())) {
            return new DocumentByRegexSplitter(documentSplitParam.getSeparator(), "\\n\\n", documentSplitParam.getChunkSize(), documentSplitParam.getOverlap());
        }
        return null;
    }

}
